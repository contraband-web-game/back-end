package com.game.contraband.domain.game.round;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.settle.RoundSettlement;
import com.game.contraband.domain.game.round.settle.RoundSettlementRule;
import com.game.contraband.domain.game.round.settle.RoundSettlementRuleSelector;
import com.game.contraband.domain.game.round.vo.InspectionState;
import com.game.contraband.domain.game.round.vo.SmuggleState;
import com.game.contraband.domain.game.vo.Money;
import lombok.Getter;

@Getter
public class Round {

    private static final Money MAX_INSPECTION_THRESHOLD = Money.from(1_000);
    private static final Money MAX_SMUGGLE_AMOUNT = Money.from(1_000);

    public static Round newRound(int roundNumber, Long smugglerId, Long inspectorId) {
        return new Round(
                roundNumber,
                RoundStatus.NEW,
                SmuggleState.initial(smugglerId),
                InspectionState.initial(inspectorId)
        );
    }

    private Round(
            int roundNumber,
            RoundStatus status,
            SmuggleState smuggleState,
            InspectionState inspectionState
    ) {
        this.roundNumber = roundNumber;
        this.status = status;
        this.smuggleState = smuggleState;
        this.inspectionState = inspectionState;
    }

    private final int roundNumber;
    private final RoundStatus status;
    private final SmuggleState smuggleState;
    private final InspectionState inspectionState;

    public Round declareSmuggleAmount(Money amount, Money smugglerBalance) {
        return declareSmuggleAmount(smuggleState.getSmugglerId(), amount, smugglerBalance);
    }

    public Round declareSmuggleAmount(long requesterId, Money amount, Money smugglerBalance) {
        validateSmuggler(requesterId, smuggleState.getSmugglerId());
        validateSmuggleDeclaration(amount, smugglerBalance);

        return new Round(
                roundNumber,
                determineStatus(true, inspectionState.isProvided()),
                smuggleState.declare(amount),
                inspectionState
        );
    }

    public Round decidePass() {
        return decidePass(inspectionState.getInspectorId());
    }

    public Round decidePass(long requesterId) {
        validateInspector(requesterId, inspectionState.getInspectorId());

        if (inspectionState.isProvided()) {
            throw new IllegalStateException("검사관의 선택은 한 번만 할 수 있습니다.");
        }

        InspectionState decided = inspectionState.decidePass(requesterId);

        return new Round(
                roundNumber,
                determineStatus(smuggleState.isDeclared(), true),
                smuggleState,
                decided
        );
    }

    public Round decideInspection(Money inspectionThreshold) {
        return decideInspection(inspectionState.getInspectorId(), inspectionThreshold);
    }

    public Round decideInspection(long requesterId, Money inspectionThreshold) {
        validateInspector(requesterId, inspectionState.getInspectorId());
        validateInspectionThreshold(inspectionThreshold);

        InspectionState decided = inspectionState.decideInspection(requesterId, inspectionThreshold);

        return new Round(
                roundNumber,
                determineStatus(smuggleState.isDeclared(), true),
                smuggleState,
                decided
        );
    }

    private RoundStatus determineStatus(boolean smuggleDeclared, boolean inspectionProvided) {
        if (smuggleDeclared && inspectionProvided) {
            return RoundStatus.INSPECTION_DECIDED;
        }
        if (smuggleDeclared) {
            return RoundStatus.SMUGGLE_DECLARED;
        }
        if (inspectionProvided) {
            return RoundStatus.INSPECTION_DECISION_DECLARED;
        }
        return RoundStatus.NEW;
    }

    private void validateSmuggler(long requesterId, Long smugglerId) {
        if (!smugglerId.equals(requesterId)) {
            throw new IllegalArgumentException("라운드에 지정된 밀수꾼만 밀수 금액을 선언할 수 있습니다.");
        }
    }

    private void validateInspector(long requesterId, Long inspectorId) {
        if (!inspectorId.equals(requesterId)) {
            throw new IllegalArgumentException("라운드에 지정된 검사관만 검문 결정을 내릴 수 있습니다.");
        }
    }

    private void validateSmuggleDeclaration(Money amount, Money smugglerBalance) {
        if (smuggleState.isDeclared()) {
            throw new IllegalStateException("이미 밀수 금액을 선언했습니다.");
        }
        if (amount.isLessThan(Money.ZERO)) {
            throw new IllegalArgumentException("밀수 금액은 0 이상이어야 합니다.");
        }
        if (amount.isGreaterThan(MAX_SMUGGLE_AMOUNT)) {
            throw new IllegalArgumentException("허용된 최대 밀수 금액을 초과할 수 없습니다.");
        }
        if (amount.isGreaterThan(smugglerBalance)) {
            throw new IllegalArgumentException("보유 금액보다 많이 밀수할 수 없습니다.");
        }
        if (amount.isNotHundredsUnit()) {
            throw new IllegalArgumentException("밀수 금액은 100원 단위여야 합니다.");
        }
    }

    private void validateInspectionThreshold(Money inspectionThreshold) {
        if (inspectionState.isProvided()) {
            throw new IllegalStateException("검사관의 선택은 한 번만 할 수 있습니다.");
        }
        if (!inspectionThreshold.isGreaterThan(Money.ZERO)) {
            throw new IllegalArgumentException("검문 기준 금액은 0보다 커야 합니다.");
        }
        if (inspectionThreshold.isNotHundredsUnit()) {
            throw new IllegalArgumentException("검문 기준 금액은 100원 단위여야 합니다.");
        }
        if (inspectionThreshold.isGreaterThan(MAX_INSPECTION_THRESHOLD)) {
            throw new IllegalArgumentException("검문 기준 금액은 최대 1000원을 초과할 수 없습니다.");
        }
    }

    public void validateReadyToSettle() {
        if (smuggleState.isNotDeclared()) {
            throw new IllegalStateException("밀수 금액이 선언되어야 정산할 수 있습니다.");
        }
        if (inspectionState.isNotProvided()) {
            throw new IllegalStateException("정산을 위해서는 검사관 선택이 완료된 상태여야 합니다.");
        }
        if (inspectionState.isDecisionNone()) {
            throw new IllegalStateException("검사관의 선택이 설정되지 않았습니다.");
        }
        if (status.isNotInspectionDecided()) {
            throw new IllegalStateException("정산은 검사관의 선택이 끝난 후에만 수행할 수 있습니다.");
        }
    }

    public RoundSettlement settle(
            Player smuggler,
            Player inspector,
            RoundSettlementRuleSelector settlementRuleSelector
    ) {
        validateReadyToSettle();
        validateRoundParticipantId(smuggler, inspector);

        RoundSettlementRule rule = settlementRuleSelector.selectRule(
                inspectionState.getDecision(),
                smuggleState.getAmount(),
                inspectionState.getThreshold()
        );

        return rule.settle(smuggler, inspector, smuggleState.getAmount(), inspectionState.getThreshold());
    }

    private void validateRoundParticipantId(Player smuggler, Player inspector) {
        if (smuggler.isNotEqualId(smuggleState.getSmugglerId())
                || inspector.isNotEqualId(inspectionState.getInspectorId())
        ) {
            throw new IllegalArgumentException("라운드에 지정된 플레이어와 정산 대상 플레이어가 일치하지 않습니다.");
        }
    }

    public boolean isNotNewStatus() {
        return this.status.isNotNew();
    }

    public boolean isNotInspectionDecided() {
        return this.status.isNotInspectionDecided();
    }

    public Money getSmuggleAmount() {
        return smuggleState.getAmount();
    }

    public boolean isSmuggleAmountDeclared() {
        return smuggleState.isDeclared();
    }

    public boolean isInspectionDecisionCompleted() {
        return status.isInspectionDecided();
    }

    public boolean isSmuggleDeclaredOnly() {
        return status.isSmuggleDeclared() && status.isNotInspectionDecided();
    }

    public boolean isNotSmuggleAmountDeclared() {
        return !this.smuggleState.isDeclared();
    }

    public boolean isInspectionDecisionProvided() {
        return inspectionState.isProvided();
    }

    public boolean canDeclareSmuggleAmount() {
        return status.isNew() || status.isInspectionDecisionDeclared();
    }

    public InspectionDecision getInspectionDecision() {
        return inspectionState.getDecision();
    }

    public Money getInspectionThreshold() {
        return inspectionState.getThreshold();
    }

    public Long getSmugglerId() {
        return smuggleState.getSmugglerId();
    }

    public Long getInspectorId() {
        return inspectionState.getInspectorId();
    }
}

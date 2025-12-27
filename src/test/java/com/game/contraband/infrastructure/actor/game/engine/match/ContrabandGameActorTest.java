package com.game.contraband.infrastructure.actor.game.engine.match;

import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateContrabandGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedPass;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedSmuggleAmount;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorIdForSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerIdForInspector;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateInspectorApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSelectionTimer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSmugglerApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartNewRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransfer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransferFailed;
import com.game.contraband.infrastructure.actor.dummy.DummyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.dummy.DummyGameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecidePass;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FinishCurrentRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FinishedGame;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundSelectionTimeout;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundTimeout;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartNewRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.TransferAmount;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.ManualTime;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandGameActorTest {

    private ActorTestKit actorTestKit;

    @BeforeEach
    void setUp() {
        actorTestKit = ActorTestKit.create(ManualTime.config());
    }

    @AfterEach
    void tearDown() {
        actorTestKit.shutdownTestKit();
    }

    @Test
    void 참여_인원_수가_2명인_ContrabandGameActor를_spawn하면_자동으로_밀수꾼_및_검사관을_선출하고_새로운_라운드를_시작한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;

        // when
        ContrabandGameTestContext context = createTestContext(List.of(smugglerId), List.of(inspectorId));
        TestProbe<ClientSessionCommand> smugglerSession = context.session(smugglerId);
        TestProbe<ClientSessionCommand> inspectorSession = context.session(inspectorId);
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        StartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        smugglerSession,
                        PropagateStartGame.class,
                        UpdateContrabandGame.class,
                        SyncContrabandGameChat.class,
                        PropagateRegisterSmugglerId.class,
                        PropagateFixedSmugglerId.class,
                        PropagateStartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        inspectorSession,
                        PropagateStartGame.class,
                        UpdateContrabandGame.class,
                        SyncContrabandGameChat.class,
                        PropagateRegisterInspectorId.class,
                        PropagateFixedInspectorId.class,
                        PropagateStartNewRound.class
                )
        );
    }

    @Test
    void 참여_인원_수가_2명인_ContrabandGameActor를_spawn하면_게임_전용_채팅방_Actor를_spawn한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;

        // when
        ContrabandGameBehaviorTestContext context = createBehaviorTestContext(
                List.of(smugglerId),
                List.of(inspectorId)
        );
        context.harness().kit().runOne();

        // then
        BehaviorTestUtils.expectSpawnedPrefix(context.harness(), "game-chat-");
    }

    @Test
    void 참여_인원_수가_4명_이상인_ContrabandGameActor를_spawn하면_밀수꾼_및_검사관_선출_단계를_진행한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;

        // when
        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2),
                List.of(inspectorId1, inspectorId2)
        );

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateStartGame.class,
                        UpdateContrabandGame.class,
                        SyncContrabandGameChat.class,
                        PropagateSelectionTimer.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateStartGame.class,
                        UpdateContrabandGame.class,
                        SyncContrabandGameChat.class,
                        PropagateSelectionTimer.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateStartGame.class,
                        UpdateContrabandGame.class,
                        SyncContrabandGameChat.class,
                        PropagateSelectionTimer.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateStartGame.class,
                        UpdateContrabandGame.class,
                        SyncContrabandGameChat.class,
                        PropagateSelectionTimer.class
                ),
                () -> ActorTestUtils.expectNoMessages(context.monitored().monitor())
        );
    }

    @Test
    void 참여_인원_수가_4명인_ContrabandGameActor에서_라운드에_참여할_밀수꾼과_검사관_후보를_등록한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2),
                List.of(inspectorId1, inspectorId2)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class
                )
        );
    }

    @Test
    void 참여_인원_수가_4명인_ContrabandGameActor에서_라운드에_참여할_검사관_후보를_확정하면_밀수꾼_팀에_확정_사실을_알린다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2),
                List.of(inspectorId1, inspectorId2)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateFixedInspectorId.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateFixedInspectorId.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateFixedInspectorIdForSmuggler.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateFixedInspectorIdForSmuggler.class
                )
        );
    }

    @Test
    void 참여_인원_수가_4명인_ContrabandGameActor에서_라운드에_참여할_밀수꾼_후보를_확정하면_검사관_팀에_확정_사실을_알린다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2),
                List.of(inspectorId1, inspectorId2)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateFixedSmugglerIdForInspector.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateFixedSmugglerIdForInspector.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateFixedSmugglerId.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateFixedSmugglerId.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드에_참여할_밀수꾼과_검사관_후보를_등록한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId3),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드에_참여할_밀수꾼_후보를_등록만_한_상태라면_후보를_자유롭게_변경할_수_있다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId2, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class,
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class,
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class,
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드에_참여할_밀수꾼_후보를_등록한_뒤_한_명이라도_해당_후보의_참여를_찬성한_경우_후보를_변경할_수_없다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId3, 1));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId2, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class,
                        PropagateSmugglerApprovalState.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드에_참여할_검사관_후보를_등록한_뒤_한_명이라도_해당_후보의_참여를_찬성한_경우_후보를_변경할_수_없다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId3, 1));
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId2, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class,
                        PropagateInspectorApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class,
                        PropagateInspectorApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId3),
                        PropagateRegisterInspectorId.class,
                        PropagateInspectorApprovalState.class,
                        PropagateInspectorApprovalState.class
                )
        );
    }

    @Test
    void 참여_인원_수가_4명인_ContrabandGameActor에서_라운드에_참여할_밀수꾼과_검사관을_확정하면_라운드를_진행한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2),
                List.of(inspectorId1, inspectorId2)
        );
        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));

        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(contrabandGameMonitor, StartNewRound.class),
                () -> ActorTestUtils.expectMessages(context.session(smugglerId1), PropagateStartNewRound.class),
                () -> ActorTestUtils.expectMessages(context.session(smugglerId2), PropagateStartNewRound.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId1), PropagateStartNewRound.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId2), PropagateStartNewRound.class)
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_후보_선발_단계에서_송금을_진행한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        // when
        contrabandGameCommand.tell(new TransferAmount(smugglerId1, smugglerId2, Money.from(1_000)));

        // then
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        TransferAmount.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateTransfer.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateTransfer.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드_진행_단계에서_송금을_진행한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        // when
        contrabandGameCommand.tell(new TransferAmount(inspectorId1, inspectorId2, Money.from(1_000)));

        // then
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        StartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateTransfer.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateTransfer.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_후보_선발_단계에서_특정_플레이어가_송금했다면_라운드_진행_단계에서_송금을_할_수_없다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        contrabandGameCommand.tell(new TransferAmount(smugglerId1, smugglerId2, Money.from(1_000)));
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        // when
        contrabandGameCommand.tell(new TransferAmount(smugglerId3, smugglerId2, Money.from(1_000)));

        // then
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        StartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateTransferFailed.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_후보_선발_단계에서_아무_후보도_등록하지_않고_제한시간이_다_되면_랜덤으로_후보를_선정한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        // when
        ManualTime manualTime = ManualTime.get(actorTestKit.system());

        manualTime.timePasses(Duration.ofSeconds(33L));

        // then
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        RoundSelectionTimeout.class,
                        StartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateStartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateStartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateStartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateStartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateStartNewRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId3),
                        PropagateStartNewRound.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드_진행_단계에서_아무_동작도_지정하지_않고_제한시간이_다_되면_각_플레이어의_소지_금액과_무관한_기본_동작을_수행하고_라운드를_종료시킨다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        // when
        ManualTime manualTime = ManualTime.get(actorTestKit.system());

        manualTime.timePasses(Duration.ofSeconds(33L));

        // then
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        RoundTimeout.class,
                        DecideSmuggleAmount.class,
                        DecidePass.class,
                        FinishCurrentRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateDecidedPass.class,
                        PropagateDecidedSmuggleAmount.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateDecidedPass.class,
                        PropagateDecidedSmuggleAmount.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateDecidedPass.class,
                        PropagateDecidedSmuggleAmount.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateDecidedPass.class,
                        PropagateDecidedSmuggleAmount.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateDecidedPass.class,
                        PropagateDecidedSmuggleAmount.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId3),
                        PropagateDecidedPass.class,
                        PropagateDecidedSmuggleAmount.class,
                        PropagateFinishedRound.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_라운드를_진행하고_결과를_확인한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        // when
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();
        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, StartNewRound.class);

        contrabandGameCommand.tell(new DecideSmuggleAmount(smugglerId1, 1_000));
        contrabandGameCommand.tell(new DecidePass(inspectorId1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        FinishCurrentRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class,
                        PropagateFinishedRound.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId3),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class,
                        PropagateFinishedRound.class
                )
        );
    }

    @Test
    void 참여_인원_수가_6명인_ContrabandGameActor에서_게임_내_남은_라운드를_모두_끝냈다면_게임을_종료하고_결과를_안내한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long smugglerId3 = 12L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        Long inspectorId3 = 22L;

        ContrabandGameTestContext context = createTestContext(
                List.of(smugglerId1, smugglerId2, smugglerId3),
                List.of(inspectorId1, inspectorId2, inspectorId3)
        );

        ActorRef<ContrabandGameCommand> contrabandGameCommand = context.commandRef();
        TestProbe<ContrabandGameCommand> contrabandGameMonitor = context.monitored().monitor();

        // 첫 번째 라운드부터 다섯 번째 라운드까지 진행
        processRoundUntilFourRound(
                contrabandGameCommand,
                inspectorId1,
                inspectorId2,
                inspectorId3,
                smugglerId1,
                smugglerId2,
                smugglerId3,
                contrabandGameMonitor
        );

        // when
        // 다섯 번째 라운드
        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, FinishCurrentRound.class);

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 5));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 5));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, StartNewRound.class);

        contrabandGameCommand.tell(new DecideSmuggleAmount(smugglerId1, 1_000));
        contrabandGameCommand.tell(new DecidePass(inspectorId1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        contrabandGameMonitor,
                        FinishedGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId1),
                        PropagateFinishedGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId2),
                        PropagateFinishedGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId3),
                        PropagateFinishedGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId1),
                        PropagateFinishedGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId2),
                        PropagateFinishedGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId3),
                        PropagateFinishedGame.class
                )
        );
    }

    private void processRoundUntilFourRound(
            ActorRef<ContrabandGameCommand> contrabandGameCommand,
            Long inspectorId1,
            Long inspectorId2,
            Long inspectorId3,
            Long smugglerId1,
            Long smugglerId2,
            Long smugglerId3,
            TestProbe<ContrabandGameCommand> contrabandGameMonitor
    ) {
        // 첫 번째 라운드
        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 1));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 1));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, StartNewRound.class);

        contrabandGameCommand.tell(new DecideSmuggleAmount(smugglerId1, 1_000));
        contrabandGameCommand.tell(new DecidePass(inspectorId1));

        // 두 번째 라운드
        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, FinishCurrentRound.class);

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, StartNewRound.class);

        contrabandGameCommand.tell(new DecideSmuggleAmount(smugglerId1, 1_000));
        contrabandGameCommand.tell(new DecidePass(inspectorId1));

        // 세 번째 라운드
        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, FinishCurrentRound.class);

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 3));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 3));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, StartNewRound.class);

        contrabandGameCommand.tell(new DecideSmuggleAmount(smugglerId1, 1_000));
        contrabandGameCommand.tell(new DecidePass(inspectorId1));

        // 네 번째 라운드
        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, FinishCurrentRound.class);

        contrabandGameCommand.tell(new RegisterInspectorId(inspectorId1, 4));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId2));
        contrabandGameCommand.tell(new FixInspectorId(inspectorId3));
        contrabandGameCommand.tell(new RegisterSmugglerId(smugglerId1, 4));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId2));
        contrabandGameCommand.tell(new FixSmugglerId(smugglerId3));

        ActorTestUtils.waitUntilMessages(contrabandGameMonitor, StartNewRound.class);

        contrabandGameCommand.tell(new DecideSmuggleAmount(smugglerId1, 1_000));
        contrabandGameCommand.tell(new DecidePass(inspectorId1));
    }

    private ContrabandGameBehaviorTestContext createBehaviorTestContext(
            List<Long> smugglerIds,
            List<Long> inspectorIds
    ) {
        TeamRoster smugglerRoster = createRoster(smugglerIds, TeamRole.SMUGGLER, "밀수꾼 팀");
        TeamRoster inspectorRoster = createRoster(inspectorIds, TeamRole.INSPECTOR, "검사관 팀");

        ContrabandGame contrabandGame = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 5);
        TestInbox<LobbyCommand> parent = TestInbox.create();

        Map<Long, TestInbox<ClientSessionCommand>> sessions = createSessionInboxes(smugglerIds, inspectorIds);
        Map<Long, ActorRef<ClientSessionCommand>> clientSessions = toSessionRefsFromInboxes(sessions);

        Behavior<ContrabandGameCommand> behavior = ContrabandGameActor.create(
                1L,
                "entity-1",
                contrabandGame,
                parent.getRef(),
                clientSessions,
                new DummyChatMessageEventPublisher(),
                new DummyGameLifecycleEventPublisher(),
                new DummyChatBlacklistRepository()
        );

        return new ContrabandGameBehaviorTestContext(
                BehaviorTestUtils.createHarness(behavior),
                sessions,
                parent
        );
    }

    private ContrabandGameTestContext createTestContext(List<Long> smugglerIds, List<Long> inspectorIds) {
        TeamRoster smugglerRoster = createRoster(smugglerIds, TeamRole.SMUGGLER, "밀수꾼 팀");
        TeamRoster inspectorRoster = createRoster(inspectorIds, TeamRole.INSPECTOR, "검사관 팀");

        ContrabandGame contrabandGame = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 5);
        TestProbe<LobbyCommand> parent = actorTestKit.createTestProbe();

        Map<Long, TestProbe<ClientSessionCommand>> sessions = createSessionProbes(smugglerIds, inspectorIds);
        Map<Long, ActorRef<ClientSessionCommand>> clientSessions = toSessionRefs(sessions);

        Behavior<ContrabandGameCommand> behavior = ContrabandGameActor.create(
                1L,
                "entity-1",
                contrabandGame,
                parent.getRef(),
                clientSessions,
                new DummyChatMessageEventPublisher(),
                new DummyGameLifecycleEventPublisher(),
                new DummyChatBlacklistRepository()
        );

        return new ContrabandGameTestContext(
                sessions,
                parent,
                ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameCommand.class, behavior)
        );
    }

    private TeamRoster createRoster(List<Long> playerIds, TeamRole teamRole, String name) {
        List<PlayerProfile> profiles = playerIds.stream()
                                                .map(playerId -> PlayerProfile.create(
                                                        playerId,
                                                        teamRole.isSmuggler() ? "밀수꾼 팀" : "검사관 팀",
                                                        teamRole
                                                ))
                                                .toList();
        return TeamRoster.create(name, teamRole, profiles);
    }

    private Map<Long, TestProbe<ClientSessionCommand>> createSessionProbes(
            List<Long> smugglerIds,
            List<Long> inspectorIds
    ) {
        Map<Long, TestProbe<ClientSessionCommand>> probes = new LinkedHashMap<>();
        Stream.concat(smugglerIds.stream(), inspectorIds.stream())
              .forEach(playerId -> probes.put(playerId, actorTestKit.createTestProbe()));

        return probes;
    }

    private Map<Long, TestInbox<ClientSessionCommand>> createSessionInboxes(
            List<Long> smugglerIds,
            List<Long> inspectorIds
    ) {
        Map<Long, TestInbox<ClientSessionCommand>> inboxes = new LinkedHashMap<>();
        Stream.concat(smugglerIds.stream(), inspectorIds.stream())
              .forEach(playerId -> inboxes.put(playerId, TestInbox.create()));

        return inboxes;
    }

    private Map<Long, ActorRef<ClientSessionCommand>> toSessionRefs(Map<Long, TestProbe<ClientSessionCommand>> sessions) {
        return sessions.entrySet()
                       .stream()
                       .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRef()));
    }

    private Map<Long, ActorRef<ClientSessionCommand>> toSessionRefsFromInboxes(
            Map<Long, TestInbox<ClientSessionCommand>> sessions
    ) {
        return sessions.entrySet()
                       .stream()
                       .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRef()));
    }

    private record ContrabandGameBehaviorTestContext(
            BehaviorTestUtils.BehaviorTestHarness<ContrabandGameCommand> harness,
            Map<Long, TestInbox<ClientSessionCommand>> sessions,
            TestInbox<LobbyCommand> lobby
    ) {
        TestInbox<ClientSessionCommand> session(Long playerId) {
            return sessions.get(playerId);
        }
    }

    private record ContrabandGameTestContext(
            Map<Long, TestProbe<ClientSessionCommand>> sessions,
            TestProbe<LobbyCommand> lobby,
            ActorTestUtils.MonitoredActor<ContrabandGameCommand> monitored
    ) {
        ActorRef<ContrabandGameCommand> commandRef() {
            return monitored.ref();
        }

        TestProbe<ClientSessionCommand> session(Long playerId) {
            return sessions.get(playerId);
        }
    }
}

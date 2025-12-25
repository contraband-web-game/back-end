package com.game.contraband.infrastructure.actor.game.chat.match;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import java.util.List;

public class ContrabandGameChatTimelines {

    private final ContrabandGameChatTimeline smugglerTimeline;
    private final ContrabandGameChatTimeline inspectorTimeline;
    private final ContrabandGameChatTimeline roundTimeline;

    public ContrabandGameChatTimelines(Long roomId) {
        this.smugglerTimeline = new ContrabandGameChatTimeline(roomId);
        this.inspectorTimeline = new ContrabandGameChatTimeline(roomId);
        this.roundTimeline = new ContrabandGameChatTimeline(roomId);
    }

    public ChatMessage appendTeamMessage(TeamRole teamRole, Long writerId, String writerName, String message) {
        return getTeamTimeline(teamRole).append(writerId, writerName, message);
    }

    public ChatMessage appendRoundMessage(Long writerId, String writerName, String message) {
        return roundTimeline.append(writerId, writerName, message);
    }

    public List<ChatMessage> maskTeamMessages(TeamRole teamRole, Long playerId) {
        return getTeamTimeline(teamRole).maskMessagesByWriter(playerId);
    }

    public List<ChatMessage> maskRoundMessages(Long playerId) {
        return roundTimeline.maskMessagesByWriter(playerId);
    }

    private ContrabandGameChatTimeline getTeamTimeline(TeamRole teamRole) {
        return switch (teamRole) {
            case SMUGGLER -> smugglerTimeline;
            case INSPECTOR -> inspectorTimeline;
        };
    }
}

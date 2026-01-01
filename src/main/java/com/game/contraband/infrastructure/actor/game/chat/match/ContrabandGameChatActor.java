package com.game.contraband.infrastructure.actor.game.chat.match;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateInspectorTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateMaskedChatMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateRoundChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateSmugglerTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher.ChatMessageEvent;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class ContrabandGameChatActor extends AbstractBehavior<ContrabandGameChatCommand> {

    public static Behavior<ContrabandGameChatCommand> create(
            Long roomId,
            String entityId,
            ChatMessageEventPublisher chatMessageEventPublisher,
            Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> clientSessionsByTeam,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(context -> {
            ContrabandGameChatMetadata metadata = new ContrabandGameChatMetadata(roomId, entityId);
            ContrabandGameChatParticipants participants = new ContrabandGameChatParticipants(clientSessionsByTeam);
            ContrabandGameChatTimelines timelines = new ContrabandGameChatTimelines(roomId);
            ContrabandRoundParticipants roundParticipants = new ContrabandRoundParticipants();
            ContrabandGameChatBlacklistListener blacklistListener = new ContrabandGameChatBlacklistListener(
                    chatBlacklistRepository,
                    blocked -> context.getSelf().tell(new MaskBlockedPlayerMessages(blocked))
            );
            return new ContrabandGameChatActor(
                    context,
                    metadata,
                    participants,
                    timelines,
                    roundParticipants,
                    chatMessageEventPublisher,
                    blacklistListener
            );
        });
    }

    private final ContrabandGameChatMetadata metadata;
    private final ContrabandGameChatParticipants participants;
    private final ContrabandGameChatTimelines timelines;
    private final ContrabandRoundParticipants roundParticipants;
    private final ChatMessageEventPublisher chatMessageEventPublisher;
    private final ContrabandGameChatBlacklistListener blacklistListener;

    private ContrabandGameChatActor(
            ActorContext<ContrabandGameChatCommand> context,
            ContrabandGameChatMetadata metadata,
            ContrabandGameChatParticipants participants,
            ContrabandGameChatTimelines timelines,
            ContrabandRoundParticipants roundParticipants,
            ChatMessageEventPublisher chatMessageEventPublisher,
            ContrabandGameChatBlacklistListener blacklistListener
    ) {
        super(context);
        this.metadata = metadata;
        this.participants = participants;
        this.timelines = timelines;
        this.roundParticipants = roundParticipants;
        this.chatMessageEventPublisher = chatMessageEventPublisher;
        this.blacklistListener = blacklistListener;
    }

    @Override
    public Receive<ContrabandGameChatCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ChatSmugglerTeam.class, this::onChatSmugglerTeam)
                .onMessage(ChatInspectorTeam.class, this::onChatInspectorTeam)
                .onMessage(ChatInRound.class, this::onChatInRound)
                .onMessage(SyncRoundChatId.class, this::onSyncRoundChatId)
                .onMessage(ClearRoundChatId.class, this::onClearRoundChatId)
                .onMessage(MaskBlockedPlayerMessages.class, this::onMaskBlockedPlayerMessages)
                .onSignal(PostStop.class, this::onPostStop)
                .build();
    }

    private Behavior<ContrabandGameChatCommand> onChatSmugglerTeam(ChatSmugglerTeam command) {
        if (!participants.hasSession(TeamRole.SMUGGLER, command.playerId())) {
            return this;
        }

        if (blacklistListener.isBlocked(command.playerId())) {
            ActorRef<ClientSessionCommand> senderSession = participants.session(TeamRole.SMUGGLER, command.playerId());
            if (senderSession != null) {
                senderSession.tell(
                        new HandleExceptionMessage(
                                ExceptionCode.CHAT_USER_BLOCKED,
                                "차단된 사용자입니다. 채팅을 보낼 수 없습니다."
                        )
                );
            }
            return this;
        }

        ChatMessage chatMessage = timelines.appendTeamMessage(
                TeamRole.SMUGGLER,
                command.playerId(),
                command.playerName(),
                command.message()
        );

        participants.forEachTeamMember(
                TeamRole.SMUGGLER,
                targetClientSession -> targetClientSession.tell(new PropagateSmugglerTeamChat(chatMessage))
        );
        chatMessageEventPublisher.publish(
                new ChatMessageEvent(
                        metadata.entityId(),
                        metadata.roomId(),
                        ChatEventType.SMUGGLER_TEAM_CHAT,
                        roundParticipants.currentRound(),
                        chatMessage
                )
        );
        return this;
    }

    private Behavior<ContrabandGameChatCommand> onChatInspectorTeam(ChatInspectorTeam command) {
        if (!participants.hasSession(TeamRole.INSPECTOR, command.playerId())) {
            return this;
        }

        if (blacklistListener.isBlocked(command.playerId())) {
            ActorRef<ClientSessionCommand> senderSession = participants.session(TeamRole.INSPECTOR, command.playerId());
            if (senderSession != null) {
                senderSession.tell(
                        new HandleExceptionMessage(
                                ExceptionCode.CHAT_USER_BLOCKED,
                                "차단된 사용자입니다. 채팅을 보낼 수 없습니다."
                        )
                );
            }
            return this;
        }

        ChatMessage chatMessage = timelines.appendTeamMessage(TeamRole.INSPECTOR, command.playerId(), command.playerName(), command.message());

        participants.forEachTeamMember(
                TeamRole.INSPECTOR,
                targetClientSession -> targetClientSession.tell(new PropagateInspectorTeamChat(chatMessage))
        );
        chatMessageEventPublisher.publish(
                new ChatMessageEvent(
                        metadata.entityId(),
                        metadata.roomId(),
                        ChatEventType.INSPECTOR_TEAM_CHAT,
                        roundParticipants.currentRound(),
                        chatMessage
                )
        );
        return this;
    }

    private Behavior<ContrabandGameChatCommand> onChatInRound(ChatInRound command) {
        if (roundParticipants.cannotRoundChat()) {
            return this;
        }
        if (roundParticipants.isNotRoundParticipant(command.playerId())) {
            return this;
        }
        if (roundParticipants.isRoundMismatch(command.currentRound())) {
            return this;
        }
        if (blacklistListener.isBlocked(command.playerId())) {
            TeamRole role = roundParticipants.isSmuggler(command.playerId()) ? TeamRole.SMUGGLER : TeamRole.INSPECTOR;
            ActorRef<ClientSessionCommand> senderSession = participants.session(role, command.playerId());
            if (senderSession != null) {
                senderSession.tell(
                        new HandleExceptionMessage(
                                ExceptionCode.CHAT_USER_BLOCKED,
                                "차단된 사용자입니다. 채팅을 보낼 수 없습니다."
                        )
                );
            }
            return this;
        }

        ChatMessage chatMessage = timelines.appendRoundMessage(
                command.playerId(),
                command.playerName(),
                command.message()
        );
        ActorRef<ClientSessionCommand> smugglerSession = participants.session(
                TeamRole.SMUGGLER,
                roundParticipants.smugglerId()
        );
        ActorRef<ClientSessionCommand> inspectorSession = participants.session(
                TeamRole.INSPECTOR,
                roundParticipants.inspectorId()
        );

        if (smugglerSession != null) {
            smugglerSession.tell(new PropagateRoundChat(chatMessage));
        }
        if (inspectorSession != null) {
            inspectorSession.tell(new PropagateRoundChat(chatMessage));
        }

        chatMessageEventPublisher.publish(
                new ChatMessageEvent(
                        metadata.entityId(),
                        metadata.roomId(),
                        ChatEventType.ROUND_CHAT,
                        roundParticipants.currentRound(),
                        chatMessage
                )
        );
        return this;
    }

    private Behavior<ContrabandGameChatCommand> onSyncRoundChatId(SyncRoundChatId command) {
        roundParticipants.registerInspector(command.inspectorId());
        roundParticipants.registerSmuggler(command.smugglerId());
        return this;
    }

    private Behavior<ContrabandGameChatCommand> onClearRoundChatId(ClearRoundChatId command) {
        roundParticipants.clear();
        return this;
    }

    private Behavior<ContrabandGameChatCommand> onMaskBlockedPlayerMessages(MaskBlockedPlayerMessages command) {
        maskAndBroadcastTeam(command.playerId(), TeamRole.SMUGGLER, ChatEventType.SMUGGLER_TEAM_CHAT);
        maskAndBroadcastTeam(command.playerId(), TeamRole.INSPECTOR, ChatEventType.INSPECTOR_TEAM_CHAT);
        maskAndBroadcastRound(command.playerId());
        return this;
    }

    private Behavior<ContrabandGameChatCommand> onPostStop(PostStop signal) {
        blacklistListener.close();
        return this;
    }

    private void maskAndBroadcastTeam(Long playerId, TeamRole teamRole, ChatEventType eventType) {
        List<ChatMessage> masked = timelines.maskTeamMessages(teamRole, playerId);
        if (masked.isEmpty()) {
            return;
        }
        broadcastMaskedToTeam(teamRole, masked, eventType);
    }

    private void maskAndBroadcastRound(Long playerId) {
        List<ChatMessage> masked = timelines.maskRoundMessages(playerId);
        if (masked.isEmpty()) {
            return;
        }
        broadcastMaskedToTeam(TeamRole.SMUGGLER, masked, ChatEventType.ROUND_CHAT);
        broadcastMaskedToTeam(TeamRole.INSPECTOR, masked, ChatEventType.ROUND_CHAT);
    }

    private void broadcastMaskedToTeam(TeamRole teamRole, List<ChatMessage> masked, ChatEventType eventType) {
        List<PropagateMaskedChatMessage> outbounds = toMaskedOutbounds(masked, eventType);
        participants.forEachTeamMember(teamRole, session -> outbounds.forEach(session::tell));
    }

    private List<PropagateMaskedChatMessage> toMaskedOutbounds(List<ChatMessage> masked, ChatEventType eventType) {
        return masked.stream()
                     .map(msg -> new PropagateMaskedChatMessage(msg.id(), eventType))
                     .toList();
    }

    public interface ContrabandGameChatCommand extends CborSerializable { }

    public record ChatSmugglerTeam(Long playerId, String playerName, String message) implements ContrabandGameChatCommand { }

    public record ChatInspectorTeam(Long playerId, String playerName, String message) implements ContrabandGameChatCommand { }

    public record ChatInRound(Long playerId, String playerName, String message, int currentRound) implements ContrabandGameChatCommand { }

    public record SyncRoundChatId(Long smugglerId, Long inspectorId) implements ContrabandGameChatCommand { }

    public record ClearRoundChatId() implements ContrabandGameChatCommand { }

    public record MaskBlockedPlayerMessages(Long playerId) implements ContrabandGameChatCommand { }
}

package com.game.contraband.infrastructure.actor.game.chat;

import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.game.chat.limit.ChatScope;

public interface GameChatProtocol {

    interface GameChatCommand extends CborSerializable { }

    record AllowedDecision(String requestId, ChatScope scope, Long playerId, long decidedAtMillis, int countInWindow) implements GameChatCommand { }

    record BlockedDecision(String requestId, ChatScope scope, Long playerId, long decidedAtMillis, int countInWindow, long mutedUntilMillis, ChatMuteReason reason, int level) implements GameChatCommand { }

    record RateLimitReleased(String requestId, ChatScope scope, Long playerId, long releasedAtMillis, int level) implements GameChatCommand { }

    interface LobbyChatCommand extends GameChatCommand { }

    interface ContrabandGameChatCommand extends GameChatCommand { }
}

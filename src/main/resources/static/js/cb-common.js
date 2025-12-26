(function (global) {
    const STORAGE_KEYS = {
        userId: 'cb_username',
        nickname: 'cb_nickname',
        lobbyPayload: 'cb_lobby_payload',
        gamePayload: 'cb_game_payload',
        roomId: 'cb_room_id',
        entityId: 'cb_entity_id'
    };

    const EXCEPTION_MESSAGES = {
        LOBBY_CREATE_FAILED: '로비 생성에 실패했습니다. 잠시 후 다시 시도해주세요.',
        LOBBY_FULL: '정원이 모두 찼습니다.',
        LOBBY_MAX_PLAYER_CHANGE_FORBIDDEN: '정원을 변경할 권한이 없습니다.',
        LOBBY_INVALID_OPERATION: '현재 로비 상태에서 수행할 수 없는 요청입니다.',
        LOBBY_TEAM_BALANCE_REQUIRED: '양 팀 인원이 같아야 작전을 시작할 수 있다. 팀을 조정해라.',
        LOBBY_READY_STATE_TEAM_CHANGE_FORBIDDEN: '이미 팀이 확정된 상태에서는 변경할 수 없다.',
        CHAT_MESSAGE_EMPTY: '메시지는 비어 있을 수 없습니다.',
        CHAT_USER_BLOCKED: '차단된 사용자입니다. 채팅을 보낼 수 없습니다.',
        GAME_ROOM_NOT_FOUND: '지정한 게임 방이 존재하지 않습니다.',
        NOT_CURRENT_ROUND_SMUGGLER: '현재 라운드를 진행하는 밀수꾼이 아닙니다.',
        NOT_CURRENT_ROUND_INSPECTOR: '현재 라운드를 진행하는 검사관이 아닙니다.',
        GAME_INVALID_STATE: '진행할 수 없는 게임 상태입니다. 새로고침 후 다시 시도해 주세요.',
        UNKNOWN_ERROR: '알 수 없는 오류가 발생했습니다.'
    };

    function resolveExceptionMessage(payload, options = {}) {
        const { overrides = {}, fallback } = options;
        const resolvedFallback = fallback || overrides.DEFAULT || EXCEPTION_MESSAGES.UNKNOWN_ERROR;
        const code = payload?.code;

        if (code && overrides[code]) {
            return overrides[code];
        }
        if (code && EXCEPTION_MESSAGES[code]) {
            return EXCEPTION_MESSAGES[code];
        }
        if (payload?.exceptionMessage) {
            return payload.exceptionMessage;
        }
        return resolvedFallback;
    }

    function saveSession(userId, nickname) {
        if (userId != null) {
            sessionStorage.setItem(STORAGE_KEYS.userId, String(userId));
            localStorage.setItem(STORAGE_KEYS.userId, String(userId));
        }
        if (nickname) {
            sessionStorage.setItem(STORAGE_KEYS.nickname, nickname);
            localStorage.setItem(STORAGE_KEYS.nickname, nickname);
        }
    }

    function loadSession() {
        return {
            userId: sessionStorage.getItem(STORAGE_KEYS.userId) || localStorage.getItem(STORAGE_KEYS.userId),
            nickname: sessionStorage.getItem(STORAGE_KEYS.nickname) || localStorage.getItem(STORAGE_KEYS.nickname)
        };
    }

    function clearSession() {
        Object.values(STORAGE_KEYS).forEach((k) => {
            sessionStorage.removeItem(k);
            localStorage.removeItem(k);
        });
    }

    function requireSession() {
        const session = loadSession();
        if (!session.userId) {
            window.location.href = '/index-new.html';
            return null;
        }
        return session;
    }

    function buildWsUrl(userId) {
        const numericId = Number(userId);
        const proto = location.protocol === 'https:' ? 'wss' : 'ws';
        return `${proto}://${location.host}/ws?playerId=${numericId}`;
    }

    async function login(userId, options = {}) {
        const { silent = false } = options;
        const numericId = Number(userId);
        if (!numericId) {
            const err = new Error('플레이어 ID가 필요합니다.');
            if (silent) return { success: false, message: err.message };
            throw err;
        }
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: numericId })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok || data.success !== true) {
                const msg = data.message || `로그인 실패 (${res.status})`;
                if (silent) return { success: false, message: msg };
                throw new Error(msg);
            }
            return { success: true, message: data.message || '로그인 성공', userId: numericId };
        } catch (err) {
            const msg = err?.message || '로그인 실패';
            if (silent) return { success: false, message: msg };
            throw err;
        }
    }

    function safeParse(json) {
        try {
            return JSON.parse(json);
        } catch (e) {
            return null;
        }
    }

    function createSocket(userId, handlers = {}) {
        const url = buildWsUrl(userId);
        const ws = new WebSocket(url);
        ws.onopen = () => handlers.onOpen && handlers.onOpen(ws, url);
        ws.onclose = (evt) => handlers.onClose && handlers.onClose(evt);
        ws.onerror = (err) => handlers.onError && handlers.onError(err);
        ws.onmessage = (evt) => {
            // Debug log for raw WS messages
            if (typeof console !== 'undefined' && console.debug) {
                console.debug('[WS RAW]', evt.data);
            }
            const parsed = safeParse(evt.data);
            if (!parsed) {
                if (typeof console !== 'undefined' && console.warn) {
                    console.warn('[WS PARSE FAIL]', evt.data);
                }
                return;
            }
            handlers.onMessage && handlers.onMessage(parsed, evt);
        };
        return ws;
    }

    function sendJson(ws, payload) {
        if (!ws || ws.readyState !== WebSocket.OPEN) return false;
        ws.send(JSON.stringify(payload));
        return true;
    }

    function saveLobbyPayload(payload) {
        const raw = JSON.stringify(payload || {});
        sessionStorage.setItem(STORAGE_KEYS.lobbyPayload, raw);
        localStorage.setItem(STORAGE_KEYS.lobbyPayload, raw);
    }

    function loadLobbyPayload() {
        const raw = sessionStorage.getItem(STORAGE_KEYS.lobbyPayload) || localStorage.getItem(STORAGE_KEYS.lobbyPayload);
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    function clearLobbyPayload() {
        sessionStorage.removeItem(STORAGE_KEYS.lobbyPayload);
        localStorage.removeItem(STORAGE_KEYS.lobbyPayload);
    }

    function saveGamePayload(payload) {
        const raw = JSON.stringify(payload || {});
        sessionStorage.setItem(STORAGE_KEYS.gamePayload, raw);
        localStorage.setItem(STORAGE_KEYS.gamePayload, raw);
    }

    function loadGamePayload() {
        const raw = sessionStorage.getItem(STORAGE_KEYS.gamePayload) || localStorage.getItem(STORAGE_KEYS.gamePayload);
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    function clearGamePayload() {
        sessionStorage.removeItem(STORAGE_KEYS.gamePayload);
        localStorage.removeItem(STORAGE_KEYS.gamePayload);
    }

    function saveRoomContext(roomId, entityId) {
        if (roomId) {
            sessionStorage.setItem(STORAGE_KEYS.roomId, String(roomId));
            localStorage.setItem(STORAGE_KEYS.roomId, String(roomId));
        }
        if (entityId != null) {
            sessionStorage.setItem(STORAGE_KEYS.entityId, String(entityId));
            localStorage.setItem(STORAGE_KEYS.entityId, String(entityId));
        }
    }

    function loadRoomContext() {
        return {
            roomId: sessionStorage.getItem(STORAGE_KEYS.roomId) || localStorage.getItem(STORAGE_KEYS.roomId),
            entityId: sessionStorage.getItem(STORAGE_KEYS.entityId) || localStorage.getItem(STORAGE_KEYS.entityId)
        };
    }

    global.cbCommon = {
        STORAGE_KEYS,
        saveSession,
        loadSession,
        clearSession,
        requireSession,
        login,
        buildWsUrl,
        createSocket,
        sendJson,
        saveLobbyPayload,
        loadLobbyPayload,
        clearLobbyPayload,
        saveGamePayload,
        loadGamePayload,
        clearGamePayload,
        saveRoomContext,
        loadRoomContext,
        safeParse,
        EXCEPTION_MESSAGES,
        resolveExceptionMessage
    };
})(window);

package com.game.contraband.domain.game.transfer;

public class TransferFailureException extends IllegalStateException {

    private final TransferFailureReason reason;

    public TransferFailureException(TransferFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public TransferFailureReason getReason() {
        return reason;
    }
}

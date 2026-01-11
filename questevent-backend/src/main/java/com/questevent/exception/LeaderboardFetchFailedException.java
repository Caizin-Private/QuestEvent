package com.questevent.exception;

public class LeaderboardFetchFailedException extends RuntimeException {

    public LeaderboardFetchFailedException(String message) {
        super(message);
    }

    public LeaderboardFetchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
 
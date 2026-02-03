package cz.muni.fi.trusted_party.exceptions;

public class InvalidTimestampException extends RuntimeException {

    public InvalidTimestampException() { }

    public InvalidTimestampException(String message) {
        super(message);
    }

    public InvalidTimestampException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTimestampException(Throwable cause) {
        super(cause);
    }

    public InvalidTimestampException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
package cz.muni.fi.trusted_party.exceptions;

public class MissingSignatureException extends RuntimeException {

    public MissingSignatureException() { }

    public MissingSignatureException(String message) {
        super(message);
    }

    public MissingSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingSignatureException(Throwable cause) {
        super(cause);
    }

    public MissingSignatureException(String message, Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
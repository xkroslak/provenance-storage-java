package cz.muni.fi.distributed_prov_system.exceptions;

public class TrustedPartyUnavailableException extends RuntimeException {

    public TrustedPartyUnavailableException() {
    }

    public TrustedPartyUnavailableException(String message) {
        super(message);
    }

    public TrustedPartyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrustedPartyUnavailableException(Throwable cause) {
        super(cause);
    }

    public TrustedPartyUnavailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

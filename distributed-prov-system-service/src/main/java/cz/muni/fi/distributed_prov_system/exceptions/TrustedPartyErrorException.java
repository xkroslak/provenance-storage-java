package cz.muni.fi.distributed_prov_system.exceptions;

public class TrustedPartyErrorException extends RuntimeException {
    public TrustedPartyErrorException() {
    }

    public TrustedPartyErrorException(String message) {
        super(message);
    }

    public TrustedPartyErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrustedPartyErrorException(Throwable cause) {
        super(cause);
    }

    public TrustedPartyErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

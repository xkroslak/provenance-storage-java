package cz.muni.fi.trusted_party.exceptions;

public class CertificateVerificationException extends RuntimeException {

    public CertificateVerificationException() {}

    public CertificateVerificationException(String message) {
        super(message);
    }

    public CertificateVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateVerificationException(Throwable cause) {
        super(cause);
    }

    public CertificateVerificationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
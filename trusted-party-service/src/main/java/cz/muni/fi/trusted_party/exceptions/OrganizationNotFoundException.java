package cz.muni.fi.trusted_party.exceptions;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException() {}
    public OrganizationNotFoundException(String message) { super(message); }
    public OrganizationNotFoundException(String message, Throwable cause) { super(message, cause); }
    public OrganizationNotFoundException(Throwable cause) { super(cause); }
    public OrganizationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
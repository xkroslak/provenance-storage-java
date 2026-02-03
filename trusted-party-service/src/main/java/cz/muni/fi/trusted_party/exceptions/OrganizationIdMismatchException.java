package cz.muni.fi.trusted_party.exceptions;

public class OrganizationIdMismatchException extends RuntimeException {
  public OrganizationIdMismatchException() {}
  public OrganizationIdMismatchException(String message) { super(message); }
  public OrganizationIdMismatchException(String message, Throwable cause) { super(message, cause); }
  public OrganizationIdMismatchException(Throwable cause) { super(cause); }
  public OrganizationIdMismatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
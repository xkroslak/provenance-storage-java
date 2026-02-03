package cz.muni.fi.trusted_party.exceptions;

public class OrganizationAlreadyExistsException extends RuntimeException {

  public OrganizationAlreadyExistsException() {}

  public OrganizationAlreadyExistsException(String message) {
    super(message);
  }

  public OrganizationAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public OrganizationAlreadyExistsException(Throwable cause) {
    super(cause);
  }

  public OrganizationAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
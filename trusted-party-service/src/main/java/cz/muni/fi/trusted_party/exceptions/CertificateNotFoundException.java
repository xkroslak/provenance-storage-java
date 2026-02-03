package cz.muni.fi.trusted_party.exceptions;

public class CertificateNotFoundException extends RuntimeException {

  public CertificateNotFoundException() {}

  public CertificateNotFoundException(String message) {
    super(message);
  }

  public CertificateNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public CertificateNotFoundException(Throwable cause) {
    super(cause);
  }

  public CertificateNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
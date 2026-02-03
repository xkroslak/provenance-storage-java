package cz.muni.fi.distributed_prov_system.exceptions;

public class TrustedPartyDisabledException extends RuntimeException {
  public TrustedPartyDisabledException() {
  }

  public TrustedPartyDisabledException(String message) {
    super(message);
  }

  public TrustedPartyDisabledException(String message, Throwable cause) {
    super(message, cause);
  }

  public TrustedPartyDisabledException(Throwable cause) {
    super(cause);
  }

  public TrustedPartyDisabledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

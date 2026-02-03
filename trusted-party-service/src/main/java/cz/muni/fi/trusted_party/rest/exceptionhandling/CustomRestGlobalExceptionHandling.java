package cz.muni.fi.trusted_party.rest.exceptionhandling;

import cz.muni.fi.trusted_party.exceptions.CertificateNotFoundException;
import cz.muni.fi.trusted_party.exceptions.CertificateVerificationException;
import cz.muni.fi.trusted_party.exceptions.DocumentNotFoundException;
import cz.muni.fi.trusted_party.exceptions.InvalidTimestampException;
import cz.muni.fi.trusted_party.exceptions.MissingSignatureException;
import cz.muni.fi.trusted_party.exceptions.OrganizationAlreadyExistsException;
import cz.muni.fi.trusted_party.exceptions.OrganizationIdMismatchException;
import cz.muni.fi.trusted_party.exceptions.OrganizationNotFoundException;
import cz.muni.fi.trusted_party.exceptions.SignatureVerificationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.util.UrlPathHelper;

import java.time.Clock;
import java.time.LocalDateTime;

public class CustomRestGlobalExceptionHandling {

    private static UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    @ExceptionHandler({
            CertificateNotFoundException.class, DocumentNotFoundException.class, OrganizationNotFoundException.class
    })
    public ResponseEntity<ApiError> handleResoursceNotFound(final Exception ex, final HttpServletRequest request) {
        final ApiError apiError = new ApiError(
                LocalDateTime.now(Clock.systemUTC()),
                HttpStatus.NOT_FOUND,
                ex.getLocalizedMessage(),
                URL_PATH_HELPER.getRequestUri(request)
        );
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({
            InvalidTimestampException.class, MissingSignatureException.class,
            OrganizationIdMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(final Exception ex, final HttpServletRequest request) {
        final ApiError apiError = new ApiError(
                LocalDateTime.now(Clock.systemUTC()),
                HttpStatus.BAD_REQUEST,
                ex.getLocalizedMessage(),
                URL_PATH_HELPER.getRequestUri(request)
        );
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({
            OrganizationAlreadyExistsException.class
    })
    public ResponseEntity<ApiError> handleConflict(final Exception ex, final HttpServletRequest request) {
        final ApiError apiError = new ApiError(
                LocalDateTime.now(Clock.systemUTC()),
                HttpStatus.CONFLICT,
                ex.getLocalizedMessage(),
                URL_PATH_HELPER.getRequestUri(request)
        );
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({
            CertificateVerificationException.class, SignatureVerificationException.class
    })
    public ResponseEntity<ApiError> handleUnauthorized(final Exception ex, final HttpServletRequest request) {
        final ApiError apiError = new ApiError(
                LocalDateTime.now(Clock.systemUTC()),
                HttpStatus.UNAUTHORIZED,
                ex.getLocalizedMessage(),
                URL_PATH_HELPER.getRequestUri(request)
        );
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }
}

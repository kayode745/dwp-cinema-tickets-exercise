package uk.gov.dwp.engineering.recruitment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to RFC 9457 problem details.
 *
 * <ul>
 *   <li>{@link InvalidBookingException}: 400 Bad Request, with the broken rule as the detail.</li>
 *   <li>Malformed JSON, unknown ticket types, wrong content type and other framework errors:
 *       handled by {@link ResponseEntityExceptionHandler} (400, 415 and so on).</li>
 *   <li>Anything unexpected: 500 with a generic detail, so internal information is not leaked.</li>
 * </ul>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  static final String INVALID_BOOKING_TITLE = "Invalid booking request";
  static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred";

  private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(InvalidBookingException.class)
  protected ProblemDetail handleInvalidBookingException(final InvalidBookingException ex) {
    LOG.info("Rejected booking request: {}", ex.getMessage());
    final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle(INVALID_BOOKING_TITLE);
    problemDetail.setDetail(ex.getMessage());
    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  protected ProblemDetail handleUnexpectedException(final Exception ex) {
    LOG.error("Unexpected error handling booking request", ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        UNEXPECTED_ERROR_DETAIL);
  }
}

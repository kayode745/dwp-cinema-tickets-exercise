package uk.gov.dwp.engineering.recruitment.domain;

import java.math.BigDecimal;

/**
 * Confirmation of a successful booking.
 *
 * <p>Extended in a non-breaking way with {@code seatCount} and {@code totalCost} so the response
 * matches the {@code BookingConfirmation} schema in {@code cinema-tickets.yaml}. The original
 * single-argument constructor and the {@code accountId()} accessor are kept for existing callers.
 *
 * @param accountId the account that was charged
 * @param seatCount the number of seats reserved
 * @param totalCost the total amount debited from the account, in pounds sterling
 */
public record BookingConfirmation(Long accountId, Long seatCount, BigDecimal totalCost) {

  /**
   * Original constructor, retained for backwards compatibility.
   *
   * @param accountId the account that was charged
   */
  public BookingConfirmation(Long accountId) {
    this(accountId, null, null);
  }
}

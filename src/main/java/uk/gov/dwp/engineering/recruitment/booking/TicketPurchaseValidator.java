package uk.gov.dwp.engineering.recruitment.booking;

import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

/**
 * Validates a ticket purchase against the business rules. Every check throws an
 * {@link InvalidBookingException} with a message explaining which rule was broken.
 *
 * <p>Validation is split into three stages so that the order can be aggregated safely: the account,
 * then the shape of each individual request, then the rules that apply to the purchase as a whole.
 */
public final class TicketPurchaseValidator {

  /** The maximum number of tickets, of any type including infants, in a single purchase. */
  public static final int MAX_TICKETS_PER_PURCHASE = 25;

  public static final String INVALID_ACCOUNT = "Account ID must be greater than zero";
  public static final String NO_TICKET_REQUESTS = "At least one ticket request must be provided";
  public static final String NULL_TICKET_REQUEST = "Ticket requests must not be null";
  public static final String MISSING_TICKET_TYPE = "Every ticket request must have a ticket type";
  public static final String NEGATIVE_TICKET_COUNT = "Ticket counts must not be negative";
  public static final String NO_TICKETS = "At least one ticket must be purchased";
  public static final String TOO_MANY_TICKETS =
      "A maximum of " + MAX_TICKETS_PER_PURCHASE + " tickets can be purchased at a time";
  public static final String NO_ADULT =
      "CHILD and INFANT tickets cannot be purchased without at least one ADULT ticket";
  public static final String TOO_MANY_INFANTS =
      "Each INFANT must sit on an ADULT's lap, so there cannot be more INFANT than ADULT tickets";

  private TicketPurchaseValidator() {
  }

  /**
   * Checks the account ID. Any ID greater than zero is assumed to be valid with sufficient funds.
   *
   * @param accountId the account to charge
   * @throws InvalidBookingException if the ID is null, zero or negative
   */
  public static void validateAccountId(final Long accountId) {
    if (accountId == null || accountId <= 0) {
      throw new InvalidBookingException(INVALID_ACCOUNT);
    }
  }

  /**
   * Checks that each request is well formed: present, typed and with a non-negative count.
   *
   * @param ticketRequests the requests to check
   * @throws InvalidBookingException if any request is malformed or none are given
   */
  public static void validateTicketRequests(final TicketRequest... ticketRequests) {
    if (ticketRequests == null || ticketRequests.length == 0) {
      throw new InvalidBookingException(NO_TICKET_REQUESTS);
    }
    for (final TicketRequest request : ticketRequests) {
      if (request == null) {
        throw new InvalidBookingException(NULL_TICKET_REQUEST);
      }
      if (request.type() == null) {
        throw new InvalidBookingException(MISSING_TICKET_TYPE);
      }
      if (request.ticketCount() < 0) {
        throw new InvalidBookingException(NEGATIVE_TICKET_COUNT);
      }
    }
  }

  /**
   * Checks the rules that apply to the purchase as a whole.
   *
   * @param order the aggregated order
   * @throws InvalidBookingException if the order breaks a business rule
   */
  public static void validateOrder(final TicketOrder order) {
    final long totalTickets = order.totalTickets();
    if (totalTickets == 0) {
      throw new InvalidBookingException(NO_TICKETS);
    }
    if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
      throw new InvalidBookingException(TOO_MANY_TICKETS);
    }
    final long adults = order.countOf(ADULT);
    if (adults == 0 && (order.countOf(CHILD) > 0 || order.countOf(INFANT) > 0)) {
      throw new InvalidBookingException(NO_ADULT);
    }
    if (order.countOf(INFANT) > adults) {
      throw new InvalidBookingException(TOO_MANY_INFANTS);
    }
  }
}

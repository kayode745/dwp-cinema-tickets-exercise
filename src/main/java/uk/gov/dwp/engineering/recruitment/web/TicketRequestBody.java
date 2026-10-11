package uk.gov.dwp.engineering.recruitment.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

/**
 * JSON body for a single ticket request. See {@link BookingRequest} for why this exists.
 *
 * @param type  the ticket type
 * @param count the number of tickets of that type; boxed so a missing value can be detected
 */
public record TicketRequestBody(
    @JsonProperty("type") TicketType type,
    @JsonProperty("count") @JsonAlias("ticketCount") Integer count) {

  /** Message used when a ticket request has no count. */
  public static final String MISSING_TICKET_COUNT = "Every ticket request must have a count";

  /**
   * Converts this body into a domain ticket request.
   *
   * @return the domain ticket request
   * @throws InvalidBookingException if the count is missing, rather than defaulting it to zero
   */
  public TicketRequest toTicketRequest() {
    if (count == null) {
      throw new InvalidBookingException(MISSING_TICKET_COUNT);
    }
    return new TicketRequest(type, count);
  }
}

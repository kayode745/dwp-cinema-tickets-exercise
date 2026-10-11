package uk.gov.dwp.engineering.recruitment.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

/**
 * JSON request body for {@code POST /cinema/bookings}.
 *
 * <p>The OpenAPI contract ({@code cinema-tickets.yaml}) names the fields {@code tickets} and
 * {@code count}, whereas the domain records use {@code ticketRequests} and {@code ticketCount}.
 * The domain package cannot be changed, so this request body follows the published contract and
 * also accepts the domain field names as aliases, so either form works.
 *
 * @param accountId the account to charge
 * @param tickets   the ticket requests
 */
public record BookingRequest(
    @JsonProperty("accountId") Long accountId,
    @JsonProperty("tickets") @JsonAlias("ticketRequests") List<TicketRequestBody> tickets) {

  /**
   * Converts the request body into domain ticket requests. Absent or null entries are passed
   * through unchanged so the service can reject them with a clear message.
   *
   * @return the ticket requests, or {@code null} if none were supplied
   */
  public TicketRequest[] toTicketRequests() {
    if (tickets == null) {
      return null;
    }
    return tickets.stream()
        .map(body -> body == null ? null : body.toTicketRequest())
        .toArray(TicketRequest[]::new);
  }
}

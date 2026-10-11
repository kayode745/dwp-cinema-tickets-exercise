package uk.gov.dwp.engineering.recruitment.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

/**
 * The number of tickets of each type in a purchase, aggregated across all ticket requests.
 *
 * <p>A purchase may contain several requests for the same type (for example two separate ADULT
 * requests); these are summed. Counts are held as {@code long} so that summing many large
 * {@code int} requests cannot overflow before the maximum-ticket rule is checked.
 */
public final class TicketOrder {

  private final Map<TicketType, Long> counts;

  private TicketOrder(final Map<TicketType, Long> counts) {
    this.counts = counts;
  }

  /**
   * Aggregates ticket requests into an order. The requests must already be structurally valid (see
   * {@link TicketPurchaseValidator#validateTicketRequests(TicketRequest...)}).
   *
   * @param ticketRequests non-null requests with a non-null type and a non-negative count
   * @return the aggregated order
   */
  public static TicketOrder from(final TicketRequest... ticketRequests) {
    final Map<TicketType, Long> counts = new EnumMap<>(TicketType.class);
    for (final TicketType type : TicketType.values()) {
      counts.put(type, 0L);
    }
    for (final TicketRequest request : ticketRequests) {
      counts.merge(request.type(), (long) request.ticketCount(), Long::sum);
    }
    return new TicketOrder(counts);
  }

  /**
   * Returns the number of tickets of the given type.
   *
   * @param type the ticket type
   * @return the aggregated count
   */
  public long countOf(final TicketType type) {
    return counts.get(type);
  }

  /**
   * Returns the total number of tickets of all types, including infants.
   *
   * @return the total ticket count
   */
  public long totalTickets() {
    return counts.values().stream().mapToLong(Long::longValue).sum();
  }

  /**
   * Returns the number of seats to reserve. Infants do not need a seat.
   *
   * @return the seat count
   */
  public long seatsRequired() {
    return counts.entrySet().stream()
        .filter(entry -> TicketTypeRules.requiresSeat(entry.getKey()))
        .mapToLong(Map.Entry::getValue)
        .sum();
  }

  /**
   * Returns the total cost of the order.
   *
   * @return the total cost in pounds sterling, with a scale of 2
   */
  public BigDecimal totalCost() {
    return counts.entrySet().stream()
        .map(entry -> TicketTypeRules.priceOf(entry.getKey())
            .multiply(BigDecimal.valueOf(entry.getValue())))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.UNNECESSARY);
  }
}

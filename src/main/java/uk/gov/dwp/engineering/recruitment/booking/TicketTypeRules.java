package uk.gov.dwp.engineering.recruitment.booking;

import java.math.BigDecimal;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

/**
 * Price and seating rules for each {@link TicketType}.
 *
 * <p>{@code TicketType} lives in the domain package, which must not be modified, so these rules are
 * kept here. The switch expressions are exhaustive: adding a new ticket type will fail compilation
 * until its price and seating rule are defined, rather than silently defaulting.
 */
public final class TicketTypeRules {

  private static final BigDecimal ADULT_PRICE = new BigDecimal("25.99");
  private static final BigDecimal CHILD_PRICE = new BigDecimal("17.50");
  private static final BigDecimal INFANT_PRICE = new BigDecimal("0.00");

  private TicketTypeRules() {
  }

  /**
   * Returns the price of a single ticket of the given type, in pounds sterling.
   *
   * @param type the ticket type
   * @return the unit price, with a scale of 2
   */
  public static BigDecimal priceOf(final TicketType type) {
    return switch (type) {
      case ADULT -> ADULT_PRICE;
      case CHILD -> CHILD_PRICE;
      case INFANT -> INFANT_PRICE;
    };
  }

  /**
   * Returns whether a ticket of the given type needs its own seat.
   *
   * @param type the ticket type
   * @return {@code false} for infants, who sit on an adult's lap; otherwise {@code true}
   */
  public static boolean requiresSeat(final TicketType type) {
    return switch (type) {
      case ADULT, CHILD -> true;
      case INFANT -> false;
    };
  }
}

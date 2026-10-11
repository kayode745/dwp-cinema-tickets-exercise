package uk.gov.dwp.engineering.recruitment.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

class TicketOrderTest {

  @Test
  void givenMixedRequests_whenAggregated_thenCountsSeatsAndCostAreCorrect() {
    final TicketOrder order = TicketOrder.from(
        new TicketRequest(ADULT, 2),
        new TicketRequest(CHILD, 1),
        new TicketRequest(INFANT, 1),
        new TicketRequest(ADULT, 1));

    assertEquals(3, order.countOf(ADULT));
    assertEquals(1, order.countOf(CHILD));
    assertEquals(1, order.countOf(INFANT));
    assertEquals(5, order.totalTickets());
    assertEquals(4, order.seatsRequired());
    assertEquals(new BigDecimal("95.47"), order.totalCost());
  }

  @Test
  void givenTypeNotRequested_whenCountOf_thenZero() {
    final TicketOrder order = TicketOrder.from(new TicketRequest(ADULT, 1));

    assertEquals(0, order.countOf(CHILD));
    assertEquals(0, order.countOf(INFANT));
  }

  @Test
  void givenOnlyInfants_whenSeatsRequired_thenZeroAndCostIsZero() {
    final TicketOrder order = TicketOrder.from(new TicketRequest(INFANT, 3));

    assertEquals(0, order.seatsRequired());
    assertEquals(new BigDecimal("0.00"), order.totalCost());
  }

  @Test
  void givenMaximumIntCounts_whenTotalTickets_thenNoOverflow() {
    final TicketOrder order = TicketOrder.from(
        new TicketRequest(ADULT, Integer.MAX_VALUE),
        new TicketRequest(CHILD, Integer.MAX_VALUE));

    assertEquals(2L * Integer.MAX_VALUE, order.totalTickets());
  }
}

package uk.gov.dwp.engineering.recruitment.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

class TicketTypeRulesTest {

  @ParameterizedTest
  @CsvSource({"ADULT, 25.99", "CHILD, 17.50", "INFANT, 0.00"})
  void givenTicketType_whenPriceOf_thenExpectedPrice(final TicketType type, final String price) {
    assertEquals(new BigDecimal(price), TicketTypeRules.priceOf(type));
  }

  @ParameterizedTest
  @CsvSource({"ADULT, true", "CHILD, true", "INFANT, false"})
  void givenTicketType_whenRequiresSeat_thenExpectedResult(final TicketType type,
      final boolean requiresSeat) {
    assertEquals(requiresSeat, TicketTypeRules.requiresSeat(type));
  }
}

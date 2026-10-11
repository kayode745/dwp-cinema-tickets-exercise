package uk.gov.dwp.engineering.recruitment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.dwp.engineering.recruitment.booking.TicketPurchaseValidator;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.web.TicketRequestBody;

/**
 * End-to-end tests of {@code POST /cinema/bookings} through the real controller, service, JSON
 * mapping and exception handler. Only the two third-party services are mocked.
 */
@SpringBootTest
class CinemaTicketsControllerTest {

  private static final String BOOKINGS_URL = "/cinema/bookings";

  @Autowired
  private WebApplicationContext context;

  @MockitoBean
  private PaymentService paymentService;

  @MockitoBean
  private SeatReservationService seatReservationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  // ---- Successful bookings ----

  static Stream<Arguments> validBookings() {
    return Stream.of(
        Arguments.of("one adult",
            booking(1, ticket("ADULT", 1)), 1, 25.99),
        Arguments.of("adult, child and infant",
            booking(1, ticket("ADULT", 1), ticket("CHILD", 1), ticket("INFANT", 1)), 2, 43.49),
        Arguments.of("duplicate types are aggregated",
            booking(1, ticket("ADULT", 1), ticket("ADULT", 1), ticket("CHILD", 2)), 4, 86.98),
        Arguments.of("exactly 25 tickets including infants",
            booking(1, ticket("ADULT", 10), ticket("CHILD", 5), ticket("INFANT", 10)), 15, 347.40),
        Arguments.of("zero-count entries are ignored",
            booking(1, ticket("ADULT", 2), ticket("CHILD", 0)), 2, 51.98));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validBookings")
  void givenValidBooking_whenPost_thenCreatedWithSeatsAndCost(final String description,
      final String json, final int expectedSeats, final double expectedCost) throws Exception {

    postBooking(json)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId").value(1))
        .andExpect(jsonPath("$.seatCount").value(expectedSeats))
        .andExpect(jsonPath("$.totalCost").value(expectedCost));

    verify(paymentService).debitAccount(1L, BigDecimal.valueOf(expectedCost).setScale(2));
    verify(seatReservationService).reserveSeats(1L, (long) expectedSeats);
  }

  @Test
  void givenDomainFieldNames_whenPost_thenAcceptedAsAliases() throws Exception {
    final String json = """
        {"accountId": 7, "ticketRequests": [{"type": "ADULT", "ticketCount": 2}]}""";

    postBooking(json)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId").value(7))
        .andExpect(jsonPath("$.seatCount").value(2))
        .andExpect(jsonPath("$.totalCost").value(51.98));
  }

  // ---- Business-rule rejections (400 with the broken rule as the detail) ----

  static Stream<Arguments> bookingsBreakingBusinessRules() {
    return Stream.of(
        Arguments.of("account ID zero", booking(0, ticket("ADULT", 1)),
            TicketPurchaseValidator.INVALID_ACCOUNT),
        Arguments.of("account ID negative", booking(-5, ticket("ADULT", 1)),
            TicketPurchaseValidator.INVALID_ACCOUNT),
        Arguments.of("account ID missing", """
            {"tickets": [{"type": "ADULT", "count": 1}]}""",
            TicketPurchaseValidator.INVALID_ACCOUNT),
        Arguments.of("tickets missing", """
            {"accountId": 1}""",
            TicketPurchaseValidator.NO_TICKET_REQUESTS),
        Arguments.of("tickets empty", """
            {"accountId": 1, "tickets": []}""",
            TicketPurchaseValidator.NO_TICKET_REQUESTS),
        Arguments.of("null ticket entry", """
            {"accountId": 1, "tickets": [{"type": "ADULT", "count": 1}, null]}""",
            TicketPurchaseValidator.NULL_TICKET_REQUEST),
        Arguments.of("ticket type missing", """
            {"accountId": 1, "tickets": [{"count": 1}]}""",
            TicketPurchaseValidator.MISSING_TICKET_TYPE),
        Arguments.of("ticket count missing", """
            {"accountId": 1, "tickets": [{"type": "ADULT"}]}""",
            TicketRequestBody.MISSING_TICKET_COUNT),
        Arguments.of("negative count", booking(1, ticket("ADULT", 2), ticket("CHILD", -1)),
            TicketPurchaseValidator.NEGATIVE_TICKET_COUNT),
        Arguments.of("no tickets in total", booking(1, ticket("ADULT", 0)),
            TicketPurchaseValidator.NO_TICKETS),
        Arguments.of("26 adult tickets", booking(1, ticket("ADULT", 26)),
            TicketPurchaseValidator.TOO_MANY_TICKETS),
        Arguments.of("26 tickets counting infants",
            booking(1, ticket("ADULT", 13), ticket("INFANT", 13)),
            TicketPurchaseValidator.TOO_MANY_TICKETS),
        Arguments.of("child without adult", booking(1, ticket("CHILD", 1)),
            TicketPurchaseValidator.NO_ADULT),
        Arguments.of("infant without adult", booking(1, ticket("INFANT", 1)),
            TicketPurchaseValidator.NO_ADULT),
        Arguments.of("more infants than adults",
            booking(1, ticket("ADULT", 1), ticket("INFANT", 2)),
            TicketPurchaseValidator.TOO_MANY_INFANTS));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("bookingsBreakingBusinessRules")
  void givenBookingBreakingBusinessRule_whenPost_thenBadRequestAndNothingCharged(
      final String description, final String json, final String expectedDetail) throws Exception {

    postBooking(json)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid booking request"))
        .andExpect(jsonPath("$.detail").value(expectedDetail));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  // ---- Malformed requests (rejected by the framework before reaching the service) ----

  static Stream<Arguments> malformedBookings() {
    return Stream.of(
        Arguments.of("unknown ticket type", booking(1, ticket("SENIOR", 1))),
        Arguments.of("non-numeric count", """
            {"accountId": 1, "tickets": [{"type": "ADULT", "count": "two"}]}"""),
        Arguments.of("count larger than an int", """
            {"accountId": 1, "tickets": [{"type": "ADULT", "count": 99999999999}]}"""),
        Arguments.of("invalid JSON", "{\"accountId\": 1, \"tickets\": ["),
        Arguments.of("empty body", ""));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("malformedBookings")
  void givenMalformedBooking_whenPost_thenBadRequestAndNothingCharged(final String description,
      final String json) throws Exception {

    postBooking(json)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenNonJsonContentType_whenPost_thenUnsupportedMediaType() throws Exception {
    mockMvc.perform(post(BOOKINGS_URL)
            .contentType(MediaType.TEXT_PLAIN)
            .content("ADULT x 1"))
        .andExpect(status().isUnsupportedMediaType());

    verifyNoInteractions(paymentService, seatReservationService);
  }

  // ---- Unexpected failures ----

  @Test
  void givenUnexpectedFailure_whenPost_thenInternalServerErrorWithoutInternalDetails()
      throws Exception {
    when(paymentService.debitAccount(anyLong(), any(BigDecimal.class)))
        .thenThrow(new IllegalStateException("connection pool exhausted"));

    postBooking(booking(1, ticket("ADULT", 1)))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));

    verifyNoInteractions(seatReservationService);
  }

  // ---- Helpers ----

  private ResultActions postBooking(final String json) throws Exception {
    return mockMvc.perform(post(BOOKINGS_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json));
  }

  private static String booking(final long accountId, final String... tickets) {
    return "{\"accountId\": %d, \"tickets\": [%s]}"
        .formatted(accountId, String.join(", ", tickets));
  }

  private static String ticket(final String type, final long count) {
    return "{\"type\": \"%s\", \"count\": %d}".formatted(type, count);
  }
}

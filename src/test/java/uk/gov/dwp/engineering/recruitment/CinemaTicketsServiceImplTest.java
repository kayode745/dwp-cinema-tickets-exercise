package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.booking.TicketPurchaseValidator;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

  private static final Long ACCOUNT_ID = 1234L;

  @Mock
  private PaymentService paymentService;

  @Mock
  private SeatReservationService seatReservationService;

  private CinemaTicketsServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CinemaTicketsServiceImpl(paymentService, seatReservationService);
  }

  static Stream<Arguments> validPurchases() {
    return Stream.of(
        Arguments.of("one adult", new TicketRequest[]{adults(1)}, "25.99", 1L),
        Arguments.of("adult, child and infant",
            new TicketRequest[]{adults(1), children(1), infants(1)}, "43.49", 2L),
        Arguments.of("family of two adults, two children, one infant",
            new TicketRequest[]{adults(2), children(2), infants(1)}, "86.98", 4L),
        Arguments.of("duplicate types are aggregated",
            new TicketRequest[]{adults(1), adults(2), children(1)}, "95.47", 4L),
        Arguments.of("zero-count requests are ignored",
            new TicketRequest[]{adults(1), children(0), infants(0)}, "25.99", 1L),
        Arguments.of("infants equal to adults", new TicketRequest[]{adults(3), infants(3)},
            "77.97", 3L),
        Arguments.of("exactly 25 adults", new TicketRequest[]{adults(25)}, "649.75", 25L),
        Arguments.of("25 tickets including infants",
            new TicketRequest[]{adults(10), children(5), infants(10)}, "347.40", 15L));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validPurchases")
  void givenValidPurchase_whenPurchaseTickets_thenPaysThenReservesAndConfirms(
      final String description, final TicketRequest[] requests, final String expectedCost,
      final long expectedSeats) {

    final BookingConfirmation confirmation = service.purchaseTickets(ACCOUNT_ID, requests);

    final BigDecimal cost = new BigDecimal(expectedCost);
    final InOrder inOrder = inOrder(paymentService, seatReservationService);
    inOrder.verify(paymentService).debitAccount(ACCOUNT_ID, cost);
    inOrder.verify(seatReservationService).reserveSeats(ACCOUNT_ID, expectedSeats);
    inOrder.verifyNoMoreInteractions();

    assertEquals(new BookingConfirmation(ACCOUNT_ID, expectedSeats, cost), confirmation);
  }

  static Stream<Arguments> invalidPurchases() {
    return Stream.of(
        Arguments.of("null account", null, new TicketRequest[]{adults(1)},
            TicketPurchaseValidator.INVALID_ACCOUNT),
        Arguments.of("zero account", 0L, new TicketRequest[]{adults(1)},
            TicketPurchaseValidator.INVALID_ACCOUNT),
        Arguments.of("negative account", -1L, new TicketRequest[]{adults(1)},
            TicketPurchaseValidator.INVALID_ACCOUNT),
        Arguments.of("null request array", ACCOUNT_ID, null,
            TicketPurchaseValidator.NO_TICKET_REQUESTS),
        Arguments.of("empty request array", ACCOUNT_ID, new TicketRequest[]{},
            TicketPurchaseValidator.NO_TICKET_REQUESTS),
        Arguments.of("null request element", ACCOUNT_ID, new TicketRequest[]{adults(1), null},
            TicketPurchaseValidator.NULL_TICKET_REQUEST),
        Arguments.of("null ticket type", ACCOUNT_ID, new TicketRequest[]{new TicketRequest(null, 1)},
            TicketPurchaseValidator.MISSING_TICKET_TYPE),
        Arguments.of("negative count", ACCOUNT_ID, new TicketRequest[]{adults(2), children(-1)},
            TicketPurchaseValidator.NEGATIVE_TICKET_COUNT),
        Arguments.of("all counts zero", ACCOUNT_ID, new TicketRequest[]{adults(0)},
            TicketPurchaseValidator.NO_TICKETS),
        Arguments.of("26 adults", ACCOUNT_ID, new TicketRequest[]{adults(26)},
            TicketPurchaseValidator.TOO_MANY_TICKETS),
        Arguments.of("26 tickets counting infants", ACCOUNT_ID,
            new TicketRequest[]{adults(13), infants(13)}, TicketPurchaseValidator.TOO_MANY_TICKETS),
        Arguments.of("huge counts do not overflow", ACCOUNT_ID,
            new TicketRequest[]{adults(Integer.MAX_VALUE), adults(Integer.MAX_VALUE)},
            TicketPurchaseValidator.TOO_MANY_TICKETS),
        Arguments.of("child without adult", ACCOUNT_ID, new TicketRequest[]{children(1)},
            TicketPurchaseValidator.NO_ADULT),
        Arguments.of("infant without adult", ACCOUNT_ID, new TicketRequest[]{infants(1)},
            TicketPurchaseValidator.NO_ADULT),
        Arguments.of("child with zero adults", ACCOUNT_ID,
            new TicketRequest[]{adults(0), children(2)}, TicketPurchaseValidator.NO_ADULT),
        Arguments.of("more infants than adults", ACCOUNT_ID,
            new TicketRequest[]{adults(1), infants(2)}, TicketPurchaseValidator.TOO_MANY_INFANTS));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidPurchases")
  void givenInvalidPurchase_whenPurchaseTickets_thenRejectedWithoutPaymentOrReservation(
      final String description, final Long accountId, final TicketRequest[] requests,
      final String expectedMessage) {

    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> service.purchaseTickets(accountId, requests));

    assertEquals(expectedMessage, exception.getMessage());
    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenNoVarargs_whenPurchaseTickets_thenRejected() {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> service.purchaseTickets(ACCOUNT_ID));

    assertEquals(TicketPurchaseValidator.NO_TICKET_REQUESTS, exception.getMessage());
    verifyNoInteractions(paymentService, seatReservationService);
  }

  private static TicketRequest adults(final int count) {
    return new TicketRequest(ADULT, count);
  }

  private static TicketRequest children(final int count) {
    return new TicketRequest(CHILD, count);
  }

  private static TicketRequest infants(final int count) {
    return new TicketRequest(INFANT, count);
  }
}

package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.booking.TicketOrder;
import uk.gov.dwp.engineering.recruitment.booking.TicketPurchaseValidator;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  public CinemaTicketsServiceImpl(PaymentService paymentService,
      SeatReservationService seatReservationService) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
  }

  /**
   * Validates the purchase, takes payment and reserves seats.
   *
   * <p>The whole request is validated before either external service is called, so an invalid
   * request never results in a payment or a reservation.
   */
  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    TicketPurchaseValidator.validateAccountId(accountId);
    TicketPurchaseValidator.validateTicketRequests(ticketRequests);

    final TicketOrder order = TicketOrder.from(ticketRequests);
    TicketPurchaseValidator.validateOrder(order);

    final BigDecimal totalCost = order.totalCost();
    final long seatCount = order.seatsRequired();

    paymentService.debitAccount(accountId, totalCost);
    seatReservationService.reserveSeats(accountId, seatCount);

    return new BookingConfirmation(accountId, seatCount, totalCost);
  }
}

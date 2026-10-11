package uk.gov.dwp.engineering.recruitment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.web.BookingRequest;

@RestController
@RequestMapping("/cinema")
public class CinemaTicketsController {

  /**
   * The Ticket Booking Service.
   */
  private final CinemaTicketsService cinemaTicketsService;

  public CinemaTicketsController(final CinemaTicketsService cinemaTicketsService) {
    this.cinemaTicketsService = cinemaTicketsService;
  }

  /**
   * Make booking endpoint handler.
   *
   * <p>Returns 201 with a {@link BookingConfirmation} on success. Invalid requests are rejected with
   * 400 and an RFC 9457 problem detail by {@code RestExceptionHandler}.
   *
   * @param bookingRequest the account and tickets to book
   * @return ResponseEntity containing the result of the booking.
   */
  @PostMapping(value = "/bookings", consumes = "application/json", produces = "application/json")
  public ResponseEntity<BookingConfirmation> makeBooking(
      @RequestBody final BookingRequest bookingRequest) {
    return new ResponseEntity<>(cinemaTicketsService.purchaseTickets(bookingRequest.accountId(),
        bookingRequest.toTicketRequests()), HttpStatus.CREATED);
  }

}

package uk.gov.dwp.engineering.recruitment.thirdparty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SeatReservationServiceImpl implements SeatReservationService {

  private final RestClient restClient;

  /**
   * The URL is injected through the constructor rather than a field. A {@code @Value} field is
   * only populated after construction, so it would still be {@code null} when the client is built.
   */
  public SeatReservationServiceImpl(final RestClient.Builder builder,
      @Value("${reservation-service.url}") final String reservationServiceUrl) {
    this.restClient = builder.baseUrl(reservationServiceUrl).build();
  }

  @Override
  public ResponseEntity<String> reserveSeats(final Long accountId, final Long seatCount) {
    // Real implementation omitted for initial iteration(s).
    return new ResponseEntity<>("TODO", HttpStatus.OK);
  }
}

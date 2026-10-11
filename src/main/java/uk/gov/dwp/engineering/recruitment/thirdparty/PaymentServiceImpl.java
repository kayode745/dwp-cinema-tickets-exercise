package uk.gov.dwp.engineering.recruitment.thirdparty;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentServiceImpl implements PaymentService {

  private final RestClient restClient;

  /**
   * The URL is injected through the constructor rather than a field. A {@code @Value} field is
   * only populated after construction, so it would still be {@code null} when the client is built.
   */
  public PaymentServiceImpl(final RestClient.Builder builder,
      @Value("${payment-service.url}") final String paymentServiceUrl) {
    this.restClient = builder.baseUrl(paymentServiceUrl).build();
  }

  @Override
  public ResponseEntity<String> debitAccount(final Long accountId, final BigDecimal amount) {
    // Real implementation omitted for initial iteration(s).
    return new ResponseEntity<>("TODO", HttpStatus.OK);
  }
}

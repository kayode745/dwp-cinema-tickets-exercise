# Cinema Tickets Service

My implementation of the DWP cinema tickets exercise: a Spring Boot service that validates a
ticket purchase, takes payment through `PaymentService` and reserves seats through
`SeatReservationService`.

## Running the service

mvn verify            
mvn spring-boot:run

```bash
curl -i -X POST http://localhost:8080/cinema/bookings \
  -H 'Content-Type: application/json' \
  -d '{"accountId": 1234, "tickets": [
        {"type": "ADULT", "count": 2},
        {"type": "CHILD", "count": 1},
        {"type": "INFANT", "count": 1}]}'
```

A valid booking returns `201 Created` with the booking confirmation, including the account ID, 
seat count and total cost.

## Error handling

Errors are handled centrally by `RestExceptionHandler` and returned as RFC 9457 problem details.

For example, a booking containing child or infant tickets without an adult ticket returns `400 Bad Request` with a message explaining the validation failure.

Malformed requests return `400`, unsupported content types return `415`, and unexpected errors return `500` with a generic message.

Validation happens before payment and seat reservation, preventing invalid requests from reaching either service.


## How it works

The service validates the whole request first, then takes payment, then reserves seats. A
rejected request never reaches either external service.

- `TicketPurchaseValidator` holds every rule, each with its own error message.
- `TicketOrder` adds up the requests by type and calculates tickets, seats and cost.
- `TicketTypeRules` holds the price and seat rule for each ticket type. It uses exhaustive
  `switch` expressions, so a new ticket type won't compile until it has both.
- `web.BookingRequest` maps the JSON body onto the domain records.

Money is held as `BigDecimal` so totals are exact.

## Assumptions

On top of the assumptions given in the exercise:

1. Infants count towards the 25-ticket limit. An infant ticket is still a ticket, even though
   it's free.
2. There can't be more infants than adults. Each infant sits on an adult's lap, and one adult
   has one lap. There's no limit on children per adult, as the rules don't give one.
3. A purchase must contain at least one ticket. If every count is zero, it's rejected.
4. A count of zero on a single request is allowed and ignored (the OpenAPI schema allows
   `minimum: 0`). Negative counts are rejected.
5. Repeated ticket types are added together, so `ADULT x1` plus `ADULT x2` is three adults.
6. The request uses the OpenAPI field names `tickets` and `count`. The domain records use
   `ticketRequests` and `ticketCount` and can't be changed, so both forms are accepted.
7. A missing `count` is rejected rather than treated as zero, as it's more likely a client
   mistake.
8. Invalid requests return 400, as the OpenAPI contract specifies, instead of the template's
    501.
9. `BookingConfirmation` now also returns `seatCount` and `totalCost`, as in the contract's
   response schema. The original constructor is kept so the change is non-breaking.
10. Payment is taken before seats are reserved, so seats are never held for an unpaid booking.
    The responses from both services aren't checked, since they're assumed always to succeed.

## Tests

`CinemaTicketsControllerTest` runs requests through the full stack with only the two external
services mocked. It covers valid bookings, every rejection rule and malformed input, and checks
that nothing is charged or reserved when a request is rejected. The service and rule classes
also have their own unit tests.

## Configuration fix

`PaymentServiceImpl` and `SeatReservationServiceImpl` previously read their base URLs from 
`@Value` fields inside their constructors. Spring injects these fields after construction, 
so the URLs were `null` when the `RestClient` instances were created. I changed both classes 
to receive the URLs through constructor parameters. Their intended behaviour is unchanged.

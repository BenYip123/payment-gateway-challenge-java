# Payment Gateway Challenge Java

This is the Java version of the Payment Gateway challenge.

## Requirements
- JDK 17
- Docker

## Run Instructions

I have packaged the app into a docker container and updated the docker compose file.

`docker-compose up` will start up both the app and the simulator for easier setup.

To send a valid payment:
```
curl -X POST http://localhost:8090/v1/payment -H "Content-Type: application/json" -d '{"card_number":"4242405343248871","expiry_month":12,"expiry_year":2027,"currency":"GBP","amount":1050,"cvv":"123"}'
```
Get a non existent payment:
```
curl http://localhost:8090/v1/payment/nonexistent-id
```
Post a payment with validation errors:
```
curl -X POST http://localhost:8090/v1/payment -H "Content-Type: application/json" -d '{"card_number":"bad","expiry_month":13,"expiry_year":2027,"currency":"XYZ","amount":-1,"cvv":""}'
```

Other accessible endpoints:

**http://localhost:8090/swagger-ui/index.html**


**http://localhost:8090/actuator/health**


**http://localhost:8090/actuator/prometheus**

## Testing
All tests can be run using:
```
./gradlew test
```

PaymentGatewayControllerTest contains some integration style tests that require the simulator to be running. Some of the simpler tests that check validation run without the simulator.
PaymentGatewayServiceTest contain unit tests that try to capture more of the individual functionality.
Manual testing was done using the curl commands mentioned earlier, capturing more of the E2E flow of the system.

## Assumptions
Some assumptions I considered:
- The card number is valid as long as it passes the internal validation
- If the bank returns a 503, we just pass it through the system and no retry logic.
- Hardcoded supported currencies of USD, GBP, EUR (requirements mention max 3)
- No API authentication
- No idempotency for the scale of this project
- In-memory payment storage

## Key Design Considerations

- Created custom annotations (`@ValidExpiryDate`, `@SupportedCurrency`) with bean validation for validating with multiple field (expiryMonth and expiryYear needed for expiryDate)
- Rejected payments return 200 — the bank is never contacted for invalid requests. No retry.
- Use adapter pattern with AcquiringBankClient and RestTemplateAcquiringBankClient to allow easily swappable implementations and easier mocking for tests.
- PaymentNotFoundException caught by CommonExceptionHandler. If the bank is unavailable we map it to a 502 Bad Gateway error.
- Use API versioning to allow for future API changes without breaking the previous version.
- Logging at various levels. Log level defaults to INFO level and can be changed in `application.properties`.
- Actuator provides health and prometheus endpoints for auto-collected HTTP/JVM metrics.
- Multi-stage DockerFile to allow Docker Compose to start both app and simulator.

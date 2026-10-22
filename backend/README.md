# Transaction Management API

A Java Spring Boot REST API for creating and managing transactions safely.

This project focuses on production-oriented backend practices including request validation, idempotent operations, explicit state transitions, optimistic locking and automated testing.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA / Hibernate
- H2 database for local development and testing
- PostgreSQL driver
- Jakarta Bean Validation
- JUnit 5
- Mockito
- MockMvc

## Features

- Create a transaction
- Retrieve a transaction by ID
- Filter transactions by customer ID and optional status
- Complete or fail a pending transaction
- Validate incoming requests
- Return consistent error responses
- Prevent duplicate transaction creation with idempotency keys
- Prevent conflicting concurrent updates with optimistic locking
- Unit and integration test coverage

## API Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/transactions` | Create a transaction |
| `GET` | `/transactions/{id}` | Retrieve a transaction by ID |
| `GET` | `/transactions?customerId={customerId}&status={status}` | Filter transactions |
| `POST` | `/transactions/{id}/complete` | Mark a pending transaction as completed |
| `POST` | `/transactions/{id}/fail` | Mark a pending transaction as failed |

## Idempotent Transaction Creation

Creating a transaction requires an `Idempotency-Key` request header.

- A new key creates a transaction and returns `201 Created`.
- Retrying a request with the same key and same payload returns the original transaction with `200 OK`.
- Reusing a key with a different payload returns `409 Conflict`.
- A database unique constraint protects against duplicate creates during concurrent requests.

## Transaction State Management

Transactions begin in the `PENDING` state.

```text
PENDING → COMPLETED
PENDING → FAILED
```

A completed or failed transaction cannot be updated again.

The API uses JPA `@Version` optimistic locking to detect concurrent updates and prevent silent data overwrites.

## Example Request

Create a transaction:

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 2c98d437-6d34-4a40-bc20-807c62099663" \
  -d '{
    "customerId": "customer-123",
    "description": "Restaurant payment",
    "amount": 45.50,
    "currency": "AUD"
  }'
```

## Run Locally

Start the application:

```bash
./mvnw spring-boot:run
```

The API runs at:

```text
http://localhost:8080
```

## Run Tests

```bash
./mvnw test
```

The test suite covers request validation, idempotency-key reuse, concurrent create handling, transaction state transitions and optimistic-locking behaviour.
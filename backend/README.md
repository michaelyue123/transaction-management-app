# Transaction Management API

A Java 21 and Spring Boot REST API for creating and managing transactions. It supports idempotent creation, customer-scoped search with optional status filtering, pagination and sorting, and guarded transaction state changes.

## Tech Stack

- Java 21
- Spring Boot 4.1.2
- Spring Web MVC and Spring Data JPA
- H2 database for local development and tests
- PostgreSQL JDBC driver
- Jakarta Bean Validation
- JUnit 5, Mockito and MockMvc

## Features

- Validate transaction creation requests.
- Make transaction creation idempotent, including safe handling of concurrent duplicate requests.
- Find transactions by ID or list them by customer, with optional status filtering.
- Paginate and sort transaction results.
- Transition pending transactions to completed or failed.
- Reject invalid state changes and report concurrent update conflicts.
- Return structured validation and API error responses.

## API

The local API base URL is `http://localhost:8080`.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/transactions` | Create a transaction (requires an `Idempotency-Key` header) |
| `GET` | `/transactions/{id}` | Retrieve a transaction by ID |
| `GET` | `/transactions?customerId={customerId}` | List a customer's transactions |
| `GET` | `/transactions?customerId={customerId}&status={status}` | Filter a customer's transactions by status |
| `POST` | `/transactions/{id}/complete` | Complete a pending transaction |
| `POST` | `/transactions/{id}/fail` | Fail a pending transaction |

Statuses are `PENDING`, `COMPLETED` and `FAILED`. New transactions start as `PENDING`; only pending transactions can be completed or failed.

### Search, pagination and sorting

The list endpoint requires `customerId` and accepts an optional `status`. Pagination uses zero-based `page` and `size` parameters. The default page size is 20 and the maximum is 100. By default, results are sorted by `createdAt` and then `id`, both descending.

Supported sort fields are `createdAt`, `amount`, `id` and `description`. Use Spring's `sort` query parameter syntax:

```text
GET /transactions?customerId=customer-123&status=PENDING&page=0&size=10&sort=amount,desc
```

The list response contains a `transactions` array and page metadata:

```json
{
  "transactions": [],
  "currentPage": 0,
  "pageSize": 10,
  "totalItems": 0,
  "totalPages": 0
}
```

### Create a transaction

The request requires non-blank `customerId`, `description` and `currency`, a positive `amount`, and a three-letter uppercase currency code.

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

The API responds with the transaction fields `id`, `customerId`, `description`, `amount`, `currency`, `createdAt`, `status` and `version`.

### Idempotency and errors

- A new idempotency key and valid request create a transaction and return `201 Created`.
- Repeating the same request with the same key returns the existing transaction with `200 OK`.
- Reusing a key with a different request returns `409 Conflict`.
- Request validation errors return `400 Bad Request`; missing transactions return `404 Not Found`.
- Invalid state transitions and optimistic-lock conflicts return `409 Conflict`.

The idempotency key is protected by a database unique constraint. Transaction updates use JPA optimistic locking to detect conflicting concurrent changes.

## Run locally

From this directory, start the API with:

```bash
./mvnw spring-boot:run
```

## Run tests

```bash
./mvnw test
```

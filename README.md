# Transaction Management App

A full-stack transaction management application built with a React frontend and a Java Spring Boot backend.

The project demonstrates production-oriented backend engineering practices, including REST API design, request validation, idempotent operations, concurrency control, transaction state management and automated testing.

## Features

- Create, retrieve and filter transactions
- Complete or fail pending transactions
- Idempotent transaction creation using an `Idempotency-Key`
- Protection against duplicate concurrent create requests
- Optimistic locking for concurrent transaction updates
- Explicit transaction state transitions
- Validation and consistent API error responses
- Automated unit and integration tests

## Tech Stack

| Area | Technologies |
| --- | --- |
| Frontend | React, TypeScript, Vite |
| Backend | Java 21, Spring Boot, Spring Data JPA |
| Database | H2 for local development/testing, PostgreSQL-ready |
| Testing | JUnit 5, Mockito, MockMvc |
| API Style | REST / JSON |

## Project Structure

```text
transaction-management-app/
├── frontend/       # React and TypeScript client
├── backend/        # Java Spring Boot REST API
├── README.md
└── .gitignore
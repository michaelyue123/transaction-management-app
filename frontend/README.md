```md
# Transaction Management Frontend

A React and TypeScript frontend for the Transaction Management App.

The application provides a user interface for creating, viewing, filtering and updating transactions through the Java Spring Boot REST API.

## Tech Stack

- React
- TypeScript
- Vite
- Fetch API

## Features

- Create transactions
- Generate and send an idempotency key when creating a transaction
- View transaction details and status
- Filter transactions by customer ID and status
- Complete or fail pending transactions
- Display validation, not-found and conflict errors returned by the backend

## Run Locally

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The application is available at:

```text
http://localhost:5173
```

## Backend Requirement

Start the backend API before using the frontend:

```bash
cd ../backend
./mvnw spring-boot:run
```

The frontend communicates with the backend at:

```text
http://localhost:8080
```

The backend enables CORS for the local Vite development server.

## Related Documentation

See the root README for the full application overview and the backend README for API design and endpoint details.
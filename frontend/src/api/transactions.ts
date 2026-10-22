import type { CreateTransactionRequest, Transaction, TransactionStatus } from "../types/transaction";

const baseURL = "http://localhost:8080/transactions";

export type TransactionAction = "complete" | "fail";

export async function fetchTransactions(
    customerId: string, 
    status?: TransactionStatus
): Promise<Transaction[]> {
    const params = new URLSearchParams({
        customerId,
    });

    if(status) {
        params.set("status", status);
    }

    const response = await fetch(
        `${baseURL}?${params.toString()}`
    );

    checkResponse(response);

    return response.json();
}

export async function createTransaction(
    request: CreateTransactionRequest
): Promise<Transaction> {
    const response = await fetch(
        baseURL, 
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Idempotency-Key": crypto.randomUUID(),
            },
            body: JSON.stringify(request),
        }
    );

    checkResponse(response);

    return response.json();
}

export async function updateTransaction(
    id: string, 
    action: TransactionAction
): Promise<Transaction> {
    const response = await fetch(
        `${baseURL}/${id}/${action}`,
        {
            method: "POST"
        }
    );

    checkResponse(response);

    return response.json();
}

function checkResponse(response: Response) {
    if(!response.ok) {
        throw new Error(`Request failed:: ${response.status}`);
    }
}
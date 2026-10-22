export interface Transaction {
    id: string;
    customerId: string;
    description: string;
    amount: number;
    currency: string;
    status: TransactionStatus;
    createdAt: string;
} 

export interface CreateTransactionRequest {
    customerId: string;
    description: string;
    amount: number;
    currency: string;
}

export type TransactionStatus = 
| "PENDING"
| "COMPLETED"
| "FAILED"
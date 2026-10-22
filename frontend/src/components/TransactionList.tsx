import type { TransactionAction } from "../api/transactions";
import type { Transaction } from "../types/transaction";
import "./TransactionList.css";

interface TransactionListProps {
    transactions: Transaction[];
    onUpdateStatus: (id: string, action: TransactionAction) => void;
}

export function TransactionList({ 
    transactions, 
    onUpdateStatus
} : TransactionListProps) {

    function simplifyCreatedAt(isoString: string): string {
        const date = new Date(isoString);

        return date.toLocaleDateString('en-AU', {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    }
    return (
        <div className="transaction-table-container">
            <table className="transaction-table">
                <thead>
                    <tr>
                        <th>Description</th>
                        <th>Amount</th>
                        <th>Created At</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>

                <tbody>
                    {transactions.map((transaction) => (
                        <tr key={transaction.id}>
                            <td>{transaction.description}</td>
                            <td>
                                {transaction.amount} {transaction.currency}
                            </td>
                            <td>
                                {simplifyCreatedAt(transaction.createdAt)}
                            </td>
                            <td>{transaction.status}</td>
                            <td>
                                <span className="transaction-actions">
                                    {transaction.status === "PENDING" && (
                                        <>
                                        <button onClick={() => onUpdateStatus(transaction.id, "complete")}>
                                            Complete
                                        </button>

                                        <button onClick={() => onUpdateStatus(transaction.id, "fail")}>
                                            Fail
                                        </button>
                                        </>
                                    )}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    )
}
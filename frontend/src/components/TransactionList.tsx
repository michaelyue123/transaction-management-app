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
                            <td className="description-cell">{transaction.description}</td>
                            <td className="amount-cell">
                                {transaction.amount} {transaction.currency}
                            </td>
                            <td>
                                {simplifyCreatedAt(transaction.createdAt)}
                            </td>
                            <td>
                                <span className={`status-badge status-${transaction.status.toLowerCase()}`}>
                                    <span className="status-dot" aria-hidden="true" />
                                    {transaction.status}
                                </span>
                            </td>
                            <td>
                                <span className="transaction-actions">
                                    {transaction.status === "PENDING" && (
                                        <>
                                        <button
                                            className="button button-success"
                                            type="button"
                                            onClick={() => onUpdateStatus(transaction.id, "complete")}
                                        >
                                            Complete
                                        </button>

                                        <button
                                            className="button button-danger"
                                            type="button"
                                            onClick={() => onUpdateStatus(transaction.id, "fail")}
                                        >
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
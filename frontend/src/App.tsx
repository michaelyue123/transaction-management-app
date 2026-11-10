import { useEffect, useMemo, useState } from "react"
import { TransactionList } from "./components/TransactionList"
import type { CreateTransactionRequest, Transaction, TransactionStatus } from "./types/transaction"
import { TransactionForm } from "./components/TransactionForm";
import "./App.css";
import { createTransaction, fetchTransactions, updateTransaction, type TransactionAction } from "./api/transactions";

type StatusFilter = 
| "ALL"
| TransactionStatus

type TransactionSortBy =
| "Newest first"
| "Oldest first"
| "Amount: high to low"
| "Amount: low to high"

const sortOptions: Record<TransactionSortBy, string> = {
  "Newest first": "createdAt,desc",
  "Oldest first": "createdAt,asc",
  "Amount: high to low": "amount,desc",
  "Amount: low to high": "amount,asc"
};

function App() {
  const [transactions, setTransactions] = useState<Transaction[]>([]); 
  // customerId input and status input
  const [statusInput, setStatusInput] = useState<StatusFilter>("ALL");
  const [customerIdInput, setCustomerIdInput] = useState<string>("");
  // selected customerId and status used in search
  const [selectedCustomerId, setSelectedCustomerId] = useState<string>("");
  const [selectedStatus, setSelectedStatus] = useState<StatusFilter>("ALL");

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<TransactionSortBy>("Newest first");

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  
  useEffect(() => {
    if(!selectedCustomerId) {
      return;
    }

    async function loadTransactions() {
      try {
        setIsLoading(true);
        setError(null);

        const status =
          selectedStatus === "ALL"
            ? undefined
            : selectedStatus;
  
        const data = await fetchTransactions(selectedCustomerId, status, page, 5, sortOptions[sortBy]);
  
        setTransactions(data.transactions);
        setTotalPages(data.totalPages);
      } catch (error: unknown) {
        if(error instanceof Error) {
          setError(error.message);
        } else {
          setError("An unexpected error occurred");
        }
      } finally {
        setIsLoading(false);
      }
    }

    loadTransactions();
  }, [selectedCustomerId, selectedStatus, page, sortBy]);

  const handleCreateTransaction = async (request: CreateTransactionRequest) => {
    const createdTransaction: Transaction = 
      await createTransaction(request);

    setTransactions((currentTransaction) => [
      ...currentTransaction,
      createdTransaction
    ])
  }

  const handleTransaction = async (id: string, action: TransactionAction) => {
    const updatedTransaction: Transaction = 
      await updateTransaction(id, action);

    setTransactions((currentTransaction) => {
      if(selectedStatus !== "ALL" && updatedTransaction.status !== selectedStatus) {
        return currentTransaction.filter(
          (transaction) => transaction.id !== id
        );
      }

      return currentTransaction.map((transaction) => 
        transaction.id === id ? updatedTransaction : transaction);
    });
  }  

  return (
    <div style={{ margin: "30px" }}>
      <TransactionForm onCreate={handleCreateTransaction} />

      <section className="search-container">
        <h2>Search Transaction</h2>

        <form
          onSubmit={(event) => {
            event.preventDefault();

            const customerId = customerIdInput.trim();

            if(!customerId) {
              return;
            }

            setSelectedCustomerId(customerId);
            setSelectedStatus(statusInput);
          }}
        >
          <label htmlFor="search-customer-id">
            Customer ID
          </label>
          {" "}
          <input 
            id="search-customer-id"
            autoComplete="off"
            value={customerIdInput}
            onChange={(event) => setCustomerIdInput(event.target.value)}
          />
          {" "}
          <label htmlFor="status-filter">Status</label>
          {" "}
          <select
            id="status-filter"
            value={statusInput}
            onChange={(event) => setStatusInput(event.target.value as StatusFilter)}
          >
            <option value="ALL">ALL</option>
            <option value="PENDING">PENDING</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="FAILED">FAILED</option>
          </select>
          {" "}
          <button type="submit">Search</button>
        </form>
      </section>

      <div className="transaction-container">
        <div className="header-actions-container">
          <h2 style={{ margin: 0, textAlign: "center"}}>Transactions</h2>

          <div className="sort-controls">
            <label htmlFor="sort-by" className="dark-label">
              Sort By
            </label>
            
            <select
              id="sort-by"
              value={sortBy}
              onChange={(event) => {
                setPage(0);
                setSortBy(event.target.value as TransactionSortBy);
              }}
              className="dark-select-dropdown"
            >
              <option value="Newest first">Newest first</option>
              <option value="Oldest first">Oldest first</option>
              <option value="Amount: high to low">Amount: high to low</option>
              <option value="Amount: low to high">Amount: low to high</option>
            </select>
          </div>
        </div>

        {isLoading && <p>Loading transactions...</p>}

        {error && (
          <p className="error-message">
            {error}
          </p>
        )}

        {!isLoading && !error && transactions.length === 0 && (
          <p>No transactions found.</p>
        )}

        {!isLoading && !error && transactions.length > 0 && (
          <TransactionList
            transactions={transactions}
            onUpdateStatus={handleTransaction}
          />
        )}

        {selectedCustomerId && !isLoading && !error && totalPages > 0 && (
          <div className="pagination">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </button>

            <span>
              {" "}Page {page + 1} of {totalPages}{" "}
            </span>

            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default App

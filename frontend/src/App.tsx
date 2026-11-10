import { useEffect, useState } from "react"
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

  // Refresh upon submission
  const [refreshCount, setRefreshCount] = useState(0);
  
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
  }, [selectedCustomerId, selectedStatus, page, sortBy, refreshCount]);

  const handleCreateTransaction = async (
    request: CreateTransactionRequest
  ) => {
    await createTransaction(request);

    // 只有新交易属于当前搜索的用户时，才需要刷新列表
    if (request.customerId === selectedCustomerId) {
      setRefreshCount((count) => count + 1);
    }
  };

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
    <main className="app-shell">
      <header className="page-header">
        <div className="brand-mark" aria-hidden="true">T</div>
        <div>
          <p className="eyebrow">TRANSACTION MANAGEMENT</p>
          <h1>Financial dashboard</h1>
          <p className="page-description">
            Create, search, and manage customer transactions in one place.
          </p>
        </div>
      </header>

      <TransactionForm onCreate={handleCreateTransaction} />

      <section className="search-container">
        <div className="section-heading">
          <div>
            <p className="section-kicker">TRANSACTION HISTORY</p>
            <h2>Find transactions</h2>
          </div>
          <p className="section-hint">Search by customer and narrow by status.</p>
        </div>

        <form
          className="search-form"
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
          <div className="search-field customer-search-field">
            <label htmlFor="search-customer-id">Customer ID</label>
            <input
              id="search-customer-id"
              autoComplete="off"
              placeholder="Enter a customer ID"
              value={customerIdInput}
              onChange={(event) => setCustomerIdInput(event.target.value)}
            />
          </div>
          <div className="search-field">
            <label htmlFor="status-filter">Status</label>
            <select
              id="status-filter"
              value={statusInput}
              onChange={(event) => setStatusInput(event.target.value as StatusFilter)}
            >
              <option value="ALL">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>
          <button className="button button-primary search-button" type="submit">Search</button>
        </form>
      </section>

      <div className="transaction-container">
        <div className="header-actions-container">
          <div>
            <p className="section-kicker">OVERVIEW</p>
            <h2>Transactions</h2>
          </div>

          <div className="sort-controls">
            <label htmlFor="sort-by" className="dark-label">Sort by</label>
            
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

        {isLoading && <p className="list-message" role="status">Loading transactions...</p>}

        {error && (
          <p className="error-message list-message" role="alert">
            {error}
          </p>
        )}

        {!isLoading && !error && transactions.length === 0 && (
          <p className="list-message">No transactions found.</p>
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
              className="button button-secondary"
              type="button"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </button>

            <span className="page-indicator">Page {page + 1} of {totalPages}</span>

            <button
              className="button button-secondary"
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </main>
  )
}

export default App

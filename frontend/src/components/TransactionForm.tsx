import { useState } from "react";
import type { CreateTransactionRequest } from "../types/transaction";
import { type SubmitEvent } from "react"; 
import "./TransactionForm.css";

interface TransactionFormProps {
    onCreate: (request: CreateTransactionRequest) => Promise<void>;
}

export function TransactionForm({ onCreate }: TransactionFormProps) {
    const [description, setDescription] = useState<string>("");
    const [amount, setAmount] = useState<string>("");
    const [currency, setCurrency] = useState<string>("AUD");
    const [customerId, setCustomerId] = useState<string>("");
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();

        // convert string to number
        const parsedAmount = Number(amount); 

        // Empty string is not allowed
        if(!description.trim() || !Number.isFinite(parsedAmount) || parsedAmount <= 0) {
            setSubmitError("Please enter valid transaction details");
            return;
        }

        try {
            setIsSubmitting(true);
            setSubmitError(null);

            const request: CreateTransactionRequest = {
                customerId,
                description,
                amount: parsedAmount,
                currency,
            }
    
            await onCreate(request);
    
            setDescription("");
            setAmount("");
            setCurrency("AUD");
            setCustomerId("");
        } catch (error: unknown) {
            if(error instanceof Error) {
                setSubmitError(error.message);
            } else {
                setSubmitError("Failed to create transaction");
            }
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <form className="ui-container" onSubmit={handleSubmit}>
            <div className="form-heading">
                <div>
                    <p className="section-kicker">NEW ENTRY</p>
                    <h2>Create transaction</h2>
                </div>
                <span className="form-note">All fields are required</span>
            </div>

            <div className="form-fields">
                <div className="form-field">
                    <label htmlFor="customerId">Customer ID</label>
                    <input
                        id="customerId"
                        type="text"
                        placeholder="Enter customer ID"
                        autoComplete="off"
                        required
                        value={customerId}
                        onChange={(event) => setCustomerId(event.target.value)}
                    />
                </div>

                <div className="form-field form-field-description">
                    <label htmlFor="description">Description</label>
                    <input
                        id="description"
                        type="text"
                        placeholder="e.g. Monthly subscription"
                        required
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="amount">Amount</label>
                    <input
                        id="amount"
                        type="number"
                        min="0.01"
                        step="0.01"
                        placeholder="0.00"
                        required
                        value={amount}
                        onChange={(event) => setAmount(event.target.value)}
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="currency">Currency</label>
                    <select
                        id="currency"
                        value={currency}
                        required
                        onChange={(event) => setCurrency(event.target.value)}
                    >
                        <option value="AUD">AUD</option>
                        <option value="USD">USD</option>
                        <option value="CNY">CNY</option>
                    </select>
                </div>

                <button
                    className="button button-primary create-button"
                    type="submit"
                    disabled={isSubmitting}
                >
                    {isSubmitting ? "Creating..." : "Create transaction"}
                </button>
            </div>
            {submitError && (
                <p className="error-message form-error" role="alert">
                    {submitError}
                </p>
            )}
        </form>
    )
}
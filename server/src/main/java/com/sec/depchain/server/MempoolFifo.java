package com.sec.depchain.server;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sec.depchain.common.Transaction;


public class MempoolFifo {
    private final List<Transaction> transactions;

    public MempoolFifo() {
        this.transactions = Collections.synchronizedList(new LinkedList<>());
    }

    // Adds transaction to the end (FIFO)
    public void addTransactionToMempool(Transaction tx) {
        if (!transactions.contains(tx)) {
            transactions.add(tx);
        }
    }

    // remove transaction from the mempool
    public void removeTransactionFromMempool(Transaction tx) {
        // System.out.println("Transaction to remove: " );
        // tx.printTransaction();
        synchronized (transactions) {
            Iterator<Transaction> iterator = transactions.iterator();
            while (iterator.hasNext()) {
                Transaction current = iterator.next();
                // System.out.println("Transaction in mempool: " );
                // current.printTransaction();
                if (current.computeTxHash().equals(tx.computeTxHash())) {
                    // System.out.println("Match found");
                    iterator.remove();
                    break;
                }
            }
        }
    }

    // Gets all transactions 
    public List<Transaction> getTransactions() {
        return transactions;
    }

    // Check if the list contains a transaction
    public boolean containsTransaction(Transaction tx) {
        return transactions.contains(tx);
    }

    // FIFO: Remove and return the first transaction
    public Transaction pollTransaction() {
        synchronized (transactions) {
            if (!transactions.isEmpty()) {
                return transactions.remove(0);
            }
        }
        return null;
    }

    public int size() {
        return transactions.size();
    }

    public void clear() {
        transactions.clear();
    }
}

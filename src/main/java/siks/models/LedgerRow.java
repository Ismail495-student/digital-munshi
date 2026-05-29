package siks.models;

import java.time.LocalDateTime;

public class LedgerRow {
    private final int id;
    private final String type; // "INVOICE" or "VOUCHER"
    private final LocalDateTime dateTime;
    private final double beforePayment;
    private final double payment;
    private final double remaining;

    public LedgerRow(int id, String type, LocalDateTime dateTime, double beforePayment, double payment, double remaining) {
        this.id = id;
        this.type = type;
        this.dateTime = dateTime;
        this.beforePayment = beforePayment;
        this.payment = payment;
        this.remaining = remaining;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public LocalDateTime getDateTime() { return dateTime; }
    public double getBeforePayment() { return beforePayment; }
    public double getPayment() { return payment; }
    public double getRemaining() { return remaining; }
}

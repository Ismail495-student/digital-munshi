package siks.models;

public class Invoice {
    private String invoiceId; // displayed SI0001 / PI0001
    private String type; // "SALE" or "PURCHASE"
    private String partyName;
    private double totalBill;
    private double payment;
    private double remaining;
    private String dateTime;

    public Invoice(String invoiceId, String type, String partyName, double totalBill, double payment, double remaining, String dateTime) {
        this.invoiceId = invoiceId;
        this.type = type;
        this.partyName = partyName;
        this.totalBill = totalBill;
        this.payment = payment;
        this.remaining = remaining;
        this.dateTime = dateTime;
    }

    // getters
    public String getInvoiceId() { return invoiceId; }
    public String getType() { return type; }
    public String getPartyName() { return partyName; }
    public double getTotalBill() { return totalBill; }
    public double getPayment() { return payment; }
    public double getRemaining() { return remaining; }
    public String getDateTime() { return dateTime; }
}

package siks.models;

public class ItemCheckResult {
    private String invoiceId;
    private String itemName;
    private double currentStock;

    public ItemCheckResult(String invoiceId, String itemName, double currentStock) {
        this.invoiceId = invoiceId;
        this.itemName = itemName;
        this.currentStock = currentStock;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getItemName() { return itemName; }
    public double getCurrentStock() { return currentStock; }
}
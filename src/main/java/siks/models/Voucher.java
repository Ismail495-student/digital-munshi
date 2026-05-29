package siks.models;

public class Voucher {
    private String voucherId;
    private String type; // Customer or Supplier
    private String name;
    private String dateTime;
    private double amount;
    private String description;

    public Voucher(String voucherId, String type, String name, String dateTime, double amount, String description) {
        this.voucherId = voucherId;
        this.type = type;
        this.name = name;
        this.dateTime = dateTime;
        this.amount = amount;
        this.description = description;
    }

    public String getVoucherId() { return voucherId; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDateTime() { return dateTime; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}

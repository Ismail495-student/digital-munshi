package siks.models;

public class Supplier {
    private String id; // SU001 format
    private String name;
    private String printName;
    private String phone;
    private String address;
    private String company;
    private String category;
    private String lastBilled;
    private double prevBalance;

    public Supplier(String id, String name, String printName, String phone, String address,
                    String company, String category, String lastBilled, double prevBalance) {
        this.id = id;
        this.name = name;
        this.printName = printName;
        this.phone = phone;
        this.address = address;
        this.company = company;
        this.category = category;
        this.lastBilled = lastBilled;
        this.prevBalance = prevBalance;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getPrintName() { return printName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCompany() { return company; }
    public String getCategory() { return category; }
    public String getLastBilled() { return lastBilled; }
    public double getPrevBalance() { return prevBalance; }
}

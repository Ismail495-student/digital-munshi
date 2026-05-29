package siks.models;

public class Customer {
    private String id;
    private String name;
    private String printName;
    private String phone;
    private String address;
    private String category; // Retailer / Wholesaler
    private String lastBilled; // "X days ago"
    private double prevBalance;

    public Customer(String id, String name, String printName, String phone, String address,
                    String category, String lastBilled, double prevBalance) {
        this.id = id;
        this.name = name;
        this.printName = printName;
        this.phone = phone;
        this.address = address;
        this.category = category;
        this.lastBilled = lastBilled;
        this.prevBalance = prevBalance;
    }
    public Customer(String name,String phone,String lastBilled, double prevBalance){
        this.name=name;
        this.phone=phone;
        this.lastBilled=lastBilled;
        this.prevBalance=prevBalance;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrintName() { return printName; }
    public void setPrintName(String printName) { this.printName = printName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLastBilled() { return lastBilled; }
    public void setLastBilled(String lastBilled) { this.lastBilled = lastBilled; }

    public double getPrevBalance() { return prevBalance; }
    public void setPrevBalance(double prevBalance) { this.prevBalance = prevBalance; }
}

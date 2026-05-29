package siks.models;

public class Item {
    private String id;
    private String name;
    private String printName;
    private double purchaseRate;
    private double retailRate;
    private double wholesaleRate;
    private int piecesPerCarton;
    private String itemCategory;
    private double stockQuantity; // in pieces
    private String company;

    public Item(String id, String name, String printName, double purchaseRate, double retailRate, double wholesaleRate,
                int piecesPerCarton, String itemCategory, double stockQuantity, String company) {
        this.id = id;
        this.name = name;
        this.printName = printName;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.wholesaleRate = wholesaleRate;
        this.piecesPerCarton = piecesPerCarton;
        this.itemCategory = itemCategory;
        this.stockQuantity = stockQuantity;
        this.company = company;
    }
    public Item(String name, double stockQuantity){
        this.name=name;
        this.stockQuantity=stockQuantity;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrintName() { return printName; }
    public double getPurchaseRate() { return purchaseRate; }
    public double getRetailRate() { return retailRate; }
    public double getWholesaleRate() { return wholesaleRate; }
    public int getPiecesPerCarton() { return piecesPerCarton; }
    public String getItemCategory() { return itemCategory; }
    public double getStockQuantity() { return stockQuantity; }
    public String getCompany() { return company; }

    // Setters
    public void setStockQuantity(double stockQuantity) { this.stockQuantity = stockQuantity; }
}

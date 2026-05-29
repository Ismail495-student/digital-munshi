package siks.models;

public class InvoiceItem {
    private String itemId; // IT### or null
    private String itemName;
    private double rate; // per piece
    private int cartons;
    private double pieces;
    private double amount; // rate * totalPieces


    public InvoiceItem(String itemId, String itemName, double rate, int cartons, double pieces, double amount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.rate = rate;
        this.cartons = cartons;
        this.pieces = pieces;
        this.amount = amount;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getRate() { return rate; }
    public int getCartons() { return cartons; }
    public double getPieces() { return pieces; }
    public double getAmount() { return amount; }

    public double getTotalPieces(int piecesPerCarton) {
        return cartons * piecesPerCarton + pieces;
    }


}

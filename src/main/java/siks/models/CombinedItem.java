package siks.models;

public class CombinedItem {
    private String itemName;
    private int totalCartons;
    private double totalPieces;

    public CombinedItem(String itemName, int totalCartons, double totalPieces) {
        this.itemName = itemName;
        this.totalCartons = totalCartons;
        this.totalPieces = totalPieces;
    }

    public String getItemName() { return itemName; }
    public int getTotalCartons() { return totalCartons; }
    public double getTotalPieces() { return totalPieces; }

    public void setTotalCartons(int totalCartons) { this.totalCartons = totalCartons; }
    public void setTotalPieces(double totalPieces) { this.totalPieces = totalPieces; }
}
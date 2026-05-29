package siks.models;

import javafx.beans.property.*;

public class ItemStock {
    private final StringProperty itemId = new SimpleStringProperty();
    private final StringProperty itemName = new SimpleStringProperty();
    private final IntegerProperty cartons = new SimpleIntegerProperty();
    private final IntegerProperty pieces = new SimpleIntegerProperty();
    private final StringProperty lastEdited = new SimpleStringProperty();
    private final StringProperty changeType = new SimpleStringProperty();

    public ItemStock(String id, String name, int cartons, int pieces, String lastEdited, String changeType) {
        this.itemId.set(id);
        this.itemName.set(name);
        this.cartons.set(cartons);
        this.pieces.set(pieces);
        this.lastEdited.set(lastEdited);
        this.changeType.set(changeType);
    }

    public StringProperty itemIdProperty() { return itemId; }
    public StringProperty itemNameProperty() { return itemName; }
    public IntegerProperty cartonsProperty() { return cartons; }
    public IntegerProperty piecesProperty() { return pieces; }
    public StringProperty lastEditedProperty() { return lastEdited; }
    public StringProperty changeTypeProperty() { return changeType; }

    public String getItemId() { return itemId.get(); }
    public String getItemName() { return itemName.get(); }
    public int getCartons() { return cartons.get(); }
    public int getPieces() { return pieces.get(); }
    public String getLastEdited() { return lastEdited.get(); }
    public String getChangeType() { return changeType.get(); }
}

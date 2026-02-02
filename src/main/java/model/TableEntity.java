package model;

import javafx.beans.property.*;

public class TableEntity {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty number = new SimpleIntegerProperty(); // Добавить
    private final IntegerProperty capacity = new SimpleIntegerProperty();
    private final StringProperty zone = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public TableEntity(int id, int capacity, String zone, String status) {
        this.id.set(id);
        this.capacity.set(capacity);
        this.zone.set(zone);
        this.status.set(status);
    }

    public int getId() { return id.get(); }
    public int getCapacity() { return capacity.get(); }
    public String getZone() { return zone.get(); }
    public String getStatus() { return status.get(); }

    public IntegerProperty idProperty() { return id; }
    public IntegerProperty capacityProperty() { return capacity; }
    public StringProperty zoneProperty() { return zone; }
    public StringProperty statusProperty() { return status; }
}
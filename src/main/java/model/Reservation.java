package model;

import javafx.beans.property.*;

public class Reservation {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty clientName = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty time = new SimpleStringProperty();
    private StringProperty endTime = new SimpleStringProperty();
    private final IntegerProperty duration = new SimpleIntegerProperty();
    private final IntegerProperty guests = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty tableInfo = new SimpleStringProperty();

    public Reservation(int id, String clientName, String date,
                       String time, int duration, int guests, String status, String tableInfo) {
        this.id.set(id);
        this.clientName.set(clientName);
        this.date.set(date);
        this.time.set(time);
        this.duration.set(duration);
        this.guests.set(guests);
        this.status.set(status);
        this.tableInfo.set(tableInfo);

        String calculatedEndTime = calculateEndTime(time, duration);
        this.endTime = new SimpleStringProperty(calculatedEndTime);
    }

    private String calculateEndTime(String startTime, int duration) {
        try {
            String[] parts = startTime.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            int totalMinutes = hours * 60 + minutes + duration;
            int endHours = totalMinutes / 60;
            int endMinutes = totalMinutes % 60;

            return String.format("%02d:%02d", endHours, endMinutes);
        } catch (Exception e) {
            return "??:??";
        }
    }

    public int getId() { return id.get(); }
    public String getClientName() { return clientName.get(); }
    public String getDate() { return date.get(); }
    public String getTime() { return time.get(); }
    public String getEndTime() { return endTime.get(); }
    public int getDuration() { return duration.get(); }
    public int getGuests() { return guests.get(); }
    public String getStatus() { return status.get(); }
    public String getTableInfo() { return tableInfo.get(); }

    public IntegerProperty idProperty() { return id; }
    public StringProperty clientNameProperty() { return clientName; }
    public StringProperty dateProperty() { return date; }
    public StringProperty timeProperty() { return time; }
    public StringProperty endTimeProperty() { return endTime; }
    public IntegerProperty durationProperty() { return duration; }
    public IntegerProperty guestsProperty() { return guests; }
    public StringProperty statusProperty() { return status; }
    public StringProperty tableInfoProperty() { return tableInfo; }
}
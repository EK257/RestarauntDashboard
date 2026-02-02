package controller;

import dao.Database;
import dao.ReservationDAO;
import model.Reservation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReservationController {
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Integer> idCol;
    @FXML private TableColumn<Reservation, String> clientCol;
    @FXML private TableColumn<Reservation, String> dateCol;
    @FXML private TableColumn<Reservation, String> timeCol;
    @FXML private TableColumn<Reservation, Integer> durationCol;
    @FXML private TableColumn<Reservation, Integer> guestsCol;
    @FXML private TableColumn<Reservation, String> statusCol;
    @FXML private TableColumn<Reservation, String> tableCol;
    @FXML private DatePicker dateFilter;

    private static final String[] TIME_SLOTS = {
            "10:00", "10:15", "10:30", "10:45",
            "11:00", "11:15", "11:30", "11:45",
            "12:00", "12:15", "12:30", "12:45",
            "13:00", "13:15", "13:30", "13:45",
            "14:00", "14:15", "14:30", "14:45",
            "15:00", "15:15", "15:30", "15:45",
            "16:00", "16:15", "16:30", "16:45",
            "17:00", "17:15", "17:30", "17:45",
            "18:00", "18:15", "18:30", "18:45",
            "19:00", "19:15", "19:30", "19:45",
            "20:00", "20:15", "20:30", "20:45",
            "21:00", "21:15", "21:30", "21:45",
            "22:00", "22:15", "22:30", "22:45"
    };

    @FXML
    private void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        clientCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        guestsCol.setCellValueFactory(new PropertyValueFactory<>("guests"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableCol.setCellValueFactory(new PropertyValueFactory<>("tableInfo"));
        loadData();
    }

    private void loadData() {
        if (dateFilter.getValue() != null) {
            reservationTable.setItems(FXCollections.observableArrayList(
                    ReservationDAO.getReservationsByDate(dateFilter.getValue().toString())));
        } else {
            reservationTable.setItems(FXCollections.observableArrayList(
                    ReservationDAO.getAllReservations()));
        }
    }

    @FXML
    private void handleFilterByDate() {
        loadData();
    }

    @FXML
    private void handleResetFilter() {
        dateFilter.setValue(null);
        loadData();
    }

    @FXML
    private void handleAddReservation() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Новое бронирование");

        ButtonType addButton = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        ButtonType findButton = new ButtonType("Найти лучшее время", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, findButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField clientField = new TextField();
        clientField.setPromptText("Имя клиента");

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        ComboBox<String> timeCombo = new ComboBox<>();
        timeCombo.getItems().addAll(TIME_SLOTS);
        timeCombo.setEditable(true);

        ComboBox<Integer> durationCombo = new ComboBox<>();
        durationCombo.getItems().addAll(15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180);
        durationCombo.setValue(120);
        durationCombo.setPromptText("Длительность (мин)");

        ComboBox<Integer> guestsCombo = new ComboBox<>();
        guestsCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 8, 10, 12);
        guestsCombo.setPromptText("Гости");

        ComboBox<String> tableCombo = new ComboBox<>();
        tableCombo.setPromptText("Столик");
        tableCombo.setDisable(true);

        Label recommendationLabel = new Label();
        recommendationLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-style: italic;");

        Runnable findBestTimeAndDate = () -> {
            int guests = guestsCombo.getValue() != null ? guestsCombo.getValue() : 2;
            int duration = durationCombo.getValue() != null ? durationCombo.getValue() : 120;

            LocalDate startDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();

            Map<String, Object> bestSlot = findBestAvailableSlot(startDate, guests, duration);

            if (bestSlot != null) {
                LocalDate bestDate = (LocalDate) bestSlot.get("date");
                String bestTime = (String) bestSlot.get("time");

                datePicker.setValue(bestDate);
                timeCombo.setValue(bestTime);
                updateTables(datePicker, timeCombo, durationCombo, guestsCombo, tableCombo, recommendationLabel);

                if (bestDate.equals(LocalDate.now())) {
                    recommendationLabel.setText("Рекомендуемое время сегодня: " + bestTime);
                } else {
                    recommendationLabel.setText("Рекомендуемое: " +
                            bestDate.format(DateTimeFormatter.ofPattern("dd.MM")) +
                            " в " + bestTime);
                }
                recommendationLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            } else {
                recommendationLabel.setText("Нет доступных слотов на ближайшие 30 дней");
                recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        };

        Runnable updateTablesRunnable = () -> updateTables(datePicker, timeCombo, durationCombo, guestsCombo, tableCombo, recommendationLabel);

        findBestTimeAndDate.run();

        timeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTablesRunnable.run());
        durationCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTablesRunnable.run());
        guestsCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTablesRunnable.run());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateTablesRunnable.run();
            }
        });

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(clientField, 1, 0);
        grid.add(new Label("Дата:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Время:"), 0, 2);
        grid.add(timeCombo, 1, 2);
        grid.add(new Label("Длительность (мин):"), 0, 3);
        grid.add(durationCombo, 1, 3);
        grid.add(new Label("Гости:"), 0, 4);
        grid.add(guestsCombo, 1, 4);
        grid.add(new Label("Столик:"), 0, 5);
        grid.add(tableCombo, 1, 5);
        grid.add(recommendationLabel, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(addButton).setDisable(true);

        Runnable checkFields = () -> {
            boolean allFilled = !clientField.getText().trim().isEmpty() &&
                    datePicker.getValue() != null &&
                    timeCombo.getValue() != null &&
                    durationCombo.getValue() != null &&
                    guestsCombo.getValue() != null &&
                    tableCombo.getValue() != null;

            dialog.getDialogPane().lookupButton(addButton).setDisable(!allFilled);
        };

        clientField.textProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        timeCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        durationCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        guestsCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        tableCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());

        Button findBtn = (Button) dialog.getDialogPane().lookupButton(findButton);
        findBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            findBestTimeAndDate.run();
        });

        dialog.setResultConverter(button -> {
            if (button == addButton) {
                Map<String, Object> result = new HashMap<>();
                result.put("client", clientField.getText());
                result.put("date", datePicker.getValue());
                result.put("time", timeCombo.getValue());
                result.put("duration", durationCombo.getValue());
                result.put("guests", guestsCombo.getValue());
                result.put("table", tableCombo.getValue());
                return result;
            }
            return null;
        });

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                String clientName = (String) data.get("client");
                String date = data.get("date").toString();
                String time = (String) data.get("time");
                int duration = (Integer) data.get("duration");
                int guests = (Integer) data.get("guests");
                String tableInfo = (String) data.get("table");

                int tableId = extractTableIdFromDisplayString(tableInfo);
                if (tableId <= 0) {
                    showError("Не удалось определить столик. Выберите другой столик.");
                    return;
                }

                int clientId = ReservationDAO.createOrGetClient(clientName);

                boolean success = ReservationDAO.addReservationWithStatus(
                        clientId, tableId, date, time, duration, guests, "Подтверждено");

                if (success) {
                    loadData();
                } else {
                    showError("Столик уже занят в это время!");
                }

            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updateTables(DatePicker datePicker, ComboBox<String> timeCombo, ComboBox<Integer> durationCombo, ComboBox<Integer> guestsCombo, ComboBox<String> tableCombo, Label recommendationLabel) {
        if (datePicker.getValue() != null && timeCombo.getValue() != null &&
                durationCombo.getValue() != null && guestsCombo.getValue() != null) {

            List<String> tables = ReservationDAO.getAvailableTables(
                    datePicker.getValue().toString(),
                    timeCombo.getValue(),
                    durationCombo.getValue(),
                    guestsCombo.getValue());

            tableCombo.getItems().clear();
            tableCombo.getItems().addAll(tables);
            tableCombo.setDisable(false);

            if (tables.isEmpty()) {
                tableCombo.setPromptText("Нет свободных столиков");
                recommendationLabel.setText("Нет свободных столиков в это время");
                recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                tableCombo.setValue(tables.get(0));
                recommendationLabel.setText("Доступно столиков: " + tables.size());
                recommendationLabel.setStyle("-fx-text-fill: #27ae60;");
            }
        } else {
            tableCombo.getItems().clear();
            tableCombo.setDisable(true);
        }
    }

    private Map<String, Object> findBestAvailableSlot(LocalDate startDate, int guests, int duration) {
        int maxDaysAhead = 30;

        List<LocalTime> timeSlots = new ArrayList<>();
        for (String slot : TIME_SLOTS) {
            timeSlots.add(LocalTime.parse(slot));
        }

        for (int dayOffset = 0; dayOffset < maxDaysAhead; dayOffset++) {
            LocalDate checkDate = startDate.plusDays(dayOffset);
            String dateStr = checkDate.toString();

            int startTimeIndex = 0;
            if (checkDate.equals(LocalDate.now())) {
                LocalTime nowTime = LocalTime.now().plusMinutes(15);
                for (int i = 0; i < timeSlots.size(); i++) {
                    if (!timeSlots.get(i).isBefore(nowTime)) {
                        startTimeIndex = i;
                        break;
                    }
                }
            }

            for (int i = startTimeIndex; i < timeSlots.size(); i++) {
                String checkTime = timeSlots.get(i).format(DateTimeFormatter.ofPattern("HH:mm"));

                List<String> tables = ReservationDAO.getAvailableTables(dateStr, checkTime, duration, guests);

                if (!tables.isEmpty()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("date", checkDate);
                    result.put("time", checkTime);
                    result.put("tablesCount", tables.size());
                    return result;
                }
            }
        }

        return null;
    }

    private int extractTableIdFromDisplayString(String displayString) {
        if (displayString == null || displayString.isEmpty()) {
            return -1;
        }

        try {
            displayString = displayString.trim();

            StringBuilder idBuilder = new StringBuilder();
            boolean foundDigit = false;

            for (int i = 0; i < displayString.length(); i++) {
                char c = displayString.charAt(i);
                if (Character.isDigit(c)) {
                    idBuilder.append(c);
                    foundDigit = true;
                } else if (foundDigit) {
                    break;
                }
            }

            if (idBuilder.length() > 0) {
                return Integer.parseInt(idBuilder.toString());
            }

            if (displayString.contains("№")) {
                String[] parts = displayString.split("№");
                if (parts.length > 1) {
                    String afterHash = parts[1];
                    for (int i = 0; i < afterHash.length(); i++) {
                        char c = afterHash.charAt(i);
                        if (Character.isDigit(c)) {
                            idBuilder.append(c);
                        } else if (idBuilder.length() > 0) {
                            break;
                        }
                    }
                    if (idBuilder.length() > 0) {
                        return Integer.parseInt(idBuilder.toString());
                    }
                }
            }

            if (displayString.contains("-")) {
                String[] parts = displayString.split("-");
                if (parts.length > 0) {
                    String firstPart = parts[0].trim();
                    for (int i = 0; i < firstPart.length(); i++) {
                        char c = firstPart.charAt(i);
                        if (Character.isDigit(c)) {
                            idBuilder.append(c);
                        } else if (idBuilder.length() > 0) {
                            break;
                        }
                    }
                    if (idBuilder.length() > 0) {
                        return Integer.parseInt(idBuilder.toString());
                    }
                }
            }

            System.err.println("Не удалось извлечь ID из строки: " + displayString);
            return -1;

        } catch (Exception e) {
            System.err.println("Ошибка при извлечении ID из: " + displayString);
            e.printStackTrace();
            return -1;
        }
    }

    @FXML
    private void handleDeleteReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите бронирование");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление");
        confirm.setHeaderText("Удалить бронирование #" + selected.getId());
        confirm.setContentText("Вы уверены?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            if (ReservationDAO.deleteReservation(selected.getId())) {
                loadData();
            } else {
                showError("Ошибка удаления");
            }
        }
    }
    @FXML
    private void handleEditReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите бронирование для редактирования");
            return;
        }

        showEditReservationDialog(selected);
    }
    private void showEditReservationDialog(Reservation reservation) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Редактирование бронирования");

        ButtonType saveButton = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType findButton = new ButtonType("Найти ближайшее время", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, findButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField clientField = new TextField(reservation.getClientName());
        clientField.setPromptText("Имя клиента");

        DatePicker datePicker = new DatePicker();
        try {
            datePicker.setValue(LocalDate.parse(reservation.getDate()));
        } catch (Exception e) {
            datePicker.setValue(LocalDate.now());
        }
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        ComboBox<String> timeCombo = new ComboBox<>();
        timeCombo.getItems().addAll(TIME_SLOTS);
        timeCombo.setValue(reservation.getTime());
        timeCombo.setEditable(true);

        ComboBox<Integer> durationCombo = new ComboBox<>();
        durationCombo.getItems().addAll(15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180);
        durationCombo.setValue(reservation.getDuration());
        durationCombo.setPromptText("Длительность (мин)");

        ComboBox<Integer> guestsCombo = new ComboBox<>();
        guestsCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 8, 10, 12);
        guestsCombo.setValue(reservation.getGuests());
        guestsCombo.setPromptText("Гости");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Подтверждено", "Активно", "Завершено", "Отменено", "Неявка");
        statusCombo.setValue(reservation.getStatus());
        statusCombo.setPromptText("Статус");

        ComboBox<String> tableCombo = new ComboBox<>();
        tableCombo.setPromptText("Столик");

        int currentTableId = getTableIdFromReservation(reservation.getId());
        String currentTableInfo = getTableInfoForId(currentTableId);

        Label recommendationLabel = new Label();
        recommendationLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-style: italic;");

        Runnable findNearestTime = () -> {
            int guests = guestsCombo.getValue() != null ? guestsCombo.getValue() : reservation.getGuests();
            int duration = durationCombo.getValue() != null ? durationCombo.getValue() : reservation.getDuration();

            Map<String, Object> bestSlot = findBestAvailableSlotForEdit(
                    datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now(),
                    guests,
                    duration,
                    reservation.getId(),
                    currentTableId);

            if (bestSlot != null) {
                LocalDate bestDate = (LocalDate) bestSlot.get("date");
                String bestTime = (String) bestSlot.get("time");
                String bestTableInfo = (String) bestSlot.get("tableInfo");

                datePicker.setValue(bestDate);
                timeCombo.setValue(bestTime);
                tableCombo.setValue(bestTableInfo);

                if (bestDate.equals(LocalDate.now())) {
                    recommendationLabel.setText("Найдено время сегодня: " + bestTime);
                } else {
                    recommendationLabel.setText("Найдено: " +
                            bestDate.format(DateTimeFormatter.ofPattern("dd.MM")) +
                            " в " + bestTime);
                }
                recommendationLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            } else {
                recommendationLabel.setText("Нет доступных слотов на ближайшие 30 дней");
                recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        };

        Runnable updateTables = () -> {
            if (datePicker.getValue() != null && timeCombo.getValue() != null &&
                    durationCombo.getValue() != null && guestsCombo.getValue() != null) {

                List<String> tables = ReservationDAO.getAvailableTablesForEdit(
                        datePicker.getValue().toString(),
                        timeCombo.getValue(),
                        durationCombo.getValue(),
                        guestsCombo.getValue(),
                        reservation.getId());

                tableCombo.getItems().clear();
                tableCombo.getItems().addAll(tables);

                if (!tables.contains(currentTableInfo)) {
                    tableCombo.getItems().add(currentTableInfo);
                }

                tableCombo.setValue(currentTableInfo);
                tableCombo.setDisable(false);

                if (tables.isEmpty() && tableCombo.getItems().size() == 1) {
                    tableCombo.setPromptText("Нет других свободных столиков");
                    recommendationLabel.setText("Только текущий столик доступен");
                    recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
                } else {
                    recommendationLabel.setText("Доступно столиков: " + tables.size());
                    recommendationLabel.setStyle("-fx-text-fill: #27ae60;");
                }
            } else {
                tableCombo.getItems().clear();
                tableCombo.setDisable(true);
            }
        };

        updateTables.run();

        timeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTables.run());
        durationCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTables.run());
        guestsCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTables.run());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateTables.run();
            }
        });

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(clientField, 1, 0);
        grid.add(new Label("Дата:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Время:"), 0, 2);
        grid.add(timeCombo, 1, 2);
        grid.add(new Label("Длительность (мин):"), 0, 3);
        grid.add(durationCombo, 1, 3);
        grid.add(new Label("Гости:"), 0, 4);
        grid.add(guestsCombo, 1, 4);
        grid.add(new Label("Статус:"), 0, 5);
        grid.add(statusCombo, 1, 5);
        grid.add(new Label("Столик:"), 0, 6);
        grid.add(tableCombo, 1, 6);
        grid.add(recommendationLabel, 0, 7, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(saveButton).setDisable(false);

        Button findBtn = (Button) dialog.getDialogPane().lookupButton(findButton);
        findBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            findNearestTime.run();
            updateTables.run();
        });

        dialog.setResultConverter(button -> {
            if (button == saveButton) {
                Map<String, Object> result = new HashMap<>();
                result.put("client", clientField.getText());
                result.put("date", datePicker.getValue());
                result.put("time", timeCombo.getValue());
                result.put("duration", durationCombo.getValue());
                result.put("guests", guestsCombo.getValue());
                result.put("status", statusCombo.getValue());
                result.put("table", tableCombo.getValue());
                result.put("reservationId", reservation.getId());
                return result;
            }
            return null;
        });

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                String date = data.get("date").toString();
                String time = (String) data.get("time");
                int duration = (Integer) data.get("duration");
                int guests = (Integer) data.get("guests");
                String status = (String) data.get("status");
                String tableInfo = (String) data.get("table");
                int reservationId = (Integer) data.get("reservationId");

                int tableId = extractTableIdFromDisplayString(tableInfo);
                if (tableId <= 0) {
                    showError("Не удалось определить столик");
                    return;
                }

                boolean success = ReservationDAO.updateReservation(
                        reservationId, tableId, date, time, duration, guests, status);

                if (success) {
                    loadData();
                } else {
                    showError("Не удалось обновить бронирование. Возможно столик занят.");
                }

            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    private Map<String, Object> findBestAvailableSlotForEdit(LocalDate startDate, int guests, int duration, int excludeReservationId, int currentTableId) {
        int maxDaysAhead = 30;

        List<LocalTime> timeSlots = new ArrayList<>();
        for (String slot : TIME_SLOTS) {
            timeSlots.add(LocalTime.parse(slot));
        }

        for (int dayOffset = 0; dayOffset < maxDaysAhead; dayOffset++) {
            LocalDate checkDate = startDate.plusDays(dayOffset);
            String dateStr = checkDate.toString();

            int startTimeIndex = 0;
            if (checkDate.equals(LocalDate.now())) {
                LocalTime nowTime = LocalTime.now().plusMinutes(15);
                for (int i = 0; i < timeSlots.size(); i++) {
                    if (!timeSlots.get(i).isBefore(nowTime)) {
                        startTimeIndex = i;
                        break;
                    }
                }
            }

            for (int i = startTimeIndex; i < timeSlots.size(); i++) {
                String checkTime = timeSlots.get(i).format(DateTimeFormatter.ofPattern("HH:mm"));

                if (ReservationDAO.isTableAvailableForEdit(currentTableId, dateStr, checkTime, duration, excludeReservationId)) {
                    String tableInfo = getTableInfoForId(currentTableId);
                    if (tableInfo != null) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("date", checkDate);
                        result.put("time", checkTime);
                        result.put("tableInfo", tableInfo);
                        return result;
                    }
                }

                List<String> tables = ReservationDAO.getAvailableTablesForEdit(
                        dateStr, checkTime, duration, guests, excludeReservationId);

                if (!tables.isEmpty()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("date", checkDate);
                    result.put("time", checkTime);
                    result.put("tableInfo", tables.get(0));
                    return result;
                }
            }
        }

        return null;
    }
    private int getTableIdFromReservation(int reservationId) {
        String sql = "SELECT table_id FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("table_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String getTableInfoForId(int tableId) {
        if (tableId <= 0) {
            return "Неизвестный столик";
        }

        String sql = "SELECT zone, capacity FROM tables WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String zone = rs.getString("zone");
                int capacity = rs.getInt("capacity");
                return String.format("%d - %s (%d мест)", tableId, zone, capacity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("%d - Неизвестный столик", tableId);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
package controller;

import dao.ReservationDAO;
import dao.TableDAO;
import model.Reservation;
import model.TableEntity;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DashboardController {
    @FXML private TableView<Reservation> todayReservationsTable;
    @FXML private TableColumn<Reservation, Integer> resIdCol;
    @FXML private TableColumn<Reservation, String> resClientCol;
    @FXML private TableColumn<Reservation, String> resTimeCol;
    @FXML private TableColumn<Reservation, Integer> resGuestsCol;
    @FXML private TableColumn<Reservation, String> resTableCol;
    @FXML private TableColumn<Reservation, String> resStatusCol;

    @FXML private TableView<TableEntity> availableTablesTable;
    @FXML private TableColumn<TableEntity, Integer> tableIdCol;
    @FXML private TableColumn<TableEntity, String> tableZoneCol;
    @FXML private TableColumn<TableEntity, Integer> tableCapacityCol;
    @FXML private TableColumn<TableEntity, String> tableStatusCol;

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
        setupTables();
        loadData();
    }

    private void setupTables() {
        resIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        resClientCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        resTimeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        resGuestsCol.setCellValueFactory(new PropertyValueFactory<>("guests"));
        resTableCol.setCellValueFactory(new PropertyValueFactory<>("tableInfo"));
        resStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        tableZoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        tableCapacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        tableStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadData() {
        String today = LocalDate.now().toString();
        List<Reservation> todayReservations = ReservationDAO.getReservationsByDate(today);
        todayReservationsTable.setItems(FXCollections.observableArrayList(todayReservations));

        List<TableEntity> allTables = TableDAO.getAllTables();
        availableTablesTable.setItems(FXCollections.observableArrayList(allTables));
    }

    @FXML
    private void handleReserveNow() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Бронь на сейчас");

        ButtonType seatButton = new ButtonType("Посадить сейчас", ButtonBar.ButtonData.OK_DONE);
        ButtonType findButton = new ButtonType("Найти ближайшее время", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(seatButton, findButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField clientField = new TextField();
        clientField.setPromptText("Имя клиента");

        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        int roundedMinute = ((currentMinute + 7) / 15) * 15;
        if (roundedMinute >= 60) {
            currentHour++;
            roundedMinute = 0;
        }

        if (currentHour > 22) {
            currentHour = 22;
            roundedMinute = 0;
        } else if (currentHour < 11) {
            currentHour = 11;
            roundedMinute = 0;
        }

        String currentTime = String.format("%02d:%02d", currentHour, roundedMinute);

        ComboBox<String> timeCombo = new ComboBox<>();

        List<String> availableTimeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(currentHour, roundedMinute);

        for (int i = 0; i <= 12; i++) {
            LocalTime slotTime = startTime.plusMinutes(i * 15);
            if (slotTime.isBefore(LocalTime.of(22, 45))) {
                availableTimeSlots.add(slotTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        }

        timeCombo.getItems().addAll(availableTimeSlots);
        timeCombo.setValue(currentTime);
        timeCombo.setEditable(false);

        ComboBox<Integer> durationCombo = new ComboBox<>();
        durationCombo.getItems().addAll(15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180);
        durationCombo.setValue(120);

        ComboBox<Integer> guestsCombo = new ComboBox<>();
        guestsCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 8, 10, 12);
        guestsCombo.setValue(2);

        ComboBox<String> tableCombo = new ComboBox<>();
        tableCombo.setPromptText("Столик");
        tableCombo.setDisable(true);

        Label recommendationLabel = new Label();
        recommendationLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-style: italic;");

        Label currentTimeLabel = new Label("Текущее время: " +
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        currentTimeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        Runnable updateTables = () -> {
            if (timeCombo.getValue() != null && guestsCombo.getValue() != null) {
                String today = LocalDate.now().toString();
                List<String> tables = ReservationDAO.getAvailableTables(
                        today, timeCombo.getValue(), durationCombo.getValue(), guestsCombo.getValue());

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
            }
        };

        Runnable findNearestAvailableTime = () -> {
            String today = LocalDate.now().toString();
            int guests = guestsCombo.getValue() != null ? guestsCombo.getValue() : 2;
            int duration = durationCombo.getValue() != null ? durationCombo.getValue() : 120;

            String bestTime = findNearestAvailableTimeForToday(today, guests, duration, currentTime);

            if (bestTime != null) {
                timeCombo.setValue(bestTime);
                updateTables.run();

                LocalTime selectedTime = LocalTime.parse(bestTime);
                LocalTime nowTime = LocalTime.now();
                long minutesDiff = ChronoUnit.MINUTES.between(nowTime, selectedTime);

                if (minutesDiff <= 30) {
                    recommendationLabel.setText("Доступно");
                    recommendationLabel.setStyle("-fx-text-fill: #27ae60;");
                } else if (minutesDiff <= 60) {
                    recommendationLabel.setText("Клиент придет через ~" + minutesDiff + " минут");
                    recommendationLabel.setStyle("-fx-text-fill: #f39c12;");
                } else {
                    recommendationLabel.setText("Внимание: ближайшее время через " + minutesDiff + " минут.");
                    recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            } else {
                recommendationLabel.setText("Нет доступных столиков в ближайшее время");
                recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        };

        findNearestAvailableTime.run();

        durationCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTables.run());
        guestsCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTables.run());
        timeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTables.run());

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(clientField, 1, 0);
        grid.add(currentTimeLabel, 0, 1, 2, 1);
        grid.add(new Label("Время посадки:"), 0, 2);
        grid.add(timeCombo, 1, 2);
        grid.add(new Label("Длительность (мин):"), 0, 3);
        grid.add(durationCombo, 1, 3);
        grid.add(new Label("Гости:"), 0, 4);
        grid.add(guestsCombo, 1, 4);
        grid.add(new Label("Столик:"), 0, 5);
        grid.add(tableCombo, 1, 5);
        grid.add(recommendationLabel, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(seatButton).setDisable(true);

        Runnable checkFields = () -> {
            boolean allFilled = !clientField.getText().trim().isEmpty() &&
                    timeCombo.getValue() != null &&
                    durationCombo.getValue() != null &&
                    guestsCombo.getValue() != null &&
                    tableCombo.getValue() != null;

            if (allFilled) {
                try {
                    LocalTime selectedTime = LocalTime.parse(timeCombo.getValue());
                    LocalTime nowTime = LocalTime.now();
                    long minutesDiff = ChronoUnit.MINUTES.between(nowTime, selectedTime);

                    if (minutesDiff > 120) {
                        recommendationLabel.setText("Внимание: клиент придет через " + minutesDiff +
                                " минут. Возможно, это должна быть предварительная бронь?");
                        recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");

                    }
                } catch (Exception e) {

                }
            }

            dialog.getDialogPane().lookupButton(seatButton).setDisable(!allFilled);
        };

        clientField.textProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        durationCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        guestsCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        tableCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        timeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            checkFields.run();
            if (newVal != null) {
                try {
                    LocalTime selectedTime = LocalTime.parse(newVal);
                    LocalTime nowTime = LocalTime.now();
                    long minutesDiff = ChronoUnit.MINUTES.between(nowTime, selectedTime);

                    if (minutesDiff < 0) {
                        recommendationLabel.setText("Время уже прошло! Выберите актуальное время.");
                        recommendationLabel.setStyle("-fx-text-fill: #e74c3c;");
                    }
                } catch (Exception e) {
                }
            }
        });

        Button findBtn = (Button) dialog.getDialogPane().lookupButton(findButton);
        findBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            findNearestAvailableTime.run();
        });

        dialog.setResultConverter(button -> {
            if (button == seatButton) {
                Map<String, Object> result = new HashMap<>();
                result.put("client", clientField.getText());
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
                String time = (String) data.get("time");
                int duration = (Integer) data.get("duration");
                int guests = (Integer) data.get("guests");
                String tableInfo = (String) data.get("table");

                int tableId = getTableIdFromDisplayString(tableInfo);
                if (tableId <= 0) {
                    showError("Не удалось определить столик");
                    return;
                }

                LocalTime selectedTime = LocalTime.parse(time);
                LocalTime nowTime = LocalTime.now();
                long minutesDiff = ChronoUnit.MINUTES.between(nowTime, selectedTime);

                if (minutesDiff < -15) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Время прошло");
                    confirm.setHeaderText("Выбранное время уже прошло");
                    confirm.setContentText("Вы уверены, что хотите посадить клиента?");

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        return;
                    }
                } else if (minutesDiff > 120) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Далекое время");
                    confirm.setHeaderText("Клиент придет только через " + minutesDiff + " минут");
                    confirm.setContentText("Может быть сделать предварительную бронь вместо посадки сейчас?");

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        return;
                    }
                }

                int clientId = ReservationDAO.createOrGetClient(clientName);
                String today = LocalDate.now().toString();

                String status;
                if (minutesDiff <= 30) {
                    status = "Активно";
                } else {
                    status = "Подтверждено";
                }

                boolean success = ReservationDAO.addReservationWithStatus(
                        clientId, tableId, today, time, duration, guests, status);

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
    private int getTableIdFromDisplayString(String displayString) {
        if (displayString == null || displayString.trim().isEmpty()) {
            return -1;
        }

        try {
            displayString = displayString.trim();

            if (displayString.matches("^\\d+\\..*")) {
                String[] parts = displayString.split("\\.", 2);
                if (parts.length > 0) {
                    return Integer.parseInt(parts[0].trim());
                }
            }

            if (displayString.matches("^\\d+\\s*-.*")) {
                String[] parts = displayString.split("-", 2);
                if (parts.length > 0) {
                    return Integer.parseInt(parts[0].trim());
                }
            }

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(displayString);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }

            if (displayString.contains("№")) {
                String afterHash = displayString.split("№")[1];
                matcher = pattern.matcher(afterHash);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group());
                }
            }

            System.err.println("Не удалось извлечь ID из строки: " + displayString);
            return -1;

        } catch (NumberFormatException e) {
            System.err.println("Ошибка парсинга числа из строки: " + displayString);
            return -1;
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при извлечении ID: " + displayString);
            e.printStackTrace();
            return -1;
        }
    }

    @FXML
    private void handleReserveLater() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Предварительная бронь");

        ButtonType reserveButton = new ButtonType("Забронировать", ButtonBar.ButtonData.OK_DONE);
        ButtonType findButton = new ButtonType("Найти лучшее время", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(reserveButton, findButton, ButtonType.CANCEL);

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

        LocalTime nowTime = LocalTime.now();
        String nearestFutureTime = getNearestFutureTime(nowTime);
        timeCombo.setValue(nearestFutureTime);

        ComboBox<Integer> durationCombo = new ComboBox<>();
        durationCombo.getItems().addAll(15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180);
        durationCombo.setValue(120);

        ComboBox<Integer> guestsCombo = new ComboBox<>();
        guestsCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 8, 10, 12);
        guestsCombo.setValue(2);

        ComboBox<String> tableCombo = new ComboBox<>();
        tableCombo.setPromptText("Столик");
        tableCombo.setDisable(true);

        Label recommendationLabel = new Label();
        recommendationLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-style: italic;");

        Runnable updateTables = () -> {
            if (datePicker.getValue() != null && timeCombo.getValue() != null &&
                    guestsCombo.getValue() != null) {

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
            }
        };

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
                updateTables.run();

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

        findBestTimeAndDate.run();

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
        grid.add(new Label("Столик:"), 0, 5);
        grid.add(tableCombo, 1, 5);
        grid.add(recommendationLabel, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(reserveButton).setDisable(true);

        Runnable checkFields = () -> {
            boolean allFilled = !clientField.getText().trim().isEmpty() &&
                    datePicker.getValue() != null &&
                    timeCombo.getValue() != null &&
                    durationCombo.getValue() != null &&
                    guestsCombo.getValue() != null &&
                    tableCombo.getValue() != null;

            dialog.getDialogPane().lookupButton(reserveButton).setDisable(!allFilled);
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
            if (button == reserveButton) {
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

                int tableId = getTableIdFromDisplayString(tableInfo);
                if (tableId <= 0) {
                    showError("Ошибка: не удалось определить столик");
                    return;
                }
                if (tableId <= 0) {
                    showError("Не удалось определить столик");
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
            }
        });
    }
    private String findNearestAvailableTimeForToday(String date, int guests, int duration, String startFromTime) {
        try {
            LocalTime startTime;
            try {
                startTime = LocalTime.parse(startFromTime);
            } catch (Exception e) {
                startTime = LocalTime.now();
            }

            List<LocalTime> timeSlots = new ArrayList<>();
            for (String slot : TIME_SLOTS) {
                timeSlots.add(LocalTime.parse(slot));
            }

            int startIndex = 0;
            for (int i = 0; i < timeSlots.size(); i++) {
                if (!timeSlots.get(i).isBefore(startTime)) {
                    startIndex = i;
                    break;
                }
            }

            int maxSlots = 12;
            for (int i = startIndex; i < Math.min(timeSlots.size(), startIndex + maxSlots); i++) {
                String checkTime = timeSlots.get(i).format(DateTimeFormatter.ofPattern("HH:mm"));

                List<String> tables = ReservationDAO.getAvailableTables(date, checkTime, duration, guests);

                if (!tables.isEmpty()) {
                    return checkTime;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
                LocalTime nowTime = LocalTime.now();
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


    private String getNearestFutureTime(LocalTime currentTime) {
        for (String timeSlot : TIME_SLOTS) {
            LocalTime slotTime = LocalTime.parse(timeSlot);
            if (slotTime.isAfter(currentTime.plusMinutes(15))) {
                return timeSlot;
            }
        }

        return "10:00";
    }

    @FXML
    private void handleMarkAsActive() {
        Reservation selected = todayReservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите бронирование");
            return;
        }

        if (!selected.getDate().equals(LocalDate.now().toString())) {
            showError("Можно отмечать только сегодняшние брони");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Отметить как активное");
        confirm.setHeaderText("Клиент прибыл?");
        confirm.setContentText("Отметить бронирование #" + selected.getId() + " как 'Активно'?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            boolean success = ReservationDAO.updateReservationStatus(selected.getId(), "Активно");
            if (!success) {
                showError("Ошибка обновления");
            } else {
                loadData();
            }
        }
    }

    @FXML
    private void handleMarkAsCompleted() {
        Reservation selected = todayReservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите бронирование");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Завершить бронирование");
        confirm.setHeaderText("Клиент ушел?");
        confirm.setContentText("Завершить бронирование #" + selected.getId() + "?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            boolean success = ReservationDAO.updateReservationStatus(selected.getId(), "Завершено");
            if (!success) {
                showError("Ошибка обновления");
            } else {
                loadData();
            }
        }
    }

    @FXML
    private void handleCancelReservation() {
        Reservation selected = todayReservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите бронирование");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Отменить бронирование");
        confirm.setHeaderText("Отменить бронирование #" + selected.getId());
        confirm.setContentText("Вы уверены?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            boolean success = ReservationDAO.updateReservationStatus(selected.getId(), "Отменено");
            if (!success) {
                showError("Ошибка отмены");
            } else {
                loadData();
            }
        }
    }

    @FXML
    private void handleFreeTable() {
        TableEntity selected = availableTablesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите столик");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Освободить столик");
        confirm.setHeaderText("Освободить столик #" + selected.getId());
        confirm.setContentText("Отметить столик как свободный?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            TableDAO.updateTableStatus(selected.getId(), "Свободен");
            loadData();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
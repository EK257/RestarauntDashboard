package controller;

import dao.TableDAO;
import model.TableEntity;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import java.util.*;

public class TableController {

    @FXML private TableView<TableEntity> tableTable;
    @FXML private TableColumn<TableEntity, Integer> idCol;
    @FXML private TableColumn<TableEntity, Integer> capCol;
    @FXML private TableColumn<TableEntity, String> zoneCol;
    @FXML private TableColumn<TableEntity, String> statusCol;

    @FXML
    private void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        capCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        zoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadData();

        tableTable.setRowFactory(tv -> {
            TableRow<TableEntity> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditTable();
                }
            });
            return row;
        });
    }

    private void loadData() {
        tableTable.setItems(FXCollections.observableArrayList(TableDAO.getAllTables()));
    }

    @FXML
    private void handleAddTable() {
        showTableDialog(null, "Добавить столик", "Добавить");
    }

    @FXML
    private void handleEditTable() {
        TableEntity selected = tableTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите столик для редактирования");
            return;
        }
        showTableDialog(selected, "Изменить столик", "Сохранить");
    }

    @FXML
    private void handleDeleteTable() {
        TableEntity selected = tableTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите столик для удаления");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление столика");
        confirm.setHeaderText("Удалить столик #" + selected.getId());
        confirm.setContentText("Вы уверены?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            if (TableDAO.deleteTable(selected.getId())) {
                loadData();
            } else {
                showError("Не удалось удалить столик. Возможно на него есть бронирования.");
            }
        }
    }

    private void showTableDialog(TableEntity table, String title, String buttonText) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle(title);

        ButtonType actionButton = new ButtonType(buttonText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(actionButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField capacityField = new TextField();
        capacityField.setPromptText("4");

        ComboBox<String> zoneCombo = new ComboBox<>();
        zoneCombo.setEditable(false);
        List<String> zones = TableDAO.getZonesFromZonesTable();
        zoneCombo.getItems().addAll(zones);
        zoneCombo.setPromptText("Выберите зону");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Свободен", "Занят", "Забронирован", "На ремонте");
        statusCombo.setPromptText("Статус");

        if (table != null) {
            capacityField.setText(String.valueOf(table.getCapacity()));
            zoneCombo.setValue(table.getZone());
            statusCombo.setValue(table.getStatus());
        } else {
            capacityField.setText("4");
            statusCombo.setValue("Свободен");
        }

        grid.add(new Label("Вместимость:"), 0, 0);
        grid.add(capacityField, 1, 0);
        grid.add(new Label("Зона:"), 0, 1);
        grid.add(zoneCombo, 1, 1);
        grid.add(new Label("Статус:"), 0, 2);
        grid.add(statusCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == actionButton) {
                Map<String, Object> result = new HashMap<>();
                result.put("capacity", capacityField.getText());
                result.put("zone", zoneCombo.getValue());
                result.put("status", statusCombo.getValue());
                result.put("id", table != null ? table.getId() : null);
                return result;
            }
            return null;
        });

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                String capacityStr = (String) data.get("capacity");
                String zone = (String) data.get("zone");
                String status = (String) data.get("status");
                Integer id = (Integer) data.get("id");

                if (capacityStr == null || capacityStr.isEmpty()) {
                    showError("Введите вместимость");
                    return;
                }
                if (zone == null || zone.isEmpty()) {
                    showError("Выберите зону");
                    return;
                }
                if (status == null || status.isEmpty()) {
                    showError("Выберите статус");
                    return;
                }

                int capacity = Integer.parseInt(capacityStr);
                if (capacity <= 0) {
                    showError("Вместимость должна быть больше 0");
                    return;
                }
                if (capacity > 50) {
                    showError("Максимальная вместимость 50");
                    return;
                }

                boolean success;
                if (id != null) {
                    success = TableDAO.updateTable(id, capacity, zone, status);
                    if (success) {
                        loadData();
                    } else {
                        showError("Не удалось обновить столик");
                    }
                } else {
                    success = TableDAO.addTable(capacity, zone, status);
                    if (success) {
                        loadData();
                    } else {
                        showError("Не удалось добавить столик");
                    }
                }

            } catch (NumberFormatException e) {
                showError("Введите число");
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        });
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
package controller;

import dao.TableDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class ZonesController {

    @FXML private TableView<String> zonesTable;
    @FXML private TableColumn<String, String> zoneNameCol;
    @FXML private TextField newZoneField;

    @FXML
    private void initialize() {
        zoneNameCol.setCellValueFactory(data -> {
            String zone = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(zone);
        });

        zonesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadZones();
    }

    private void loadZones() {
        List<String> zones = TableDAO.getZonesFromZonesTable();
        zonesTable.setItems(FXCollections.observableArrayList(zones));
    }

    @FXML
    private void handleAddZone() {
        String zoneName = newZoneField.getText().trim();

        if (zoneName.isEmpty()) {
            showError("Введите название зоны");
            return;
        }
        List<String> existingZones = TableDAO.getZonesFromZonesTable();
        if (existingZones.contains(zoneName)) {
            showError("Зона уже существует");
            return;
        }

        boolean success = TableDAO.addZone(zoneName);

        if (success) {
            newZoneField.clear();
            loadZones();
        } else {
            showError("Не удалось добавить зону");
        }
    }

    @FXML
    private void handleDeleteZone() {
        String selectedZone = zonesTable.getSelectionModel().getSelectedItem();

        if (selectedZone == null) {
            showError("Выберите зону для удаления");
            return;
        }

        if (TableDAO.hasTablesInZone(selectedZone)) {
            showError("Нельзя удалить зону, в ней есть столики");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление зоны");
        confirm.setHeaderText("Удалить зону: " + selectedZone);
        confirm.setContentText("Вы уверены?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            boolean success = TableDAO.deleteZone(selectedZone);

            if (success) {
                loadZones();
            } else {
                showError("Не удалось удалить зону");
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
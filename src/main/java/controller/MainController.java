package controller;

import dao.TableDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.util.Map;

public class MainController {
    @FXML private BorderPane rootPane;
    @FXML private Label globalStatsLabel;

    private Timeline statsUpdateTimeline;

    @FXML
    private void initialize() {
        startStatsUpdater();
        openDashboard();
    }

    private void startStatsUpdater() {
        updateGlobalStats();

        statsUpdateTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), event -> updateGlobalStats())
        );
        statsUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        statsUpdateTimeline.play();
    }

    @FXML
    private void openDashboard() {
        loadPage("/fxml/dashboard.fxml");
    }

    @FXML
    private void openReservations() {
        loadPage("/fxml/reservations.fxml");
    }

    @FXML
    private void openTables() {
        loadPage("/fxml/tables.fxml");
    }

    @FXML
    private void openZones() {
        loadPage("/fxml/zones.fxml");
    }

    private void loadPage(String path) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(path));
            rootPane.setCenter(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateGlobalStats() {
        Map<String, Object> stats = TableDAO.getTableStatistics();

        if (stats.isEmpty() || (int)stats.get("total") == 0) {
            globalStatsLabel.setText("Загрузка: 0%");
            globalStatsLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            return;
        }

        int total = (int) stats.get("total");
        int totalActive = (int) stats.getOrDefault("totalActive", total);
        int maintenance = (int) stats.getOrDefault("maintenance", 0);
        int busy = (int) stats.getOrDefault("busy", 0);
        double loadPercentage = (double) stats.get("loadPercentage");

        String statsText;
        if (maintenance > 0) {
            statsText = String.format("Загрузка: %.0f%% (%d/%d) | %d на ремонте",
                    loadPercentage, busy, totalActive, maintenance);
        } else {
            statsText = String.format("Загрузка: %.0f%% (%d/%d)",
                    loadPercentage, busy, totalActive);
        }

        globalStatsLabel.setText(statsText);

        if (loadPercentage > 80) {
            globalStatsLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else if (loadPercentage > 50) {
            globalStatsLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        } else {
            globalStatsLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    public void shutdown() {
        if (statsUpdateTimeline != null) {
            statsUpdateTimeline.stop();
        }
    }
}
import controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private MainController mainController;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());

            mainController = loader.getController();

            stage.setTitle("Система бронирования");
            stage.setScene(scene);
            stage.setWidth(1440);
            stage.setHeight(900);
            stage.setMinWidth(900);
            stage.setMinHeight(600);

            stage.setOnCloseRequest(e -> {
                if (mainController != null) {
                    mainController.shutdown();
                }
            });

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }}
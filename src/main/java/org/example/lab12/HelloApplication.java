package org.example.lab12;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/lab12/graph.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/org/example/lab12/static/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Graph Shortest Path Finder");
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
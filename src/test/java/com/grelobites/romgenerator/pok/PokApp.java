package com.grelobites.romgenerator.pok;
import com.grelobites.romgenerator.Constants;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PokApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(PokApp.class.getResource("/pokapp.fxml"));
        primaryStage.setTitle("Poke App Tester");
        Scene scene = new Scene(root, 600, 255);
        scene.getStylesheets().add(Constants.getThemeResourceUrl());
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

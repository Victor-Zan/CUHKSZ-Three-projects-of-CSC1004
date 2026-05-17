package com.example.gomokogame;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        GomokuGame game = new GomokuGame();
        game.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
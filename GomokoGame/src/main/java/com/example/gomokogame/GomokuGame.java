package com.example.gomokogame;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class GomokuGame {
    private static final int BOARD_SIZE = 20;
    private static final int CELL_SIZE = 30;
    private static final int BOARD_WIDTH = CELL_SIZE * (BOARD_SIZE - 1);
    private static final int BOARD_HEIGHT = CELL_SIZE * (BOARD_SIZE - 1);
    private static final int OFFSET = CELL_SIZE; // 偏移量，使棋盘居中
    private static final int CLICK_TOLERANCE = 15; // 点击容差（像素）

    private Board board;
    private GameLogic logic;
    private Player blackPlayer, whitePlayer;
    private Player currentPlayer;
    private TimeManager timeManager;

    // GUI 组件
    private Canvas canvas;
    private Label currentPlayerLabel;
    private Label blackStatsLabel;
    private Label whiteStatsLabel;
    private Label timerLabel;
    private Label winnerLabel;

    private boolean gameActive = true;
    private Timeline timerTimeline;

    public void start(Stage primaryStage) {
        // 初始化游戏数据
        board = new Board(BOARD_SIZE);
        logic = new GameLogic(BOARD_SIZE);
        blackPlayer = new Player("黑方", Color.BLACK);
        whitePlayer = new Player("白方", Color.WHITE);
        currentPlayer = blackPlayer;
        timeManager = new TimeManager(30, this::onTimeOut); // 30秒时限

        // 创建主界面
        BorderPane root = new BorderPane();

        // 棋盘画布
        canvas = new Canvas(BOARD_WIDTH + 2 * OFFSET, BOARD_HEIGHT + 2 * OFFSET);
        drawBoard();
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);

        // 右侧信息面板
        VBox infoPanel = new VBox(15);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setAlignment(Pos.TOP_CENTER);
        infoPanel.setStyle("-fx-background-color: #f0f0f0;");

        currentPlayerLabel = new Label("当前玩家: 黑方");
        currentPlayerLabel.setFont(Font.font(16));
        blackStatsLabel = new Label("黑方: 步数 0, 最长连珠 0");
        whiteStatsLabel = new Label("白方: 步数 0, 最长连珠 0");
        timerLabel = new Label("剩余时间: 30秒");
        timerLabel.setFont(Font.font(14));
        winnerLabel = new Label("");
        winnerLabel.setFont(Font.font(16));
        winnerLabel.setTextFill(Color.RED);

        Button resetBtn = new Button("重置游戏");
        resetBtn.setOnAction(e -> resetGame());
        Button exitBtn = new Button("退出");
        exitBtn.setOnAction(e -> Platform.exit());

        infoPanel.getChildren().addAll(currentPlayerLabel, blackStatsLabel, whiteStatsLabel,
                timerLabel, winnerLabel, resetBtn, exitBtn);

        root.setCenter(canvas);
        root.setRight(infoPanel);

        Scene scene = new Scene(root);
        primaryStage.setTitle("五子棋 Gomoku");
        primaryStage.setScene(scene);
        primaryStage.show();

        startTimer();
        updateStats();
    }

    private void drawBoard() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        // 画网格线
        for (int i = 0; i < BOARD_SIZE; i++) {
            gc.strokeLine(OFFSET, OFFSET + i * CELL_SIZE,
                    OFFSET + (BOARD_SIZE - 1) * CELL_SIZE, OFFSET + i * CELL_SIZE);
            gc.strokeLine(OFFSET + i * CELL_SIZE, OFFSET,
                    OFFSET + i * CELL_SIZE, OFFSET + (BOARD_SIZE - 1) * CELL_SIZE);
        }

        // 画棋子
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int stone = board.getStone(row, col);
                if (stone != 0) {
                    double x = OFFSET + col * CELL_SIZE;
                    double y = OFFSET + row * CELL_SIZE;
                    double radius = CELL_SIZE * 0.4;
                    if (stone == 1) {
                        gc.setFill(Color.BLACK);
                    } else {
                        gc.setFill(Color.WHITE);
                    }
                    gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                    gc.setStroke(Color.BLACK);
                    gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
                }
            }
        }
    }

    private void handleMouseClick(MouseEvent e) {
        if (!gameActive) return;

        double mouseX = e.getX();
        double mouseY = e.getY();

        // 计算最近的交叉点
        int col = (int) Math.round((mouseX - OFFSET) / CELL_SIZE);
        int row = (int) Math.round((mouseY - OFFSET) / CELL_SIZE);

        // 检查是否在棋盘范围内且点击在容差内
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            double exactX = OFFSET + col * CELL_SIZE;
            double exactY = OFFSET + row * CELL_SIZE;
            double distance = Math.hypot(mouseX - exactX, mouseY - exactY);
            if (distance <= CLICK_TOLERANCE) {
                placeStone(row, col);
            }
        }
    }

    private void placeStone(int row, int col) {
        if (!gameActive) return;
        if (!board.isEmpty(row, col)) {
            showWarning("该位置已有棋子，请重新选择！");
            return;
        }

        int stoneValue = (currentPlayer == blackPlayer) ? 1 : 2;
        board.setStone(row, col, stoneValue);
        drawBoard();

        // 更新玩家数据
        currentPlayer.incrementMoves();
        int maxLength = logic.getMaxConnectedLength(board, stoneValue);
        if (maxLength > currentPlayer.getMaxLength()) {
            currentPlayer.setMaxLength(maxLength);
        }
        updateStats();

        // 检查胜负
        if (logic.checkWin(board, row, col, stoneValue)) {
            gameActive = false;
            timeManager.stopTimer();
            winnerLabel.setText(currentPlayer.getName() + " 获胜！");
            showWarning(currentPlayer.getName() + " 获胜！");
            return;
        }

        // 切换玩家
        switchPlayer();
        startTimer(); // 重置计时器
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == blackPlayer) ? whitePlayer : blackPlayer;
        currentPlayerLabel.setText("当前玩家: " + currentPlayer.getName());
        winnerLabel.setText(""); // 清空获胜信息
        // 切换玩家后重新开始计时（在placeStone中调用startTimer）
    }

    private void onTimeOut() {
        Platform.runLater(() -> {
            if (!gameActive) return;
            showWarning(currentPlayer.getName() + " 超时！轮到对方下棋。");
            switchPlayer();
            startTimer();
        });
    }

    private void startTimer() {
        timeManager.stopTimer();
        timeManager.startTimer();
        // 更新界面显示剩余时间（通过TimeManager的周期性回调）
        timeManager.setTimeUpdateCallback(remaining -> {
            Platform.runLater(() -> timerLabel.setText("剩余时间: " + remaining + "秒"));
        });
    }

    private void updateStats() {
        blackStatsLabel.setText(String.format("黑方: 步数 %d, 最长连珠 %d",
                blackPlayer.getMoves(), blackPlayer.getMaxLength()));
        whiteStatsLabel.setText(String.format("白方: 步数 %d, 最长连珠 %d",
                whitePlayer.getMoves(), whitePlayer.getMaxLength()));
    }

    private void resetGame() {
        board.reset();
        logic.reset(); // 若有需要
        blackPlayer.reset();
        whitePlayer.reset();
        currentPlayer = blackPlayer;
        gameActive = true;
        winnerLabel.setText("");
        drawBoard();
        updateStats();
        timeManager.stopTimer();
        startTimer();
        currentPlayerLabel.setText("当前玩家: 黑方");
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

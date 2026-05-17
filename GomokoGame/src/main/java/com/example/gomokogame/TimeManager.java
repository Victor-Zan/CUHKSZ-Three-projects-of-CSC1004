package com.example.gomokogame;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.Consumer;

public class TimeManager {
    private int timeLimit; // 秒
    private int remaining;
    private Timeline timeline;
    private Runnable onTimeout;
    private Consumer<Integer> timeUpdateCallback;

    public TimeManager(int timeLimit, Runnable onTimeout) {
        this.timeLimit = timeLimit;
        this.remaining = timeLimit;
        this.onTimeout = onTimeout;
    }

    public void startTimer() {
        remaining = timeLimit;
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining--;
            if (timeUpdateCallback != null) {
                timeUpdateCallback.accept(remaining);
            }
            if (remaining <= 0) {
                timeline.stop();
                if (onTimeout != null) {
                    onTimeout.run();
                }
            }
        }));
        timeline.setCycleCount(timeLimit);
        timeline.play();
    }

    public void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    public void setTimeUpdateCallback(Consumer<Integer> callback) {
        this.timeUpdateCallback = callback;
    }
}
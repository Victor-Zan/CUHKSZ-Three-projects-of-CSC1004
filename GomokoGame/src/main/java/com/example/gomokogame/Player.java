package com.example.gomokogame;

import javafx.scene.paint.Color;

public class Player {
    private String name;
    private Color color;
    private int moves;
    private int maxLength;

    public Player(String name, Color color) {
        this.name = name;
        this.color = color;
        this.moves = 0;
        this.maxLength = 0;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public int getMoves() {
        return moves;
    }

    public void incrementMoves() {
        moves++;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void reset() {
        moves = 0;
        maxLength = 0;
    }
}

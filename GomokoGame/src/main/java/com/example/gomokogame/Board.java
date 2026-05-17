package com.example.gomokogame;

public class Board {
    private int size;
    private int[][] grid; // 0空, 1黑, 2白

    public Board(int size) {
        this.size = size;
        this.grid = new int[size][size];
    }

    public int getStone(int row, int col) {
        return grid[row][col];
    }

    public void setStone(int row, int col, int value) {
        grid[row][col] = value;
    }

    public boolean isEmpty(int row, int col) {
        return grid[row][col] == 0;
    }

    public void reset() {
        grid = new int[size][size];
    }

    public int getSize() {
        return size;
    }
}

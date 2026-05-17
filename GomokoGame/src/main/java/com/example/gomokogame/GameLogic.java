package com.example.gomokogame;

public class GameLogic {
    private int boardSize;

    public GameLogic(int boardSize) {
        this.boardSize = boardSize;
    }

    public void reset() {
        // 无状态需要重置
    }

    // 检查在(row, col)落下stone后是否获胜
    public boolean checkWin(Board board, int row, int col, int stone) {
        return checkDirection(board, row, col, stone, 1, 0) || // 水平
                checkDirection(board, row, col, stone, 0, 1) || // 垂直
                checkDirection(board, row, col, stone, 1, 1) || // 正斜
                checkDirection(board, row, col, stone, 1, -1);   // 反斜
    }

    private boolean checkDirection(Board board, int row, int col, int stone, int dx, int dy) {
        int count = 1;
        // 正方向延伸
        count += countConsecutive(board, row, col, stone, dx, dy);
        // 反方向延伸
        count += countConsecutive(board, row, col, stone, -dx, -dy);
        return count >= 5;
    }

    private int countConsecutive(Board board, int row, int col, int stone, int dx, int dy) {
        int count = 0;
        int r = row + dx;
        int c = col + dy;
        while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board.getStone(r, c) == stone) {
            count++;
            r += dx;
            c += dy;
        }
        return count;
    }

    // 计算给定棋子颜色的最大连珠长度（用于统计）
    public int getMaxConnectedLength(Board board, int stone) {
        int max = 0;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board.getStone(i, j) == stone) {
                    int len = getMaxLengthAt(board, i, j, stone);
                    if (len > max) max = len;
                }
            }
        }
        return max;
    }

    private int getMaxLengthAt(Board board, int row, int col, int stone) {
        int max = 1;
        max = Math.max(max, getDirectionLength(board, row, col, stone, 1, 0));
        max = Math.max(max, getDirectionLength(board, row, col, stone, 0, 1));
        max = Math.max(max, getDirectionLength(board, row, col, stone, 1, 1));
        max = Math.max(max, getDirectionLength(board, row, col, stone, 1, -1));
        return max;
    }

    private int getDirectionLength(Board board, int row, int col, int stone, int dx, int dy) {
        int count = 1;
        // 正方向
        int r = row + dx, c = col + dy;
        while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board.getStone(r, c) == stone) {
            count++;
            r += dx;
            c += dy;
        }
        // 反方向
        r = row - dx;
        c = col - dy;
        while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board.getStone(r, c) == stone) {
            count++;
            r -= dx;
            c -= dy;
        }
        return count;
    }
}

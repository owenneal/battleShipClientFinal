package org.example;

import java.util.List;
import java.util.Random;

public class AIOpponent {
    private int boardSize;
    private Random random;
    private Cell[][] board;
    private Mode mode;
    private List<int[]> targetCells;

    private enum Mode {
        HUNT, TARGET
    }

    public AIOpponent(int boardSize) {
        this.boardSize = boardSize;
        this.random = new Random();
        this.board = new Cell[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                this.board[i][j] = new Cell(i, j);
            }
        }
    }

    public void generateBoard() {
        int[] shipSizes = {5, 4, 3, 2, 1}; // Sizes of the ships to be placed
        for (int shipSize : shipSizes) {
            while (true) {
                int x = random.nextInt(boardSize);
                int y = random.nextInt(boardSize);
                String orientation = random.nextBoolean() ? "horizontal" : "vertical";

                if (canPlaceShip(x, y, shipSize, orientation)) {
                    placeShip(x, y, shipSize, orientation);
                    break;
                }
            }
        }
    }

    public int[] generateAttack() {
        int x = random.nextInt(boardSize);
        int y = random.nextInt(boardSize);
        return new int[]{x, y};
    }

    private boolean canPlaceShip(int x, int y, int shipSize, String orientation) {
        for (int i = 0; i < shipSize; i++) {
            int cellX = orientation.equals("horizontal") ? x + i : x;
            int cellY = orientation.equals("horizontal") ? y : y + i;

            if (cellX < 0 || cellX >= boardSize || cellY < 0 || cellY >= boardSize || board[cellX][cellY].ship != null) {
                return false;
            }
        }
        return true;
    }

    private void placeShip(int x, int y, int shipSize, String orientation) {
        for (int i = 0; i < shipSize; i++) {
            int cellX = orientation.equals("horizontal") ? x + i : x;
            int cellY = orientation.equals("horizontal") ? y : y + i;

            board[cellX][cellY].setShip(new Battleship(shipSize, orientation));
        }
    }

    public int[][] getBoardCopy() {
        int[][] copy = new int[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                copy[i][j] = board[i][j].ship == null ? 0 : 1;
            }
        }
        return copy;
    }

    public Cell[][] getBoard() {
        return board;
    }

    public int[] generateAttackAdvanced() {
        int x, y;

        if (mode == Mode.HUNT) {
            do {
                x = random.nextInt(boardSize);
                y = random.nextInt(boardSize);
            } while (board[x][y].isHit());
        } else {
            int[] target = targetCells.remove(0);
            x = target[0];
            y = target[1];
            if (targetCells.isEmpty()) {
                mode = Mode.HUNT;
            }
        }

        return new int[]{x, y};
    }

    public void registerHit(int x, int y) {
        board[x][y].setHit(true);
        if (board[x][y].ship != null) {
            mode = Mode.TARGET;
            addSurroundingCellsToTargetList(x, y);
        }
    }

    private void addSurroundingCellsToTargetList(int x, int y) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] direction : directions) {
            int newX = x + direction[0];
            int newY = y + direction[1];
            if (newX >= 0 && newX < boardSize && newY >= 0 && newY < boardSize && !board[newX][newY].isHit()) {
                targetCells.add(new int[]{newX, newY});
            }
        }
    }

    public void setMode(String mode) {
        if (mode.equalsIgnoreCase("HUNT")) {
            this.mode = Mode.HUNT;
        } else if (mode.equalsIgnoreCase("TARGET")) {
            this.mode = Mode.TARGET;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
    }

}
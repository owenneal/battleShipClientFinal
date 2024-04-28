package org.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ClientData implements Serializable {
    private static final long serialVersionUID = 12L;
    private int[][] array;
    private List<String> users;
    private String userName;
    private final HashMap<String, List<Coordinate>> shipCoordinates = new HashMap<>();
    private Game currentGame = null;
    private String type;
    private boolean boardCompleted = false;
    public Cell[][] cellBoard;
    private boolean ready = false;
    public int boardSize = 10;

    public ClientData(int rows, int cols, String userName, String type) {
        this.array = new int[rows][cols];
        this.userName = userName;
        this.type = type;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isBoardCompleted() {
        return boardCompleted;
    }

    public void setBoardCompleted(boolean boardCompleted) {
        this.boardCompleted = boardCompleted;
    }

    public Cell[][] getCellBoard() {
        return cellBoard;
    }

    public void setCellBoard(Cell[][] cellBoard) {
        this.cellBoard = cellBoard;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(Game currentGame) {
        this.currentGame = currentGame;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int[][] getArray() {
        return array;
    }

    public void setArray(int[][] array) {
        this.array = array;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    // Add a method to add a ship's coordinates
    public void addShipCoordinates(String shipType, int x, int y, int size, String orientation) {
        // If the ship type is not in the map, add it
        if (!shipCoordinates.containsKey(shipType)) {
            shipCoordinates.put(shipType, new ArrayList<>());
        }

        // Add the coordinates to the list of the ship type
        shipCoordinates.get(shipType).add(new Coordinate(x, y));
    }

    // Add a method to get a ship's coordinates
    public List<Coordinate> getShipCoordinates(String shipType) {
        return shipCoordinates.get(shipType);
    }
    public Set<String> getAllShipTypes() {
        return shipCoordinates.keySet();
    }

    public void resetShipCoordinates() {
        shipCoordinates.clear();
    }

    public boolean allShipsSunk() {
        for (List<Coordinate> shipCoordinates : shipCoordinates.values()) {
            for (Coordinate coordinate : shipCoordinates) {
                int x = coordinate.getX();
                int y = coordinate.getY();
                if (!cellBoard[x][y].isHit()) {
                    return false; // A ship that hasn't been sunk was found
                }
            }
        }
        return true; // All ships have been sunk
    }


}

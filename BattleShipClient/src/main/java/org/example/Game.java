package org.example;

import java.io.Serializable;
import java.util.*;

public class Game implements Serializable {
    private static final long serialVersionUID = 12L;
    private final Map<String, ClientData> clientDataMap;
    private final List<String> group;
    private final String gameName;
    private final List<String> readyPlayers = new ArrayList<>();
    private String currentPlayer;
    private boolean firstTurn = true;
    private int destroyedShipsCount = 0;
    private static final int TOTAL_SHIPS = 10;


    public Game(List<String> group, String gameName) {
        this.clientDataMap = new HashMap<>();
        this.group = group;
        this.gameName = gameName;
    }

    public void incrementDestroyedShipsCount() {
        this.destroyedShipsCount++;
    }

    public boolean allShipsDestroyed() {
        return this.destroyedShipsCount == TOTAL_SHIPS;
    }
    public boolean isFirstTurn() {
        return firstTurn;
    }

    public void setFirstTurn(boolean firstTurn) {
        this.firstTurn = firstTurn;
    }

    public void addReadyPlayer(String username) {
        readyPlayers.add(username);
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean allPlayersReady() {
        return readyPlayers.size() == group.size();
    }

    public void addPlayer(String username, ClientData clientData) {
        this.clientDataMap.put(username, clientData);
    }

    public ClientData getClientData(String username) {
        return this.clientDataMap.get(username);
    }

    public boolean allBoardsCompleted() {
        for (ClientData clientData : this.clientDataMap.values()) {
            if (!clientData.isBoardCompleted()) {
                return false;
            }
        }
        return true;
    }

    public Map<String, ClientData> getClientDataMap() {
        return this.clientDataMap;
    }

    public String getGameName() {
        return gameName;
    }

    public List<String> getGroup() {
        return group;
    }

    public boolean isPlayerTurn(String username) {
        return username.equals(currentPlayer);
    }

    public void nextPlayer() {
        int currentIndex = group.indexOf(currentPlayer);
        int nextIndex = (currentIndex + 1) % group.size();
        currentPlayer = group.get(nextIndex);
    }

    public void randomFirstPlayer() {
        Random random = new Random();
        int randomIndex = random.nextInt(group.size());
        currentPlayer = group.get(randomIndex);
    }
}

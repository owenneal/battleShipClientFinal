package org.example;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

//Owen Neal
//CS 342


public class Client extends Thread{
    private Cell[][] clientCellBoard;
    private Cell[][] opponentCellBoard;

    private int totalVictories = 0;
    Socket socketClient;
    private final BooleanProperty bothBoardsCompleteProperty = BooleanProperty.booleanProperty(new SimpleObjectProperty<>(false));
    private int[][] clientArray;
    private int[][] opponentArray;
    ObjectOutputStream out;
    ObjectInputStream in;
    private String username;
    private String opponent;
    private final BooleanProperty isGameReady = BooleanProperty.booleanProperty(new SimpleObjectProperty<>(false));
    private final Map<String, Boolean> shipsDestroyed = new HashMap<String, Boolean>() {{
//        put("ship1", false);
//        put("ship2", false);
//        put("ship3", false);
//        put("ship4", false);
        put("ship5", false);
    }};
    private Consumer<Serializable> callback;
    private final HashMap<String, List<String>> groupMessageHistory = new HashMap<>();
    private final HashMap<String, List<String>> availableGroupChats = new HashMap<>();
    private boolean userNameAssigned = true;
    private final ObjectProperty<ClientData> clientDataProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ClientData> opponentDataProperty = new SimpleObjectProperty<>();
    Game currentGame = null;
    ClientData clientData = null;
    ClientData opponentData = null;

    Client(Consumer<Serializable> call){
        this.username = generateUsername();
        callback = call;
    }
    public BooleanProperty bothBoardsCompleteProperty() {
        return bothBoardsCompleteProperty;
    }
   public void checkIfBothBoardsComplete() {
    if (clientData != null && opponentData != null && clientData.isBoardCompleted() && opponentData.isBoardCompleted()) {
        bothBoardsCompleteProperty.set(true);
        //System.out.println("Both boards are complete");
    } else {
        System.out.println("Both boards are not complete");
    }
}

    public BooleanProperty isGameReadyProperty() {
        return isGameReady;
    }

    public void setIsGameReadyProperty(boolean ready) {
        isGameReady.set(ready);
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }
    public void setClientCellBoard(Cell[][] clientCellBoard) {
        this.clientCellBoard = clientCellBoard;
    }

    public void setOpponentCellBoard(Cell[][] opponentCellBoard) {
        this.opponentCellBoard = opponentCellBoard;
    }

    public Cell[][] getClientCellBoard() {
        return clientCellBoard;
    }

    public Cell[][] getOpponentCellBoard() {
        return opponentCellBoard;
    }

    public void setClientDataComplete(boolean complete) {
        clientData.setBoardCompleted(complete);
    }

    public void setOpponentDataComplete(boolean complete) {
        opponentData.setBoardCompleted(complete);
    }

    public void setTotalVictories(int totalVictories) {
        this.totalVictories = totalVictories;
    }

    public int getTotalVictories() {
        return totalVictories;
    }

    public void setOpponentArray(int[][] opponentArray) {
        this.opponentArray = opponentArray;
    }

    public void setClientArray(int[][] clientArray) {
        this.clientArray = clientArray;
    }
    public String getUsername() {
        return username;
    }
    public Game getGame(String gameName) {
        return currentGame;
    }
    public ObjectProperty<ClientData> clientDataProperty() {
        return clientDataProperty;
    }
    public ClientData getClientData() {
        return clientDataProperty.get();
    }

    public ClientData getClientDataS() {
        return clientData;
    }
    public ClientData getOpponentData() {
        return opponentDataProperty.get();
    }
    public void setOpponentDataProperty(ClientData opponentData) {
        this.opponentDataProperty.set(opponentData);
    }
    public void setClientDataProperty(ClientData clientData) {
        this.clientDataProperty.set(clientData);
    }
    public void sendClientData(ClientData clientData) {
        System.out.println("Sending client data: " + clientData.getType());
        try {
            out.reset();
            out.writeObject(clientData);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendGameRequest(String opponent, String gameName) {
        try {
            out.writeObject(new Message(username, Collections.singletonList(opponent), "gameRequest", "gameRequest", gameName));
            System.out.println("Sent game request to " + opponent);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean getUserNameAssigned() {
        return userNameAssigned;
    }

    public String getGameName() {
        return currentGame.getGameName();
    }

    public void updateUsername(String name) {
        this.username = name;
    }
    public void setClientData(ClientData clientData) {
        this.clientData = clientData;
    }
    public String generateUsername() {
        Random rand = new Random();
        int randNum = rand.nextInt(1000);
        return "User" + randNum;
    }

    public void checkAndSendAllShipsDestroyed() {
        // Check if all the ships have been destroyed
        if (!shipsDestroyed.containsValue(false)) {
            // If all the ships have been destroyed, send a message to the server
            try {
                // Create a new Message object with the appropriate information
                Message message = new Message(username, Collections.singletonList(opponent), username, "allShipsDestroyed", currentGame.getGameName());
                //isGameOver = true;
                // Send the message to the server
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("An error occurred while sending the all ships destroyed message: " + e.getMessage());
            }
        }
    }

    public void processAttackCoordinates(int x, int y) {
        System.out.println("Processing attack coordinates");
        // Create a new Coordinate object for the attack coordinates
        Coordinate attackCoordinates = new Coordinate(x, y);

        // Check if any of the ship's coordinates match the attack coordinates
        for (String shipType : clientData.getAllShipTypes()) {
            List<Coordinate> shipCoordinates = clientData.getShipCoordinates(shipType);
            System.out.println(shipCoordinates);

            // Find the coordinate in shipCoordinates that matches the attack coordinates
            for (Coordinate shipCoordinate : shipCoordinates) {
                if (shipCoordinate.getX() == attackCoordinates.getX() && shipCoordinate.getY() == attackCoordinates.getY()) {
                    // If the ship's coordinate matches the attack coordinates, set the beenHit field of the ship's coordinate to true
                    shipCoordinate.setBeenHit(true);
                    System.out.println("Ship hit: " + shipType);
                    //System.out.println("Ship coordinates: " + shipCoordinates);
                    System.out.println(shipCoordinate.getBeenHit());
                    clientData.getCellBoard()[x][y].setHit(true);

                    // Check if all the cells of the ship have been hit
                    if (allCellsOfShipHit(shipCoordinates)) {
                        // If all the cells of the ship have been hit, update the shipsDestroyed map
                        shipsDestroyed.put(shipType, true);
                    }
                    // Break the loop as we've found the matching coordinate
                    break;
                }
            }
        }
        // Do not call checkAndSendAllShipsDestroyed here
           // checkAndSendAllShipsDestroyed();
        if (clientData.allShipsSunk()) {
            try {
                // Create a new Message object with the appropriate information
                Message message = new Message(username, Collections.singletonList(opponent), username, "allShipsDestroyed", currentGame.getGameName());
                //isGameOver = true;
                // Send the message to the server
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("An error occurred while sending the all ships destroyed message: " + e.getMessage());
            }
        }
    }


    public String checkHitOrMiss(int x, int y, int[][] array) {

        if (array[x][y] == 1) {
            return "Hit";
        } else {
            return "Miss";
        }
    }

    // Add a method to check if all the cells of a ship have been hit
    private boolean allCellsOfShipHit(List<Coordinate> shipCoordinates) {
        // All the cells of a ship have been hit if the beenHit field of all the coordinates is true
        for (Coordinate coordinate : shipCoordinates) {
            if (!coordinate.getBeenHit()) {
                System.out.println("Ship not destroyed");
                return false;

            }
        }
        System.out.println("Ship destroyed");
        return true;
    }

    // Add a getter for the shipsDestroyed map
    public Map<String, Boolean> getShipsDestroyed() {
        return shipsDestroyed;
    }

    public void run() {
        try {
            socketClient = new Socket("127.0.0.1", 5555);
            out = new ObjectOutputStream(socketClient.getOutputStream());
            in = new ObjectInputStream(socketClient.getInputStream());
            socketClient.setTcpNoDelay(true);
        } catch (Exception e) {
            System.err.println("An error occurred in the client run method: " + e.getMessage());
            callback.accept("Connection to server failed");
        }


        while (true) {
            try {
                Object data = in.readObject();
                if (data instanceof ClientData) {
                    ClientData clientDataLocal = (ClientData) data;
                    if (clientDataLocal.getType().equals("complete") && clientDataLocal.getCurrentGame().getGameName().equals(currentGame.getGameName())) {
                        if (clientDataLocal.getUserName().equals(opponent)) {
                            setOpponentDataComplete(true);
                            setOpponentDataProperty(clientDataLocal);
                            setOpponentArray(clientDataLocal.getArray());
                            setOpponentCellBoard(clientDataLocal.getCellBoard());
                            System.out.println("Opponent array updated");
                            System.out.println("Opponent array complete: " + clientDataLocal.isBoardCompleted());
                            callback.accept(clientDataLocal);
                            checkIfBothBoardsComplete();
                        }
                    } else if (clientDataLocal.getUserName().equals(username)) {
                        clientData = clientDataLocal;
                        System.out.println("Client data: " + clientDataLocal.getUserName());
                        System.out.println("Client data type: " + Arrays.deepToString(clientDataLocal.getArray()));
                        clientData.setArray(clientDataLocal.getArray());
                        setClientDataProperty(clientDataLocal);
                        setClientArray(clientData.getArray());
                        callback.accept(clientData);
                        setClientArray(clientDataLocal.getArray());
                        setClientCellBoard(clientDataLocal.getCellBoard());
                        setClientDataProperty(clientDataLocal);
                        System.out.println("Client array updated");
                        System.out.println("Client array complete: " + clientDataLocal.isBoardCompleted());
                    }
                } else if (data instanceof Message) {
                    Message message = (Message) data;
                    switch (message.getType()) {
                        case "midGame":
                            System.out.println("Received midGame message");
                            callback.accept(message);
                            break;
                        case "group":
                            updateAvailableGroupChats(message.getGroupName(), message.getReceiver());
                            updateGroupChatHistory(message.getGroupName(), message.getSender() + ": " + message.getMessage());
                            break;
                        case "usernameCheck":
                            callback.accept(message);
                            break;
                        case "gameCheck":
                            //latch.countDown();
                            callback.accept(message);
                            break;
                        case "gameRequest":
                            opponent = message.getSender();
                            System.out.println("Received game request from " + message.getSender());
                            Platform.runLater(() -> {
                                boolean accept = isAccept(message);
                                try {
                                    out.writeObject(new Message(username, Collections.singletonList(message.getSender()), accept ? "accept" : "reject", "gameRequestResponse", message.getGroupName()));
                                    out.flush();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                               // accept = false;
                            });
                            break;
                        case "gameRequestResponse":
                            if (message.getMessage().equals("accept")) {
                                System.out.println("Game request accepted");
                                //setIsGameReadyProperty(true);
                                // callback.accept("Game request accepted");
                                //TODO need to set the opponent
                            } else {
                                System.out.println("Game request rejected");
                            }
                            break;
                        case "attackResult":
                            String result = message.getMessage();
                            System.out.println("Attack result: " + result);
                            String[] parts = result.split(" at ");

                            // Get the hit
                            String hit = parts[0];

                            // Get the coordinates string and remove the parentheses
                            String coordinates = parts[1].replace("(", "").replace(")", "");

                            // Split the coordinates string by the ", " delimiter
                            String[] coordinatesParts = coordinates.split(", ");

                            // Convert the x and y coordinates from strings to integers
                            int x = Integer.parseInt(coordinatesParts[0]);
                            int y = Integer.parseInt(coordinatesParts[1]);

                            //Now you have the hit and the x and y coordinates
                            System.out.println("Hit: " + hit);
                            System.out.println("x: " + x);
                            System.out.println("y: " + y);
                            Message a = new Message(message.getSender(), null, hit, "attackResult", currentGame.getGameName());
                            callback.accept(a);
                            // checkAndSendAllShipsDestroyed();
                            if (message.getSender().equals(opponent) && hit.equals("Hit")) {
                                processAttackCoordinates(x, y);
                            }
                            break;
                        case "gameOver":
                            // resetGameData();
                            callback.accept(message);
                            //endGameAndPrepareForNewOne();

                            break;
                        default:
                            callback.accept(message);
                            break;
                    }
                } else if (data instanceof Game) {

                    Game game = (Game) data;
                    currentGame = game;

                    System.out.println("Received game data" + game.getGameName() + game.getGroup());

                    ClientData opponentData = game.getClientDataMap().values().stream().filter(data1 -> !data1.getUserName().equals(username)).findFirst().orElse(null);
                    opponent = opponentData.getUserName();
                    this.opponentData = game.getClientData(opponent);
                    setOpponentDataProperty(opponentData);
                    assert opponentData != null;

                    setOpponentArray(opponentData.getArray());
                    System.out.println("Opponent data: " + opponentData.getUserName());

                    setClientDataProperty(game.getClientData(username));
                    this.clientData = game.getClientData(username);
                    setClientArray(clientData.getArray());
                    callback.accept(clientData);

                } else {
                    //callback.accept(data);
                }
            } catch (Exception e) {
                System.err.println("An error occurred while processing messages: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean isAccept(Message message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Request");
        alert.setHeaderText("Game Request from: " + message.getSender());
        alert.setContentText("Do you accept the game request?: " + message.getSender() + " wants to play a game with you.");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public void endGameAndPrepareForNewOne() {
        // Reset game-related variables
        clientData.setCurrentGame(null);
        clientData.resetShipCoordinates();
        setClientDataComplete(false);
        setOpponentDataComplete(false);
        currentGame = null;
        opponentData = null;
        opponent = null;
        bothBoardsCompleteProperty.set(false);
        opponentDataProperty.set(null);
        clientDataProperty.set(null);
        opponentCellBoard = null;
        clientCellBoard = null;
        clientArray = null;
        opponentArray = null;
        shipsDestroyed.clear();
        shipsDestroyed.put("ship5", false);
        isGameReady.set(false);
        clientData.setType("reset");
        clientData.setBoardCompleted(false);
        sendClientData(clientData);

    }

    public void sendClientDataRequest(CountDownLatch latch) {
        try {
            out.writeObject(new Message(username, null, "requesting client data", "clientDataRequest", null));
            out.flush();
            latch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendReady(String data, List<String> recipients, String group) {
        try {
            List<String> rec = new ArrayList<>(recipients);
            out.writeObject(new Message(username, rec, data, "ready", group));
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendAttackCoordinates(int x, int y) {
        try {
            // Create a new Message object with the coordinates of the clicked cell
            Message message = new Message(username, Collections.singletonList(opponent), "(" + x + ", " + y + ")", "attack", currentGame.getGameName());
            // Send the message to the server
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("An error occurred while sending the attack coordinates: " + e.getMessage());
        }
    }

    public void sendUsername(String name) {
        try {
            out.writeObject(new Message(name, null, " has joined the chat", "joined", null));
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void checkAvailableUserNames(String name, CountDownLatch latch) {
        try {
            out.writeObject(new Message(name, null, "checking available usernames", "usernameCheck", null));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Save the original callback
        Consumer<Serializable> originalCallback = callback;
        callback = data -> {
            if (data instanceof Message) {
                Message message = (Message) data;
                if (message.getType().equals("usernameCheck")) {
                    userNameAssigned = !message.getMessage().equals("Username taken");
                    latch.countDown();
                    //Restore the original callback
                    callback = originalCallback;
                }
            }
        };
    }

    public void updateAvailableGroupChats(String groupName, List<String> recipients) {
        //if (!availableGroupChats.containsKey(groupName)) {
        List<String> groupMessages = new ArrayList<>(recipients);
        availableGroupChats.put(groupName, groupMessages);
        System.out.println("Group: " + groupName + " has been created");
        //}
    }

    public void updateGroupChatHistory(String groupName, String message) {
        if (!groupMessageHistory.containsKey(groupName)) {
            groupMessageHistory.put(groupName, new ArrayList<>());
        }
        groupMessageHistory.get(groupName).add(message);
    }


}


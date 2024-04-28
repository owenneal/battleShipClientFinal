package org.example;

import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.Image;

import javax.xml.parsers.SAXParser;

public class GuiClient extends Application{


    //GUI components
    HashMap<String, Scene> sceneMap;
    Client clientConnection;
    ComboBox<String> groupListComboBox;
    ListView<String> listItems2;
    ObservableList<String> connectedClients = FXCollections.observableArrayList();
    ObservableList<String> groupList = FXCollections.observableArrayList();
    String currentGroup = "";
    boolean usernameAssigned;
    private List<ClientData> clientDataList = new ArrayList<>();
    private ClientData clientData;
    public String currentOpponent;
    public String currentGameName;

    private Game game;
    private CountDownLatch latch;
    boolean isGameplaySceneActive = false;

    Button attackButton = new Button("Attack");
    Label turnLabel = new Label("Your turn");
    public int[][] array;
    Label hitMissLabel = new Label();
    private Stage primaryStage;

     ChangeListener<ClientData> clientDataChangeListener = ((observable, oldValue, newValue) -> {
        if (newValue != null) {
            // Check if clientConnection.getGame() returns anything
            Game game = clientConnection.getGame(clientConnection.getClientData().getUserName());
            if (game != null) {
                Platform.runLater(() -> {
                    switchToBoardScene(primaryStage);
                });
            }
        }
    });

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        //create client connection
        clientConnection = new Client(data-> Platform.runLater(()->{
            //handle messages incoming
            if (data instanceof Message) {
                Message message = (Message) data;
                if (message.getType().equals("connectedClients")) {
                    handleConnectedUsernames(message.getReceiver());
                    if (currentGroup.equals("Global")) {
                        listItems2.getItems().add("Connected clients: " + message.getReceiver());
                    }
                }
                if (message.getType().equals("joined")) {
                    if (currentGroup.equals("Global")) {
                        listItems2.getItems().add(message.getSender() + message.getMessage());
                    }
                }
                if (message.getType().equals("group")) {
                    handleNewGroup(message.getGroupName());
                }
                if (message.getType().equals("usernameCheck")) {
                    usernameAssigned = message.getMessage().equals("Username available");
                }
                if (message.getType().equals("groupList")) {
                    List<String> groupsCopy = new ArrayList<>(message.getReceiver());
                }
                if (message.getType().equals("gameCheck")) {
                    System.out.println("Game received: " + message.getMessage());
                    latch.countDown();
                }
                if (message.getType().equals("midGame")) {
                    if (message.getMessage().equals(clientConnection.getUsername())) {
                        turnLabel.setText("Your turn");
                        attackButton.setDisable(false);
                    } else {
                        turnLabel.setText("Opponent's turn");
                        attackButton.setDisable(true);
                    }
                }
                if (message.getType().equals("attackResult")) {
                    String result = message.getMessage();
                    //System.out.println("Attack result: " + result);
                    //System.out.println("Sender: " + message.getSender() + " current user: " + clientConnection.getClientData().getUserName());
                    if (message.getSender().equals(clientConnection.getClientData().getUserName())) {
                        if (result.equals("Hit")) {
                            hitMissLabel.setText("You hit the opponent's ship");
                        } else {
                            hitMissLabel.setText("You missed the opponents ship");
                        }
                    } else {
                        if (result.equals("Hit")) {
                            hitMissLabel.setText("The opponent hit your ship");
                        } else {
                            hitMissLabel.setText("The opponent missed your ship");
                        }
                    }

                }
                if (message.getType().equals("gameOver")) {
                    System.out.println("Game over");
                    turnLabel.setText("Game over");
                    attackButton.setDisable(true);
                    //message.getMessage(); is the winner
                    clientConnection.clientDataProperty().addListener(clientDataChangeListener);
                    switchToContinueScene(primaryStage, message.getMessage());
                    System.out.println("Winner: " + message.getMessage());

                }
            } else if (data instanceof ClientData) {
                if (clientConnection.getClientData().getType().equals("initial")) {
                    clientData = (ClientData) data;
                    clientDataList.add(clientData);
                    array = clientData.getArray();
                    //clientConnection.setClientData(clientData);
                    clientConnection.setClientCellBoard(clientData.getCellBoard());
                    clientConnection.setClientArray(clientData.getArray());
                    System.out.println("ClientData received: " + Arrays.deepToString(clientData.getArray()));
                    clientConnection.sendClientData(clientConnection.getClientData());
                }

            } else if (data instanceof Game) {
                Game game = (Game) data;
                System.out.println("game");
                System.out.println("Game received: " + game);

            }
        }));

        clientConnection.start();

        clientConnection.bothBoardsCompleteProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue /*&& !isGameplaySceneActive*/){
                System.out.println("Both boards are complete");
                // Switch to the gameplay scene
                Platform.runLater(() -> {
                    // Assuming you have a method to switch to the gameplay scene
                    clientConnection.getClientData().setType("midGame");
                    sceneMap.put("gameplay", createGameplayScene());
                    primaryStage.setScene(sceneMap.get("gameplay"));
//                    isGameplaySceneActive = true;
                });
            }
        });

        clientConnection.clientDataProperty().addListener(clientDataChangeListener);

        sceneMap = new HashMap<>();
        sceneMap.put("username", createUsernameScene(primaryStage));
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setScene(sceneMap.get("username"));

        primaryStage.setTitle("BATTLESHIP");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void switchToContinueScene(Stage primaryStage, String winner) {
        sceneMap.put("continue", createContinueScene(winner));
        Platform.runLater(() -> primaryStage.setScene(sceneMap.get("continue")));
    }

    private Scene createOpponentScene(Stage primaryStage) {
        int v = clientConnection.getTotalVictories();
        BorderPane rootPane = new BorderPane();
        rootPane.setId("opponentScene");

        Label titleLabel = new Label("BATTLESHIP");
        Label opponentLabel = new Label("Select an opponent:");

        Label usernameLabel = new Label("Username: " + clientConnection.getUsername());

        Label victoriesLabel = new Label("Victories: " + v);
        victoriesLabel.getStyleClass().add("opponent-labels");


        ComboBox<String> opponentComboBox = new ComboBox<>(connectedClients);

        Button selectButton = new Button("Start");
        Button aiButton = new Button("Play against AI");
        selectButton.setDisable(true);

        TextField gameNameField = new TextField();
        gameNameField.setPromptText("Enter game name");

        CountDownLatch latch2 = new CountDownLatch(1);

        aiButton.setOnAction(e -> {

            try {
                clientConnection.sendClientDataRequest(latch2);
                latch2.await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            sceneMap.put("aiBoard", aiBoardScene());
            primaryStage.setScene(sceneMap.get("aiBoard"));
        });

        selectButton.setOnAction(e -> {
            String opponent = opponentComboBox.getSelectionModel().getSelectedItem();
            currentOpponent = opponent;
            clientConnection.setOpponent(opponent);
            if (opponent != null) {
                String gameName = gameNameField.getText();
                currentGameName = gameName;
                clientConnection.sendGameRequest(opponent, gameName);
                gameNameField.clear();
            }
            opponentComboBox.getSelectionModel().clearSelection();
        });

        gameNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Enable the select button only if both fields have a value
            selectButton.setDisable(newValue.trim().isEmpty() || opponentComboBox.getSelectionModel().getSelectedItem() == null);
        });

        // Create a listener for the opponent combo box
        opponentComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Enable the select button only if both fields have a value
            selectButton.setDisable(newValue == null || gameNameField.getText().trim().isEmpty());
        });

        VBox topVbox = new VBox(10, titleLabel, usernameLabel, victoriesLabel);
        topVbox.setAlignment(Pos.CENTER);
        topVbox.setPadding(new Insets(30, 0, 0, 0)); // Add padding to the top of the VBox

        VBox centerVbox = new VBox(10, opponentLabel, opponentComboBox, gameNameField);
        centerVbox.setAlignment(Pos.CENTER);

        // Create a VBox for the right components
        VBox bottomVbox = new VBox(10, selectButton, aiButton);
        bottomVbox.setAlignment(Pos.CENTER);

        // Set the components to the BorderPane
        rootPane.setTop(topVbox);
        rootPane.setCenter(centerVbox);
        //rootPane.setLeft(leftVbox);
        rootPane.setBottom(bottomVbox);
        rootPane.setPadding(new Insets(20, 0, 50, 0)); // Add padding to the top and bottom of the BorderPane

        opponentLabel.getStyleClass().add("opponent-labels");
        usernameLabel.getStyleClass().add("opponent-labels");
        titleLabel.getStyleClass().add("title-label");
        gameNameField.getStyleClass().add("game-name-text-field");

        Scene scene = new Scene(rootPane, 900, 700);
        scene.getStylesheets().add("file:src/main/java/org/example/style.css");
        scene.setOnMouseMoved(event -> {
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            // Calculate the new background position
            double bgPosX = -mouseX * 0.1; // Adjust the multiplier to change the speed of the movement
            double bgPosY = -mouseY * 0.1;

            // Update the background position
            scene.getRoot().setStyle("-fx-background-position: " + bgPosX + "px " + bgPosY + "px;");
        });
        return scene;
    }

    public void switchToBoardScene(Stage primaryStage) {
        sceneMap.put("board", createBoardScene());
        Platform.runLater(() -> { primaryStage.setScene(sceneMap.get("board")); });
    }

    public Scene createUsernameScene(Stage primaryStage) {
        Label usernameLabel = new Label("Enter your username: ");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(200);
        Button userNameButton = new Button("Submit");
        VBox userBox = new VBox(10, usernameLabel, usernameField, userNameButton);
        userBox.setAlignment(Pos.CENTER);
        userNameButton.setOnAction(e->{
            String username = usernameField.getText();
            if (!username.isEmpty()) {
                CountDownLatch latch = new CountDownLatch(1);
                clientConnection.checkAvailableUserNames(username, latch);
                try {
                    latch.await();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
            if (clientConnection.getUserNameAssigned()) {
                clientConnection.updateUsername(username);
                clientConnection.sendUsername(username);
                //sceneMap.put("client", createClientGui(username));
                sceneMap.put("opponent", createOpponentScene(primaryStage));
                primaryStage.setScene(sceneMap.get("opponent"));

            } else {
                usernameField.clear();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Username already taken");
                alert.setContentText("Please enter a different username");
                alert.showAndWait();
            }
        });


        BorderPane pane = new BorderPane();
        pane.setCenter(userBox);
        Scene userScene = new Scene(pane, 300, 300);
        pane.setId("usernameScene"); // Set the ID of the BorderPane (pane
        usernameLabel.getStyleClass().add("username-label");
        userNameButton.getStyleClass().add("username-button");
        usernameField.getStyleClass().add("username-text-field");
        userScene.getStylesheets().add("file:src/main/java/org/example/style.css");
        userScene.setOnMouseMoved(event -> {
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            // Calculate the new background position
            double bgPosX = -mouseX * 0.1; // Adjust the multiplier to change the speed of the movement
            double bgPosY = -mouseY * 0.1;

            // Update the background position
            userScene.getRoot().setStyle("-fx-background-position: " + bgPosX + "px " + bgPosY + "px;");
           // System.out.println("Mouse X: " + mouseX + ", Mouse Y: " + mouseY);
           // System.out.println("Background Position X: " + bgPosX + ", Background Position Y: " + bgPosY);
        });
        return userScene;
    }

    public Scene aiBoardScene() {
        final int[] shipsPlaced = {0};
        clientConnection.clientDataProperty().removeListener(clientDataChangeListener);

        Button readyButton = new Button("Ready");
        readyButton.setDisable(true); // Initially, the button is disabled
        clientData = clientConnection.getClientData();
        int boardSize = 10;
        Cell[][] board = new Cell[boardSize][boardSize];
        GridPane pane = new GridPane();

        // Create a label to display the username
        Label usernameLabel = new Label("Username: " + clientConnection.getUsername());
        usernameLabel.setTextFill(Color.WHITE); // Set the text color to white

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Cell cell = new Cell(i, j);
                cell.setOnMouseClicked(event -> {
                    if (shipsPlaced[0] == 1) {
                        readyButton.setDisable(false);
                        System.out.println("All ships placed");
                        return;
                    }
                    String orientation = event.getButton() == MouseButton.PRIMARY ? "horizontal" : "vertical";
                    int size = 5 - shipsPlaced[0]; // Set the size based on the number of ships placed
                    // Check if the ship can be placed in the cell
                    if ((orientation.equals("horizontal") && cell.x + size > boardSize) ||
                            (orientation.equals("vertical") && cell.y + size > boardSize)) {
                        System.out.println("Cannot place ship out of bounds");
                        return;
                    }

                    // Check if the cells where the ship will be placed are empty
                    for (int k = 0; k < size; k++) {
                        int x = cell.x;
                        int y = cell.y;
                        if (orientation.equals("horizontal")) {
                            x += k;
                        } else {
                            y += k;
                        }
                        if (board[x][y].ship != null) {
                            System.out.println("Cannot place ship over another ship");
                            return;
                        }
                    }

                    // Update the ClientData object's board array and place the ship in the cells
                    for (int k = 0; k < size; k++) {
                        int x = cell.x;
                        int y = cell.y;
                        if (orientation.equals("horizontal")) {
                            x += k;
                        } else {
                            y += k;
                        }
                        clientConnection.getClientData().getArray()[x][y] = 1;
                        board[x][y].setShip(new Battleship(size, orientation));
                        clientConnection.getClientData().addShipCoordinates("ship" + size, x, y, size, orientation);
                        // System.out.println("Ship coordinates: " + clientConnection.getClientData().getShipCoordinates("ship" + size));
                        if (k == 0) {
                            System.out.println("The ship starts at cell (" + cell.x + ", " + cell.y + ")");
                            System.out.println("The ship occupies the following coordinates in the array:");
                        }
                        System.out.println("(" + x + ", " + y + ")");
                    }
                    shipsPlaced[0]++; // Increment the shipsPlaced counter only when a ship is actually placed
                    // After updating the array, send the updated ClientData object back to the server
                    if (shipsPlaced[0] == 1) {
                        readyButton.setDisable(false);
                    }
                });

                board[i][j] = cell;
                pane.add(cell, i, j);
            }
        }

        readyButton.setOnAction(e -> {
            clientConnection.getClientData().setCellBoard(board);
            clientConnection.setClientCellBoard(board);
            readyButton.setDisable(true); // Disable the button after sending the ClientData object
            switchToAiGameplayScene(primaryStage);
        });
        BorderPane borderPane = new BorderPane();
        borderPane.setId("boardScene");
        borderPane.setTop(usernameLabel);
        borderPane.setCenter(pane);
        borderPane.setBottom(readyButton);
        borderPane.setPadding(new Insets(10));
        Scene scene = new Scene(borderPane, 600, 600);
        scene.getStylesheets().add("file:src/main/java/org/example/style.css");

        return scene;
    }

    public void switchToAiGameplayScene(Stage primaryStage) {
        sceneMap.put("aiGameplay", createAIGameplayScene());
        Platform.runLater(() -> { primaryStage.setScene(sceneMap.get("aiGameplay")); });
    }

    public Scene createAIGameplayScene() {
        //clientConnection.sendReady("ready", clientConnection.getGame(clientConnection.getGameName()).getGroup(), clientConnection.getGameName());
        Cell[][] clientCellBoard = clientConnection.getClientCellBoard();
        int boardSize = clientCellBoard.length;
        Label attackCoordinatesLabel = new Label("Attack coordinates: ");
        attackButton.setDisable(true);
        // Create a new grid pane for the clientCellBoard
        GridPane clientBoardPane = new GridPane();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Cell cell = clientCellBoard[i][j];
                clientBoardPane.add(cell, i, j);
            }
        }

        // Create a new grid that is clickable
        ClickableCell[][] clickableGrid = new ClickableCell[boardSize][boardSize];
        GridPane clickableGridPane = new GridPane();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                ClickableCell cell = new ClickableCell(i, j);
                cell.setOnMouseClicked(event -> {
                    // Send the message to the server
                    //clientConnection.sendAttackCoordinates(cell.x, cell.y);
                    attackCoordinatesLabel.setText("Attack coordinates: (" + cell.x + ", " + cell.y + ")");
                    if (turnLabel.getText().equals("Your turn")) {
                        attackButton.setDisable(false);
                    }
                    cell.setStyle("-fx-background-color: red;");
                });
                clickableGrid[i][j] = cell;
                clickableGridPane.add(cell, i, j);
            }
        }

        AIOpponent aiOpponent = new AIOpponent(boardSize);
        aiOpponent.generateBoard();
        int[][] arrayAI = aiOpponent.getBoardCopy();
        System.out.println("AI board: " + Arrays.deepToString(arrayAI));

        attackButton.setOnAction(e -> {
            // Get the attack coordinates from the label's text
            String[] coordinates = attackCoordinatesLabel.getText().split(": ")[1].split(", ");
            int x = Integer.parseInt(coordinates[0].substring(1));
            int y = Integer.parseInt(coordinates[1].substring(0, coordinates[1].length() - 1));

            String res = clientConnection.checkHitOrMiss(x, y, arrayAI);
            System.out.println("Result: " + res);
            if (res.equals("Hit")) {
                arrayAI[x][y] = 2;
                clickableGridPane.getChildren().get(x * boardSize + y).setStyle("-fx-background-color: red;");
            } else {
                clickableGridPane.getChildren().get(x * boardSize + y).setStyle("-fx-background-color: blue;");
            }
            // Send the attack coordinates to the server
            //  clientConnection.sendAttackCoordinates(x, y);
            // Disable the attack button and reset the label
            attackButton.setDisable(true);

            int[] attackAI = aiOpponent.generateAttack();
            clickableGridPane.getChildren().get(attackAI[0] * boardSize + attackAI[1]).setStyle("-fx-background-color: blue;");
            String aiRes = clientConnection.checkHitOrMiss(attackAI[0], attackAI[1], array);
            System.out.println("AI attack result: " + aiRes);
            if (aiRes.equals("Hit")) {
                array[attackAI[0]][attackAI[1]] = 2;
            }
            attackButton.setDisable(false);
        });


        clientBoardPane.setAlignment(Pos.CENTER);
        clickableGridPane.setAlignment(Pos.CENTER);
        // Add both panes to a VBox
        HBox hbox = new HBox(10, turnLabel, attackButton, attackCoordinatesLabel, hitMissLabel);
        hbox.setAlignment(Pos.CENTER);
        VBox vbox = new VBox(10, clientBoardPane, clickableGridPane, hbox); // 10 is the spacing between the panes
        vbox.setAlignment(Pos.CENTER);
        StackPane stackPane = new StackPane();
        stackPane.setId("aiGameplayScene");
        stackPane.getChildren().add(vbox);
        stackPane.setAlignment(Pos.CENTER);
        Scene scene = new Scene(stackPane, 750, 750);
        scene.getStylesheets().add("file:src/main/java/org/example/style.css");

        return scene;
    }

    public Scene createBoardScene() {
        final int[] shipsPlaced = {0};
        clientConnection.clientDataProperty().removeListener(clientDataChangeListener);

        Button readyButton = new Button("Ready");
        readyButton.setDisable(true); // Initially, the button is disabled
        game = clientConnection.getGame(clientConnection.getGameName());
        clientData = clientConnection.getClientData();
        int boardSize = clientConnection.getClientData().getArray().length;
        Cell[][] board = new Cell[boardSize][boardSize];
        GridPane pane = new GridPane();

        // Create a label to display the username
        Label usernameLabel = new Label(clientConnection.getUsername());
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Cell cell = new Cell(i, j);
                cell.setOnMouseClicked(event -> {
                    if (shipsPlaced[0] == 1) {
                        readyButton.setDisable(false);
                        System.out.println("All ships placed");
                        return;
                    }
                    String orientation = event.getButton() == MouseButton.PRIMARY ? "horizontal" : "vertical";
                    int size = 5 - shipsPlaced[0]; // Set the size based on the number of ships placed
                    // Check if the ship can be placed in the cell
                    if ((orientation.equals("horizontal") && cell.x + size > boardSize) ||
                            (orientation.equals("vertical") && cell.y + size > boardSize)) {
                        System.out.println("Cannot place ship out of bounds");
                        return;
                    }

                    // Check if the cells where the ship will be placed are empty
                    for (int k = 0; k < size; k++) {
                        int x = cell.x;
                        int y = cell.y;
                        if (orientation.equals("horizontal")) {
                            x += k;
                        } else {
                            y += k;
                        }
                        if (board[x][y].ship != null) {
                            System.out.println("Cannot place ship over another ship");
                            return;
                        }
                    }

                    // Update the ClientData object's board array and place the ship in the cells
                    for (int k = 0; k < size; k++) {
                        int x = cell.x;
                        int y = cell.y;
                        if (orientation.equals("horizontal")) {
                            x += k;
                        } else {
                            y += k;
                        }
                        clientConnection.getClientData().getArray()[x][y] = 1;
                        board[x][y].setShip(new Battleship(size, orientation));
                        clientConnection.getClientData().addShipCoordinates("ship" + size, x, y, size, orientation);
                       // System.out.println("Ship coordinates: " + clientConnection.getClientData().getShipCoordinates("ship" + size));
                        if (k == 0) {
                            System.out.println("The ship starts at cell (" + cell.x + ", " + cell.y + ")");
                            System.out.println("The ship occupies the following coordinates in the array:");
                        }
                        System.out.println("(" + x + ", " + y + ")");
                    }
                    shipsPlaced[0]++; // Increment the shipsPlaced counter only when a ship is actually placed
                    // After updating the array, send the updated ClientData object back to the server
                    if (shipsPlaced[0] == 1) {
                        readyButton.setDisable(false);
                    }


                });

                board[i][j] = cell;
                pane.add(cell, i, j);
            }
        }

        readyButton.setOnAction(e -> {
            // Send the ClientData object to the server when the button is clicked
            clientConnection.getClientData().setType("complete");
            clientConnection.setClientDataComplete(true);
            clientConnection.getClientData().setCellBoard(board);
            clientConnection.setClientCellBoard(board);
            clientConnection.getClientData().setBoardCompleted(true);
            readyButton.setDisable(true); // Disable the button after sending the ClientData object
            clientConnection.sendClientData(clientConnection.getClientData());
            clientConnection.checkIfBothBoardsComplete();
        });


        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(20));
        //borderPane.setTop(usernameLabel);
        pane.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Place your ships: ");
        titleLabel.getStyleClass().add("title-label");

        VBox vbox = new VBox(10, usernameLabel, titleLabel, pane, readyButton);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(50, 0, 0, 0)); // Add more padding to the top of the VBox

        borderPane.setCenter(vbox);
        Scene scene = new Scene(borderPane, 800, 600);
        borderPane.setId("boardScene");
        readyButton.getStyleClass().add("ready-button");
        usernameLabel.getStyleClass().add("username-label");

        scene.setOnMouseMoved(event -> {
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            // Calculate the new background position
            double bgPosX = -mouseX * 0.1; // Adjust the multiplier to change the speed of the movement
            double bgPosY = -mouseY * 0.1;

            // Update the background position
            scene.getRoot().setStyle("-fx-background-position: " + bgPosX + "px " + bgPosY + "px;");
            // System.out.println("Mouse X: " + mouseX + ", Mouse Y: " + mouseY);
            // System.out.println("Background Position X: " + bgPosX + ", Background Position Y: " + bgPosY);
        });


        scene.getStylesheets().add("file:src/main/java/org/example/style.css");

        return scene;
    }
    public Scene createGameplayScene() {
        clientConnection.sendReady("ready", clientConnection.getGame(clientConnection.getGameName()).getGroup(), clientConnection.getGameName());
        Cell[][] clientCellBoard = clientConnection.getClientCellBoard();
        int boardSize = clientCellBoard.length;
        Label attackCoordinatesLabel = new Label("Attack coordinates: ");
        attackButton.setDisable(true);
        // Create a new grid pane for the clientCellBoard
        GridPane clientBoardPane = new GridPane();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Cell cell = clientCellBoard[i][j];
                clientBoardPane.add(cell, i, j);
            }
        }

        // Create a new grid that is clickable
        ClickableCell[][] clickableGrid = new ClickableCell[boardSize][boardSize];
        GridPane clickableGridPane = new GridPane();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                ClickableCell cell = new ClickableCell(i, j);
                cell.setOnMouseClicked(event -> {
                    // Send the message to the server
                    //clientConnection.sendAttackCoordinates(cell.x, cell.y);
                    attackCoordinatesLabel.setText("Attack coordinates: (" + cell.x + ", " + cell.y + ")");
                    if (turnLabel.getText().equals("Your turn")) {
                        attackButton.setDisable(false);
                    }
                    cell.setStyle("-fx-background-color: red;");
                });
                clickableGrid[i][j] = cell;
                clickableGrid[i][j].setStyle("-fx-background-color: red;");
                clickableGridPane.add(cell, i, j);
            }
        }

        Image missileImage = new Image("file:styleImages/shipImages/missileLaunch.jpg"); // replace with the path to your missile image
        ImageView missileView = new ImageView(missileImage);
        missileView.setFitWidth(50); // adjust size as needed
        missileView.setFitHeight(50); // adjust size as needed
        Rotate rotate = new Rotate(90, Rotate.Z_AXIS);
        missileView.getTransforms().add(rotate);


        clientBoardPane.setAlignment(Pos.CENTER);
        clientBoardPane.setStyle("-fx-background-color: transparent;");
        clickableGridPane.setAlignment(Pos.CENTER);
        // Add both panes to a VBox
        HBox hbox = new HBox(10, turnLabel, attackButton, attackCoordinatesLabel, hitMissLabel);
        hbox.setAlignment(Pos.CENTER);
        VBox vbox = new VBox(10, clientBoardPane, clickableGridPane, hbox); // 10 is the spacing between the panes
        vbox.setAlignment(Pos.CENTER);

        StackPane stackPane = new StackPane();

        stackPane.setAlignment(Pos.CENTER);
        stackPane.setId("gameplayScene");

//        Pane pane = new Pane();
//        pane.getChildren().add(vbox);

        Scene scene = new Scene(stackPane, 900, 700);
        scene.getStylesheets().add("file:src/main/java/org/example/style.css");

        // 2. Set the initial position of the missile
        missileView.setX(-50); // start off-screen
        missileView.setY(scene.getHeight() / 2); // middle of the screen

        // Add the missile to the scene
        //stackPane.getChildren().add(missileView);
        stackPane.getChildren().add(missileView);
        stackPane.getChildren().add(vbox);

        // 3. Create a TranslateTransition for the missile
        TranslateTransition missileAnimation = new TranslateTransition(Duration.seconds(2), missileView); // adjust duration as needed
        missileAnimation.setByX(scene.getWidth() + 50);

        scene.setOnMouseMoved(event -> {
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            // Calculate the new background position
            double bgPosX = -mouseX * 0.1; // Adjust the multiplier to change the speed of the movement
            double bgPosY = -mouseY * 0.1;

            // Update the background position
            scene.getRoot().setStyle("-fx-background-position: " + bgPosX + "px " + bgPosY + "px;");
            // System.out.println("Mouse X: " + mouseX + ", Mouse Y: " + mouseY);
            // System.out.println("Background Position X: " + bgPosX + ", Background Position Y: " + bgPosY);
        });

        attackButton.setOnAction(e -> {
            // Get the attack coordinates from the label's text
            String[] coordinates = attackCoordinatesLabel.getText().split(": ")[1].split(", ");
            int x = Integer.parseInt(coordinates[0].substring(1));
            int y = Integer.parseInt(coordinates[1].substring(0, coordinates[1].length() - 1));

            // Send the attack coordinates to the server
            clientConnection.sendAttackCoordinates(x, y);

            // Disable the attack button and reset the label
            attackButton.setDisable(true);
            attackCoordinatesLabel.setText("Attack coordinates: None");
            missileAnimation.playFromStart();

        });

        return scene;
    }

    public Scene createContinueScene(String winner) {
        // Create a label to display the winner
        Label winnerLabel = new Label("Winner: " + winner);
        winnerLabel.setTextFill(Color.WHITE); // Set the text color to white
        winnerLabel.setStyle("-fx-font-size: 20px;"); // Set the font size to 20px

        // Create a button to continue to the opponent selection screen
        Button continueButton = new Button("Continue");
        continueButton.setOnAction(e -> {
            // Switch to the opponent selection scene
            clientConnection.endGameAndPrepareForNewOne();
            sceneMap.put("opponent", createOpponentScene(primaryStage));
            primaryStage.setScene(sceneMap.get("opponent"));
        });

        // Create a button to exit the application
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> {
            // Exit the application
            Platform.exit();
            System.exit(0);
        });

        // Add the label and buttons to a VBox
        VBox vbox = new VBox(10, winnerLabel, continueButton, exitButton); // 10 is the spacing between the nodes
        vbox.setAlignment(Pos.CENTER);
        System.out.println("Winner: " + winner);
        System.out.println("Client username: " + clientConnection.getUsername());
        if (winner.equals(clientConnection.getUsername())) {
            System.out.println("You won!");
            Image backgroundImage = new Image("file:styleImages/shipImages/continueSceneWin.jpg"); // replace with the path to your winning background image
            vbox.setBackground(new Background(new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        } else {
            System.out.println("You lost!");
            Image backgroundImage = new Image("file:styleImages/shipImages/continueSceneLoss.jpg"); // replace with the path to your losing background image
            vbox.setBackground(new Background(new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        }

        // Create a new scene with the VBox
        Scene scene = new Scene(vbox, 300, 300);
        ///return new Scene(vbox, 500, 500);
        //scene.getStylesheets().add("file:src/main/java/org/example/style.css");
        return scene;
    }

    public void handleConnectedUsernames(List<String> u) {
        Platform.runLater(()->{
            if (!u.isEmpty()) {
                this.connectedClients.clear();
                this.connectedClients.addAll(u);
                //recipientListComboBox.setItems(connectedClients);
            }
        });
    }

    public void handleNewGroup(String groupName) {
        Platform.runLater(()->{
            if (!this.groupList.contains(groupName)) {
                this.groupList.add(groupName);
                groupListComboBox.setItems(groupList);
            }
        });
    }
}
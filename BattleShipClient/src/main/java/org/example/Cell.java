package org.example;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.Serializable;

public class Cell extends Rectangle implements Serializable {
    private static final long serialVersionUID = 12L;

    public static final int SIZE = 30;
    public int x, y;
    public Battleship ship = null;
    int shipSize;

    private boolean isHit;


    public Cell(int x, int y) {
        super(SIZE, SIZE);
        this.x = x;
        this.y = y;
        this.isHit = false;
        setFill(Color.TRANSPARENT);
        setStroke(Color.BLACK);
//        setTranslateX(x * SIZE);
//        setTranslateY(y * SIZE);
    }

    public void setShip(Battleship ship) {
        this.ship = ship;
        setFill(Color.TRANSPARENT);
        setStroke(Color.GREEN);
    }

    public void setHit(boolean isHit) {
        this.isHit = isHit;
        if (isHit) {
            // Change the cell's color to indicate it has been hit
            setFill(Color.RED);
        } else {
            setFill(Color.BLUE);
        }
    }

    public boolean isHit() {
        return isHit;
    }
}


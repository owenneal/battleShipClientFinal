package org.example;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.Serializable;

public class ClickableCell extends Rectangle implements Serializable {
    private static final long serialVersionUID = 12L;
    public static final int SIZE = 30;
    public int x, y;

    public ClickableCell(int x, int y) {
        super(SIZE, SIZE);
        this.x = x;
        this.y = y;
        setFill(Color.WHITE);
        setStroke(Color.BLACK);
    }
}

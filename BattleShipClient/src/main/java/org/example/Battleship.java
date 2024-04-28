package org.example;

import java.io.Serializable;

public class Battleship implements Serializable {
    private static final long serialVersionUID = 12L;
    private int x;
    private int y;
    private int size;
    private String orientation;

    public Battleship(int size, String orientation) {
        this.size = size;
        this.orientation = orientation;

    }

    // getters and setters...
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }
}

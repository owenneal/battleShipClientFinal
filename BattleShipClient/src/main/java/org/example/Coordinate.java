package org.example;

import java.io.Serializable;

public class Coordinate implements Serializable {
    private static final long serialVersionUID = 12L;

    private final int x;
    private final int y;
    private boolean beenHit;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
        this.beenHit = false;
    }

    public void setBeenHit(boolean beenHit) {
        this.beenHit = beenHit;
    }

    public boolean getBeenHit() {
        return beenHit;
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

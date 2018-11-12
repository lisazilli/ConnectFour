package com.example.android.connectfour;

public class GameService {
    private static GameService _instance = new GameService();
    public static final int RED  = 1;
    public static final int YELLOW  = 2;
    public static final int NONE  = 0;
    private int color = NONE;

    public static GameService getInstance() {
        return _instance;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}

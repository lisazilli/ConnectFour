package com.example.android.connectfour;

public class GameService {
    private static GameService _instance = new GameService();
    public static final int RED  = 1;
    public static final int YELLOW  = 2;
    public static final int NONE  = 0;
    private int color = NONE;
    private int rows = 0;
    private int columns = 0;

    private Boolean gameInplay = Boolean.FALSE;

    public CellView[][] cells;

    public static GameService getInstance() {
        return _instance;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void createCells(int rows, int columns){
        gameInplay = Boolean.TRUE;
        this.rows = rows;
        this.columns = columns;
        cells = new CellView[rows][columns];
    }

    public void setCell(CellView cellView) {
        cells[cellView.getRow()][cellView.getColumn()] = cellView;
    }

    public boolean isWon() {
        //Check to see if there are four consecutive numbers in a row
        for (int row = 0; row < rows; row++){
            for (int column = 0; column < columns - 3; column++){
                if (cells[row][column].color == this.color &&
                        cells[row][column + 1].color == this.color &&
                        cells[row][column + 2].color == this.color &&
                        cells[row][column + 3].color == this.color){

                    //????Show winning cells
                    gameInplay = Boolean.FALSE;
                    return Boolean.TRUE;

                }
            }
        }


        //Check to see if there are four consecutive numbers in a column
        for (int column = 0; column < columns; column++){
            for (int row = 0; row <  rows - 3; row++){
                if (cells[row][column].color == this.color &&
                        cells[row + 1][column].color == this.color &&
                        cells[row + 2][column].color == this.color &&
                        cells[row + 3][column].color == this.color){

                    //????Show winning cells
                    gameInplay = Boolean.FALSE;
                    return Boolean.TRUE;
                }
            }
        }
        //Check to see if there are four consecutive numbers on major diagonals
        for (int row = 0; row < rows - 3; row++){
            for (int column = 0; column < columns - 3; column++){
                if (cells[row][column].color == this.color &&
                        cells[row + 1][column + 1].color == this.color &&
                        cells[row + 2][column + 2].color == this.color &&
                        cells[row + 3][column + 3].color == this.color){

                    //????Show winning cells
                    gameInplay = Boolean.FALSE;
                    return Boolean.TRUE;
                }
            }
        }
        //Check to see if there are four consecutive numbers on sub diagonals
        for (int row = 0; row < rows - 3; row++){
            for (int column = 3; column < columns; column++){
                if (cells[row][column].color == this.color &&
                        cells[row + 1][column - 1].color == this.color &&
                        cells[row + 2][column - 2].color == this.color &&
                        cells[row + 3][column - 3].color == this.color){

                    //????Show winning cells
                    gameInplay = Boolean.FALSE;
                    return Boolean.TRUE;
                }
            }
        }

        return Boolean.FALSE;
    }

    //Check to see if the board is full
    public Boolean isFull(){
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                if (cells[i][j].color == NONE){
                    return Boolean.FALSE;
                }
            }
        }
        gameInplay = Boolean.FALSE;
        return Boolean.TRUE;
    }
    //Check cell availability
    public Boolean isAvailable(int row, int column){
        if (row == (rows - 1))
            return Boolean.TRUE;

        //If cell below is occupied, set cell availability to true
        if (cells[row + 1][column].color != NONE){
            return Boolean.TRUE;
        }
        //If cell below isn't occupied, set cell availability to false
        else {
            return Boolean.FALSE;
        }
    }

    public Boolean getGameInplay() {
        return gameInplay;
    }

    public void setGameInplay(Boolean gameInplay) {
        this.gameInplay = gameInplay;
    }
}

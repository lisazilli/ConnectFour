package com.example.android.connectfour;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

public class PlayView implements View.OnClickListener {

    private MainActivity activity;

    public PlayView( MainActivity activity ) {
        this.activity = activity;

        Button resetBoard = activity.findViewById(R.id.reset);
        resetBoard.setOnClickListener( this );
    }

    public void onClick(View v) {
        /*if ( v.getId() == R.id.startGame ) {
            if ( GameService.getInstance().getColor() != GameService.NONE) {
                Toast.makeText(activity.getApplicationContext(),
                        "Game is in session. Reset to start a new game.",
                        Toast.LENGTH_LONG).show();
                return;
            } else {
                GameService.getInstance().setColor(GameService.YELLOW);
            }
        }*/
        if ( v.getId() == R.id.reset ) {
            reset();
        }
    }

    public void hostReset () {
        // For the host reset do not clear client address
        activity.gameboard.removeAllViews();
        createBoard();
        activity.setupGameArea(false);
        new WriteThread (activity, activity.p2pClientAddress, MainActivity.P2P_ClIENT_PORT,
                MainActivity.RESET_MSG, null).start();

    }

    public void reset() {
        if (activity.p2pInfo.groupFormed && activity.p2pInfo.isGroupOwner) {
            hostReset();
        } else {
            activity.gameboard.removeAllViews();
            activity.p2pClientAddress = null;
            createBoard();
            activity.setupGameArea(false);
        }
    }

    public void respondToHostReset() {
        activity.gameboard.removeAllViews();
        createBoard();
        activity.setupGameArea(false);
    }

    public void createBoard() {
        int width = activity.gameboard.getWidth();
        int height = activity.gameboard.getHeight();
        int columns = activity.gameboard.getColumnCount();
        int rows = activity.gameboard.getRowCount();
        int numTiles = columns*rows;

        int columnSize = (width/columns);
        int rowSize = (height/rows);
        int margin = 0;

        GameService.getNewInstance().createCells(rows,columns);
        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++) {
                CellView cell = new CellView(activity);
                cell.setPadding(0, 0, 0, 0);
                GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
                layoutParams.width = columnSize - (2*margin);
                layoutParams.height = rowSize - (2*margin);
                layoutParams.leftMargin = margin;
                layoutParams.rightMargin = margin;
                layoutParams.topMargin = margin;
                layoutParams.bottomMargin = margin;
                cell.setLayoutParams(layoutParams);
                cell.setPosition(i,j);
                GameService.getInstance().setCell(cell);
                activity.gameboard.addView(cell);
            }
        }
        activity.gameboard.invalidate();
        Log.d("ConnectFour", String.format("Created [%d] cells.", numTiles));
        // Set the color for players
        if (activity.p2pInfo.groupFormed && activity.p2pInfo.isGroupOwner) {
            GameService.getInstance().setColor(GameService.YELLOW);
        } else {
            GameService.getInstance().setColor(GameService.RED);
        }
    }
}

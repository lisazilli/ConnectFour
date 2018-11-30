package com.example.android.connectfour;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

    private GridLayout gameboard;
    WifiP2pManager.Channel p2pChannel;
    WifiP2pManager p2pManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        Button resetBoard = this.findViewById(R.id.reset);
        resetBoard.setOnClickListener( this );

        Button startGame = this.findViewById(R.id.startGame);
        startGame.setOnClickListener( this );

        Button quitGame = this.findViewById(R.id.quitGame);
        quitGame.setOnClickListener( this );

        this.gameboard = this.findViewById(R.id.boardview);
        this.gameboard.getViewTreeObserver().addOnGlobalLayoutListener(this.boardLayoutListener());
    }

    public void onClick(View v) {
        if ( v.getId() == R.id.startGame ) {
            if ( GameService.getInstance().getColor() != GameService.NONE) {
                Toast.makeText(this,
                        "Game is in session. Reset to start a new game.",
                        Toast.LENGTH_LONG).show();
                return;
            } else {
                GameService.getInstance().setColor(GameService.YELLOW);
            }
        }
        if ( v.getId() == R.id.reset ) {
            GameService.getInstance().setColor(GameService.NONE);
            PlayActivity.this.gameboard.removeAllViews();
            createBoard();
        }
        if ( v.getId() == R.id.quitGame ) {

        }
    }

    public void createBoard() {
        int width = PlayActivity.this.gameboard.getMeasuredWidth();
        int height = PlayActivity.this.gameboard.getMeasuredHeight();
        int columns = PlayActivity.this.gameboard.getColumnCount();
        int rows = PlayActivity.this.gameboard.getRowCount();
        int numTiles = columns*rows;

        double columnSize = (width/columns);
        double rowSize = (height/rows);

        int cellSize = (int) Math.floor((columnSize<rowSize?columnSize:rowSize));

        GameService.getInstance().createCells(rows,columns);

        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++) {
                CellView cell = new CellView(PlayActivity.this);
                cell.setPadding(0, 0, 0, 0);
                GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
                layoutParams.width = cellSize;
                layoutParams.height = cellSize;
                layoutParams.leftMargin = 0;
                layoutParams.rightMargin = 0;
                layoutParams.topMargin = 5;
                layoutParams.bottomMargin = 5;
                cell.setLayoutParams(layoutParams);
                cell.setPosition(i,j);
                GameService.getInstance().setCell(cell);
                PlayActivity.this.gameboard.addView(cell);
                PlayActivity.this.gameboard.getLayoutParams().width = cellSize * columns;
                PlayActivity.this.gameboard.getLayoutParams().height = cellSize * rows;
            }
        }
    }

    ViewTreeObserver.OnGlobalLayoutListener boardLayoutListener()
    {
        return new ViewTreeObserver.OnGlobalLayoutListener()
        {

            @Override
            public void onGlobalLayout()
            {
                PlayActivity.this.createBoard();

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    PlayActivity.this.gameboard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else
                {
                    PlayActivity.this.gameboard.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        };
    }
}

package com.example.android.connectfour;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private GridLayout gameboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button resetBoard = this.findViewById(R.id.reset);
        resetBoard.setOnClickListener( this );

        Button startGame = this.findViewById(R.id.startGame);
        startGame.setOnClickListener( this );

        Button generateCode = this.findViewById(R.id.generateCode);
        generateCode.setOnClickListener( this );

        Button enterCode = this.findViewById(R.id.enterCode);
        enterCode.setOnClickListener( this );

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
            MainActivity.this.gameboard.removeAllViews();
            createBoard();
        }
        if ( v.getId() == R.id.generateCode ) {
            generateCode();
        }
    }

    public void createBoard() {
        int width = MainActivity.this.gameboard.getMeasuredWidth();
        int height = MainActivity.this.gameboard.getMeasuredHeight();
        int columns = MainActivity.this.gameboard.getColumnCount();
        int rows = MainActivity.this.gameboard.getRowCount();
        int numTiles = columns*rows;

        double columnSize = (width/columns);
        double rowSize = (height/rows);

        int cellSize = (int) Math.floor((columnSize<rowSize?columnSize:rowSize));

        GameService.getInstance().createCells(rows,columns);

        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++) {
                CellView cell = new CellView(MainActivity.this);
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
                MainActivity.this.gameboard.addView(cell);
                MainActivity.this.gameboard.getLayoutParams().width = cellSize * columns;
                MainActivity.this.gameboard.getLayoutParams().height = cellSize * rows;
            }
        }
    }

    //
    // Source: https://gist.github.com/Fast0n/1c34728a1dc7adce57ad0f6d8133d46d
    // Some modifications made
    //
    public void generateCode()
    {
        final String DATA = "123456789ABCDEFGHJKLMNOPQRSTUVWXYZ";
        final int CODE_LENGTH = 4;
        Random RANDOM = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(DATA.charAt(RANDOM.nextInt(DATA.length())));
        }
        Toast.makeText(this,
                "Code: " + sb.toString(),
                Toast.LENGTH_LONG).show();
    }

    ViewTreeObserver.OnGlobalLayoutListener boardLayoutListener()
    {
        return new ViewTreeObserver.OnGlobalLayoutListener()
        {

            @Override
            public void onGlobalLayout()
            {
                MainActivity.this.createBoard();

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    MainActivity.this.gameboard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else
                {
                    MainActivity.this.gameboard.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        };
    }
}

package com.example.android.connectfour;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;

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

        this.gameboard = this.findViewById(R.id.boardview);
        this.gameboard.getViewTreeObserver().addOnGlobalLayoutListener(this.boardLayoutListener());
    }

    public void onClick(View v) {
        if ( v.getId() == R.id.startGame ) {
            GameService.getInstance().setColor(GameService.YELLOW);
        }
        if ( v.getId() == R.id.reset ) {
            GameService.getInstance().setColor(GameService.NONE);
            MainActivity.this.gameboard.removeAllViews();
            createBoard();
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

        for(int x=0;x<=numTiles-1;x++)
        {
            CellView cell = new CellView(MainActivity.this);
            cell.setPadding(0, 0, 0, 0);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
            layoutParams.width = cellSize;
            layoutParams.height = cellSize;
            layoutParams.leftMargin=0;
            layoutParams.rightMargin=0;
            layoutParams.topMargin=5;
            layoutParams.bottomMargin=5;
            cell.setLayoutParams(layoutParams);

            MainActivity.this.gameboard.addView(cell);
            MainActivity.this.gameboard.getLayoutParams().width=cellSize * columns;
            MainActivity.this.gameboard.getLayoutParams().height=cellSize * rows;
        }
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

package com.example.android.connectfour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class CellView extends View {

    int color;
    int row = 0;
    int column = 0;

    private RectF rect;
    private Paint paint;

    public CellView(Context context) {
        super(context);
        color = new Integer(GameService.getInstance().getColor());
    }

    public CellView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        color = new Integer(GameService.getInstance().getColor());
    }

    public CellView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        color = new Integer(GameService.getInstance().getColor());
    }

    public void setPosition(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow(){
        return this.row;
    }

    public int getColumn() {
        return this.column;
    }

    public boolean onTouchEvent( MotionEvent event ) {
        if ( event.getAction() != MotionEvent.ACTION_UP )
            return true;

        if (!GameService.getInstance().getGameInplay()){
            Toast.makeText(this.getContext(),
                    "Game is over, please restart the game",
                    Toast.LENGTH_LONG).show();
            return true;
        }
        /*Toast.makeText(this.getContext(),
                String.format("Test Square[%d,%d]",row,column),
                Toast.LENGTH_LONG).show();*/
        if ( color > 0 || !GameService.getInstance().isAvailable(row,column)) {
            Toast.makeText(this.getContext(),
                    "Square not available, please select an available square.",
                    Toast.LENGTH_LONG).show();
            return true;
        }

        color = new Integer(GameService.getInstance().getColor());
        if (color == GameService.YELLOW){
            color = GameService.RED;
            GameService.getInstance().setColor(color);
        } else if (color == GameService.RED){
            color = GameService.YELLOW;
            GameService.getInstance().setColor(color);
        } else {
            Toast.makeText(this.getContext(),
                    "Please start a game first!",
                    Toast.LENGTH_LONG).show();
            return true;
        }

        if (GameService.getInstance().isWon()) {
            Toast.makeText(this.getContext(),
                    String.format("Player %d wins!", color),
                    Toast.LENGTH_LONG).show();
        } else if (GameService.getInstance().isFull()) {
            Toast.makeText(this.getContext(),
                    String.format("Board is full. No winners."),
                    Toast.LENGTH_LONG).show();
        }
        invalidate();
        return true;
    }

    protected void onDraw(Canvas canvas) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        canvas.drawRect(0,0,canvas.getWidth(),canvas.getHeight(), paint);

        //Mark as yellow
        if (GameService.getInstance().getColor() == GameService.YELLOW){
            //Mark as empty
            paint.setColor(Color.YELLOW);
            rect = new RectF( 10, 10,getWidth() - 20, getHeight() - 20);
            canvas.drawOval(rect, paint);
        }
        //Mark as red
        else if (GameService.getInstance().getColor() == GameService.RED){
            paint.setColor(Color.RED);
            rect = new RectF( 10, 10,getWidth() - 20, getHeight() - 20);
            canvas.drawOval(rect, paint);
        }
        //Mark as empty
        else{
            paint.setColor(Color.WHITE);
            rect = new RectF( 10, 10,getWidth() - 20, getHeight() - 20);
            canvas.drawOval(rect, paint);
        }
    }
}

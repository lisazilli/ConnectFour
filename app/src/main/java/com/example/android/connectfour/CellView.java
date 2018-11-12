package com.example.android.connectfour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class CellView extends View {

    int color;
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

    private RectF rect;

    public boolean onTouchEvent( MotionEvent event ) {
        if ( event.getAction() != MotionEvent.ACTION_UP )
            return true;

        if ( color > 0) {
            Toast.makeText(this.getContext(),
                    "Cell already taken. Pick another cell.",
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
        invalidate();
        return true;
    }

    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
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

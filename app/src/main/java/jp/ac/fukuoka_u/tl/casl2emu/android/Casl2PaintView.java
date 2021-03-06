package jp.ac.fukuoka_u.tl.casl2emu.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import java.util.ArrayList;

public class Casl2PaintView extends View {

    Paint paint = new Paint();
    OutputBuffer buffer = OutputBuffer.getInstance();
    static ArrayList<Casl2Figure> drawObjectArray;
    public Casl2PaintView(Context context) {
        super(context);
    }

    public void setDrawObjectArray(ArrayList<Casl2Figure> drawObjectArray) {
        Casl2PaintView.drawObjectArray = drawObjectArray;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawRect(100,100,300,300,paint);
        ArrayList<Casl2Figure> figureArrayList = drawObjectArray;

        if(figureArrayList!=null) {
            for (Casl2Figure f : figureArrayList) {
                paint.setColor(f.color);
                paint.setStrokeWidth(f.getWidth());
                switch (f.getType()) {
                    case 1:
                        float[] circleprop = (float[]) f.getProp();
                        canvas.drawCircle(circleprop[0], circleprop[1], circleprop[2], paint);
                        break;
                    case 2:
                        canvas.drawRect((Rect) f.getProp(), paint);
                        break;
                    case 3:
                        float[] lp = (float[]) f.getProp();
                        canvas.drawLine(lp[0], lp[1], lp[2], lp[3], paint);
                        break;
                    case 4:
                        float[] pointprop = (float[]) f.getProp();
                        canvas.drawPoint(pointprop[0], pointprop[1], paint);
                        break;
                }
            }
        }
    }
}

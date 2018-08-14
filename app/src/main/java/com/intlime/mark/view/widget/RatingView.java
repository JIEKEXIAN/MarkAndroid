package com.intlime.mark.view.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import com.intlime.mark.tools.DensityUtils;

/**
 * Created by root on 16-1-5.
 */
public class RatingView extends TextView {
    private Paint paint;

    private float rate = 0.0f;
    private int fgColor = Color.parseColor("#000000");
    private int bgColor = Color.parseColor("#c4c4c4");

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
        setText(String.format("%.1f", rate));
    }

    public int getFgColor() {
        return fgColor;
    }

    public void setFgColor(int fgColor) {
        this.fgColor = fgColor;
    }

    public int getBgColor() {
        return bgColor;
    }

    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
    }

    public RatingView(Context context) {
        this(context, null);
    }

    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setGravity(Gravity.CENTER);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(bgColor);
        paint.setStrokeWidth(DensityUtils.dp2px(getContext(), 1));
        canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2,
                (getMeasuredWidth() / 2) - paint.getStrokeWidth() * 2f, paint);
        paint.setColor(fgColor);
        paint.setStrokeWidth(DensityUtils.dp2px(getContext(), 2));
        canvas.drawArc(new RectF(paint.getStrokeWidth(), paint.getStrokeWidth(),
                        getMeasuredWidth() - paint.getStrokeWidth(), getMeasuredHeight() - paint.getStrokeWidth()),
                -90, 360 * (rate / 10f), false, paint);
    }
}

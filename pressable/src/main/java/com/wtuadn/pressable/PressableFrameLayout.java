package com.wtuadn.pressable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by root on 15-12-17.
 */
public class PressableFrameLayout extends FrameLayout implements IPressable {

    public PressableFrameLayout(Context context) {
        this(context, null);
    }

    public PressableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        PressableUtils.init(this, attrs);
    }

    @Override
    public void setPressableDrawable(Drawable pressableDrawable) {
        setForeground(pressableDrawable);
    }
}

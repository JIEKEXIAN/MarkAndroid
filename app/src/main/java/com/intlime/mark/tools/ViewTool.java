package com.intlime.mark.tools;

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by root on 16-1-8.
 */
public class ViewTool {
    public static void setBackground(View view, Drawable background) {
        int paddingLeft = view.getPaddingLeft();
        int paddingTop = view.getPaddingTop();
        int paddingRight = view.getPaddingRight();
        int paddingBottom = view.getPaddingBottom();
        view.setBackgroundDrawable(background);
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    public static boolean pointInView(View view, float x, float y) {
        return x >= 0 && x < (view.getRight() - view.getLeft())
                && y >= 0 && y < (view.getBottom() - view.getTop());
    }
}

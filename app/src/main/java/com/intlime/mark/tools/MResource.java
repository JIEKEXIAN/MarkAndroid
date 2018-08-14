package com.intlime.mark.tools;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class MResource {
    static Resources resources;

    public static void init(Context context) {
        resources = context.getResources();
    }

    public static String getString(int id) {
        return resources.getString(id);
    }

    public static String[] getStringArray(int id) {
        return resources.getStringArray(id);
    }

    public static int getColor(int id) {
        return resources.getColor(id);
    }

    public static ColorStateList getColorStateList(int id){
        return resources.getColorStateList(id);
    }

    public static Drawable getDrawable(int id) {
        return resources.getDrawable(id);
    }

    public static int getDimensionPixelOffset(int id) {
        return resources.getDimensionPixelOffset(id);
    }

    public static int getDimensionPixelSize(int id) {
        return resources.getDimensionPixelSize(id);
    }

    public static int getInt(int id) {
        return resources.getInteger(id);
    }

}

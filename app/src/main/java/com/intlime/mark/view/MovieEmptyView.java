package com.intlime.mark.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.MResource;

/**
 * Created by root on 16-1-23.
 */
public class MovieEmptyView extends LinearLayout {
    public MovieEmptyView(Context context, int imgRes, String text1, String text2) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(imgRes);
        addView(imageView);

        TextView textView1 = new TextView(context);
        textView1.setGravity(Gravity.CENTER);
        textView1.setText(text1);
        textView1.setTextSize(14);
        textView1.setTextColor(MResource.getColor(R.color.a_main_text_color));
        textView1.setPadding(0, DensityUtils.dp2px(context, 16), 0, DensityUtils.dp2px(context, 31));
        addView(textView1);

        TextView textView2 = new TextView(context);
        textView2.setGravity(Gravity.CENTER);
        textView2.setText(text2);
        textView2.setTextSize(13);
        textView2.setTextColor(Color.parseColor("#304750"));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(DensityUtils.dp2px(context, 2));
        drawable.setColor(Color.parseColor("#ebebeb"));
        textView2.setBackgroundDrawable(drawable);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, DensityUtils.dp2px(context, 48));
        lp.leftMargin = DensityUtils.dp2px(context, 20);
        lp.rightMargin = DensityUtils.dp2px(context, 20);
        addView(textView2, lp);
    }
}

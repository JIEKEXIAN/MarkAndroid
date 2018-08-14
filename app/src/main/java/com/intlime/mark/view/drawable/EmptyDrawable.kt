package com.intlime.mark.view.drawable

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.intlime.mark.R
import com.intlime.mark.tools.ImageTool

/**
 * Created by wtuadn on 16/04/28.
 */
class EmptyDrawable(width: Float, height: Float)
: ResizeDrawable(ColorDrawable(Color.parseColor("#e1e1e1")), ImageTool.getBitmap(R.drawable.movie_empty_icon, null), width, height) {
}
package com.intlime.mark.tools.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.intlime.mark.tools.DensityUtils;

/**
 * bitmap转圆角
 * Created by wtu on 15-8-25.
 */
public class RoundTransform extends BitmapTransformation {
    private static float radius = 0f;
    private Paint paint;
    private PorterDuffXfermode porterDuffXfermode;

    public RoundTransform(Context context) {
        this(context, 4);
    }

    public RoundTransform(Context context, float dp) {
        super(context);
        radius = DensityUtils.dp2px(context, dp);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        final Bitmap toReuse = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888);

        if (toTransform == null) {
            return null;
        }
        // From ImageView/Bitmap.createScaledBitmap.
        final float scale;
        float dx = 0, dy = 0;
        Matrix m = new Matrix();
        if (toTransform.getWidth() * outHeight > outWidth * toTransform.getHeight()) {
            scale = (float) outHeight / (float) toTransform.getHeight();
            dx = (outWidth - toTransform.getWidth() * scale) * 0.5f;
        } else {
            scale = (float) outWidth / (float) toTransform.getWidth();
            dy = (outHeight - toTransform.getHeight() * scale) * 0.5f;
        }

        m.setScale(scale, scale);
        m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        final Bitmap result;
        if (toReuse != null) {
            result = toReuse;
        } else {
            result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.TRANSPARENT);
        paint.setXfermode(null);
        if (radius != 0) {
            canvas.drawRoundRect(new RectF(0, 0, outWidth, outHeight), radius, radius, paint);
            paint.setXfermode(porterDuffXfermode);
        }
        canvas.drawBitmap(toTransform, m, paint);

        return result;
    }

    @Override
    public String getId() {
        return getClass().getName() + Math.round(radius);
    }
}

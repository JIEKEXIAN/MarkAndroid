package com.intlime.mark.view.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.CheckBox;

import com.intlime.mark.R;


/**
 * 自定义开关按钮，
 */
public class SwitchButton extends CheckBox {
    /**
     * Switch底部灰色样式图片
     */
    private Bitmap mSwitchBgUnseleted;
    /**
     * Switch底部绿色样式图
     */
    private Bitmap mSwitchBgSeleted;
    /**
     * Switch灰色的球
     */
    private Bitmap mSwitchSliderUnSeleted;
    /**
     * Switch绿色的球
     */
    private Bitmap mSwitchSliderSeleted;

    private float mCurrentX = 0;

    /**
     * Switch 最大移动距离
     */
    private int mMoveLength;
    /**
     * 第一次按下的有效区域
     */
    private float mLastX = 0;
    /**
     * 绘制的目标区域大小
     */
    private Rect mDest = null;
    /** 截取源图片的大小  */
    /**
     * Switch 移动的偏移量
     */
    private int mMoveDeltX = 0;

    private boolean mFlag = false;

    /**
     * 最大透明度，就是不透明
     */
    private final int MAX_ALPHA = 255;
    /**
     * 当前透明度，这里主要用于如果控件的enable属性为false时候设置半透明 ，即不可以点击
     */
    private int mAlpha = MAX_ALPHA;
    /**
     * Switch 判断是否在拖动
     */
    private boolean mIsScrolled = false;

    private int left;
    private int top;

    public SwitchButton(Context context) {
        this(context, null);
    }

    public SwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    /**
     * 初始化相关资源
     *
     * @param attrs
     */
    private void init(AttributeSet attrs) {
        int btn_width = 10;
        int btn_height = 10;
        int btn_radius = 0;
        int btn_stroke = 0;
        int btn_bgColor_unSelected = 10;
        int btn_bgColor_selected = 10;
        int btn_slider_unSelected = 10;
        int btn_slider_selected = 10;

        if (attrs != null && getContext() != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.switchButton);
            if (typedArray != null) {
                btn_width = (int) typedArray.getDimension(R.styleable.switchButton_btn_width, 10.0f);
                btn_height = (int) typedArray.getDimension(R.styleable.switchButton_btn_height, 10.0f);
                btn_radius = (int) typedArray.getDimension(R.styleable.switchButton_btn_radius, 0f);
                btn_stroke = (int) typedArray.getDimension(R.styleable.switchButton_btn_stroke, 0f);
                btn_bgColor_unSelected = typedArray.getColor(R.styleable.switchButton_btn_bgColor_unSelected, Color.TRANSPARENT);
                btn_bgColor_selected = typedArray.getColor(R.styleable.switchButton_btn_bgColor_selected, Color.TRANSPARENT);
                btn_slider_unSelected = typedArray.getColor(R.styleable.switchButton_btn_slider_unSelected, Color.TRANSPARENT);
                btn_slider_selected = typedArray.getColor(R.styleable.switchButton_btn_slider_selected, Color.TRANSPARENT);

                typedArray.recycle();
            }
        }
        mSwitchBgSeleted = getBg(btn_width, btn_height, btn_bgColor_selected, btn_radius);
        mSwitchBgUnseleted = getBg(btn_width, btn_height, btn_bgColor_unSelected, btn_radius);

        if (btn_width < (btn_height * 2)) {
            mSwitchSliderSeleted = getSlider(btn_height, btn_height, btn_slider_selected, btn_radius, btn_stroke);
            mSwitchSliderUnSeleted = getSlider(btn_height, btn_height, btn_slider_unSelected, btn_radius, btn_stroke);
        } else {
            mSwitchSliderSeleted = getSlider(btn_width / 2, btn_height, btn_slider_selected, btn_radius, btn_stroke);
            mSwitchSliderUnSeleted = getSlider(btn_width / 2, btn_height, btn_slider_unSelected, btn_radius, btn_stroke);
        }

        mMoveLength = mSwitchBgSeleted.getWidth() - mSwitchSliderSeleted.getWidth();
    }

    /**
     * 背景
     */
    private Bitmap getBg(int width, int height, int color, int radius) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawRoundRect(
                new RectF(0, 0, width, height), radius, radius, paint);
        return bitmap;
    }

    /**
     * 滑块
     */
    private Bitmap getSlider(int width, int height, int color, int radius, int stroke) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawRoundRect(
                new RectF(stroke, stroke, width - stroke, height - stroke), radius - stroke, radius - stroke, paint);
        return bitmap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        setMeasuredDimension(mSwitchBgSeleted.getWidth(), mSwitchBgSeleted.getHeight());
        left = (getMeasuredWidth() - mSwitchBgSeleted.getWidth()) / 2;
        top = (getMeasuredHeight() - mSwitchBgSeleted.getHeight()) / 2;
        //绘制区域大小
        mDest = new Rect(left, top, left + mSwitchBgSeleted.getWidth(), top + mSwitchBgSeleted.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.saveLayerAlpha(new RectF(mDest), mAlpha, Canvas.MATRIX_SAVE_FLAG
                | Canvas.CLIP_SAVE_FLAG | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
                | Canvas.FULL_COLOR_LAYER_SAVE_FLAG
                | Canvas.CLIP_TO_LAYER_SAVE_FLAG);
        //如果是关闭的
        if (!isChecked()) {
            if (mMoveDeltX > 0) {//向右滑动了
                if (mMoveDeltX < mMoveLength / 2) {//滑动距离小于一半
                    canvas.drawBitmap(mSwitchBgUnseleted, left, top, null); //灰色背景
                    canvas.drawBitmap(mSwitchSliderUnSeleted, mMoveDeltX, top, null); //灰色按钮
                } else {//滑动距离大于一半
                    canvas.drawBitmap(mSwitchBgSeleted, left, top, null); //绿色背景
                    canvas.drawBitmap(mSwitchSliderSeleted, mMoveDeltX + left, top, null); //绿色按钮
                }
            } else {
                canvas.drawBitmap(mSwitchBgUnseleted, left, top, null); //灰色背景
                canvas.drawBitmap(mSwitchSliderUnSeleted, left, top, null); //灰色按钮
            }
        } else {
            if (mMoveDeltX < 0) {//向左滑动了
                if (Math.abs(mMoveDeltX) < mMoveLength / 2) {//滑动距离小于一半
                    canvas.drawBitmap(mSwitchBgSeleted, left, top, null); //绿色背景
                    canvas.drawBitmap(mSwitchSliderSeleted, mMoveLength + mMoveDeltX + left, top, null); //绿色按钮
                } else {//滑动距离大于一半
                    canvas.drawBitmap(mSwitchBgUnseleted, 0, top, null); //灰色背景
                    canvas.drawBitmap(mSwitchSliderUnSeleted, mMoveLength + mMoveDeltX + left, top, null); //灰色按钮
                }
            } else {
                canvas.drawBitmap(mSwitchBgSeleted, left, top, null); //绿色背景
                canvas.drawBitmap(mSwitchSliderSeleted, mMoveLength + left, top, null); //绿色按钮
            }
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentX = event.getX();
                mMoveDeltX = (int) (mCurrentX - mLastX);
                if (Math.abs(mMoveDeltX) > 3) {
                    //设置了3这个误差距离，可以更好的实现点击效果
                    mIsScrolled = true;
                }
                // 如果开关开着向右滑动，或者开关关着向左滑动（这时候是不需要处理的）
                if ((isChecked() && mMoveDeltX > 0) || (!isChecked() && mMoveDeltX < 0)) {
                    mFlag = true;
                    mMoveDeltX = 0;
                }
                if (Math.abs(mMoveDeltX) > mMoveLength) {
                    mMoveDeltX = mMoveDeltX > 0 ? mMoveLength : 0;
                    if (mMoveDeltX == 0) {
                        setChecked(false);
                    } else {
                        setChecked(true);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //如果没有滑动过，就看作一次点击事件
                if (!mIsScrolled) {
                    startScroll(0, (isChecked() ? -mMoveLength : mMoveLength), !isChecked());
                    return true;
                }
                mIsScrolled = false;
                if (Math.abs(mMoveDeltX) > 0 && Math.abs(mMoveDeltX) <= mMoveLength / 2) {
                    startScroll(0, -mMoveDeltX, isChecked());
                    return true;
                } else if (Math.abs(mMoveDeltX) > mMoveLength / 2 && Math.abs(mMoveDeltX) <= mMoveLength) {
                    startScroll(mMoveDeltX,
                            (isChecked() ? -mMoveLength - mMoveDeltX : mMoveLength - mMoveDeltX), !isChecked());
                    return true;
                } else if (mMoveDeltX == 0 && mFlag) {
                    // 这时候得到的是不需要进行处理的，因为已经move过了
                    mMoveDeltX = 0;
                    mFlag = false;
                }
            default:
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mAlpha = enabled ? MAX_ALPHA : MAX_ALPHA / 2;
        super.setEnabled(enabled);
        invalidate();
    }

    public void click() {
        startScroll(0, (isChecked() ? -mMoveLength : mMoveLength), !isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        mMoveDeltX = checked ? mMoveLength : 0;
        invalidate();
    }

    public void startScroll(int startX, int dx, final boolean endCheckState) {
        ValueAnimator animator = ValueAnimator.ofInt(startX, startX + dx).setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mMoveDeltX = (Integer) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setChecked(endCheckState);
            }
        });
        animator.start();
    }
}
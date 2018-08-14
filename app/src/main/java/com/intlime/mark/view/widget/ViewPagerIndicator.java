package com.intlime.mark.view.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.intlime.mark.tools.LogTool;

import java.util.List;

/**
 * @author wtu
 */
public class ViewPagerIndicator extends LinearLayout {
    /**
     * 绘制指示器的画笔
     */
    private Paint mPaint;
    /**
     * 指示器宽度
     */
    private int mIndicatorWidth;
    /**
     * 指示器高度
     */
    private int mIndicatorHeight;
    /**
     * 初始时，指示器的偏移量
     */
    private int mInitTranslationX;
    /**
     * 手指滑动时的偏移量
     */
    private float mTranslationX;
    /**
     * tab数量
     */
    private int mTabVisibleCount = 2;

    /**
     * tab上的内容
     */
    private List<String> mTabTitles;
    /**
     * 与之绑定的ViewPager
     */
    public ViewPager mViewPager;

    private float textSize = 16f;
    /**
     * 标题正常时的颜色
     */
    private int colorTextNormal = Color.GRAY;
    /**
     * 标题选中时的颜色
     */
    private int colorTextHighlight = Color.GREEN;

    public void setmIndicatorHeight(int mIndicatorHeight) {
        this.mIndicatorHeight = mIndicatorHeight;
    }

    public void setmIndicatorWidth(int mIndicatorWidth) {
        this.mIndicatorWidth = mIndicatorWidth;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setColorTextNormal(int colorTextNormal) {
        this.colorTextNormal = colorTextNormal;
    }

    public void setColorTextHighlight(int colorTextHighlight) {
        this.colorTextHighlight = colorTextHighlight;
    }

    public void setIndicatorColor(int color) {
        mPaint.setColor(color);
    }

    public ViewPagerIndicator(Context context) {
        this(context, null);
    }

    public ViewPagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 初始化画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.FILL);
        mPaint.setPathEffect(new CornerPathEffect(3));
    }

    /**
     * 绘制指示器
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        // 画笔平移到正确的位置
        canvas.translate(mInitTranslationX + mTranslationX, getHeight() - mIndicatorHeight);
        canvas.drawRect(new Rect(0, 0, mIndicatorWidth, mIndicatorHeight), mPaint);
        canvas.restore();

        super.dispatchDraw(canvas);
    }

    /**
     * 初始化三角形的宽度
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (isInEditMode()) {
            return;
        }

        if (mIndicatorWidth == 0) {
            mIndicatorWidth = getWidth() / mTabVisibleCount;
        }
        if (mIndicatorHeight == 0) {
            mIndicatorHeight = getHeight() / 10;
        }

        // 初始时的偏移量
        mInitTranslationX = getWidth() / mTabVisibleCount / 2 - mIndicatorWidth / 2;
    }

    /**
     * 设置可见的tab的数量
     *
     * @param count
     */
    public void setVisibleTabCount(int count) {
        this.mTabVisibleCount = count;
    }

    /**
     * 设置tab的标题内容 可选，可以自己在布局文件中写死
     *
     * @param mTabVisibleCount 可视的tab数量，它们将平分父布局
     */
    public void setTabItemTitles(List<String> datas, int mTabVisibleCount) {
        // 如果传入的list有值，则移除布局文件中设置的view
        if (datas != null && datas.size() > 0 && mTabVisibleCount > 0) {
            this.removeAllViews();
            this.mTabTitles = datas;
            this.mTabVisibleCount = mTabVisibleCount;
            for (String title : mTabTitles) {
                // 添加view
                addView(generateTextView(title));
            }
            // 设置item的click事件
            setItemClickEvent();
        }
    }

    // 对外的ViewPager的回调接口
    private OnPageChangeListener onPageChangeListener;

    // 对外的ViewPager的回调接口的设置
    public void setOnPageChangeListener(OnPageChangeListener pageChangeListener) {
        this.onPageChangeListener = pageChangeListener;
    }

    // 设置关联的ViewPager
    public void setViewPager(ViewPager mViewPager, int pos) {
        this.mViewPager = mViewPager;

        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // 设置字体颜色高亮
                highLightTextView(position);

                // 回调
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                // 滚动
                scroll(position, positionOffset);

                // 回调
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageScrolled(position,
                            positionOffset, positionOffsetPixels);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // 回调
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageScrollStateChanged(state);
                }

            }
        });
        // 设置当前页
        mViewPager.setCurrentItem(pos);
        // 高亮
        highLightTextView(pos);
    }

    /**
     * 高亮文本
     */
    private void highLightTextView(int position) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int textColor;
            if (i == position) {
                textColor = colorTextHighlight;
            } else {
                textColor = colorTextNormal;
            }
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(textColor);
            } else if (view instanceof ViewGroup) {
                highLightTextView((ViewGroup) view, textColor);
            }
        }
    }

    private void highLightTextView(ViewGroup parent, int textColor) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(textColor);
            } else if (view instanceof ViewGroup) {
                highLightTextView((ViewGroup) view, textColor);
            }
        }
    }

    /**
     * 设置点击事件
     */
    public void setItemClickEvent() {
        int cCount = getChildCount();
        for (int i = 0; i < cCount; i++) {
            final int j = i;
            View view = getChildAt(i);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mViewPager.setCurrentItem(j);
                }
            });
        }
    }

    /**
     * 根据标题生成我们的TextView
     *
     * @param text
     * @return
     */
    private TextView generateTextView(String text) {
        TextView tv = new TextView(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.weight = 1;
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(colorTextNormal);
        tv.setText(text);
        tv.setTextSize(textSize);
        tv.setLayoutParams(lp);
        return tv;
    }

    /**
     * 指示器跟随手指滚动，以及容器滚动
     *
     * @param position
     * @param offset
     */
    public void scroll(int position, float offset) {
        /**
         * <pre>
         *  0-1:position=0 ;1-0:postion=0;
         * </pre>
         */
        // 不断改变偏移量，invalidate
        mTranslationX = getWidth() / mTabVisibleCount * (position + offset);

        int tabWidth = getWidth() / mTabVisibleCount;

        // 容器滚动，当移动到倒数最后一个的时候，开始滚动
        if (offset > 0 && position >= (mTabVisibleCount - 2)
                && getChildCount() > mTabVisibleCount) {
            if (mTabVisibleCount != 1) {
                this.scrollTo((position - (mTabVisibleCount - 2)) * tabWidth
                        + (int) (tabWidth * offset), 0);
            } else
            // 为count为1时 的特殊处理
            {
                this.scrollTo(
                        position * tabWidth + (int) (tabWidth * offset), 0);
            }
        }

        invalidate();
    }

    /**
     * 设置布局中view的一些必要属性；如果设置了setTabTitles，布局中view则无效
     */
    @Override
    protected void onFinishInflate() {
        LogTool.d("TAG", "onFinishInflate");
        super.onFinishInflate();

        if (isInEditMode()) {
            return;
        }
        int cCount = getChildCount();

        if (cCount == 0)
            return;

        for (int i = 0; i < cCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view
                    .getLayoutParams();
            lp.weight = 1;
            view.setLayoutParams(lp);
        }
        // 设置点击事件
        setItemClickEvent();
    }
}

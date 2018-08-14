package com.intlime.mark.view.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

import com.intlime.mark.R;

/**
 * Created by root on 15-12-4.
 */
public class ClearEditText extends EditText {
    private boolean isTouchInClear = false;
    private Drawable clearIcon;

    public void setClearIcon(int resId) {
        setClearIcon(getContext().getResources().getDrawable(resId));
    }

    public void setClearIcon(Drawable clearIcon) {
        this.clearIcon = clearIcon;
        int paddingRight = clearIcon.getIntrinsicWidth();
        if (getCompoundDrawablePadding() < paddingRight / 5) {
            setCompoundDrawablePadding(paddingRight / 5);
        }
        if (getPaddingRight() < paddingRight) {
            setPadding(getPaddingLeft(), getPaddingTop(), paddingRight, getPaddingBottom());
        }
    }

    public ClearEditText(Context context) {
        this(context, null);
    }

    public ClearEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClearIcon(R.drawable.edit_clear_icon);
        post(new Runnable() {
            @Override
            public void run() {
                if (isFocused()) {
                    checkClearIcon();
                } else {
                    Drawable[] drawables = getCompoundDrawables();
                    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], null, drawables[3]);
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!TextUtils.isEmpty(getText())) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    if (event.getX() > getWidth() - clearIcon.getIntrinsicWidth() * 2) {
                        isTouchInClear = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (isTouchInClear && event.getX() > getWidth() - clearIcon.getIntrinsicWidth() * 2) {
                        if (isFocused())
                            setText("");
                    }
                    isTouchInClear = false;
                    break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            checkClearIcon();
        } else {
            Drawable[] drawables = getCompoundDrawables();
            setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], null, drawables[3]);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        checkClearIcon();
    }

    private void checkClearIcon() {
        Drawable[] drawables = getCompoundDrawables();
        if (TextUtils.isEmpty(getText())) {
            setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], null, drawables[3]);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], clearIcon, drawables[3]);
        }
    }
}

package com.intlime.mark.tools;

import android.text.DynamicLayout;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.intlime.mark.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by root on 15-12-3.
 */
public class StringTool {

    public static void handleLinesToExpand(final TextView textView, final int line) {
        textView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                textView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (line != Integer.MAX_VALUE && textView.getLineCount() > line) {
                    int start = textView.getLayout().getLineStart(0);
                    int end = textView.getLayout().getLineEnd(line - 1);

                    final String currentText = textView.getText().toString();
                    String toDisplayed;
                    while (true) {
                        toDisplayed = String.format("%s...展开", currentText.substring(start, end));
                        DynamicLayout layout = new DynamicLayout(toDisplayed, textView.getPaint(),
                                textView.getLayout().getWidth(), Layout.Alignment.ALIGN_NORMAL, 0, 0f, true);
                        if (layout.getLineCount() > line && end > 1) {
                            end -= 1;
                        } else {
                            break;
                        }
                    }
                    SpannableString spannable = new SpannableString(toDisplayed);
                    spannable.setSpan(new ClickableSpan() {
                        int color = MResource.getColor(R.color.link_color);

                        @Override
                        public void onClick(View widget) {
                            textView.setText(currentText);
                            textView.setMovementMethod(null);
                        }

                        @Override
                        public void updateDrawState(TextPaint ds) {
                            ds.setColor(color);
                        }
                    }, spannable.length() - 2, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textView.setText(spannable);
                    textView.setMovementMethod(getMovementMethod());
                }
                return true;
            }
        });
    }

    private static TouchDelegateMovementMethod movementMethod;

    private static TouchDelegateMovementMethod getMovementMethod() {
        if (movementMethod == null) {
            movementMethod = new TouchDelegateMovementMethod();
        }
        return movementMethod;
    }

    private static class TouchDelegateMovementMethod extends LinkMovementMethod {
        private int extra = 4;

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = buffer.getSpans(off - extra, off + extra, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget);
                    } else {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                    }

                    return true;
                } else if (line + 1 < layout.getLineCount()) {
                    off = layout.getOffsetForHorizontal(line + 1, x);
                    link = buffer.getSpans(off - extra, off + extra, ClickableSpan.class);

                    if (link.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            link[0].onClick(widget);
                        } else {
                            Selection.setSelection(buffer,
                                    buffer.getSpanStart(link[0]),
                                    buffer.getSpanEnd(link[0]));
                        }

                        return true;
                    } else {
                        Selection.removeSelection(buffer);
                    }
                } else {
                    Selection.removeSelection(buffer);
                }
            }
            return super.onTouchEvent(widget, buffer, event);
        }
    }

    /**
     * 时间戳转日期
     *
     * @param timestamp int型的时间戳
     * @return
     */
    public static String getTimeToShow(int timestamp) {
        int second = ((int) (System.currentTimeMillis() / 1000)) - timestamp;
        if (second < 10) {
            return "刚刚";
        } else if (second < 60) {
            return second + "秒前";
        } else if (second < 3600) {
            return second / 60 + "分钟前";
        } else if (second < 86400) {
            return second / 3600 + "小时前";
        } else if (second < 864000) {
            return second / 86400 + "天前";
        } else {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timestamp * 1000L);
            int year = calendar.get(Calendar.YEAR);
            if (year == Calendar.getInstance().get(Calendar.YEAR)) {
                SimpleDateFormat format = new SimpleDateFormat("MM月dd日");//设置日期格式
                format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                return format.format(new Date(timestamp * 1000L));
            } else {
                SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");//设置日期格式
                format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                return format.format(new Date(timestamp * 1000L));
            }
        }
    }

    public static String getQiniuScaledImgUrl(String url, int width, int height) {
        if (!TextUtils.isEmpty(url) && url.contains("7xqnv7") && width > 0 && height > 0) {//是七牛的图片
            url = url.split("\\?")[0] + "?imageView2/1/w/" + width + "/h/" + height;
        }
        return url;
    }
}

package com.intlime.mark.view.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.view.recyclerview.RecyclerAdapter;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by root on 16-2-22.
 */
public class Banner extends RecyclePager {
    private int duration = 5000;
    private SwitchTask switchTask;
    private boolean canSwitch = true;
    private boolean isSwitching;
    private Paint paint;
    private int dotCount;
    private ViewFactory<View, Object> viewFactory;
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                Session.isScreenOn = true;
                startSwitch();
            } else {
                Session.isScreenOn = false;
                stopSwitch();
            }
        }
    };

    public void setDotCount(int dotCount) {
        this.dotCount = dotCount;
    }

    public void setCanSwitch(boolean canSwitch) {
        if (this.canSwitch && !canSwitch) {
            stopSwitch();
        }
        this.canSwitch = canSwitch;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Banner(Context context, ViewFactory viewFactory) {
        super(context);
        this.viewFactory = viewFactory;
        setHasFixedSize(true);
        setAdapter(new Adapter());
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        switchTask = new SwitchTask(this);
        paint = new Paint();
        paint.setAntiAlias(true);

        registerReciver();
    }

    private void registerReciver() {
        try{
            IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            AppEngine.getContext().registerReceiver(broadcastReceiver, filter);
        }catch (Exception ignore){
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startSwitch();
        } else {
            stopSwitch();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startSwitch();
        registerReciver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSwitch();
        try{
            AppEngine.getContext().unregisterReceiver(broadcastReceiver);
        }catch (Exception ignore){
        }
    }

    public void stopSwitch() {
        if (canSwitch) {
            isSwitching = false;
            removeCallbacks(switchTask);
        }
    }

    public void startSwitch() {
        if (canSwitch) {
            if (!isSwitching)
                postDelayed(switchTask, duration);
            isSwitching = true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            startSwitch();
        } else if (action == MotionEvent.ACTION_DOWN) {
            stopSwitch();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (dotCount <= 1) return;
        int width = getMeasuredWidth();
        int dotWidth = DensityUtils.dp2px(getContext(), 6);
        int dotSpace = DensityUtils.dp2px(getContext(), 9);
        int dotX = (width - dotWidth * dotCount - dotSpace * (dotCount - 1)) / 2;
        int dotY = (int) (getMeasuredHeight() * 0.95);
        for (int i = 0; i < dotCount; i++) {
            if (getCurrentItem() % dotCount == i) {
                paint.setColor(Color.WHITE);
            } else {
                paint.setColor(Color.parseColor("#66000000"));
            }
            canvas.drawCircle(dotX + (i + 1) * dotWidth + i * dotSpace - dotWidth / 2, dotY, dotWidth / 2, paint);
        }
    }

    private static class SwitchTask implements Runnable {

        private final WeakReference<Banner> reference;

        SwitchTask(Banner banner) {
            this.reference = new WeakReference<>(banner);
        }

        @Override
        public void run() {
            Banner banner = reference.get();
            if (Session.isScreenOn && banner != null && banner.isSwitching && banner.canSwitch
                    && banner.isShown() && !WWindowManager.getInstance().getWindows().isEmpty()) {
                int page = banner.getCurrentItem() + 1;
                banner.smoothScrollToPosition(page);
                banner.postDelayed(banner.switchTask, banner.duration);
            }
        }
    }

    private class Adapter extends RecyclerAdapter implements OnClickListener {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = viewFactory.createView();
            view.setOnClickListener(this);
            return new ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            viewFactory.apply(holder.itemView, viewFactory.values.get(position % viewFactory.values.size()));
        }

        @Override
        public int getNormalItemCount() {
            return 0;
        }

        @Override
        public int getItemCount() {
            return viewFactory.values.size() <= 1 ? viewFactory.values.size() : Integer.MAX_VALUE;
        }

        @Override
        public void onClick(View v) {
            int position = ((LayoutParams) v.getLayoutParams()).getViewLayoutPosition();
            if (position < 0) return;
            viewFactory.onItemClick(v, viewFactory.values.get(position % viewFactory.values.size()));
        }
    }

    public abstract static class ViewFactory<V extends View, T> {
        protected List<T> values;

        public ViewFactory(List<T> values) {
            this.values = values;
        }

        protected abstract V createView();//创建view

        protected abstract void apply(V v, T t);//更新view

        protected void onItemClick(V v, T t) {
        }
    }
}

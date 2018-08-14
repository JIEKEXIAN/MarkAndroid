package com.intlime.mark.adapter;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.intlime.mark.R;
import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.tools.db.MovieDbManager;
import com.intlime.mark.tools.glide.MovieCoverKey;
import com.intlime.mark.view.MovieDoneView;
import com.intlime.mark.view.MovieTodoView;
import com.intlime.mark.view.drawable.EmptyDrawable;
import com.intlime.mark.view.recyclerview.RecyclerCursorAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wtu on 15/12/29.
 */
public class MovieGridCursorAdapter extends RecyclerCursorAdapter<MovieBean> {
    public static final RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
    private Drawable placeholder = new ColorDrawable(Color.parseColor("#e1e1e1"));
    private Drawable error = new EmptyDrawable(68f, 68f);

    private List<Integer> multiSelectList;//保存选中的位置
    private boolean multiSelect;
    private Drawable multiChecked = MResource.getDrawable(R.drawable.grid_multi_checked);
    private Drawable multiUnchecked = MResource.getDrawable(R.drawable.grid_multi_unchecked);
    private View.OnClickListener outMultiSelectListener;

    public List<Integer> getMultiSelectList() {
        return multiSelectList;
    }

    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
        if (multiSelect) {
            recyclerItemListener.createContextMenuable = false;
        } else {
            recyclerItemListener.createContextMenuable = true;
            multiSelectList.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void setOutMultiSelectListener(View.OnClickListener outMultiSelectListener) {
        this.outMultiSelectListener = outMultiSelectListener;
    }

    public MovieGridCursorAdapter(int is_done) {
        multiSelectList = new ArrayList<>();
        changeCursor(is_done == 0 ? MovieTodoView.Companion.getMyCursor() : MovieDoneView.Companion.getMyCursor());
    }

    @Override
    public MovieBean getItem(int position) {
        boolean bool = mCursor.moveToPosition(position - headerSize);
        if (!bool) return null;
        return MovieDbManager.getInstance().getItemByCursor(mCursor);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);

        if (viewHolder instanceof Holder) {
            final Holder holder = (Holder) viewHolder;

            String name = mCursor.getString(MovieDbManager.NAME_P);
            String img_url = mCursor.getString(MovieDbManager.IMAGE_P);

            holder.textView1.setText(name);

            Glide.with(holder.imageView1.getContext())
                    .load(img_url)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .signature(new MovieCoverKey(img_url))
                    .placeholder(placeholder)
                    .error(error)
                    .into(holder.imageView1);

            if (multiSelect) {
                holder.checkbox.setVisibility(View.VISIBLE);
                if (multiSelectList.contains(position)) {
                    holder.checkbox.setImageDrawable(multiChecked);
                } else {
                    holder.checkbox.setImageDrawable(multiUnchecked);
                }
                holder.itemView.setOnClickListener(multiSelectListener);
            } else {
                holder.checkbox.setVisibility(View.INVISIBLE);
            }
            handleLongPressHint();
        }
    }

    private void handleLongPressHint() {
        if (SettingManager.getInstance().canShowLongPressHint()) {
            BaseActivity baseActivity = WWindowManager.getInstance().getCurrentActivity();
            if (baseActivity == null) return;
            SettingManager.getInstance().setCanShowLongPressHint(false);
            final Dialog dialog = new Dialog(baseActivity, R.style.mydialog);
            ImageView imageView = new ImageView(AppEngine.getContext());
            imageView.setImageResource(R.drawable.long_press_hint);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.setContentView(imageView);
            Window window = dialog.getWindow();
            android.view.WindowManager.LayoutParams lp = window.getAttributes();
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.x = WWindowManager.getInstance().getWidth() / 15;
            lp.y = WWindowManager.getInstance().getWidth() / -15;
            lp.dimAmount = 0f;
            window.setAttributes(lp);
            dialog.show();
            Session.map.put("hint_dialog", dialog);
            ThreadManager.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dialog.isShowing()) dialog.dismiss();
                    } catch (Exception ignore) {
                    }
                }
            }, 5000);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Session.map.remove("hint_dialog");
                }
            });
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        if (viewHolder == null) {
            viewHolder = new Holder(View.inflate(AppEngine.getContext(), R.layout.movie_grid_item_layout, null));
        }
        return viewHolder;
    }

    private View.OnClickListener multiSelectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = recyclerView.getChildLayoutPosition(v);
            Holder holder = (Holder) recyclerView.findViewHolderForLayoutPosition(position);
            if (multiSelectList.contains(position)) {
                multiSelectList.remove((Integer) position);
                holder.checkbox.setImageDrawable(multiUnchecked);
            } else {
                multiSelectList.add(position);
                holder.checkbox.setImageDrawable(multiChecked);
            }
            if (outMultiSelectListener != null) {
                outMultiSelectListener.onClick(v);
            }
        }
    };

    private class Holder extends RecyclerView.ViewHolder {
        ImageView imageView1;
        TextView textView1;
        ImageView checkbox;

        public Holder(final View itemView) {
            super(itemView);
            imageView1 = (ImageView) itemView.findViewById(R.id.image_view);
            textView1 = (TextView) itemView.findViewById(R.id.title);
            checkbox = (ImageView) itemView.findViewById(R.id.checkbox);

            int width = (WWindowManager.getInstance().getWidth() - DensityUtils.dp2px(AppEngine.getContext(), 48)) / 3;
            imageView1.getLayoutParams().height = (int) (width * 1.4679);
            imageView1.setLayoutParams(imageView1.getLayoutParams());
        }
    }
}

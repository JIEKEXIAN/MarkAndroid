package com.intlime.mark.adapter;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.view.drawable.EmptyDrawable;
import com.intlime.mark.view.recyclerview.RecyclerListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 15-12-22.
 */
public class MovieSearchAdapter extends RecyclerListAdapter<MovieBean> {
    private List<Integer> handlingList;//记录处于处理状态的位置

    private Drawable placeholder = new ColorDrawable(Color.parseColor("#e1e1e1"));
    private Drawable error = new EmptyDrawable(50f, 50f);
    private Drawable toAddDrawable = MResource.getDrawable(R.drawable.search_add_icon);
    private Drawable addedDrawable = MResource.getDrawable(R.drawable.search_added_icon);

    private View.OnClickListener addClickListener;

    public void setAddClickListener(View.OnClickListener addClickListener) {
        this.addClickListener = addClickListener;
    }

    public MovieSearchAdapter(List<MovieBean> lists) {
        super(lists);
        handlingList = new ArrayList<>();
    }

    public void handling(int position) {
        handlingList.add(position);
    }

    public void handled(int position) {
        handlingList.remove((Integer) position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        if (viewHolder == null) {
            viewHolder = new Holder(View.inflate(AppEngine.getContext(), R.layout.movie_search_item_layout, null));
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);

        if (viewHolder instanceof Holder) {
            final Holder holder = (Holder) viewHolder;
            MovieBean bean = lists.get(position);

            if (handlingList.contains(position)) {
                holder.progressBar.setVisibility(View.VISIBLE);
            } else {
                if (bean.getId() > 0) {
                    holder.add.setImageDrawable(addedDrawable);
                } else {
                    holder.add.setImageDrawable(toAddDrawable);
                }
                holder.progressBar.setVisibility(View.GONE);
            }

            holder.textView1.setText(bean.getName());

            Glide.with(holder.imageView1.getContext())
                    .load(bean.getImage())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(placeholder)
                    .error(error)
                    .into(holder.imageView1);
        }
    }

    public class Holder extends RecyclerView.ViewHolder {
        private ImageView imageView1;
        private TextView textView1;
        public ImageView add;
        public ProgressBar progressBar;

        public Holder(final View itemView) {
            super(itemView);
            imageView1 = (ImageView) itemView.findViewById(R.id.image_view);
            textView1 = (TextView) itemView.findViewById(R.id.title);
            add = (ImageView) itemView.findViewById(R.id.add_button);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);

            int width = (WWindowManager.getInstance().getWidth() - DensityUtils.dp2px(AppEngine.getContext(), 48)) / 3;
            imageView1.getLayoutParams().height = (int) (width * 1.467);
            imageView1.setLayoutParams(imageView1.getLayoutParams());

            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (addClickListener != null && !progressBar.isShown()) {
                        addClickListener.onClick(v);
                    }
                }
            });
            add.post(new Runnable() {
                @Override
                public void run() {
                    Rect rect = new Rect();
                    add.getHitRect(rect);
                    int px = DensityUtils.dp2px(add.getContext(), 20);
                    rect.left -= px;
                    rect.top -= px;
                    rect.right += px;
                    rect.bottom += px;
                    itemView.setTouchDelegate(new TouchDelegate(rect, add));
                }
            });
        }
    }
}

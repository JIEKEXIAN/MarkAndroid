package com.intlime.mark.adapter;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.tools.StringTool;
import com.intlime.mark.tools.glide.MovieListKey;
import com.intlime.mark.view.recyclerview.RecyclerListAdapter;

import java.util.List;

/**
 * Created by root on 16-2-22.
 */
public class DiscoverAdapter extends RecyclerListAdapter<MovieListBean> {
    private Drawable likeChecked = MResource.getDrawable(R.drawable.discover_like_checked);
    private Drawable likeUnchecked = MResource.getDrawable(R.drawable.discover_like_unchecked);

    public DiscoverAdapter(List<MovieListBean> lists) {
        super(lists);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        if (viewHolder == null) {
            View view = View.inflate(AppEngine.getContext(), R.layout.movie_discover_item_layout, null);
            viewHolder = new Holder(view);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);
        if (viewHolder instanceof Holder) {
            MovieListBean bean = getItem(position);
            Holder holder = (Holder) viewHolder;
            setContent(bean, holder);
        }
    }

    private void setContent(final MovieListBean bean, Holder holder) {
        String imgUrl = StringTool.getQiniuScaledImgUrl(bean.getImg_url(), (int) (holder.imgViewWidth * 0.95), (int) (holder.imgViewHeight * 0.95));
        if (holder.getLayoutPosition() < 30 + headerSize) {
            Glide.with(holder.imageView.getContext())
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .signature(new MovieListKey(bean, imgUrl))
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            Glide.with(holder.imageView.getContext())
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .centerCrop()
                    .into(holder.imageView);
        }
        holder.name.setText(bean.getName());
        holder.likes.setText(Integer.toString(bean.getLikes()));
        if(TextUtils.isEmpty(bean.getCat_name())){
            holder.catName.setVisibility(View.GONE);
        }else {
            holder.catName.setText(String.format("# %s  |   ", bean.getCat_name()));
            holder.catName.setVisibility(View.VISIBLE);
        }
        if (bean.getLiked() == 1) {
            holder.likesIcon.setImageDrawable(likeChecked);
        } else {
            holder.likesIcon.setImageDrawable(likeUnchecked);
        }
        if (bean.isNew() == 1) {
            holder.tag.setVisibility(View.VISIBLE);
        } else {
            holder.tag.setVisibility(View.GONE);
        }
    }

    public class Holder extends RecyclerView.ViewHolder {
        ImageView imageView;
        int imgViewWidth;
        int imgViewHeight;
        TextView name;
        TextView catName;
        ImageView likesIcon;
        TextView likes;
        public View tag;

        public Holder(final View itemView) {
            super(itemView);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = DensityUtils.dp2px(AppEngine.getContext(), 8f);
            itemView.setLayoutParams(lp);
            imageView = (ImageView) itemView.findViewById(R.id.image_view);
            name = (TextView) itemView.findViewById(R.id.name);
            catName = (TextView) itemView.findViewById(R.id.cat_name);
            likesIcon = (ImageView) itemView.findViewById(R.id.likes_icon);
            likes = (TextView) itemView.findViewById(R.id.likes);
            tag = itemView.findViewById(R.id.tag);

            imgViewWidth = WWindowManager.getInstance().getWidth() - DensityUtils.dp2px(AppEngine.getContext(), 14);
            imageView.getLayoutParams().height = imgViewHeight = (int) (imgViewWidth * 0.487);
            imageView.setLayoutParams(imageView.getLayoutParams());
        }
    }
}

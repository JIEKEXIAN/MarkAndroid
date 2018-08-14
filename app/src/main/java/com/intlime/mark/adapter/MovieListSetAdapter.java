package com.intlime.mark.adapter;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.intlime.mark.R;
import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.tools.StringTool;
import com.intlime.mark.tools.ZhugeTool;
import com.intlime.mark.tools.glide.RoundTransform;
import com.intlime.mark.view.recyclerview.RecyclerListAdapter;

import java.util.List;

import kotlin.Pair;

/**
 * Created by root on 16-1-29.
 */
public class MovieListSetAdapter extends RecyclerListAdapter<MovieListBean> {

    private View.OnClickListener itemClickListener;

    private Drawable likeChecked = MResource.getDrawable(R.drawable.like_checked);
    private Drawable likeUnchecked = MResource.getDrawable(R.drawable.like_unchecked);

    private RoundTransform roundTransform = new RoundTransform(AppEngine.getContext(), 4);

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public MovieListSetAdapter(List<MovieListBean> lists) {
        super(lists);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        if (viewHolder == null) {
            viewHolder = new Holder(View.inflate(AppEngine.getContext(), R.layout.movie_list_set_item_layout, null));
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);

        if (viewHolder instanceof Holder) {
            MovieListBean bean = getItem(position);
            Holder holder = (Holder) viewHolder;
            String imgUrl = StringTool.getQiniuScaledImgUrl(bean.getImg_url(), (int) (holder.imgViewWidth * 0.95), (int) (holder.imgViewHeight * 0.95));
            Glide.with(holder.imageView.getContext())
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .transform(roundTransform)
                    .into(holder.imageView);
            holder.name.setText(bean.getName());
            holder.likes.setText(Integer.toString(bean.getLikes()));
            if (bean.getLiked() == 1) {
                holder.likes.setCompoundDrawablesWithIntrinsicBounds(null, likeChecked, null, null);
            } else {
                holder.likes.setCompoundDrawablesWithIntrinsicBounds(null, likeUnchecked, null, null);
            }
        }
    }

    private class Holder extends RecyclerView.ViewHolder {
        ImageView imageView;
        int imgViewWidth;
        int imgViewHeight;
        TextView name;
        TextView likes;
        View tag;

        public Holder(View itemView) {
            super(itemView);
            itemView.setPadding(0, DensityUtils.dp2px(itemView.getContext(), 7), 0, 0);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView = (ImageView) itemView.findViewById(R.id.image_view);
            name = (TextView) itemView.findViewById(R.id.name);
            likes = (TextView) itemView.findViewById(R.id.likes);
            tag = itemView.findViewById(R.id.tag);
            tag.setVisibility(View.GONE);

            imgViewWidth = WWindowManager.getInstance().getWidth() - DensityUtils.dp2px(AppEngine.getContext(), 14);
            imageView.getLayoutParams().height = imgViewHeight = (int) (imgViewWidth * 0.446);
            imageView.setLayoutParams(imageView.getLayoutParams());

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onClick(v);
                    }
                }
            });
            likes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = getAdapterPosition();
                    final MovieListBean bean = getItem(position);
                    if (bean.getLiked() == 1) {
                        bean.setLiked(0);
                        bean.setLikes(bean.getLikes() - 1);
                        likes.setCompoundDrawablesWithIntrinsicBounds(null, likeUnchecked, null, null);
//                        ZhugeTool.INSTANCE.track("影单取消点赞", ZhugeTool.INSTANCE.getTrackArg(new Pair<>("影单名称", bean.getName())));
                    } else {
                        bean.setLiked(1);
                        bean.setLikes(bean.getLikes() + 1);
                        likes.setCompoundDrawablesWithIntrinsicBounds(null, likeChecked, null, null);
                        ZhugeTool.INSTANCE.track("影单点赞", ZhugeTool.INSTANCE.getTrackArg(new Pair<>("影单名称", bean.getName())));
                    }
                    likes.setText(Integer.toString(bean.getLikes()));
                    NetManager.getInstance().likeMovieList(bean.getId(), bean.getLiked() == 1 ? 1 : 2,
                            new NetRequestCallBack() {
                                @Override
                                public void onSuccess(ArrayMap result) {
                                    Intent intent = new Intent(BaseActivity.RELOAD_DISCOVER_ACTION);
                                    intent.putExtra(BaseActivity.BEAN, bean);
                                    AppEngine.getContext().sendBroadcast(intent);
                                }

                                @Override
                                public void onFail(ArrayMap result, int error_code) {
                                    ThreadManager.getInstance().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (bean.getLiked() == 1) {
                                                bean.setLiked(0);
                                                bean.setLikes(bean.getLikes() - 1);
                                                if (position == getAdapterPosition()) {
                                                    likes.setText(Integer.toString(bean.getLikes()));
                                                    likes.setCompoundDrawablesWithIntrinsicBounds(null, likeUnchecked, null, null);
                                                }
                                            } else {
                                                bean.setLiked(1);
                                                bean.setLikes(bean.getLikes() + 1);
                                                if (position == getAdapterPosition()) {
                                                    likes.setText(Integer.toString(bean.getLikes()));
                                                    likes.setCompoundDrawablesWithIntrinsicBounds(null, likeChecked, null, null);
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                }
            });
        }
    }
}

package com.intlime.mark.view.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.Adapter;
import static android.support.v7.widget.RecyclerView.ViewHolder;

/**
 * Created by root on 15-12-22.
 */
public abstract class RecyclerAdapter extends Adapter {

    public abstract int getNormalItemCount();

    protected RecyclerView recyclerView;
    protected RecyclerItemListener recyclerItemListener;

    private List<View> headerList;
    private List<View> footerList;
    protected int headerSize;
    protected int footerSize;

    public final void clearHeaderFooter() {
        headerSize = 0;
        footerSize = 0;
        if (headerList != null) headerList.clear();
        if (footerList != null) footerList.clear();
    }

    public final int getHeaderSize() {
        return headerSize;
    }

    public final int getFooterSize() {
        return footerSize;
    }

    public List<View> getFooterList() {
        if (footerList == null) {
            footerList = new ArrayList<>();
        }
        return footerList;
    }

    public List<View> getHeaderList() {
        if (headerList == null) {
            headerList = new ArrayList<>();
        }
        return headerList;
    }

    public final void addHeaderView(View view) {
        addHeaderView(view, headerSize, true);
    }

    public final void addHeaderView(View view, boolean notify) {
        addHeaderView(view, headerSize, notify);
    }

    public final void addHeaderView(View view, int position) {
        addHeaderView(view, position, true);
    }

    public final void addHeaderView(View view, int position, boolean notify) {
        if (headerList == null) {
            headerList = new ArrayList<>();
        }
        headerList.add(position, view);
        headerSize = headerList.size();
        if (notify) {
            notifyItemInserted(position);
        }
        if (position == 0) {
            recyclerView.scrollToPosition(0);
        }
    }

    public final void addFooterView(View view) {
        addFooterView(view, 0, true);
    }

    public final void addFooterView(View view, boolean notify) {
        addFooterView(view, 0, notify);
    }

    public final void addFooterView(View view, int position) {
        addFooterView(view, position, true);
    }

    public final void addFooterView(View view, int position, boolean notify) {
        if (footerList == null) {
            footerList = new ArrayList<>();
        }
        footerList.add(position, view);
        footerSize = footerList.size();
        if (notify) {
            notifyItemInserted(headerSize + getNormalItemCount() + position);
        }
    }

    public final void removeHeaderView(View view) {
        if (headerList != null) {
            int position = headerList.indexOf(view);
            if (position > -1 && position < headerSize) {
                headerList.remove(view);
                headerSize = headerList.size();
                notifyItemRemoved(position);
            }
        }
    }

    public final void removeHeaderView(int position) {
        if (headerList != null) {
            if (position > -1 && position < headerSize) {
                headerList.remove(position);
                headerSize = headerList.size();
                notifyItemRemoved(position);
            }
        }
    }

    public final void removeFooterView(View view) {
        if (footerList != null) {
            int position = footerList.indexOf(view);
            if (position > -1 && position < footerSize) {
                footerList.remove(view);
                footerSize = footerList.size();
                notifyItemRemoved(headerSize + getNormalItemCount() + position);
            }
        }
    }

    public final void removeFooterView(int position) {
        if (footerList != null) {
            if (position > -1 && position < footerSize) {
                footerList.remove(position);
                footerSize = footerList.size();
                notifyItemRemoved(headerSize + getNormalItemCount() + position);
            }
        }
    }

    public final View getHeaderView(int position) {
        if (headerList != null) {
            return headerList.get(position);
        } else {
            throw new RuntimeException("header is null!");
        }
    }

    public final View getFooterView(int position) {
        if (footerList != null) {
            return footerList.get(position);
        } else {
            throw new RuntimeException("footer is null!");
        }
    }

    @Override
    public int getItemCount() {
        return headerSize + getNormalItemCount() + footerSize;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < headerSize) {
            return 1;//header
        } else if (position >= headerSize + getNormalItemCount()) {
            return -1;//footer
        } else {
            return 0;//normal
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 1) {
            return new HeaderViewHolder(new FrameLayout(parent.getContext()));
        } else if (viewType == -1) {
            return new FooterViewHolder(new FrameLayout(parent.getContext()));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HeaderViewHolder) {
            if (position < 0 || position >= headerSize) return;
            View headerView = headerList.get(position);
            if (headerView.getParent() != null)
                ((ViewGroup) headerView.getParent()).removeView(headerView);
            ((ViewGroup) viewHolder.itemView).removeAllViews();
            ((ViewGroup) viewHolder.itemView).addView(headerView);
        } else if (viewHolder instanceof FooterViewHolder) {
            if (position < 0 || position - headerSize - getNormalItemCount() >= footerSize) return;
            View footerView = footerList.get(position - headerSize - getNormalItemCount());
            if (footerView.getParent() != null)
                ((ViewGroup) footerView.getParent()).removeView(footerView);
            ((ViewGroup) viewHolder.itemView).removeAllViews();
            ((ViewGroup) viewHolder.itemView).addView(footerView);
        } else if (viewHolder != null) {
            if (recyclerItemListener != null) {
                if (recyclerItemListener.clickable) {
                    viewHolder.itemView.setOnClickListener(recyclerItemListener);
                } else {
                    viewHolder.itemView.setOnClickListener(null);
                }
                if (recyclerItemListener.longClickable) {
                    viewHolder.itemView.setOnLongClickListener(recyclerItemListener);
                } else {
                    viewHolder.itemView.setOnLongClickListener(null);
                }
                if (recyclerItemListener.createContextMenuable) {
                    viewHolder.itemView.setOnCreateContextMenuListener(recyclerItemListener);
                } else {
                    viewHolder.itemView.setOnCreateContextMenuListener(null);
                }
            } else {
                viewHolder.itemView.setOnClickListener(null);
                viewHolder.itemView.setOnLongClickListener(null);
                viewHolder.itemView.setOnCreateContextMenuListener(null);
            }
        }
    }


    public final void notifyNormalItemInserted(int position) {
        notifyItemInserted(headerSize + position);
        if (position == 0) {
            recyclerView.scrollToPosition(0);
        }
    }

    public final void notifyNormalItemRemoved(int position) {
        notifyItemRemoved(headerSize + position);
    }

    public final void notifyNormalItemMoved(int fromPosition, int toPosition) {
        notifyItemMoved(headerSize + fromPosition, headerSize + toPosition);
    }

    public final void notifyNormalItemChanged(int position) {
        notifyItemChanged(headerSize + position);
    }


    private class HeaderViewHolder extends ViewHolder {
        public HeaderViewHolder(View itemView) {
            super(itemView);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private class FooterViewHolder extends ViewHolder {
        public FooterViewHolder(View itemView) {
            super(itemView);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}

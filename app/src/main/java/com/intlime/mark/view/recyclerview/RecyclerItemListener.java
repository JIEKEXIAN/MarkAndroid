package com.intlime.mark.view.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;

/**
 * Created by root on 15-12-28.
 */
public abstract class RecyclerItemListener implements
        View.OnClickListener, View.OnLongClickListener, View.OnCreateContextMenuListener {

    protected RecyclerView recyclerView;

    public boolean clickable = false;
    public boolean longClickable = false;
    public boolean createContextMenuable = false;

    @Override
    public final void onClick(View v) {
        onItemClick(v, recyclerView.getChildLayoutPosition(v));
    }

    public void onItemClick(View v, int position) {
    }

    @Override
    public final boolean onLongClick(View v) {
        return onItemLongClick(v, recyclerView.getChildLayoutPosition(v));
    }

    public boolean onItemLongClick(View v, int position) {
        return true;
    }

    @Override
    public final void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        onItemCreateContextMenu(menu, v, menuInfo, recyclerView.getChildLayoutPosition(v));
    }

    public void onItemCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo, int position) {
    }
}

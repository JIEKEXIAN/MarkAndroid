package com.intlime.mark.view.recyclerview;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

/**
 * Created by wtu on 15/12/29.
 */
public abstract class RecyclerCursorAdapter<T> extends RecyclerAdapter {
    protected Cursor mCursor;

    public Cursor getmCursor() {
        return mCursor;
    }

    @Override
    public int getNormalItemCount() {
        return mCursor.getCount();
    }

    public abstract T getItem(int position);

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null && !old.isClosed()) {
            old.close();
        }
    }

    private Cursor swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        this.mCursor = cursor;
        return oldCursor;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);
        mCursor.moveToPosition(position - headerSize);
    }
}

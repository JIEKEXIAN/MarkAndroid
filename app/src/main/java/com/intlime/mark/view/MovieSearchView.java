package com.intlime.mark.view;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.activitys.MainActivity;
import com.intlime.mark.activitys.MovieDetailActivity;
import com.intlime.mark.adapter.MovieSearchAdapter;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.CryptTool;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.db.DBHelper;
import com.intlime.mark.tools.db.MovieDbManager;
import com.intlime.mark.view.recyclerview.GridItemDecoration;
import com.intlime.mark.view.recyclerview.LoadRecyclerView;
import com.intlime.mark.view.recyclerview.RecyclerItemListener;
import com.intlime.mark.view.widget.ClearEditText;
import com.intlime.mark.view.widget.MovieStatusView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by root on 16-1-21.
 */
public class MovieSearchView extends RelativeLayout implements Runnable, LoadRecyclerView.OnLoadListener, View.OnClickListener {
    private MovieSearchAdapter adapter;
    private int id_position = MovieDbManager.ID_P;
    private int db_num_position = MovieDbManager.DBNUM_P;

    private final int limit = 12;
    private ArrayBlockingQueue<String> blockingQueue;
    private ExecutorService singlePool;
    private NetRequestCallBack requestCallBack;
    public ClearEditText editText;
    private View movie_word_layout;
    private View hintLayout;
    private ProgressBar progressBar;
    public LoadRecyclerView recyclerView;
    private Cursor cursor;

    public MovieSearchView(Context context) {
        this(context, null);
    }

    public MovieSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        View.inflate(context, R.layout.movie_search_layout, this);
        setBackgroundResource(R.color.bg);
        init();
    }

    private void init() {
        initTitleBar();
        initEditText();
        initRecyclerView();
        initEmptyView();

        newCursor();
        blockingQueue = new ArrayBlockingQueue<String>(3);
        singlePool = Executors.newSingleThreadExecutor();
    }

    private void newCursor() {
        SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
        cursor = db.rawQuery("select * from " + MovieDbManager.TABEL, null);
    }

    private void initTitleBar() {
        findViewById(R.id.close).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
    }

    private void initEditText() {
        editText = (ClearEditText) findViewById(R.id.edit_text);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
        });
    }

    private void initRecyclerView() {
        recyclerView = (LoadRecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    WWindowManager.getInstance().hideInput();
                }
            }
        });
        adapter = new MovieSearchAdapter(new ArrayList<MovieBean>());
        adapter.setAddClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setRecyclerItemListener(new MyRecyclerItemListener());
        recyclerView.setLoadListener(this);
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) != 0)
                    return layoutManager.getSpanCount();
                else
                    return 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new GridItemDecoration(DensityUtils.dp2px(getContext(), 12), layoutManager.getSpanCount()));
    }

    private void initEmptyView() {
        View emptyView = findViewById(R.id.empty_view);
        hintLayout = emptyView.findViewById(R.id.hint_layout);
        progressBar = (ProgressBar) emptyView.findViewById(R.id.progressBar);
        movie_word_layout = emptyView.findViewById(R.id.movie_word_layout);
        final TextView movie_word = (TextView) emptyView.findViewById(R.id.word);
        final TextView movie_word_name = (TextView) emptyView.findViewById(R.id.name);
        hintLayout.setVisibility(GONE);
        progressBar.setVisibility(GONE);
        recyclerView.registerEmptyView(emptyView);

        final String[] strings = SettingManager.getInstance().getMovieWord();
        movie_word_name.setText(strings[0]);
        movie_word.setText(strings[1]);
        NetManager.getInstance().getMovieWord(new NetRequestCallBack() {
            @Override
            public void onSuccess(ArrayMap result) {
                if (TextUtils.isEmpty(strings[0]) && TextUtils.isEmpty(strings[1])) {
                    final String[] words = SettingManager.getInstance().getMovieWord();
                    ThreadManager.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            movie_word_name.setText(words[0]);
                            movie_word.setText(words[1]);
                        }
                    });
                }
            }
        });
    }

    private void search(String key) {
        if (!TextUtils.isEmpty(key)) {
            movie_word_layout.setVisibility(GONE);
            hintLayout.setVisibility(GONE);
            progressBar.setVisibility(VISIBLE);
            recyclerView.setCanLoad(false);
            recyclerView.loadFinish();
        } else {
            movie_word_layout.setVisibility(VISIBLE);
            hintLayout.setVisibility(GONE);
            progressBar.setVisibility(GONE);
        }
        adapter.lists.clear();
        adapter.notifyDataSetChanged();

        try {
            blockingQueue.put(key);
            singlePool.execute(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            final String key = blockingQueue.take();
            if (requestCallBack != null) {
                requestCallBack.abortHttpRequest();
            }
            if (!TextUtils.isEmpty(key)) {
                requestCallBack = new NetRequestCallBack() {
                    @Override
                    public void onDefault() {
                        if (blockingQueue.isEmpty()) {
                            ThreadManager.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    movie_word_layout.setVisibility(GONE);
                                    hintLayout.setVisibility(VISIBLE);
                                    progressBar.setVisibility(GONE);
                                }
                            });
                        }
                    }

                    @Override
                    public void onSuccess(ArrayMap result) {
                        if (cursor == null || cursor.isClosed()) return;
                        final List<MovieBean> list = (List) result.get("movies");
                        if (!list.isEmpty()) {
                            int i;
                            cursor.moveToPosition(-1);
                            while (cursor.moveToNext()) {
                                for (i = 0; i < list.size(); i++) {
                                    MovieBean bean = list.get(i);
                                    if (bean.getDb_num().equals(cursor.getString(db_num_position))) {
                                        bean.setId(cursor.getInt(id_position));
                                        bean.setCursorPosition(cursor.getPosition());
                                    }
                                }
                            }
                            ThreadManager.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    if (list.size() < limit) {
                                        recyclerView.setCanLoad(false);
                                    } else {
                                        recyclerView.setCanLoad(true);
                                    }
                                    adapter.lists.addAll(list);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                };
                NetManager.getInstance().searchMovie(key, requestCallBack);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onChange() {
        newCursor();
        if (!adapter.lists.isEmpty()) {
            for (int i = 0; i < adapter.lists.size(); i++) {
                MovieBean bean = adapter.lists.get(i);
                bean.setId(0);
                bean.setCursorPosition(-1);
            }
            int i;
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                for (i = 0; i < adapter.lists.size(); i++) {
                    MovieBean bean = adapter.lists.get(i);
                    if (bean.getDb_num().equals(cursor.getString(db_num_position))) {
                        bean.setId(cursor.getInt(id_position));
                        bean.setCursorPosition(cursor.getPosition());
                    }
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoad() {
        requestCallBack = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                recyclerView.loadFinish();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                if (cursor == null || cursor.isClosed()) return;
                final List<MovieBean> list = (List) result.get("movies");
                if (!list.isEmpty()) {
                    int i;
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        for (i = 0; i < list.size(); i++) {
                            MovieBean bean = list.get(i);
                            if (bean.getDb_num().equals(cursor.getString(db_num_position))) {
                                bean.setId(cursor.getInt(id_position));
                                bean.setCursorPosition(cursor.getPosition());
                            }
                        }
                    }
                    ThreadManager.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            if (list.size() < limit) {
                                recyclerView.setCanLoad(false);
                            }
                            int start = adapter.lists.size();
                            adapter.lists.addAll(list);
                            adapter.notifyItemRangeChanged(start, list.size());
                        }
                    });
                } else {
                    ThreadManager.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.setCanLoad(false);
                        }
                    });
                }
            }
        };
        NetManager.getInstance().searchMovie(
                editText.getText().toString(),
                adapter.getNormalItemCount(), requestCallBack);
    }

    private class MyRecyclerItemListener extends RecyclerItemListener {

        public MyRecyclerItemListener() {
            clickable = true;
            longClickable = true;
        }

        @Override
        public void onItemClick(View v, int position) {
            MovieBean bean = adapter.getItem(position);
            if (bean == null) return;
            Intent intent = new Intent(AppEngine.getContext(), MovieDetailActivity.class);
            if (bean.getCursorPosition() >= 0 && bean.getCursorPosition() < cursor.getCount()) {
                boolean bool = cursor.moveToPosition(bean.getCursorPosition());
                if (!bool) return;
                MovieBean temp = MovieDbManager.getInstance().getItemByCursor(cursor);
                temp.setImage(bean.getImage());
                temp.setDb_rating(bean.getDb_rating());
                bean = temp;
            }
            intent.putExtra(BaseActivity.BEAN, bean);
            intent.putExtra("type", 1);
            WWindowManager.getInstance().getCurrentActivity().startActivity(intent);
        }

        @Override
        public boolean onItemLongClick(View v, final int position) {
            MovieBean bean = adapter.getItem(position);
            if (bean.getCursorPosition() >= 0 && bean.getCursorPosition() < cursor.getCount()) {
                boolean bool = cursor.moveToPosition(bean.getCursorPosition());
                if (!bool) return true;
                bean = MovieDbManager.getInstance().getItemByCursor(cursor);
            }
            MovieStatusView.INSTANCE.show(bean, bean.getId() > 0 ? 0 : 1, null, null, new MovieStatusView.OnStatusChangeListener() {
                @Override
                public void onStatusChange(@NonNull MovieBean bean1) {
                    adapter.lists.remove(position);
                    adapter.lists.add(position, bean1);
                    adapter.notifyNormalItemChanged(position);
                }
            });
            return true;
        }
    }

    @Override
    public void onClick(final View v) {
        final int position = recyclerView.getChildLayoutPosition((View) v.getParent());
        final MovieBean bean = adapter.getItem(position);
        final MovieSearchAdapter.Holder holder = (MovieSearchAdapter.Holder)
                recyclerView.findViewHolderForLayoutPosition(position);
        if (bean.getId() > 0) {
            final DialogTool.ConfirmDialog confirmDialog = DialogTool.getConfirmDialog("确认删除", "确定", "取消");
            confirmDialog.confirm_agree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDialog.dismiss();
                    adapter.handling(position);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    NetManager.getInstance().deleteMovie(Integer.toString(bean.getId()), new NetRequestCallBack() {
                        @Override
                        public void onDefault() {
                            adapter.handled(position);
                        }

                        @Override
                        public void onSuccess(ArrayMap result) {
                            ThreadManager.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    holder.progressBar.setVisibility(View.GONE);
                                    holder.add.setImageResource(R.drawable.search_add_icon);
                                }
                            });
                            MovieDbManager.getInstance().delete(bean);
                            AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.RELOAD_ALL_ACTION));
                            bean.setId(0);
                            bean.setCursorPosition(-1);
                            newCursor();
                        }
                    });
                }
            });
            confirmDialog.confirm_disagree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDialog.dismiss();
                }
            });
            confirmDialog.show();
        } else {
            adapter.handling(position);
            holder.progressBar.setVisibility(View.VISIBLE);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("db_num", bean.getDb_num()));
            params.add(new BasicNameValuePair("img_url", CryptTool.base64Encode(CryptTool.encrypt(bean.getImage()))));
            params.add(new BasicNameValuePair("dbrating",
                    CryptTool.base64Encode(CryptTool.encrypt(Float.toString(bean.getDb_rating())))));
            NetRequestCallBack callBack = new NetRequestCallBack() {
                @Override
                public void onDefault() {
                    adapter.handled(position);
                }

                @Override
                public void onSuccess(ArrayMap result) {
                    ThreadManager.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            holder.progressBar.setVisibility(View.GONE);
                            holder.add.setImageResource(R.drawable.search_added_icon);
                        }
                    });
                    MovieBean temp = (MovieBean) result.get("bean");
                    bean.setId(temp.getId());
                    bean.setDb_rating(temp.getDb_rating());
                    bean.setPubdate(temp.getPubdate());
                    bean.setDuration(temp.getDuration());
                    bean.setMovieType(temp.getMovieType());
                    bean.setUpdate_time(temp.getUpdate_time());
                    bean.setPubdateTimestamp(temp.getPubdateTimestamp());
                    MovieDbManager.getInstance().insert(bean);
                    AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.RELOAD_ALL_ACTION));
                    newCursor();
                }

                @Override
                public void onFail(ArrayMap result, int error_code) {
                    ThreadManager.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            };
            NetManager.getInstance().addMovie(params, callBack);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        WWindowManager.getInstance().showSoftInput(editText);
    }

    public void exit() {
        ThreadManager.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                if (requestCallBack != null) {
                    requestCallBack.abortHttpRequest();
                }
            }
        });
        WWindowManager.getInstance().hideInput();
        ((MainActivity) getContext()).switchByAnimation(((MainActivity) getContext()).getCurrentShowTab(), null);
        ((ViewGroup) getParent()).removeView(MovieSearchView.this);
        singlePool.shutdown();
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }
}

package com.intlime.mark.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;

/**
 * Created by root on 16-1-19.
 */
public class MovieHeaderView extends RelativeLayout {
    private TextView word;
    private TextView name;


    public MovieHeaderView(Context context) {
        super(context);
        View.inflate(context, R.layout.movie_header_layout, this);
        word = (TextView) findViewById(R.id.word);
        name = (TextView) findViewById(R.id.name);
        updateMovieWord();
    }

    public void updateMovieWord() {
        String[] strings = SettingManager.getInstance().getMovieWord();
        if (TextUtils.isEmpty(strings[0]) && TextUtils.isEmpty(strings[1])) {
            NetManager.getInstance().getMovieWord(new NetRequestCallBack());
        } else {
            name.setText(strings[0]);
            word.setText(strings[1]);
        }
    }
}

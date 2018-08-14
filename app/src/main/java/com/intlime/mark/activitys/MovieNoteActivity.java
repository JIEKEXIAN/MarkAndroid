package com.intlime.mark.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.db.MovieDbManager;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 16-2-2.
 */
public class MovieNoteActivity extends BaseActivity {
    private MovieBean bean;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bean = getIntent().getParcelableExtra(BEAN);
        if (bean == null) {
            finish();
            return;
        }
        if (bean.getNote() == null) bean.setNote("");
        setContentView(R.layout.activity_movie_note_layout);
    }

    @Override
    protected void initOther() {
        TextView textView = (TextView) findViewById(R.id.title);
        textView.setText(bean.getName());

        editText = (EditText) findViewById(R.id.edit_text);
        editText.setText(bean.getNote());
        editText.setSelection(editText.getText().length());
    }

    public void onClick(View view) {
        final String text = editText.getText().toString();
        if (text.equals(bean.getNote())) {
            finish();
            return;
        }
        NetRequestCallBack callback = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                bean.setNote(text);
                MovieDbManager.getInstance().update(bean);
                Intent intent = new Intent(RELOAD_ALL_ACTION);
                intent.putExtra(BEAN, bean);
                sendBroadcast(intent);
                finish();
            }
        };
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("watchsay", text));
        NetManager.getInstance().noteMovie(bean.getId(), params, callback);
    }

    @Override
    public void onBackPressed() {
        if (editText.getText().toString().equals(bean.getNote())) {
            super.onBackPressed();
        } else {
            final DialogTool.ConfirmDialog dialog = DialogTool.getConfirmDialog("离开更新将不会保存，" +
                    "要保存更改，点击留下然后点击右上方完成按钮。", "留下", "离开");
            dialog.confirm_agree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.confirm_disagree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MovieNoteActivity.super.onBackPressed();
                }
            });
            dialog.show();
        }
    }
}

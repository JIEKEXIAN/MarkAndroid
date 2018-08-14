package com.intlime.mark.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.intlime.mark.R;

/**
 * Created by root on 15-12-25.
 */
public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_layout);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                finish();
                break;
            case R.id.protocol:
                Intent intent = new Intent(this, WebActivity.class);
                intent.putExtra("url", "http://mark.intlime.com/Index/protocol");
                startActivity(intent);
                break;
        }
    }
}

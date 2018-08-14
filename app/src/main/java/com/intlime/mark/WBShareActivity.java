package com.intlime.mark;

import android.os.Bundle;

import com.umeng.socialize.media.WBShareCallBackActivity;

/**
 * Created by root on 16-3-11.
 */
public class WBShareActivity extends WBShareCallBackActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (Exception e) {
            finish();
        }
    }
}

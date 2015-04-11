/**
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Created by: yuanguozheng
 * Created: 2015年3月9日 下午3:53:22
 */
package com.baidu.clipboardlistener;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

/**
 * 剪贴板监听服务
 */
public class ClipboardService extends Service {

    private static boolean mIsInApp;

    private OnPrimaryClipChangedListener mOnPrimaryClipChangedListener = new OnPrimaryClipChangedListener() {

        @Override
        public void onPrimaryClipChanged() {
            startSelectorActivity();
        }
    };

    private ClipboardManager mClipboardManager;

    public static void setInAppMode(boolean isInApp) {
        mIsInApp = isInApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mIsInApp = false;
        mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mClipboardManager.addPrimaryClipChangedListener(mOnPrimaryClipChangedListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClipboardManager.removePrimaryClipChangedListener(mOnPrimaryClipChangedListener);
    }

    private void startSelectorActivity() {
        if (mIsInApp) {
            return;
        }
        String content = getClipContent();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        Intent intent = new Intent(this, SelectorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CreateNoteActivity.CLIP_CONTENT, content);
        startActivity(intent);
    }

    private String getClipContent() {
        ClipData clip = mClipboardManager.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence text = clip.getItemAt(0).getText();
            if (!TextUtils.isEmpty(text)) {
                return text.toString();
            }
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

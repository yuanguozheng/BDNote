/**
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Created by: yuanguozheng
 * Created: 2015年3月9日 下午3:53:22
 */
package com.baidu.clipboardlistener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.clipboardlistener.evernote.EvernoteApi;
import com.evernote.client.android.EvernoteSession.AuthCallback;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.type.Note;

/**
 * 新建笔记并发布对话框界面
 */
public class CreateNoteActivity extends Activity implements OnClickListener {

    public static final String CLIP_CONTENT = "creatnote.content";
    public static final String PLATFORM = "platform";

    private static final String LOGTAG = "CreateNote";

    private EditText mTitleEditText;
    private EditText mContentEditText;
    private Button mCreateButton;
    private ProgressDialog mDialog;

    private NoteHelper mNoteHelper;

    private NoteHelper getNoteHelperByPlatform(String platform) {
        if (platform.equals(getString(R.string.platform_evernote))) {
            return new EvernoteHelper();
        } else if (platform.equals(getString(R.string.platform_baidu))) {
            return new BaidunoteHelper();
        }
        return null;
    }

    static abstract class NoteHelper {

        abstract boolean isLoggedIn();

        abstract boolean login();

        abstract boolean saveNote(String title, String content);
    }

    class EvernoteHelper extends NoteHelper {

        @Override
        boolean isLoggedIn() {
            return EvernoteApi.getInstance().isLoggedIn();
        }

        @Override
        boolean saveNote(String title, String content) {
            EvernoteApi.getInstance().createNote(title, content, mNoteCreateCallback);
            return true;
        }

        @Override
        boolean login() {
            EvernoteApi.getInstance().login(CreateNoteActivity.this, mAuthCallback);
            return true;
        }

    }

    class BaidunoteHelper extends NoteHelper {

        @Override
        boolean isLoggedIn() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        boolean saveNote(String title, String content) {
            mNoteCreateCallback.onSuccess(null);
            return false;
        }

        @Override
        boolean login() {
            // TODO Auto-generated method stub
            return false;
        }

    }

    private AuthCallback mAuthCallback = new AuthCallback() {

        @Override
        public void callback(boolean success) {
            if (!success) {
                Toast.makeText(CreateNoteActivity.this, "登陆失败!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        setupViews();
        updateContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ClipboardService.setInAppMode(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ClipboardService.setInAppMode(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateContent();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_btn_create:
                saveNote();
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void setupViews() {
        mTitleEditText = (EditText) findViewById(R.id.creat_et_title);
        mContentEditText = (EditText) findViewById(R.id.create_et_content);
        mCreateButton = (Button) findViewById(R.id.create_btn_create);
        mCreateButton.setOnClickListener(this);
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay();
        mTitleEditText.setWidth((int) (d.getWidth() * 0.7));
    }

    private void updateContent() {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        String platform = intent.getStringExtra(PLATFORM);
        mNoteHelper = getNoteHelperByPlatform(platform);
        if (mNoteHelper == null) {
            Toast.makeText(this, "传入平台错误", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!mNoteHelper.isLoggedIn()) {
            mNoteHelper.login();
        }
        String content = intent.getStringExtra(CLIP_CONTENT);
        if (!TextUtils.isEmpty(content)) {
            mContentEditText.setText(content);
        }
    }

    /**
     * Saves text field content as note to selected notebook, or default notebook if no notebook select
     */
    private void saveNote() {
        String title = mTitleEditText.getText().toString();
        String content = mContentEditText.getText().toString();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(getApplicationContext(), "内容不完整", Toast.LENGTH_LONG).show();
            return;
        }
        showProgressDialog();
        mNoteHelper.saveNote(title, content);
    }

    // Callback used as a result of creating a note in a normal notebook or a linked notebook
    private OnClientCallback<Note> mNoteCreateCallback = new OnClientCallback<Note>() {
        @Override
        public void onSuccess(Note note) {
            Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_LONG).show();
            dismissProgressDialog();
            finish();
        }

        @Override
        public void onException(Exception exception) {
            Log.e(LOGTAG, "Error saving note", exception);
            Toast.makeText(getApplicationContext(), "保存失败", Toast.LENGTH_LONG).show();
            dismissProgressDialog();
        }
    };

    protected void showProgressDialog() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(this);
        }
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.setMessage("加载中...");
        mDialog.show();
    }

    protected void dismissProgressDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}

/**
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Created by: yuanguozheng
 * Created: 2015年3月9日 下午3:53:22
 */

package com.baidu.clipboardlistener;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * 笔记平台选择对话框
 */
public class SelectorActivity extends Activity implements OnItemClickListener {

    private ListView mListView;
    private String mContent;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);
        init();
        Intent intent = getIntent();
        if (intent == null) {
            return;
        } else {
            String action = intent.getAction();
            String type = intent.getType();
            if (!TextUtils.isEmpty(action) && !TextUtils.isEmpty(type)) {
                if (type.startsWith("image/")) {
                    Uri image = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (image!=null) {
                        mImageUri = image;
                    }
                }
            }
        }
        String content = intent.getStringExtra(CreateNoteActivity.CLIP_CONTENT);
        if (!TextUtils.isEmpty(content)) {
            mContent = content;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String platform = (String) view.getTag();
        Intent intent = new Intent(this, CreateNoteActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CreateNoteActivity.CLIP_CONTENT, mContent);
        intent.putExtra(CreateNoteActivity.IMAGE_URI, mImageUri);
        intent.putExtra(CreateNoteActivity.PLATFORM, platform);
        startActivity(intent);
        finish();
    }

    private void init() {
        setTitle(R.string.title_activity_selector);
        mListView = (ListView) findViewById(R.id.lv_platforms);
        mListView.setOnItemClickListener(this);
        String[] platforms = getResources().getStringArray(R.array.platforms_array);
        mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, platforms) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                v.setTag(getItem(position));
                return v;
            };
        });
    }
}

/**
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Created by: yuanguozheng
 * Created: 2015年3月9日 下午3:53:22
 */
package com.baidu.clipboardlistener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * 主界面，登录/注销
 */
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, ClipboardService.class);
        startService(intent);
        Toast.makeText(this, "笔记已启动", Toast.LENGTH_LONG).show();
        finish();
    }

}

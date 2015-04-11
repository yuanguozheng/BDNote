/**
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Created by: yuanguozheng
 * Created: 2015年3月9日 下午3:53:22
 */
package com.baidu.clipboardlistener;

import android.app.Application;

import cn.adbshell.common.app.ResourcesManager;
import cn.adbshell.common.app.SystemManager;

import com.baidu.clipboardlistener.evernote.EvernoteApi;

/**
 * 全局类
 */
public class ClipApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ResourcesManager.init(this);
        SystemManager.init(this);
        EvernoteApi.init(this);
    }
}

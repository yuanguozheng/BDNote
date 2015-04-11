/**
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Created by: chenming03
 * Created: 2015年3月9日 下午3:53:22
 */
package com.baidu.clipboardlistener.evernote;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.evernote.client.android.AsyncLinkedNoteStoreClient;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.thrift.transport.TTransportException;

import java.util.List;

/**
 * 封装后的EvernoteAPI
 */
public class EvernoteApi {

    private static final String LOGTAG = "EvernoteApi";

    /**
     * You MUST change the following values to run this sample application.
     */
    // Your Evernote API key. See http://dev.evernote.com/documentation/cloud/
    // Please obfuscate your code to help keep these values secret.
    private static final String CONSUMER_KEY = "yuanguozheng";
    private static final String CONSUMER_SECRET = "7236c0360a03bcc6";

    // Initial development is done on Evernote's testing service, the sandbox.
    // Change to HOST_PRODUCTION to use the Evernote production service
    // once your code is complete, or HOST_CHINA to use the Yinxiang Biji
    // (Evernote China) production service.
    private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.SANDBOX;

    // Set this to true if you want to allow linked notebooks for accounts that can only access a single
    // notebook.
    private static final boolean SUPPORT_APP_LINKED_NOTEBOOKS = true;

    /**
     * *he following values are simply part of the demo application.
     */
    protected EvernoteSession mEvernoteSession;

    private static EvernoteApi sInstance;

    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new EvernoteApi(context);
        }
    }

    private EvernoteApi(Context context) {
        // Set up the Evernote Singleton Session
        mEvernoteSession =
                EvernoteSession.getInstance(context, CONSUMER_KEY, CONSUMER_SECRET, EVERNOTE_SERVICE,
                        SUPPORT_APP_LINKED_NOTEBOOKS);
    }

    public static EvernoteApi getInstance() {
        return sInstance;
    }

    public void createNote(String title, String content, OnClientCallback<Note> createNoteCallback) {
        Note note = new Note();
        note.setTitle(title);
        // TODO: line breaks need to be converted to render in ENML
        note.setContent(EvernoteUtil.NOTE_PREFIX + content + EvernoteUtil.NOTE_SUFFIX);
        if (!mEvernoteSession.getAuthenticationResult().isAppLinkedNotebook()) {
            try {
                mEvernoteSession.getClientFactory().createNoteStoreClient().createNote(note, createNoteCallback);
            } catch (TTransportException exception) {
                Log.e(LOGTAG, "Error creating notestore", exception);
                createNoteCallback.onException(exception);
            }
        } else {
            createNoteInAppLinkedNotebook(note, createNoteCallback);
        }
    }

    /**
     * Creates the specified note in an app's linked notebook. Used when an app only has access to a single notebook,
     * and that notebook is a linked notebook.
     * 
     * @param note the note to be created
     * @param createNoteCallback called on success or failure
     */
    protected void createNoteInAppLinkedNotebook(final Note note, final OnClientCallback<Note> createNoteCallback) {
        OnClientCallback<Pair<AsyncLinkedNoteStoreClient, LinkedNotebook>> callback =
                new OnClientCallback<Pair<AsyncLinkedNoteStoreClient, LinkedNotebook>>() {
                    @Override
                    public void onSuccess(final Pair<AsyncLinkedNoteStoreClient, LinkedNotebook> pair) {
                        // Rely on the callback to dismiss the dialog
                        pair.first.createNoteAsync(note, pair.second, createNoteCallback);
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.e(LOGTAG, "Error creating linked notestore", exception);
                        createNoteCallback.onException(exception);
                    }
                };
        invokeOnAppLinkedNotebook(callback);
    }

    /**
     * Helper method for apps that have access to a single notebook, and that notebook is a linked notebook ... find
     * that notebook, gets access to it, and calls back to the caller.
     * 
     * @param callback invoked on error or with a client to the linked notebook
     */
    private void invokeOnAppLinkedNotebook(
            final OnClientCallback<Pair<AsyncLinkedNoteStoreClient, LinkedNotebook>> callback) {
        try {
            // We need to get the one and only linked notebook
            OnClientCallback<List<LinkedNotebook>> clientCallback = new OnClientCallback<List<LinkedNotebook>>() {
                @Override
                public void onSuccess(List<LinkedNotebook> linkedNotebooks) {
                    createLinkedNoteStoreClientAsync(linkedNotebooks, callback);
                }

                @Override
                public void onException(Exception exception) {
                    callback.onException(exception);
                }
            };
            mEvernoteSession.getClientFactory().createNoteStoreClient().listLinkedNotebooks(clientCallback);
        } catch (TTransportException exception) {
            callback.onException(exception);
        }
    }

    private void createLinkedNoteStoreClientAsync(List<LinkedNotebook> linkedNotebooks,
            final OnClientCallback<Pair<AsyncLinkedNoteStoreClient, LinkedNotebook>> callback) {
        // We should only have one linked notebook
        if (linkedNotebooks.size() != 1) {
            Log.e(LOGTAG, "Error getting linked notebook - more than one linked notebook");
            callback.onException(new Exception("Not single linked notebook"));
            return;
        }
        final LinkedNotebook linkedNotebook = linkedNotebooks.get(0);
        OnClientCallback<AsyncLinkedNoteStoreClient> linkedNoteCallback =
                new OnClientCallback<AsyncLinkedNoteStoreClient>() {
                    @Override
                    public void onSuccess(AsyncLinkedNoteStoreClient asyncLinkedNoteStoreClient) {
                        // Finally create the note in the linked notebook
                        callback.onSuccess(new Pair<AsyncLinkedNoteStoreClient, LinkedNotebook>(
                                asyncLinkedNoteStoreClient, linkedNotebook));
                    }

                    @Override
                    public void onException(Exception exception) {
                        callback.onException(exception);
                    }
                };
        mEvernoteSession.getClientFactory().createLinkedNoteStoreClientAsync(linkedNotebook, linkedNoteCallback);
    }

    /**
     * Called when the user taps the "Log in to Evernote" button. Initiates the Evernote OAuth process
     */
    public void login(Context context) {
        mEvernoteSession.authenticate(context);
    }

    /**
     * Called when the user taps the "Log in to Evernote" button. Clears Evernote Session and logs out
     */
    public void logout(Context context) {
        try {
            mEvernoteSession.logOut(context);
        } catch (InvalidAuthenticationException e) {
            Log.e(LOGTAG, "Tried to call logout with not logged in", e);
        }
    }

    public boolean isLoggedIn() {
        return mEvernoteSession.isLoggedIn();
    }
}
/*
 * Copyr  2012 Evernote Corporation
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.evernote.client.android;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import cn.adbshell.common.util.UIUtil;

import com.evernote.androidsdk.R;
import com.evernote.client.android.EvernoteSession.AuthCallback;
import com.evernote.client.oauth.EvernoteAuthToken;
import com.evernote.client.oauth.YinxiangApi;
import com.evernote.edam.userstore.BootstrapInfo;
import com.evernote.edam.userstore.BootstrapProfile;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.EvernoteApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;

/**
 * An Android Activity for authenticating to Evernote using OAuth. Third parties should not need to use this class
 * directly.
 * 
 * 
 * class created by @tylersmithnet
 */
public class EvernoteOAuthDialog extends Dialog {

    private static final String LOGTAG = "EvernoteOAuthActivity";

    static final String EXTRA_EVERNOTE_SERVICE = "EVERNOTE_HOST";
    static final String EXTRA_CONSUMER_KEY = "CONSUMER_KEY";
    static final String EXTRA_CONSUMER_SECRET = "CONSUMER_SECRET";
    static final String EXTRA_REQUEST_TOKEN = "REQUEST_TOKEN";
    static final String EXTRA_REQUEST_TOKEN_SECRET = "REQUEST_TOKEN_SECRET";
    static final String EXTRA_SUPPORT_APP_LINKED_NOTEBOOKS = "SUPPORT_APP_LINKED_NOTEBOOKS";
    static final String EXTRA_BOOTSTRAP_SELECTED_PROFILE_POS = "BOOTSTRAP_SELECTED_PROFILE_POS";
    static final String EXTRA_BOOTSTRAP_SELECTED_PROFILE = "BOOTSTRAP_SELECTED_PROFILE";
    static final String EXTRA_BOOTSTRAP_SELECTED_PROFILES = "BOOTSTRAP_SELECTED_PROFILES";

    private int mEvernoteService = -1;

    private BootstrapProfile mSelectedBootstrapProfile;
    private int mSelectedBootstrapProfilePos = 0;
    private ArrayList<BootstrapProfile> mBootstrapProfiles = new ArrayList<BootstrapProfile>();

    private String mConsumerKey = null;
    private String mConsumerSecret = null;
    private String mRequestToken = null;
    private String mRequestTokenSecret = null;
    private boolean mSupportAppLinkedNotebooks = false;

    private WebView mWebView;

    private AsyncTask mBeginAuthSyncTask = null;
    private AsyncTask mCompleteAuthSyncTask = null;

    private ProgressDialog mProgressDialog;

    private AuthCallback mCallback;

    public EvernoteOAuthDialog(Context context, int service, String key, String secret, boolean support,
            AuthCallback callback) {
        super(context, android.R.style.Theme_Black_NoTitleBar);
        setCanceledOnTouchOutside(false);
        mEvernoteService = service;
        mConsumerKey = key;
        mConsumerSecret = secret;
        mSupportAppLinkedNotebooks = support;
        mCallback = callback;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.esdk__webview);
        mWebView = (WebView) findViewById(R.id.esdk__webview);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.getSettings().setJavaScriptEnabled(true);
        if (TextUtils.isEmpty(mConsumerKey) || TextUtils.isEmpty(mConsumerSecret)) {
            exit(false);
            return;
        }
        if (mSelectedBootstrapProfile == null) {
            mBeginAuthSyncTask = new BootstrapAsyncTask().execute();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit(false);
    }

    /**
     * Specifies a URL scheme that uniquely identifies callbacks to this application after a user authorizes access to
     * their Evernote account in our WebView.
     */
    private String getCallbackScheme() {
        return "en-oauth";
    }

    /**
     * Create a Scribe OAuthService object that can be used to perform OAuth authentication with the appropriate
     * Evernote service.
     */
    private OAuthService createService() {
        Class<? extends Api> apiClass = null;
        String host = mSelectedBootstrapProfile.getSettings().getServiceHost();
        if (host != null && !host.startsWith("http")) {
            host = "https://" + host;
        }
        if (host.equals(EvernoteSession.HOST_SANDBOX)) {
            apiClass = EvernoteApi.Sandbox.class;
        } else if (host.equals(EvernoteSession.HOST_PRODUCTION)) {
            apiClass = EvernoteApi.class;
        } else if (host.equals(EvernoteSession.HOST_CHINA)) {
            apiClass = YinxiangApi.class;
        } else {
            throw new IllegalArgumentException("Unsupported Evernote host: " + host);
        }
        return new ServiceBuilder().provider(apiClass).apiKey(mConsumerKey).apiSecret(mConsumerSecret)
                .callback(getCallbackScheme() + "://callback").build();
    }

    /**
     * Get a request token from the Evernote service and send the user to our WebView to authorize access.
     */
    private class BootstrapAsyncTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected String doInBackground(Void...params) {
            String url = null;
            try {
                EvernoteSession session = EvernoteSession.getOpenSession();
                if (session != null) {
                    // Network request
                    BootstrapManager.BootstrapInfoWrapper infoWrapper =
                            session.getBootstrapSession().getBootstrapInfo();

                    if (infoWrapper != null) {
                        BootstrapInfo info = infoWrapper.getBootstrapInfo();
                        if (info != null) {
                            mBootstrapProfiles = (ArrayList<BootstrapProfile>) info.getProfiles();
                            if (mBootstrapProfiles != null && mBootstrapProfiles.size() > 0
                                    && mSelectedBootstrapProfilePos < mBootstrapProfiles.size()) {
                                mSelectedBootstrapProfile = mBootstrapProfiles.get(mSelectedBootstrapProfilePos);
                            }
                        }
                    }
                }
                if (mSelectedBootstrapProfile == null
                        || TextUtils.isEmpty(mSelectedBootstrapProfile.getSettings().getServiceHost())) {
                    Log.d(LOGTAG, "Bootstrap did not return a valid host");
                    return null;
                }

                OAuthService service = createService();

                Log.i(LOGTAG, "Retrieving OAuth request token...");
                Token reqToken = service.getRequestToken();
                mRequestToken = reqToken.getToken();
                mRequestTokenSecret = reqToken.getSecret();

                Log.i(LOGTAG, "Redirecting user for authorization...");
                url = service.getAuthorizationUrl(reqToken);
                if (mSupportAppLinkedNotebooks) {
                    url += "&supportLinkedSandbox=true";
                }
            } catch (BootstrapManager.ClientUnsupportedException cue) {
                return null;
            } catch (Exception ex) {
                Log.e(LOGTAG, "Failed to obtain OAuth request token", ex);
            }
            return url;
        }

        /**
         * Open a WebView to allow the user to authorize access to their account.
         * 
         * @param url The URL of the OAuth authorization web page.
         */
        @Override
        protected void onPostExecute(String url) {
            UIUtil.dismissDialogSafe(mProgressDialog);
            if (!TextUtils.isEmpty(url)) {
                mWebView.loadUrl(url);
            } else {
                exit(false);
            }
        }
    }

    /**
     * An AsyncTask to complete the OAuth process after successful user authorization.
     */
    private class CompleteAuthAsyncTask extends AsyncTask<Uri, Void, EvernoteAuthToken> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected EvernoteAuthToken doInBackground(Uri...uris) {
            EvernoteAuthToken authToken = null;
            if (uris == null || uris.length == 0) {
                return null;
            }
            Uri uri = uris[0];

            if (!TextUtils.isEmpty(mRequestToken)) {
                OAuthService service = createService();
                String verifierString = uri.getQueryParameter("oauth_verifier");
                String appLnbString = uri.getQueryParameter("sandbox_lnb");
                boolean isAppLinkedNotebook = "true".equalsIgnoreCase(appLnbString);
                if (TextUtils.isEmpty(verifierString)) {
                    Log.i(LOGTAG, "User did not authorize access");
                } else {
                    Verifier verifier = new Verifier(verifierString);
                    Log.i(LOGTAG, "Retrieving OAuth access token...");
                    try {
                        Token reqToken = new Token(mRequestToken, mRequestTokenSecret);
                        authToken =
                                new EvernoteAuthToken(service.getAccessToken(reqToken, verifier), isAppLinkedNotebook);
                    } catch (Exception ex) {
                        Log.e(LOGTAG, "Failed to obtain OAuth access token", ex);
                    }
                }
            } else {
                Log.d(LOGTAG, "Unable to retrieve OAuth access token, no request token");
            }
            return authToken;
        }

        /**
         * Save the authentication information resulting from a successful OAuth authorization and complete the
         * activity.
         */

        @Override
        protected void onPostExecute(EvernoteAuthToken authToken) {
            UIUtil.dismissDialogSafe(mProgressDialog);
            if (EvernoteSession.getOpenSession() == null) {
                exit(false);
                return;
            }
            exit(EvernoteSession.getOpenSession().persistAuthenticationToken(getContext().getApplicationContext(),
                    authToken, mSelectedBootstrapProfile.getSettings().getServiceHost()));
        }
    }

    /**
     * Exit the activity and display a toast message.
     * 
     * @param success Whether the OAuth process completed successfully.
     */
    private void exit(final boolean success) {
        UIUtil.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(),
                        success ? R.string.esdk__evernote_login_successful : R.string.esdk__evernote_login_failed,
                        Toast.LENGTH_LONG).show();
                dismiss();
                if (mCallback != null) {
                    mCallback.callback(success);
                }
            }
        });
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage(getContext().getString(R.string.esdk__loading));
            mProgressDialog.show();
        }
    }

    /**
     * Overrides the callback URL and authenticate
     */
    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (uri.getScheme().equals(getCallbackScheme())) {
                if (mCompleteAuthSyncTask == null) {
                    mCompleteAuthSyncTask = new CompleteAuthAsyncTask().execute(uri);
                }
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    };

    /**
     * Allows for showing progress
     */
    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            // TODO by mchen EvernoteOAuthActivity.this.setProgress(newProgress * 1000);
        }
    };
}

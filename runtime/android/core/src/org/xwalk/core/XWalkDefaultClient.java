// Copyright (c) 2013 Intel Corporation. All rights reserved
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

// TODO(yongsheng): remove public modifier.
public class XWalkDefaultClient extends XWalkClient {

    // Strings for displaying Dialog.
    private static String mAlertTitle;
    private static String mSslAlertTitle;
    private static String mOKButton;
    private static String mCancelButton;

    private Context mContext;
    private AlertDialog mDialog;
    private XWalkView mView;
    private HttpAuthDatabase mDatabase;
    private static final String HTTP_AUTH_DATABASE_FILE = "http_auth.db";

    public XWalkDefaultClient(Context context, XWalkView view) {
        mDatabase = new HttpAuthDatabase(context.getApplicationContext(), HTTP_AUTH_DATABASE_FILE);
        mContext = context;
        mView = view;
    }

    /***
     * Retrieve the HTTP authentication username and password for a given
     * host & realm pair.
     *
     * @param host The host for which the credentials apply.
     * @param realm The realm for which the credentials apply.
     * @return String[] if found, String[0] is username, which can be null and
     *         String[1] is password. Return null if it can't find anything.
     */
    public String[] getHttpAuthUsernamePassword(String host, String realm) {
        return mDatabase.getHttpAuthUsernamePassword(host, realm);
    }

    @Override
    public void onReceivedError(XWalkView view, int errorCode,
            String description, String failingUrl) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(android.R.string.dialog_alert_title)
                .setMessage(description)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        mDialog = dialogBuilder.create();
        mDialog.show();
    }

    @Override
    public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback,
            SslError error) {
        final ValueCallback<Boolean> valueCallback = callback;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        // Don't use setOnDismissListener because it's supported since API level 17.
        dialogBuilder.setTitle(R.string.ssl_alert_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        valueCallback.onReceiveValue(true);
                        dialog.dismiss();
                    }
                }).setNegativeButton(android.R.string.cancel, null)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        valueCallback.onReceiveValue(false);
                    }
                });
        mDialog = dialogBuilder.create();
        mDialog.show();
    }

    /***
     * Set the HTTP authentication credentials for a given host and realm.
     *
     * @param host The host for the credentials.
     * @param realm The realm for the credentials.
     * @param username The username for the password. If it is null, it means
     *                 password can't be saved.
     * @param password The password
     */
    public void setHttpAuthUsernamePassword(String host, String realm,
            String username, String password) {
        mDatabase.setHttpAuthUsernamePassword(host, realm, username, password);
    }

    private void showHttpAuthDialog( final XWalkHttpAuthHandler handler,
            final String host, final String realm) {
        LinearLayout layout = new LinearLayout(mContext);
        final EditText userNameEditText = new EditText(mContext);
        final EditText passwordEditText = new EditText(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPaddingRelative(10, 0, 10, 20);
        userNameEditText.setHint(R.string.http_auth_user_name);
        passwordEditText.setHint(R.string.http_auth_password);
        layout.addView(userNameEditText);
        layout.addView(passwordEditText);

        final Activity curActivity = mView.getActivity();
        AlertDialog.Builder httpAuthDialog = new AlertDialog.Builder(curActivity);
        httpAuthDialog.setTitle(R.string.http_auth_title)
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.http_auth_log_in, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String userName = userNameEditText.getText().toString();
                        String password = passwordEditText.getText().toString();
                        setHttpAuthUsernamePassword(host, realm, userName, password);
                        handler.proceed(userName, password);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        handler.cancel();
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    @Override
    public void onReceivedHttpAuthRequest(XWalkView view,
            XWalkHttpAuthHandler handler, String host, String realm) {
        if (view != null) {
            showHttpAuthDialog(handler, host, realm);
        }
    }

    @Override
    public void onCloseWindow(XWalkView view) {
        if (view != null && view.getActivity() != null) {
            view.getActivity().finish();
        }
    }
}
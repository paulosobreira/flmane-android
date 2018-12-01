package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    Activity mContext;

    /**
     * Instantiate the interface and set the context
     */
    WebAppInterface(Activity c) {
        mContext = c;
    }

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void exitApp() {
        Intent intent = new Intent(mContext, LoginActivity.class);
        mContext.startActivity(intent);
        mContext.finish();
    }
}
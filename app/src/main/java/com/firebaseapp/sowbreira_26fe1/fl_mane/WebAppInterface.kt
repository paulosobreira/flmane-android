package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.app.Activity
import android.content.Intent
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val mContext: Activity) {

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun exitApp() {
        val intent = Intent(mContext, LoginActivity::class.java)
        mContext.startActivity(intent)
        mContext.finish()
    }
}

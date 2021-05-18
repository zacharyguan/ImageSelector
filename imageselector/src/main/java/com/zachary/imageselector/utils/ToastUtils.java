package com.zachary.imageselector.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.zachary.imageselector.R;

public class ToastUtils {
    public static void showWxToast(Context context, String msg) {
        Toast toast = new Toast(context.getApplicationContext());
        TextView textView = (TextView) LayoutInflater.from(context).inflate(R.layout.toast_wx_msg, null);
        textView.setText(msg);
        toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 174);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
}

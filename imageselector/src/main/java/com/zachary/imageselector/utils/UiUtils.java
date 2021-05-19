package com.zachary.imageselector.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.zachary.imageselector.R;

public class UiUtils {
    public static void showWxToast(Context context, String msg) {
        Toast toast = new Toast(context.getApplicationContext());
        TextView textView = (TextView) LayoutInflater.from(context).inflate(R.layout.toast_wx_msg, null);
        textView.setText(msg);
        toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 174);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    private static AlertDialog dialog;
    public static void showLoading(Context context) {
        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        ImageView ivLoading = layout.findViewById(R.id.iv_loading);
        RotateAnimation mRotateAnimation;
        mRotateAnimation = new RotateAnimation(0, 360, Animation.RESTART, 0.5f, Animation.RESTART, 0.5f);
        mRotateAnimation.setDuration(700);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);

        mRotateAnimation.setStartTime(Animation.START_ON_FIRST_FRAME);
        LinearInterpolator lin = new LinearInterpolator();
        mRotateAnimation.setInterpolator(lin);

        ivLoading.startAnimation(mRotateAnimation);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout);
        builder.setCancelable(true);

        if( dialog != null ) {
            dialog.dismiss();
            dialog = null;
        }

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    public static void hideLoading() {
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.cancel();
            }
            dialog = null;
        }
    }
}

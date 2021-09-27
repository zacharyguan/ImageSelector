package com.zachary.imageselector.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.chrisbanes.photoview.PhotoView;
import com.zachary.imageselector.R;

public class PreviewView extends FrameLayout {
    PhotoView mPhotoView;
    ImageView mPlayButton;

    public PreviewView(@NonNull Context context) {
        this(context, null);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPhotoView = new PhotoView(getContext());
        addView(mPhotoView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));

        mPlayButton = new ImageView(getContext());
        mPlayButton.setImageResource(R.drawable.is_icon_play);
        mPlayButton.setBackgroundResource(R.drawable.is_bg_icon_play);
        int length = getContext().getResources().getDisplayMetrics().widthPixels/4;
        addView(mPlayButton, new LayoutParams(length, length, Gravity.CENTER));
    }

    public PhotoView getPhotoView() {
        return mPhotoView;
    }

    public void setPhotoView(PhotoView mPhotoView) {
        this.mPhotoView = mPhotoView;
    }

    public ImageView getPlayButton() {
        return mPlayButton;
    }

    public void setPlayButton(ImageView mPlayButton) {
        this.mPlayButton = mPlayButton;
    }
}

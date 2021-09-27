package com.zachary.imageselector.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.zachary.imageselector.entry.Image;
import com.zachary.imageselector.entry.Video;
import com.zachary.imageselector.utils.ImageUtil;
import com.zachary.imageselector.utils.VersionUtils;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.zachary.imageselector.view.PreviewView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ImagePagerAdapter extends PagerAdapter {

    private Context mContext;
    private List<PreviewView> viewList = new ArrayList<>(4);
    private List<Image> mImgList;
    private OnItemClickListener mListener;
    private boolean isAndroidQ = VersionUtils.isAndroidQ();

    public ImagePagerAdapter(Context context, List<Image> imgList) {
        this.mContext = context;
        createImageViews();
        mImgList = imgList;
    }

    private void createImageViews() {
        for (int i = 0; i < 4; i++) {
            PreviewView previewView = new PreviewView(mContext);
            previewView.getPhotoView().setAdjustViewBounds(true);
            viewList.add(previewView);
        }
    }

    @Override
    public int getCount() {
        return mImgList == null ? 0 : mImgList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof PreviewView) {
            PreviewView view = (PreviewView) object;
            view.getPhotoView().setImageDrawable(null);
            view.getPhotoView().setOnClickListener(null);
            view.getPlayButton().setOnClickListener(null);
            viewList.add(view);
            container.removeView(view);
        }
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final PreviewView currentView = viewList.remove(0);
        final Image image = mImgList.get(position);
        container.addView(currentView);
        if (image.isGif()) {
            currentView.getPhotoView().setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(mContext).load(isAndroidQ ? image.getUri() : image.getPath()).override(720,1080)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(currentView.getPhotoView());
        } else {
            Glide.with(mContext)
                    .load(isAndroidQ ? image.getUri() : image.getPath()).asBitmap().diskCacheStrategy(DiskCacheStrategy.NONE).override(720,1080).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    int bw = resource.getWidth();
                    int bh = resource.getHeight();
                    if (bw > 4096 || bh > 4096) {
                        Bitmap bitmap = ImageUtil.zoomBitmap(resource, 4096, 4096);
                        setBitmap(currentView.getPhotoView(), bitmap);
                    } else {
                        setBitmap(currentView.getPhotoView(), resource);
                    }
                }
            });
        }
        currentView.getPhotoView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(position, image);
                }
            }
        });
        if (image instanceof Video) {
            currentView.getPlayButton().setVisibility(View.VISIBLE);
            currentView.getPlayButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onPlayClick(position, (Video) image);
                    }
                }
            });
        } else {
            currentView.getPlayButton().setVisibility(View.GONE);
        }
        return currentView;
    }

    private void setBitmap(PhotoView imageView, Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        if (bitmap != null) {
            int bw = bitmap.getWidth();
            int bh = bitmap.getHeight();
            int vw = imageView.getWidth();
            int vh = imageView.getHeight();
            if (bw != 0 && bh != 0 && vw != 0 && vh != 0) {
                if (1.0f * bh / bw > 1.0f * vh / vw) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    float offset = (1.0f * bh * vw / bw - vh) / 2;
                    adjustOffset(imageView, offset);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mListener = l;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, Image image);
        void onPlayClick(int position, Video video);
    }

    private void adjustOffset(PhotoView view, float offset) {
        PhotoViewAttacher attacher = view.getAttacher();
        try {
            Field field = PhotoViewAttacher.class.getDeclaredField("mBaseMatrix");
            field.setAccessible(true);
            Matrix matrix = (Matrix) field.get(attacher);
            matrix.postTranslate(0, offset);
            Method method = PhotoViewAttacher.class.getDeclaredMethod("resetMatrix");
            method.setAccessible(true);
            method.invoke(attacher);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

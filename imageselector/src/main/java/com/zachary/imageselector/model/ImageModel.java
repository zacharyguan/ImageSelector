package com.zachary.imageselector.model;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;

import com.zachary.imageselector.R;
import com.zachary.imageselector.entry.Folder;
import com.zachary.imageselector.entry.Image;
import com.zachary.imageselector.utils.ImageUtil;
import com.zachary.imageselector.utils.StringUtils;
import com.zachary.imageselector.utils.UriUtils;
import com.zachary.imageselector.utils.VersionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageModel {
    private static boolean filterByResolutions = false;//"8192x4096","6720x3360","5376x2688","4096x2048"

    /**
     * 缓存图片
     */
    private static ArrayList<Folder> cacheImageList = null;
    private static boolean isNeedCache = false;
    private static PhotoContentObserver observer;

    /**
     * 预加载图片
     *
     * @param context
     */
    public static void preloadAndRegisterContentObserver(final Context context) {
        isNeedCache = true;
        if (observer == null) {
            observer = new PhotoContentObserver(context.getApplicationContext());
            context.getApplicationContext().getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);
        }
        preload(context);
    }

    private static void preload(final Context context) {
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
            loadImageForSDCard(context, true, null);
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache(Context context) {
        isNeedCache = false;
        if (observer != null) {
            context.getApplicationContext().getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (ImageModel.class) {
                    if (cacheImageList != null) {
                        cacheImageList.clear();
                        cacheImageList = null;
                    }
                }
            }
        }).start();
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param callback
     */
    public static void loadImageForSDCard(final Context context, final DataCallback callback) {
        loadImageForSDCard(context, false, callback);
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param isPreload 是否是预加载
     * @param callback
     */
    private static void loadImageForSDCard(final Context context, final boolean isPreload, final DataCallback callback) {
        //由于扫描图片是耗时的操作，所以要在子线程处理。
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (ImageModel.class) {
                    String imageCacheDir = ImageUtil.getImageCacheDir(context);
                    ArrayList<Folder> folders = null;
                    if (cacheImageList == null || isPreload) {
                        ArrayList<Image> imageList = loadImage(context);
                        Collections.sort(imageList, new Comparator<Image>() {
                            @Override
                            public int compare(Image image, Image t1) {
                                if (image.getTime() > t1.getTime()) {
                                    return 1;
                                } else if (image.getTime() < t1.getTime()) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                        ArrayList<Image> images = new ArrayList<>();

                        for (Image image : imageList) {
                            // 过滤不存在或未下载完成的图片
                            boolean exists = !"downloading".equals(getExtensionName(image.getPath())) && checkImgExists(image.getPath());
                            //过滤剪切保存的图片；
                            boolean isCutImage = ImageUtil.isCutImage(imageCacheDir, image.getPath());
                            if (!isCutImage && exists) {
                                images.add(image);
                            }
                        }
                        Collections.reverse(images);
                        folders = splitFolder(context, images);
                        if (isNeedCache) {
                            cacheImageList = folders;
                        }
                    } else {
                        folders = cacheImageList;
                    }

                    if (callback != null) {
                        callback.onSuccess(folders);
                    }
                }
            }
        }).start();
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @return
     */
    private static synchronized ArrayList<Image> loadImage(Context context) {

        //扫描图片
        Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = context.getContentResolver();

        Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.MIME_TYPE,
                        MediaStore.Images.Media.SIZE},
                MediaStore.MediaColumns.SIZE + ">0",
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC");

        ArrayList<Image> images = new ArrayList<>();

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                // 获取图片的路径
                long id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Images.Media._ID));
                String path = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                //获取图片名称
                String name = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                //获取图片时间
                long time = mCursor.getLong(
                        mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));

                if (String.valueOf(time).length() < 13) {
                    time *= 1000;
                }

                //获取图片类型
                String mimeType = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));

                //获取图片uri
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(id)).build();

                images.add(new Image(path, time, name, mimeType, uri));
            }
            mCursor.close();
        }
        return images;
    }

    /**
     * 检查图片是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     *
     * @param filePath
     * @return
     */
    private static boolean checkImgExists(String filePath) {
        return new File(filePath).exists();
    }

    private static String getPathForAndroidQ(Context context, long id) {
        return UriUtils.getPathForUri(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(id)).build());
    }

    /**
     * 把图片按文件夹拆分，第一个文件夹保存所有的图片
     *
     * @param images
     * @return
     */
    private static ArrayList<Folder> splitFolder(Context context, ArrayList<Image> images) {
        ArrayList<Folder> folders = new ArrayList<>();
        folders.add(new Folder(context.getString(R.string.selector_all_image), images));
		if (filterByResolutions) {
		    folders.addAll(filterByResolutions(context, images));
		}

        if (images != null && !images.isEmpty()) {
            int size = images.size();
            for (int i = 0; i < size; i++) {
                String path = images.get(i).getPath();
                String name = getFolderName(path);
                if (StringUtils.isNotEmptyString(name)) {
                    Folder folder = getFolder(name, folders);
                    folder.addImage(images.get(i));
                }
            }
        }
        return folders;
    }

    private static ArrayList<Folder> filterByResolutions(Context context, ArrayList<Image> images) {
        ArrayList<Folder> ret = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            final String[] resolutionStrings = {"8192x4096","6720x3360","5376x2688","4096x2048"};
            int[][] resolutions = new int[resolutionStrings.length][2];
            for (int i = 0; i < resolutionStrings.length; i++ ) {
                String[] values = resolutionStrings[i].split("x");
                resolutions[i][0] = Integer.parseInt(values[0]);
                resolutions[i][1] = Integer.parseInt(values[1]);
            }
            Folder[] folders = new Folder[resolutionStrings.length];

            for (Image image : images) {
                try {
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    if (VersionUtils.isAndroidQ()) {
                        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(image.getUri(), "r");
                        BitmapFactory.decodeFileDescriptor(parcelFileDescriptor.getFileDescriptor(), null, options);
                    } else {
                        BitmapFactory.decodeFile(image.getPath(), options);
                    }

                    for (int i = 0; i< resolutionStrings.length; i++) {
                        if (options.outWidth == resolutions[i][0] && options.outHeight == resolutions[i][1]) {
                            if (folders[i] == null) {
                                folders[i] = new Folder(context.getString(R.string.selector_resolution,resolutionStrings[i]));
                            }
                            folders[i].addImage(image);
                        }
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < resolutionStrings.length; i++) {
                if (folders[i] != null) {
                    ret.add(folders[i]);
                }
            }
        }
        return ret;
    }

    /**
     * Java文件操作 获取文件扩展名
     */
    public static String getExtensionName(String filename) {
        if (filename != null && filename.length() > 0) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    /**
     * 根据图片路径，获取图片文件夹名称
     *
     * @param path
     * @return
     */
    private static String getFolderName(String path) {
        if (StringUtils.isNotEmptyString(path)) {
            String[] strings = path.split(File.separator);
            if (strings.length >= 2) {
                return strings[strings.length - 2];
            }
        }
        return "";
    }

    private static Folder getFolder(String name, List<Folder> folders) {
        if (!folders.isEmpty()) {
            int size = folders.size();
            for (int i = 0; i < size; i++) {
                Folder folder = folders.get(i);
                if (name.equals(folder.getName())) {
                    return folder;
                }
            }
        }
        Folder newFolder = new Folder(name);
        folders.add(newFolder);
        return newFolder;
    }

    public interface DataCallback {
        void onSuccess(ArrayList<Folder> folders);
    }

    private static class PhotoContentObserver extends ContentObserver {

        private WeakReference<Context> contextRef;

        PhotoContentObserver(Context appContext) {
            super(null);
            contextRef = new WeakReference<>(appContext);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            preload(contextRef.get());
        }
    }
}

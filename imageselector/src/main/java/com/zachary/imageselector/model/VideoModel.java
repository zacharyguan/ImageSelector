package com.zachary.imageselector.model;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import com.zachary.imageselector.R;
import com.zachary.imageselector.entry.Folder;
import com.zachary.imageselector.entry.Image;
import com.zachary.imageselector.entry.Video;
import com.zachary.imageselector.utils.StringUtils;
import com.zachary.imageselector.utils.UriUtils;
import com.zachary.imageselector.utils.VersionUtils;
import com.zachary.imageselector.utils.VideoUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VideoModel {

    /**
     * 缓存视频
     */
    private static ArrayList<Folder> cacheVideoList = null;
    private static boolean isNeedCache = false;
    private static VideoContentObserver observer;
    private static boolean isAndroidQ = VersionUtils.isAndroidQ();

    /**
     * 预加载视频
     *
     * @param context
     */
    public static void preloadAndRegisterContentObserver(final Context context) {
        isNeedCache = true;
        if (observer == null) {
            observer = new VideoContentObserver(context.getApplicationContext());
            context.getApplicationContext().getContentResolver().registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer);
        }
        preload(context);
    }

    private static void preload(final Context context) {
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载视频。
            loadVideoForSDCard(context, true, null);
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
                synchronized (VideoModel.class) {
                    if (cacheVideoList != null) {
                        cacheVideoList.clear();
                        cacheVideoList = null;
                    }
                }
            }
        }).start();
    }

    /**
     * 从SDCard加载视频
     *
     * @param context
     * @param callback
     */
    public static void loadVideoForSDCard(final Context context, final DataCallback callback) {
        loadVideoForSDCard(context, false, callback);
    }

    /**
     * 从SDCard加载视频
     *
     * @param context
     * @param isPreload 是否是预加载
     * @param callback
     */
    private static void loadVideoForSDCard(final Context context, final boolean isPreload, final DataCallback callback) {
        //由于扫描视频是耗时的操作，所以要在子线程处理。
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (VideoModel.class) {
                    ArrayList<Folder> folders = null;
                    if (cacheVideoList == null || isPreload) {
                        ArrayList<Image> videoList = loadVideo(context);
                        Collections.sort(videoList, new Comparator<Image>() {
                            @Override
                            public int compare(Image video, Image t1) {
                                if (video.getTime() > t1.getTime()) {
                                    return 1;
                                } else if (video.getTime() < t1.getTime()) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                        ArrayList<Image> videos = new ArrayList<>();

                        for (Image video : videoList) {
                            // 过滤不存在或未下载完成的视频
                            boolean exists = !"downloading".equals(getExtensionName(video.getPath())) && checkImgExists(video.getPath());
                            if (exists) {
                                videos.add(video);
                            }
                        }
                        Collections.reverse(videos);
                        folders = splitFolder(context, videos);
                        if (isNeedCache) {
                            cacheVideoList = folders;
                        }
                    } else {
                        folders = cacheVideoList;
                    }

                    if (callback != null) {
                        callback.onSuccess(folders);
                    }
                }
            }
        }).start();
    }

    /**
     * 从SDCard加载视频
     *
     * @param context
     * @return
     */
    private static synchronized ArrayList<Image> loadVideo(Context context) {

        //扫描视频
        Uri mVideoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = context.getContentResolver();

        Cursor mCursor = mContentResolver.query(mVideoUri, new String[]{
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.MIME_TYPE,
                        MediaStore.Video.Media.SIZE,
                },
                MediaStore.MediaColumns.SIZE + ">0",
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC");

        ArrayList<Image> videos = new ArrayList<>();

        //读取扫描到的视频
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                // 获取视频的路径
                long id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Video.Media._ID));
                String path = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                //获取视频名称
                String name = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                //获取视频时间
                long time = mCursor.getLong(
                        mCursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));

                if (String.valueOf(time).length() < 13) {
                    time *= 1000;
                }

                //获取视频类型
                String mimeType = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE));

                //获取视频uri
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(id)).build();

                String duration = isAndroidQ ? VideoUtils.getRingDuring(context, uri) : VideoUtils.getRingDuring(path);

                videos.add(new Video(path, time, name, mimeType, uri, formatDuration(duration)));
            }
            mCursor.close();
        }
        return videos;
    }

    private static String formatDuration(String duration) {
        if (duration != null && !duration.isEmpty()) {
            try {
                long totalSecs = Long.valueOf(duration)/1000;
                long mins = totalSecs/60;
                long secs = totalSecs%60;
                return String.format(Locale.CHINA, "%d:%02d", mins, secs);
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * 检查视频是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     *
     * @param filePath
     * @return
     */
    private static boolean checkImgExists(String filePath) {
        return new File(filePath).exists();
    }

    private static String getPathForAndroidQ(Context context, long id) {
        return UriUtils.getPathForUri(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(id)).build());
    }

    /**
     * 把视频按文件夹拆分，第一个文件夹保存所有的视频
     *
     * @param videos
     * @return
     */
    private static ArrayList<Folder> splitFolder(Context context, ArrayList<Image> videos) {
        ArrayList<Folder> folders = new ArrayList<>();
        folders.add(new Folder(context.getString(R.string.selector_all_video), videos));

        if (videos != null && !videos.isEmpty()) {
            int size = videos.size();
            for (int i = 0; i < size; i++) {
                String path = videos.get(i).getPath();
                String name = getFolderName(path);
                if (StringUtils.isNotEmptyString(name)) {
                    Folder folder = getFolder(name, folders);
                    folder.addImage(videos.get(i));
                }
            }
        }
        return folders;
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
     * 根据视频路径，获取视频文件夹名称
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

    private static class VideoContentObserver extends ContentObserver {

        private WeakReference<Context> contextRef;

        VideoContentObserver(Context appContext) {
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

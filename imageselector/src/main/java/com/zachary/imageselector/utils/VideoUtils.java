package com.zachary.imageselector.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class VideoUtils {

    public static String getRingDuring(Context context, Uri uri){
        String duration=null;
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();

        try {
            if (uri != null) {
                mmr.setDataSource(context, uri);
            }
            duration = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            mmr.release();
        }
        Log.e("test","duration "+duration);
        return duration;
    }

    public static String getRingDuring(String path){
        String duration=null;
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();

        try {
            if (path != null) {
                mmr.setDataSource(path);
            }
            duration = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            mmr.release();
        }
        Log.e("test","duration "+duration);
        return duration;
    }
}

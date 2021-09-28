package com.zachary.imageselector.entry;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * @Author teach liang
 * @Description 封装请求参数
 * @Date 2019/9/23
 */
public class RequestConfig implements Parcelable {

    public boolean isCrop = false; // 是否剪切
    public boolean useCamera = true;  // 是否支持拍照
    public boolean onlyTakePhoto = false; // 仅拍照，不打开相册。true时，useCamera也必定为true。
    public boolean isSingle = false; // 是否单选
    public boolean canPreview = true; // 是否可以点击图片预览
    public int maxSelectCount; //图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
    public ArrayList<String> selected; //接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，重新打开选择器，允许用户把先前选过的图片传进来，并把这些图片默认为选中状态。
    public float cropRatio = 1.0f; // 图片剪切的宽高比，宽固定为手机屏幕的宽。
    public boolean onlyVideo = false;//仅视频
    public boolean canChangeOriginalDrawing = true;//是否显示原图开关
    public boolean isOriginalDrawing = false;//是否使用原图
    public int requestCode;

    public RequestConfig() { }

    protected RequestConfig(Parcel in) {
        isCrop = in.readByte() != 0;
        useCamera = in.readByte() != 0;
        onlyTakePhoto = in.readByte() != 0;
        isSingle = in.readByte() != 0;
        canPreview = in.readByte() != 0;
        maxSelectCount = in.readInt();
        selected = in.createStringArrayList();
        cropRatio = in.readFloat();
        onlyVideo = in.readByte() != 0;
        canChangeOriginalDrawing = in.readByte() != 0;
        isOriginalDrawing = in.readByte() != 0;
        requestCode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isCrop ? 1 : 0));
        dest.writeByte((byte) (useCamera ? 1 : 0));
        dest.writeByte((byte) (onlyTakePhoto ? 1 : 0));
        dest.writeByte((byte) (isSingle ? 1 : 0));
        dest.writeByte((byte) (canPreview ? 1 : 0));
        dest.writeInt(maxSelectCount);
        dest.writeStringList(selected);
        dest.writeFloat(cropRatio);
        dest.writeByte((byte) (onlyVideo ? 1 : 0));
        dest.writeByte((byte) (canChangeOriginalDrawing ? 1 : 0));
        dest.writeByte((byte) (isOriginalDrawing ? 1 : 0));
        dest.writeInt(requestCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RequestConfig> CREATOR = new Creator<RequestConfig>() {
        @Override
        public RequestConfig createFromParcel(Parcel in) {
            return new RequestConfig(in);
        }

        @Override
        public RequestConfig[] newArray(int size) {
            return new RequestConfig[size];
        }
    };
}

package com.zachary.imageselector;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zachary.imageselector.adapter.FolderAdapter;
import com.zachary.imageselector.adapter.ImageAdapter;
import com.zachary.imageselector.entry.Folder;
import com.zachary.imageselector.entry.Image;
import com.zachary.imageselector.entry.RequestConfig;
import com.zachary.imageselector.model.ImageModel;
import com.zachary.imageselector.utils.DateUtils;
import com.zachary.imageselector.utils.ImageSelector;
import com.zachary.imageselector.utils.ImageUtil;
import com.zachary.imageselector.utils.UiUtils;
import com.zachary.imageselector.utils.UriUtils;
import com.zachary.imageselector.utils.VersionUtils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ImageSelectorActivity extends AppCompatActivity {

    private TextView tvTime;
    private TextView tvFolderName;
    private ImageView ivFolderIndicator;
    private TextView tvConfirm;
    private TextView tvPreview;
    private FrameLayout btnConfirm;
    private FrameLayout btnPreview;
    private RecyclerView rvImage;
    private RecyclerView rvFolder;
    private View masking;
    private LinearLayout btnOriginalDrawing;
    private ImageView ivOriginalDrawing;

    private ImageAdapter mAdapter;
    private GridLayoutManager mLayoutManager;

    private ArrayList<Folder> mFolders;
    private Folder mFolder;
    private boolean applyLoadImage = false;
    private boolean applyCamera = false;
    private static final int PERMISSION_WRITE_EXTERNAL_REQUEST_CODE = 0x00000011;
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 0x00000012;

    private static final int CAMERA_REQUEST_CODE = 0x00000010;
    private Uri mCameraUri;
    private String mCameraImagePath;
    private long mTakeTime;

    private boolean isOpenFolder;
    private boolean isShowTime;
    private boolean isInitFolder;
    private boolean isSingle;
    private boolean canPreview = true;
    private int mMaxCount;

    private boolean useCamera = true;
    private boolean onlyTakePhoto = false;

    private boolean isOriginalDrawing = false;

    private Handler mMainHandler = new Handler();
    private Runnable mHide = new Runnable() {
        @Override
        public void run() {
            hideTime();
        }
    };
    private Runnable mShowLoadingTask = new Runnable() {
        @Override
        public void run() {
            UiUtils.showLoading(ImageSelectorActivity.this);
        }
    };

    //用于接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开选择器，允许用
    // 户把先前选过的图片传进来，并把这些图片默认为选中状态。
    private ArrayList<String> mSelectedImages;

    /**
     * 启动图片选择器
     *
     * @param activity
     * @param requestCode
     * @param config
     */
    public static void openActivity(Activity activity, int requestCode, RequestConfig config) {
        Intent intent = new Intent(activity, ImageSelectorActivity.class);
        intent.putExtra(ImageSelector.KEY_CONFIG, config);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动图片选择器
     *
     * @param fragment
     * @param requestCode
     * @param config
     */
    public static void openActivity(Fragment fragment, int requestCode, RequestConfig config) {
        Intent intent = new Intent(fragment.getActivity(), ImageSelectorActivity.class);
        intent.putExtra(ImageSelector.KEY_CONFIG, config);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动图片选择器
     *
     * @param fragment
     * @param requestCode
     * @param config
     */
    public static void openActivity(android.app.Fragment fragment, int requestCode, RequestConfig config) {
        Intent intent = new Intent(fragment.getActivity(), ImageSelectorActivity.class);
        intent.putExtra(ImageSelector.KEY_CONFIG, config);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        RequestConfig config = intent.getParcelableExtra(ImageSelector.KEY_CONFIG);
        mMaxCount = config.maxSelectCount;
        isSingle = config.isSingle;
        canPreview = config.canPreview;
        useCamera = config.useCamera;
        mSelectedImages = config.selected;
        onlyTakePhoto = config.onlyTakePhoto;
        if (onlyTakePhoto) {
            // 仅拍照
            checkPermissionAndCamera();
        } else {
            setContentView(R.layout.activity_image_select);
            setStatusBarColor();
            initView();
            initListener();
            initImageList();
            checkPermissionAndLoadImages();
            hideFolderList();
            setSelectImageCount(0);
        }
    }

    /**
     * 修改状态栏颜色
     */
    private void setStatusBarColor() {
        if (VersionUtils.isAndroidL()) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#373c3d"));
        }
    }

    private void initView() {
        rvImage = findViewById(R.id.rv_image);
        rvFolder = findViewById(R.id.rv_folder);
        tvConfirm = findViewById(R.id.tv_confirm);
        tvPreview = findViewById(R.id.tv_preview);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnPreview = findViewById(R.id.btn_preview);
        tvFolderName = findViewById(R.id.tv_folder_name);
        ivFolderIndicator = findViewById(R.id.iv_folder_indicator);
        tvTime = findViewById(R.id.tv_time);
        masking = findViewById(R.id.masking);
        btnOriginalDrawing = findViewById(R.id.btn_original_drawing);
        ivOriginalDrawing = findViewById(R.id.iv_original_drawing);
    }

    private void initListener() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Image> images = new ArrayList<>();
                images.addAll(mAdapter.getSelectImages());
                toPreviewActivity(images, 0);
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm();
            }
        });

        findViewById(R.id.btn_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInitFolder) {
                    if (isOpenFolder) {
                        closeFolder();
                    } else {
                        openFolder();
                    }
                }
            }
        });

        masking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFolder();
            }
        });

        rvImage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                changeTime();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                changeTime();
            }
        });
        btnOriginalDrawing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOriginalDrawing = !isOriginalDrawing;
                ivOriginalDrawing.setSelected(isOriginalDrawing);
            }
        });
    }

    /**
     * 初始化图片列表
     */
    private void initImageList() {
        // 判断屏幕方向
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLayoutManager = new GridLayoutManager(this, 4);
        } else {
            mLayoutManager = new GridLayoutManager(this, 7);
        }

        rvImage.setLayoutManager(mLayoutManager);
        mAdapter = new ImageAdapter(this, mMaxCount, isSingle, canPreview);
        rvImage.setAdapter(mAdapter);
        ((SimpleItemAnimator) rvImage.getItemAnimator()).setSupportsChangeAnimations(false);
        if (mFolders != null && !mFolders.isEmpty()) {
            setFolder(mFolders.get(0));
        }
        mAdapter.setOnImageSelectListener(new ImageAdapter.OnImageSelectListener() {
            @Override
            public void OnImageSelect(Image image, boolean isSelect, int selectCount) {
                setSelectImageCount(selectCount);
            }
        });
        mAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(Image image, int position) {
                toPreviewActivity(mAdapter.getData(), position);
            }

            @Override
            public void OnCameraClick() {
                checkPermissionAndCamera();
            }
        });
    }

    /**
     * 初始化图片文件夹列表
     */
    private void initFolderList() {
        if (mFolders != null && !mFolders.isEmpty()) {
            isInitFolder = true;
            rvFolder.setLayoutManager(new LinearLayoutManager(ImageSelectorActivity.this));
            FolderAdapter adapter = new FolderAdapter(ImageSelectorActivity.this, mFolders);
            adapter.setOnFolderSelectListener(new FolderAdapter.OnFolderSelectListener() {
                @Override
                public void OnFolderSelect(Folder folder) {
                    setFolder(folder);
                    closeFolder();
                }
            });
            rvFolder.setAdapter(adapter);
        }
    }

    /**
     * 刚开始的时候文件夹列表默认是隐藏的
     */
    private void hideFolderList() {
        rvFolder.post(new Runnable() {
            @Override
            public void run() {
                rvFolder.setTranslationY(-rvFolder.getHeight());
                rvFolder.setVisibility(View.GONE);
                rvFolder.setBackgroundColor(Color.WHITE);
            }
        });
    }

    /**
     * 设置选中的文件夹，同时刷新图片列表
     *
     * @param folder
     */
    private void setFolder(Folder folder) {
        if (folder != null && mAdapter != null && !folder.equals(mFolder)) {
            mFolder = folder;
            tvFolderName.setText(folder.getName());
            rvImage.scrollToPosition(0);
            mAdapter.refresh(folder.getImages(), folder.isUseCamera());
        }
    }

    private void setSelectImageCount(int count) {
        if (count == 0) {
            btnConfirm.setEnabled(false);
            btnPreview.setEnabled(false);
            tvConfirm.setText(R.string.selector_send);
            tvPreview.setText(R.string.selector_preview);
        } else {
            btnConfirm.setEnabled(true);
            btnPreview.setEnabled(true);
            tvPreview.setText(getString(R.string.selector_preview) + "(" + count + ")");
            if (isSingle) {
                tvConfirm.setText(R.string.selector_send);
            } else if (mMaxCount > 0) {
                tvConfirm.setText(getString(R.string.selector_send) + "(" + count + "/" + mMaxCount + ")");
            } else {
                tvConfirm.setText(getString(R.string.selector_send) + "(" + count + ")");
            }
        }
    }

    /**
     * 弹出文件夹列表
     */
    private void openFolder() {
        if (!isOpenFolder) {
            masking.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    -rvFolder.getHeight(), 0).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    rvFolder.setVisibility(View.VISIBLE);
                }
            });
            Animation indicatorAnim = AnimationUtils.loadAnimation(this,  R.anim.is_folder_indicator);
            indicatorAnim.setInterpolator(new LinearInterpolator());
            indicatorAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    ivFolderIndicator.setImageResource(R.drawable.is_icon_arrow_up);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            ivFolderIndicator.startAnimation(indicatorAnim);
            animator.start();
            isOpenFolder = true;
        }
    }

    /**
     * 收起文件夹列表
     */
    private void closeFolder() {
        if (isOpenFolder) {
            masking.setVisibility(View.GONE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    0, -rvFolder.getHeight()).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rvFolder.setVisibility(View.GONE);
                }
            });
            Animation indicatorAnim = AnimationUtils.loadAnimation(this,  R.anim.is_folder_indicator);
            indicatorAnim.setInterpolator(new LinearInterpolator());
            indicatorAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    ivFolderIndicator.setImageResource(R.drawable.is_icon_arrow_down);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            ivFolderIndicator.startAnimation(indicatorAnim);
            animator.start();
            isOpenFolder = false;
        }
    }

    /**
     * 隐藏时间条
     */
    private void hideTime() {
        if (isShowTime) {
            ObjectAnimator.ofFloat(tvTime, "alpha", 1, 0).setDuration(300).start();
            isShowTime = false;
        }
    }

    /**
     * 显示时间条
     */
    private void showTime() {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(tvTime, "alpha", 0, 1).setDuration(300).start();
            isShowTime = true;
        }
    }

    /**
     * 改变时间条显示的时间（显示图片列表中的第一个可见图片的时间）
     */
    private void changeTime() {
        int firstVisibleItem = getFirstVisibleItem();
        Image image = mAdapter.getFirstVisibleImage(firstVisibleItem);
        if (image != null) {
            String time = DateUtils.getImageTime(this, image.getTime());
            tvTime.setText(time);
            showTime();
            mMainHandler.removeCallbacks(mHide);
            mMainHandler.postDelayed(mHide, 350);
        }
    }

    private int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();
    }

    private void showLoading() {
        mMainHandler.postDelayed(mShowLoadingTask, 300);
    }

    private void hideLoading() {
        mMainHandler.removeCallbacksAndMessages(mShowLoadingTask);
        UiUtils.hideLoading();
    }

    private String getCompressedImagePath(String originalPath) {
        String name = DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.getDefault())).toString();
        String path = ImageUtil.getImageCacheDir(this);
        Bitmap bitmap = ImageUtil.decodeSampledBitmapByWidthFromFile(this, originalPath, 1080);
        if (bitmap != null) {
            return ImageUtil.saveImage(bitmap, path, name);
        }
        return originalPath;
    }

    private Thread mCompressSaveThread = new Thread(new Runnable() {
        @Override
        public void run() {
            final ArrayList<String> images = new ArrayList<>();
            for (Image image : mAdapter.getSelectImages()) {
                images.add(getCompressedImagePath(image.getPath()));
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMainHandler.removeCallbacksAndMessages(mHide);
                    saveImageAndFinish(images, false);
                    mCompressSaveThread = null;
                }
            });
        }
    });
    private void confirm() {
        if (mAdapter == null) {
            return;
        }
        //因为图片的实体类是Image，而我们返回的是String数组，所以要进行转换。
        if (isOriginalDrawing) {
            final ArrayList<Image> selectImages = mAdapter.getSelectImages();
            final ArrayList<String> images = new ArrayList<>();
            for (Image image : selectImages) {
                images.add(image.getPath());
            }
            saveImageAndFinish(images, false);
        } else {//如果未选择使用原图则压缩
            if (mCompressSaveThread == null || mCompressSaveThread.isAlive()) {
                return;
            }
            showLoading();
            mCompressSaveThread.start();
        }
    }

    private void saveImageAndFinish(final ArrayList<String> images, final boolean isCameraImage) {
        //点击确定，把选中的图片通过Intent传给上一个Activity。
        setResult(images, isCameraImage);
        finish();
    }

    private void setResult(ArrayList<String> images, boolean isCameraImage) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(ImageSelector.SELECT_RESULT, images);
        intent.putExtra(ImageSelector.IS_CAMERA_IMAGE, isCameraImage);
        setResult(RESULT_OK, intent);
    }

    private void toPreviewActivity(ArrayList<Image> images, int position) {
        if (images != null && !images.isEmpty()) {
            PreviewActivity.openActivity(this, images,
                    mAdapter.getSelectImages(), isSingle, mMaxCount, position, isOriginalDrawing);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (applyLoadImage) {
            applyLoadImage = false;
            checkPermissionAndLoadImages();
        }
        if (applyCamera) {
            applyCamera = false;
            checkPermissionAndCamera();
        }
    }

    /**
     * 处理图片预览页返回的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImageSelector.RESULT_CODE) {
            if (data != null) {
                //同步预览页的是否使用原图
                isOriginalDrawing = data.getBooleanExtra(ImageSelector.IS_ORIGINAL_DRAWING, isOriginalDrawing);
                ivOriginalDrawing.setSelected(isOriginalDrawing);
                if (data.getBooleanExtra(ImageSelector.IS_CONFIRM, false)) {
                    //如果用户在预览页点击了确定，就直接把用户选中的图片返回给用户。
                    confirm();
                    return;
                }
            }
            //否则，就刷新当前页面。
            mAdapter.notifyDataSetChanged();
            setSelectImageCount(mAdapter.getSelectImages().size());
        } else if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> images = new ArrayList<>();
                Uri savePictureUri = null;
                if (VersionUtils.isAndroidQ()) {
                    savePictureUri = mCameraUri;
                    images.add(UriUtils.getPathForUri(this, mCameraUri));
                } else {
                    savePictureUri = Uri.fromFile(new File(mCameraImagePath));
                    images.add(mCameraImagePath);
                }
                ImageUtil.savePicture(this,savePictureUri,mTakeTime);
                saveImageAndFinish(images, true);
            } else {
                if (onlyTakePhoto) {
                    finish();
                }
            }
        }
    }

    /**
     * 横竖屏切换处理
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mLayoutManager != null && mAdapter != null) {
            //切换为竖屏
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                mLayoutManager.setSpanCount(4);
            }
            //切换为横屏
            else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mLayoutManager.setSpanCount(7);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 检查权限并加载SD卡里的图片。
     */
    private void checkPermissionAndLoadImages() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            Toast.makeText(this, "没有图片", Toast.LENGTH_LONG).show();
            return;
        }
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
            loadImageForSDCard();
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(ImageSelectorActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_REQUEST_CODE);
        }
    }

    /**
     * 检查权限并拍照。
     */
    private void checkPermissionAndCamera() {
        int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasCameraPermission == PackageManager.PERMISSION_GRANTED
                && hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有调起相机拍照。
            openCamera();
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(ImageSelectorActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA_REQUEST_CODE);
        }
    }

    /**
     * 处理权限申请的回调。
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_WRITE_EXTERNAL_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，加载图片。
                loadImageForSDCard();
            } else {
                //拒绝权限，弹出提示框。
                showExceptionDialog(true);
            }
        } else if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，有调起相机拍照。
                openCamera();
            } else {
                //拒绝权限，弹出提示框。
                showExceptionDialog(false);
            }
        }
    }

    /**
     * 发生没有权限等异常时，显示一个提示dialog.
     */
    private void showExceptionDialog(final boolean applyLoad) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.selector_hint)
                .setMessage(R.string.selector_permissions_hint)
                .setNegativeButton(R.string.selector_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                }).setPositiveButton(R.string.selector_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                startAppSettings();
                if (applyLoad) {
                    applyLoadImage = true;
                } else {
                    applyCamera = true;
                }
            }
        }).show();
    }

    /**
     * 从SDCard加载图片。
     */
    private void loadImageForSDCard() {
        ImageModel.loadImageForSDCard(this, new ImageModel.DataCallback() {
            @Override
            public void onSuccess(ArrayList<Folder> folders) {
                mFolders = folders;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFolders != null && !mFolders.isEmpty()) {
                            initFolderList();
                            mFolders.get(0).setUseCamera(useCamera);
                            setFolder(mFolders.get(0));
                            if (mSelectedImages != null && mAdapter != null) {
                                mAdapter.setSelectedImages(mSelectedImages);
                                mSelectedImages = null;
                                setSelectImageCount(mAdapter.getSelectImages().size());
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * 调起相机拍照
     */
    private void openCamera() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            Uri photoUri = null;

            if (VersionUtils.isAndroidQ()) {
                photoUri = createImagePathUri();
            } else {
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null) {
                    mCameraImagePath = photoFile.getAbsolutePath();
                    if (VersionUtils.isAndroidN()) {
                        //通过FileProvider创建一个content类型的Uri
                        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".imageSelectorProvider", photoFile);
                    } else {
                        photoUri = Uri.fromFile(photoFile);
                    }
                }
            }

            mCameraUri = photoUri;
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE);
                mTakeTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * 创建一条图片地址uri,用于保存拍照后的照片
     *
     * @return 图片的uri
     */
    public Uri createImagePathUri() {
        String status = Environment.getExternalStorageState();
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        long time = System.currentTimeMillis();
        String imageName = timeFormatter.format(new Date(time));
        // ContentValues是我们希望这条记录被创建时包含的数据信息
        ContentValues values = new ContentValues(2);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            return getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = String.format("JPEG_%s.jpg", timeStamp);
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File tempFile = new File(storageDir, imageFileName);
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }
        return tempFile;
    }

    /**
     * 启动应用的设置
     */
    private void startAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && isOpenFolder) {
            closeFolder();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        hideLoading();
        super.onDestroy();
    }
}

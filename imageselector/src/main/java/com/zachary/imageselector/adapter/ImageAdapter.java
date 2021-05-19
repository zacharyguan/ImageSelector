package com.zachary.imageselector.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zachary.imageselector.R;
import com.zachary.imageselector.entry.Image;
import com.zachary.imageselector.utils.UiUtils;
import com.zachary.imageselector.utils.VersionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Image> mImages;
    private LayoutInflater mInflater;

    //保存选中的图片
    private ArrayList<Image> mSelectImages = new ArrayList<>();
    private OnImageSelectListener mSelectListener;
    private OnItemClickListener mItemClickListener;
    private int mMaxCount;
    private boolean isSingle;
    private boolean isViewImage;

    private static final int TYPE_CAMERA = 1;
    private static final int TYPE_IMAGE = 2;

    private boolean useCamera;

    private boolean isAndroidQ = VersionUtils.isAndroidQ();

    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     * @param isViewImage 是否点击放大图片查看
     */
    public ImageAdapter(Context context, int maxCount, boolean isSingle, boolean isViewImage) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        this.isViewImage = isViewImage;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = mInflater.inflate(R.layout.adapter_images_item, parent, false);
            return new ViewHolder(view);
        } else {
            View view = mInflater.inflate(R.layout.adapter_camera, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_IMAGE) {
            final Image image = getImage(position);
            Glide.with(mContext).load(isAndroidQ ? image.getUri() : image.getPath())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(holder.ivImage);

            setItemSelect(holder, mSelectImages.contains(image), position);

            holder.ivGif.setVisibility(image.isGif() ? View.VISIBLE : View.GONE);

            //点击选中/取消选中图片
            holder.btnSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedImage(holder, image);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isViewImage) {
                        if (mItemClickListener != null) {
                            int p = holder.getAdapterPosition();
                            mItemClickListener.OnItemClick(image, useCamera ? p - 1 : p);
                        }
                    } else {
                        checkedImage(holder, image);
                    }
                }
            });
        } else if (getItemViewType(position) == TYPE_CAMERA) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.OnCameraClick();
                    }
                }
            });
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.size() > 0 && "selectIndex".equals(payloads.get(0))) {
            setItemSelect(holder, mSelectImages.contains(getImage(position)), position);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (useCamera && position == 0) {
            return TYPE_CAMERA;
        } else {
            return TYPE_IMAGE;
        }
    }

    private void checkedImage(ViewHolder holder, Image image) {
        if (mSelectImages.contains(image)) {
            //如果图片已经选中，就取消选中
            unSelectImage(image);
            setItemSelect(holder, false, holder.getLayoutPosition());
        } else if (isSingle) {
            //如果是单选，就先清空已经选中的图片，再选中当前图片
            clearImageSelect();
            selectImage(image);
            setItemSelect(holder, true, holder.getLayoutPosition());
        } else if (mMaxCount <= 0 || mSelectImages.size() < mMaxCount) {
            //如果不限制图片的选中数量，或者图片的选中数量
            // 还没有达到最大限制，就直接选中当前图片。
            selectImage(image);
            setItemSelect(holder, true, holder.getLayoutPosition());
        } else {
            UiUtils.showWxToast(holder.itemView.getContext(), String.format(Locale.CHINA, "你最多只能选择%d张图片", mMaxCount));
        }
    }

    /**
     * 选中图片
     *
     * @param image
     */
    private void selectImage(Image image) {
        mSelectImages.add(image);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(image, true, mSelectImages.size());
        }
    }

    /**
     * 取消选中图片
     *
     * @param image
     */
    private void unSelectImage(Image image) {
        mSelectImages.remove(image);
        if (!isSingle) {
            notifyItemRangeChanged(useCamera ? 1 : 0, getImageCount(), "selectIndex");
        }
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(image, false, mSelectImages.size());
        }
    }


    @Override
    public int getItemCount() {
        return useCamera ? getImageCount() + 1 : getImageCount();
    }

    private int getImageCount() {
        return mImages == null ? 0 : mImages.size();
    }

    public ArrayList<Image> getData() {
        return mImages;
    }

    public void refresh(ArrayList<Image> data, boolean useCamera) {
        mImages = data;
        this.useCamera = useCamera;
        notifyDataSetChanged();
    }

    private Image getImage(int position) {
        return mImages.get(useCamera ? position - 1 : position);
    }

    public Image getFirstVisibleImage(int firstVisibleItem) {
        if (mImages != null && !mImages.isEmpty()) {
            if (useCamera) {
                return mImages.get(firstVisibleItem > 0 ? firstVisibleItem - 1 : 0);
            } else {
                return mImages.get(firstVisibleItem < 0 ? 0 : firstVisibleItem);
            }
        }
        return null;
    }

    /**
     * 设置图片选中和未选中的效果
     */
    private void setItemSelect(ViewHolder holder, boolean isSelect, int position) {
        if (isSelect) {
            if (isSingle) {
                holder.tvSelect.setBackgroundResource(R.drawable.is_icon_image_select);
                holder.tvSelect.setText("");
            } else {
                holder.tvSelect.setBackgroundResource(R.drawable.is_drawable_image_select);
                holder.tvSelect.setText(String.valueOf(mSelectImages.indexOf(getImage(position))+1));
            }
            holder.ivMasking.setAlpha(0.5f);
        } else {
            holder.tvSelect.setBackgroundResource(R.drawable.is_drawable_unselected);
            holder.tvSelect.setText("");
            holder.ivMasking.setAlpha(0.2f);
        }
    }

    private void clearImageSelect() {
        if (mImages != null && mSelectImages.size() == 1) {
            int index = mImages.indexOf(mSelectImages.get(0));
            mSelectImages.clear();
            if (index != -1) {
                notifyItemChanged(useCamera ? index + 1 : index);
            }
        }
    }

    public void setSelectedImages(ArrayList<String> selected) {
        if (mImages != null && selected != null) {
            for (String path : selected) {
                if (isFull()) {
                    return;
                }
                for (Image image : mImages) {
                    if (path.equals(image.getPath())) {
                        if (!mSelectImages.contains(image)) {
                            mSelectImages.add(image);
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }


    private boolean isFull() {
        if (isSingle && mSelectImages.size() == 1) {
            return true;
        } else if (mMaxCount > 0 && mSelectImages.size() == mMaxCount) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<Image> getSelectImages() {
        return mSelectImages;
    }

    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
        FrameLayout btnSelect;
        TextView tvSelect;
        ImageView ivMasking;
        ImageView ivGif;
        ImageView ivCamera;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            btnSelect = itemView.findViewById(R.id.btn_select);
            tvSelect = itemView.findViewById(R.id.tv_select);
            ivMasking = itemView.findViewById(R.id.iv_masking);
            ivGif = itemView.findViewById(R.id.iv_gif);

            ivCamera = itemView.findViewById(R.id.iv_camera);
        }
    }

    public interface OnImageSelectListener {
        void OnImageSelect(Image image, boolean isSelect, int selectCount);
    }

    public interface OnItemClickListener {
        void OnItemClick(Image image, int position);

        void OnCameraClick();
    }
}

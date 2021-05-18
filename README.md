# ImageSelector
Android图片选择器，仿微信的图片选择器的样式和效果。支持图片的单选、限数量的多选和不限数量的多选。支持图片预览和图片文件夹的切换。支持图片单选并剪裁。

先上效果图：

![相册](https://github.com/zachary/ImageSelector/blob/master/%E6%95%88%E6%9E%9C%E5%9B%BE/%E7%9B%B8%E5%86%8C.jpg)  ![文件夹](https://github.com/zachary/ImageSelector/blob/master/%E6%95%88%E6%9E%9C%E5%9B%BE/%E6%96%87%E4%BB%B6%E5%A4%B9.jpg)  ![预览](https://github.com/zachary/ImageSelector/blob/master/%E6%95%88%E6%9E%9C%E5%9B%BE/%E9%A2%84%E8%A7%88.jpg)

**1、引入依赖**

在Project的build.gradle在添加以下代码

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
在Module的build.gradle在添加以下代码

```
	implementation 'com.github.zachary:ImageSelector:1.7.0'
```
ImageSelector从1.5.0版本开始使用了Glide 4.x的版本，由于Glide 3.x版本和4.x版本在使用上有所差异，如果你的项目使用了Glide 3.x版本，而又不想升级到4.x,那么你也可以使用ImageSelector:1.4.0版本，它和新的版本在使用和功能上都会有所差异。[ImageSelector 1.4.0](https://github.com/zachary/ImageSelector/blob/master/README1.4.0.md)

**2、配置AndroidManifest.xml**

***注意：*** 1.7.0版本后，不需要再配置FileProvider，ImageSelector内部已经配置了。
```xml
//储存卡的读写权限
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
//调用相机权限
<uses-permission android:name="android.permission.CAMERA" />

//图片选择Activity
<activity android:name="zachary.imageselector.ImageSelectorActivity"
	//去掉Activity的ActionBar。
	//使用者可以根据自己的项目去配置，不一定要这样写，只要不Activity的ActionBar去掉就可以了。
    android:theme="@style/Theme.AppCompat.Light.NoActionBar"
    //横竖屏切换处理。
    //如果要支持横竖屏切换，一定要加上这句，否则在切换横竖屏的时候会发生异常。
    android:configChanges="orientation|keyboardHidden|screenSize"/>
    
//图片预览Activity
<activity android:name="zachary.imageselector.PreviewActivity"
    android:theme="@style/Theme.AppCompat.Light.NoActionBar"
    android:configChanges="orientation|keyboardHidden|screenSize"/>

//图片剪切Activity
<activity
    android:name="zachary.imageselector.ClipImageActivity"
    android:theme="@style/Theme.AppCompat.Light.NoActionBar" />

<!-- Android 7.0 文件共享配置，1.7.0之前必须配置，1.7.0后不需要 -->
<provider
    android:name="android.support.v4.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```
在res/xml文件夹下创建file_paths.xml文件(名字可以自己定义)

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>

    <!-- 这个是保存拍照图片的路径,必须配置。 -->
    <external-path
        name="images"
        path="Pictures" />
</paths>
```

**3、调起图片选择器**

ImageSelector支持图片的单选、限数量的多选和不限数量的多选。还可以设置是否使用相机、是否剪切图片等配置。ImageSelector提供了统一的调起相册的方法。
```java
 //单选
 ImageSelector.builder()
        .useCamera(true) // 设置是否使用拍照
        .setSingle(true)  //设置是否单选
	.setViewImage(true) //是否点击放大图片查看,，默认为true
        .start(this, REQUEST_CODE); // 打开相册

//限数量的多选(比喻最多9张)
ImageSelector.builder()
        .useCamera(true) // 设置是否使用拍照
        .setSingle(false)  //设置是否单选
        .setMaxSelectCount(9) // 图片的最大选择数量，小于等于0时，不限数量。
	.setSelected(selected) // 把已选的图片传入默认选中。
	.setViewImage(true) //是否点击放大图片查看,，默认为true
        .start(this, REQUEST_CODE); // 打开相册

//不限数量的多选
ImageSelector.builder()
        .useCamera(true) // 设置是否使用拍照
        .setSingle(false)  //设置是否单选
        .setMaxSelectCount(0) // 图片的最大选择数量，小于等于0时，不限数量。
	.setSelected(selected) // 把已选的图片传入默认选中。
	.setViewImage(true) //是否点击放大图片查看,，默认为true
        .start(this, REQUEST_CODE); // 打开相册

//单选并剪裁
ImageSelector.builder()
       .useCamera(true) // 设置是否使用拍照
       .setCrop(true)  // 设置是否使用图片剪切功能。
       .setSingle(true)  //设置是否单选
       .setViewImage(true) //是否点击放大图片查看,，默认为true
       .start(this, REQUEST_CODE); // 打开相册
```
REQUEST_CODE就是调用者自己定义的启动Activity时的requestCode，这个相信大家都能明白。selected可以在再次打开选择器时，把原来已经选择过的图片传入，使这些图片默认为选中状态。

**4、接收选择器返回的数据**

在Activity的onActivityResult方法中接收选择器返回的数据。
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && data != null) {
	    //获取选择器返回的数据
            ArrayList<String> images = data.getStringArrayListExtra(
            ImageSelectorUtils.SELECT_RESULT);
	    
	    	/**
     		* 是否是来自于相机拍照的图片，
     		* 只有本次调用相机拍出来的照片，返回时才为true。
     		* 当为true时，图片返回的结果有且只有一张图片。
		*/
	    boolean isCameraImage = data.getBooleanExtra(ImageSelector.IS_CAMERA_IMAGE, false);
        }
    }
```
ImageSelectorUtils.SELECT_RESULT是接收数据的key。数据是以ArrayList的字符串数组返回的，就算是单选，返回的也是ArrayList数组，只不过这时候ArrayList只有一条数据而已。ArrayList里面的数据就是选中的图片的文件路径。

**适配android 10**

兼容android 10的手机请使用1.7.0版本。

由于android 10不允许应用直接访问外部文件，所以在android 10及以上的手机，ImageSelect返回的图片链接可能无法直接加载,因为ImageSelector返回的是图片在手机里的地址。但是可以通过uri进行加载，ImageSelector内部提供了一些方法可以供外部使用，用于适配android 10。

如何在Android 10加载手机本地图片，请看我的[这篇博客](https://juejin.im/post/5d80ef726fb9a06aeb10f223)。
```
//是否是android 10及以上
VersionUtils.isAndroidQ();

// android 10可以通过图片uri加载手机本地图片。

//图片链接转uri
Uri uri = UriUtils.getImageContentUri(Context context, String path);

//通过uri加载图片
 Glide.with(mContext).load(uri).into(ivImage);
 ivImage.setImageURI(uri);
 // 或者
Bitmap bitmap = ImageUtil.getBitmapFromUri(Context context, Uri uri);
```

***注意：*** 剪切返回的图片的图片链接是放在应用的私有目录的，所以剪切返回的图片可以直接用path加载，不需要转成uri再加载。ImageSelector提供了判断图片链接是否是剪切的图片的方法。
```
// 是否是剪切返回的图片
ImageUtil.isCutImage(mContext, path);
```

想要了解ImageSelector的实现思路和核心代码的同学请看这里：[Android 实现一个仿微信的图片选择器](https://juejin.im/post/5919086244d904006c692abb)

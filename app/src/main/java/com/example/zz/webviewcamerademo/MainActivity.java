package com.example.zz.webviewcamerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.example.zz.webviewcamerademo.utils.RxPermissionUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 类描述：webview点击调用摄像机
 * 创建时间：2018/1/4 15:23
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private WebView webview;
    private static final int REQUEST_CAMERA = 0x000000;
    private static final int REQUEST_VIDEO = 0x000001;
    private ValueCallback<Uri> mFileCallBack;
    private ValueCallback<Uri[]> mHighFileCallBack;
    private String mPhotoPath = "";
    private Uri mPhotoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //权限申请
        boolean permisssionAllow = RxPermissionUtil.getInstance().PermissionRequest(this,
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        initView();
    }


    /**
     * 初始化UI
     */
    private void initView() {
        webview = ((WebView) findViewById(R.id.webview_id));
        webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webview.setScrollbarFadingEnabled(false);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webview.setWebContentsDebuggingEnabled(true);                      //运行版本大于API 19 的可以在chrome等调试界面
        }
        //使用的是本地的html,如若网络html，自行配置WebViewClient
        webview.setWebChromeClient(new MyWebChromeClinet());
        webview.loadUrl("file:///android_asset/test.html");
    }


    /**
     * 核心代码(现在Android版本基本都>4.0所以在这里只考虑4.0以上的方案)
     */
    class MyWebChromeClinet extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return super.onJsAlert(view,url,message,result);
        }

        //android  4.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            if (mFileCallBack != null) {
                mFileCallBack.onReceiveValue(null);
            }
            mFileCallBack = uploadMsg;
            //判断是图片还是video
            if ("image/*".equals(acceptType)) {
                intoCamera();
            } else if ("video/*".equals(acceptType)) {
                intoVideo();
            }

        }

        //android 5.0+
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (mHighFileCallBack != null) {
                mHighFileCallBack.onReceiveValue(null);
            }
            mHighFileCallBack = filePathCallback;
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if ("image/*".equals(acceptTypes[0])) {
                intoCamera();
            } else if ("video/*".equals(acceptTypes[0])) {
                intoVideo();
            }
            return true;
        }
    }

    public void intoVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_VIDEO);
        }
    }


    /**
     * 跳转到相机功能
     */
    public void intoCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile == null) {
                return;
            }
            //TODO   图片格式按照如下格式
            mPhotoPath = "file:" + photoFile.getAbsolutePath();                                 //图片的处理
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                //Android 7.0以上需要特殊处理
                mPhotoURI = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoURI);
            }
            startActivityForResult(intent, REQUEST_CAMERA);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAMERA && requestCode != REQUEST_VIDEO) {
            return;
        }
        Uri mUri = null;                                         //5.0以下
        Uri[] mUris = null;
        switch (requestCode) {
            case REQUEST_CAMERA:
                //图片的操作
                if (data == null) {
                    //拿不到data的值用本地文件进行上传
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        //Android 7.0以上文件
                        mUri = mPhotoURI;
                        mUris = new Uri[]{mUri};
                    } else {
                        if (mPhotoPath != null) {
                            mUri = Uri.parse(mPhotoPath);
                            mUris = new Uri[]{mUri};
                        }
                    }
                } else {
                    Uri getUri = data.getData();
                    if (getUri != null) {
                        mUri = getUri;
                        mUris = new Uri[]{getUri};                                                  //5.0+Uri格式化
                    }
                }

                break;
            case REQUEST_VIDEO:
                //video的操作
                mUri = data.getData();
                mUris = new Uri[]{mUri};
                break;
            default:
                break;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //4.0+
            mFileCallBack.onReceiveValue(mUri);
            mFileCallBack = null;
            return;
        } else {
            mHighFileCallBack.onReceiveValue(mUris);
            mHighFileCallBack = null;
            return;
        }
    }


    /**
     * 创建一个图片文件
     */
    public File createImageFile() {
        File imageFile = null;
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
        String imageFileName = timeStamp + "-";
        File fileDirection = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!fileDirection.exists()) {
            fileDirection.mkdirs();
        }
        try {

            imageFile = File.createTempFile(imageFileName, ".jpg", fileDirection);
        } catch (IOException e) {
            Log.d(TAG, "create  image file fail");
        }
        mPhotoPath = imageFile.getAbsolutePath();
        Log.d(TAG, mPhotoPath);
        return imageFile;
    }
}

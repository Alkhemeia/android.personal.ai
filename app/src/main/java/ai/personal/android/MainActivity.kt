package ai.personal.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.webkit.WebSettings.RenderPriority
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import java.io.File
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec


class MainActivity : Activity() {
    private lateinit var mContext: Context
    internal var mLoaded = false
    internal var doubleBackToExitPressedOnce = false

    internal var URL = "https://app.personal.ai/login"
    internal val USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Pixel a5 Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/114.0.5735.130 Mobile Safari/537.36"
    internal var TAG = MainActivity::class.qualifiedName;

    private lateinit var btnTryAgain: Button
    private lateinit var mWebView: WebView
    private lateinit var prgs: ProgressBar
    private var viewSplash: View? = null
    private lateinit var layoutSplash: RelativeLayout
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var layoutNoInternet: RelativeLayout

    //for attach files
    private var mCameraPhotoPath: String? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        mContext = this
        mWebView = findViewById<View>(R.id.webview) as WebView
        prgs = findViewById<View>(R.id.progressBar) as ProgressBar
        btnTryAgain = findViewById<View>(R.id.btn_try_again) as Button
        viewSplash = findViewById(R.id.view_splash) as View
        layoutWebview = findViewById<View>(R.id.layout_webview) as RelativeLayout
        layoutNoInternet = findViewById<View>(R.id.layout_no_internet) as RelativeLayout
        /** Layout of Splash screen View  */
        layoutSplash = findViewById<View>(R.id.layout_splash) as RelativeLayout

        //request for show website
        requestForWebview()

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            prgs.visibility = View.VISIBLE
            layoutSplash.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            requestForWebview()
        }
    }


    private fun requestForWebview() {

        if (!mLoaded) {
            requestWebView()
            Handler().postDelayed({
                prgs.visibility = View.VISIBLE
                mWebView.visibility = View.VISIBLE
            }, 3000)

        } else {
            mWebView.visibility = View.VISIBLE
            prgs.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.GONE
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestWebView() {
        /** Layout of webview screen View  */
        if (internetCheck(mContext)) {
            mWebView.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            mWebView.loadUrl(URL)
        } else {
            prgs.visibility = View.GONE
            mWebView.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.VISIBLE

            return
        }

        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.settings.javaScriptEnabled = true
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        mWebView.settings.userAgentString = USER_AGENT;
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.databaseEnabled = true

        // this force use chromeWebClient
        mWebView.settings.setSupportMultipleWindows(false)
        mWebView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {

                Log.d(TAG, "URL: " + url!!)
                if (internetCheck(mContext)) {
                    view.loadUrl(url);
                } else {
                    prgs.visibility = View.GONE
                    mWebView.visibility = View.GONE
                    layoutSplash.visibility = View.GONE
                    layoutNoInternet.visibility = View.VISIBLE
                }

                mWebView.addJavascriptInterface(JavaScriptInterface(mContext), "AndroidInterface")
                mWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        mWebView.loadUrl("javascript:(function() { " +
                                "Notification = function(title, options) { " +
                                "   window.AndroidInterface.showNotification(title, options.body);" +
                                "}" +
                                "})()")
                    }
                }

                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (prgs.visibility == View.GONE) {
                    prgs.visibility = View.VISIBLE
                }
            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                mLoaded = true
                if (prgs.visibility == View.VISIBLE)
                    prgs.visibility = View.GONE

                // check if layoutSplash is still there, get it away!
                Handler().postDelayed({
                    layoutSplash.visibility = View.GONE
                    //viewSplash.getBackground().setAlpha(255);
                }, 2000)
            }
        }

        //file attach request
        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                    webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(this@MainActivity.packageManager) != null) {
                    // Create the File where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex)
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile))
                    } else {
                        takePictureIntent = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent?>
                if (takePictureIntent != null) {
                    intentArray = arrayOf(takePictureIntent)
                } else {
                    intentArray = arrayOfNulls(0)
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

                return true
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }

        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        return
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }

        if (doubleBackToExitPressedOnce) {
            return super.onKeyDown(keyCode, event)
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        return true
    }

    companion object {
        val INPUT_FILE_REQUEST_CODE = 1
        val EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION"

        //for security
        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        fun generateKey(): SecretKey {
            val random = SecureRandom()
            val key = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0)
            //random.nextBytes(key);
            return SecretKeySpec(key, "AES")
        }

        /*@Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidParameterSpecException::class, IllegalBlockSizeException::class, BadPaddingException::class, UnsupportedEncodingException::class)
        fun encryptMsg(message: String, secret: SecretKey): ByteArray {
            var cipher: Cipher? = null
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher!!.init(Cipher.ENCRYPT_MODE, secret)
            return cipher.doFinal(message.toByteArray(charset("UTF-8")))
        }

        @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class, InvalidParameterSpecException::class, InvalidAlgorithmParameterException::class, InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class, UnsupportedEncodingException::class)
        fun decryptMsg(cipherText: ByteArray, secret: SecretKey): String {
            var cipher: Cipher? = null
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher!!.init(Cipher.DECRYPT_MODE, secret)
            return String(cipher.doFinal(cipherText), charset("UTF-8"))
        }*/

        fun internetCheck(context: Context): Boolean {
            var available = false
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (connectivity != null) {
                val networkInfo = connectivity.allNetworkInfo
                if (networkInfo != null) {
                    for (i in networkInfo.indices) {
                        if (networkInfo[i].state == NetworkInfo.State.CONNECTED) {
                            available = true
                            break
                        }
                    }
                }
            }
            return available
        }
    }
}
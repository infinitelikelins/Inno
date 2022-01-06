package com.bearya.mobile.inno.activity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MenuItem
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.bearya.mobile.inno.R
import com.bearya.mobile.inno.databinding.ActivityScannerCodeBinding
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.tamsiree.rxfeature.scaner.CameraManager
import com.tamsiree.rxfeature.scaner.OnRxScanerListener
import com.tamsiree.rxfeature.scaner.decoding.InactivityTimer
import com.tamsiree.rxkit.RxAnimationTool
import com.tamsiree.rxkit.RxBeepTool
import com.tamsiree.rxkit.TLog
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class ScannerCodeActivity : AppCompatActivity() {

    private var inactivityTimer: InactivityTimer? = null

    /**
     * 扫描处理
     */
    private var handler: CaptureActivityHandler? = null

    /**
     * 扫描边界的宽度
     */
    private var mCropWidth = 0

    /**
     * 扫描边界的高度
     */
    private var mCropHeight = 0

    /**
     * 是否有预览
     */
    private var hasSurface = false

    /**
     * 扫描成功后是否震动
     */
    private val vibrate = true

    private var mScannerListener: OnRxScanerListener? = object : OnRxScanerListener {
        override fun onFail(type: String?, message: String?) {

        }

        override fun onSuccess(type: String?, result: Result?) {
            ImageUploadActivity.start(this@ScannerCodeActivity, result?.text)
            finish()
        }
    }

    private lateinit var bindView: ActivityScannerCodeBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView = ActivityScannerCodeBinding.inflate(layoutInflater)
        setContentView(bindView.root)
        //界面控件初始化
        initDecode()
        //扫描动画初始化
        initScannerAnimation()
        //初始化 CameraManager
        CameraManager.init(this)
        hasSurface = false
        inactivityTimer = InactivityTimer(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "扫描地址二维码"
    }

    private fun initDecode() {
        multiFormatReader = MultiFormatReader()

        // 解码的参数
        val hints = Hashtable<DecodeHintType, Any?>(2)
        // 可以解析的编码类型
        var decodeFormats = Vector<BarcodeFormat?>()
        if (decodeFormats.isEmpty()) {
            decodeFormats = Vector()
            val PRODUCT_FORMATS = Vector<BarcodeFormat?>(5)
            PRODUCT_FORMATS.add(BarcodeFormat.UPC_A)
            PRODUCT_FORMATS.add(BarcodeFormat.UPC_E)
            PRODUCT_FORMATS.add(BarcodeFormat.EAN_13)
            PRODUCT_FORMATS.add(BarcodeFormat.EAN_8)
            // PRODUCT_FORMATS.add(BarcodeFormat.RSS14);
            val ONE_D_FORMATS = Vector<BarcodeFormat?>(PRODUCT_FORMATS.size + 4)
            ONE_D_FORMATS.addAll(PRODUCT_FORMATS)
            ONE_D_FORMATS.add(BarcodeFormat.CODE_39)
            ONE_D_FORMATS.add(BarcodeFormat.CODE_93)
            ONE_D_FORMATS.add(BarcodeFormat.CODE_128)
            ONE_D_FORMATS.add(BarcodeFormat.ITF)
            val QR_CODE_FORMATS = Vector<BarcodeFormat?>(1)
            QR_CODE_FORMATS.add(BarcodeFormat.QR_CODE)
            val DATA_MATRIX_FORMATS = Vector<BarcodeFormat?>(1)
            DATA_MATRIX_FORMATS.add(BarcodeFormat.DATA_MATRIX)

            // 这里设置可扫描的类型，我这里选择了都支持
            decodeFormats.addAll(ONE_D_FORMATS)
            decodeFormats.addAll(QR_CODE_FORMATS)
            decodeFormats.addAll(DATA_MATRIX_FORMATS)
        }
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        multiFormatReader?.setHints(hints)
    }

    override fun onResume() {
        super.onResume()
        val surfaceHolder = bindView.capturePreview.holder
        if (hasSurface) {
            //Camera初始化
            initCamera(surfaceHolder)
        } else {
            surfaceHolder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (!hasSurface) {
                        hasSurface = true
                        initCamera(holder)
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    hasSurface = false
                }
            })
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }

    override fun onPause() {
        super.onPause()
        if (handler != null) {
            handler?.quitSynchronously()
            handler?.removeCallbacksAndMessages(null)
            handler = null
        }
        CameraManager.get().closeDriver()
    }

    override fun onDestroy() {
        inactivityTimer?.shutdown()
        mScannerListener = null
        super.onDestroy()
    }

    private fun initScannerAnimation() {
        RxAnimationTool.ScaleUpDowm(bindView.captureScanLine)
    }

    var cropWidth: Int
        get() = mCropWidth
        set(cropWidth) {
            mCropWidth = cropWidth
            CameraManager.FRAME_WIDTH = mCropWidth
        }

    var cropHeight: Int
        get() = mCropHeight
        set(cropHeight) {
            mCropHeight = cropHeight
            CameraManager.FRAME_HEIGHT = mCropHeight
        }

    private fun initCamera(surfaceHolder: SurfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder)
            val point = CameraManager.get().cameraResolution
            val width = AtomicInteger(point.y)
            val height = AtomicInteger(point.x)
            val cropWidth1 =
                bindView.captureCropLayout.width * width.get() / bindView.captureContainer.width
            val cropHeight1 =
                bindView.captureCropLayout.height * height.get() / bindView.captureContainer.height
            cropWidth = cropWidth1
            cropHeight = cropHeight1
        } catch (ioe: IOException) {
            return
        } catch (ioe: RuntimeException) {
            return
        }
        if (handler == null) {
            handler = CaptureActivityHandler()
        }
    }

    fun handleDecode(result: Result) {
        inactivityTimer?.onActivity()
        //扫描成功之后的振动与声音提示
        RxBeepTool.playBeep(this, vibrate)
        val result1 = result.text
        TLog.v("二维码/条形码 扫描结果", result1)
        mScannerListener?.onSuccess("From to Camera", result)
    }

    //==============================================================================================解析结果 及 后续处理 end
    @SuppressLint("HandlerLeak")
    internal inner class CaptureActivityHandler : Handler() {
        var decodeThread: DecodeThread? = null
        private var state: State
        override fun handleMessage(message: Message) {
            if (message.what == R.id.auto_focus) {
                if (state == State.PREVIEW) {
                    CameraManager.get().requestAutoFocus(this, R.id.auto_focus)
                }
            } else if (message.what == R.id.restart_preview) {
                restartPreviewAndDecode()
            } else if (message.what == R.id.decode_succeeded) {
                state = State.SUCCESS
                handleDecode(message.obj as Result) // 解析成功，回调
            } else if (message.what == R.id.decode_failed) {
                state = State.PREVIEW
                CameraManager.get().requestPreviewFrame(decodeThread!!.getHandler(), R.id.decode)
            }
        }

        fun quitSynchronously() {
            state = State.DONE
            decodeThread!!.interrupt()
            CameraManager.get().stopPreview()
            val quit = Message.obtain(decodeThread!!.getHandler(), R.id.quit)
            quit.sendToTarget()
            try {
                decodeThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                removeMessages(R.id.decode_succeeded)
                removeMessages(R.id.decode_failed)
                removeMessages(R.id.decode)
                removeMessages(R.id.auto_focus)
            }
        }

        private fun restartPreviewAndDecode() {
            if (state == State.SUCCESS) {
                state = State.PREVIEW
                CameraManager.get().requestPreviewFrame(decodeThread?.getHandler(), R.id.decode)
                CameraManager.get().requestAutoFocus(this, R.id.auto_focus)
            }
        }

        init {
            decodeThread = DecodeThread()
            decodeThread?.start()
            state = State.SUCCESS
            CameraManager.get().startPreview()
            restartPreviewAndDecode()
        }
    }

    internal inner class DecodeThread : Thread() {
        private val handlerInitLatch: CountDownLatch = CountDownLatch(1)
        private var handler: Handler? = null
        fun getHandler(): Handler? {
            try {
                handlerInitLatch.await()
            } catch (ie: InterruptedException) {
                // continue?
            }
            return handler
        }

        override fun run() {
            Looper.prepare()
            handler = DecodeHandler()
            handlerInitLatch.countDown()
            Looper.loop()
        }

    }

    @SuppressLint("HandlerLeak")
    internal inner class DecodeHandler : Handler() {
        override fun handleMessage(message: Message) {
            if (message.what == R.id.decode) {
                decode(message.obj as ByteArray, message.arg1, message.arg2)
            } else if (message.what == R.id.quit) {
                Looper.myLooper()?.quit()
            }
        }
    }

    private var multiFormatReader: MultiFormatReader? = null
    private fun decode(data: ByteArray, width: Int, height: Int) {
        var width1 = width
        var height1 = height
        val start = System.currentTimeMillis()
        var rawResult: Result? = null

        //modify here
        val rotatedData = ByteArray(data.size)
        for (y in 0 until height1) {
            for (x in 0 until width1) {
                rotatedData[x * height1 + height1 - y - 1] = data[x + y * width1]
            }
        }
        // Here we are swapping, that's the difference to #11
        val tmp = width1
        width1 = height1
        height1 = tmp
        val source = CameraManager.get().buildLuminanceSource(rotatedData, width1, height1)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            if (bitmap.width > 0 && bitmap.height > 0) {
                rawResult = multiFormatReader?.decodeWithState(bitmap)
            } else {
                multiFormatReader?.reset()
            }
        } catch (e: ReaderException) {
            // continue
        } finally {
            multiFormatReader?.reset()
        }
        if (rawResult != null) {
            val end = System.currentTimeMillis()
            TLog.d(
                ContentValues.TAG, """Found barcode (${end - start} ms):
$rawResult"""
            )
            val message = Message.obtain(handler, R.id.decode_succeeded, rawResult)
            val bundle = Bundle()
            bundle.putParcelable("barcode_bitmap", source.renderCroppedGreyscaleBitmap())
            message.data = bundle
            //TLog.d(TAG, "Sending decode succeeded message...");
            message.sendToTarget()
        } else {
            val message = Message.obtain(handler, R.id.decode_failed)
            message.sendToTarget()
        }
    }

    private enum class State {
        //预览
        PREVIEW,  //成功
        SUCCESS,  //完成
        DONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
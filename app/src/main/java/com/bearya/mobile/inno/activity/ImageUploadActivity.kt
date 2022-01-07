package com.bearya.mobile.inno.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bearya.mobile.inno.R
import com.bearya.mobile.inno.adapter.ImageAdapter
import com.bearya.mobile.inno.databinding.ActivityImageUploadBinding
import com.bearya.mobile.inno.model.SocketViewModel
import com.bearya.mobile.inno.status.SocketStatus
import com.bearya.mobile.inno.tools.GlideEngine
import com.kaopiz.kprogresshud.KProgressHUD
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.thecode.aestheticdialogs.*

class ImageUploadActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context, address: String?) {
            if (address != null && address.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) {
                context.startActivity(
                    Intent(
                        context,
                        ImageUploadActivity::class.java
                    ).putExtra("address", address)
                )
            } else {
                Toast.makeText(context, "二维码异常", Toast.LENGTH_LONG).show()
            }
        }
    }

    private lateinit var bindView: ActivityImageUploadBinding
    private val socketViewModel: SocketViewModel by viewModels()
    private val imagesAdapter: ImageAdapter by lazy { ImageAdapter() }
    private var kProgressHUD: KProgressHUD? = null
    private var canUploadImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView = ActivityImageUploadBinding.inflate(layoutInflater)
        setContentView(bindView.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "同步图库"

        bindView.images.layoutManager =
            GridLayoutManager(this, 3, LinearLayoutManager.VERTICAL, false)

        imagesAdapter.setOnItemClickListener { _, _, position ->
            run {
                if (imagesAdapter.data.size - 1 == position) {
                    openLib()
                } else {
                    AlertDialog.Builder(this).setTitle("上传取消").setMessage("确定要取消上传该张图片吗?")
                        .setPositiveButton("确定") { _, _ ->
                            imagesAdapter.removeAt(position)
                        }.setNegativeButton("取消") { _, _ ->

                        }.show()
                }
            }
        }

        bindView.images.adapter = imagesAdapter
        imagesAdapter.setEmptyView(R.layout.empty_message)
        imagesAdapter.addData(LocalMedia())

        socketViewModel.message.observe(this) {
            bindView.message.text = it
            kProgressHUD?.setLabel(it)
        }

        socketViewModel.socketStatus.observe(this) { socketStatus ->
            when (socketStatus) {
                SocketStatus.SOCKET_CONNECTION_AIDL,
                SocketStatus.SOCKET_CONNECTION_FAIL,
                SocketStatus.SOCKET_CONNECTION_DISCONNECT -> canUploadImage = false
                SocketStatus.SOCKET_CONNECTION_SUCCESS -> canUploadImage = true
                SocketStatus.SOCKET_SEND_START -> showHUD()
                SocketStatus.SOCKET_SEND_COMPLETED -> hideHUD()
                else -> {}
            }
        }

        socketViewModel.initSocket(intent?.getStringExtra("address") ?: "")
    }

    private fun openLib() {
        PictureSelector.create(this)
            .openGallery(PictureMimeType.ofImage())
            .imageEngine(GlideEngine.createGlideEngine())
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: MutableList<LocalMedia>?) {
                    if (result != null) {
                        imagesAdapter.addData(imagesAdapter.data.size - 1, result)
                    }
                }

                override fun onCancel() {

                }
            })
    }

    private fun showHUD() {
        if (kProgressHUD == null) {
            kProgressHUD = KProgressHUD.create(this, KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("文件正在同步中...")
                .setDimAmount(0.5f)
                .setAutoDismiss(false)
                .setCancellable(false)
                .show()
        }
    }

    private fun hideHUD() {
        if (kProgressHUD != null) {
            kProgressHUD?.dismiss()
        }
        kProgressHUD = null
        imagesAdapter.setNewInstance(mutableListOf(LocalMedia()))
        AestheticDialog.Builder(this, DialogStyle.EMOTION, DialogType.SUCCESS)
            .setTitle("文件同步完成")
            .setMessage("同步文件可在小贝端查看")
            .setCancelable(true)
            .setAnimation(DialogAnimation.SHRINK)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_upload_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        } else if (item.itemId == R.id.upload_gallery) {
            if (canUploadImage && imagesAdapter.data.size > 1) {
                val files = imagesAdapter.data.subList(0, imagesAdapter.data.size - 1)
                socketViewModel.sendFiles(files)
            } else if (canUploadImage) {
                Toast.makeText(this, "你还没有选择上传的图片", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "局域网还没有联接成功", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
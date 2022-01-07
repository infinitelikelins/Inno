package com.bearya.mobile.inno.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bearya.mobile.inno.R
import com.bearya.mobile.inno.databinding.ActivityMainBinding
import com.vmadalin.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), View.OnClickListener,
    EasyPermissions.PermissionCallbacks {

    private val permissionCameraRequestCode = 0x16

    private lateinit var bindView: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindView.root)
        supportActionBar?.title = "图库同步工具"
        bindView.scanner.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (view?.id == R.id.scanner) {
            if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
                startActivity(Intent(this, ScannerCodeActivity::class.java))
            } else {
                EasyPermissions.requestPermissions(
                    this,
                    "扫一扫需要使用摄像头的使用权限",
                    permissionCameraRequestCode,
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (requestCode == permissionCameraRequestCode)
            Toast.makeText(this, "您拒绝使用摄像头扫描二维码", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == permissionCameraRequestCode)
            startActivity(Intent(this, ScannerCodeActivity::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}
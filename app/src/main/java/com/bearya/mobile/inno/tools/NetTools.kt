package com.bearya.mobile.inno.tools

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.tamsiree.rxkit.RxNetTool

fun getIPAddress(context: Context): String? =
    if (RxNetTool.isConnected(context) && RxNetTool.isWifiConnected(context)) {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager?)
            ?.connectionInfo?.ipAddress?.let { Formatter.formatIpAddress(it) }
    } else null

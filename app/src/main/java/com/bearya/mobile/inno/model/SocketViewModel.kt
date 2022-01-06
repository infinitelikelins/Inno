package com.bearya.mobile.inno.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bearya.mobile.inno.ext.setData
import com.bearya.mobile.inno.status.SocketStatus
import com.luck.picture.lib.entity.LocalMedia
import com.orhanobut.logger.Logger
import com.xuhao.didi.core.iocore.interfaces.ISendable
import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.socket.client.sdk.OkSocket
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SocketViewModel : ViewModel() {

    private var mConnectionManager: IConnectionManager? = null
    private var socketActionAdapter: SocketActionAdapter? = null

    val message: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val socketStatus: MutableLiveData<SocketStatus> by lazy { MutableLiveData<SocketStatus>() }
    private var sendFilesList: MutableList<LocalMedia>? = null

    fun initSocket(address: String) {
        socketStatus.setData(SocketStatus.SOCKET_CONNECTION_AIDL)
        mConnectionManager = OkSocket.open(address, 7596)
        socketActionAdapter = object : SocketActionAdapter() {
            override fun onSocketConnectionSuccess(info: ConnectionInfo?, action: String?) {
                super.onSocketConnectionSuccess(info, action)
                socketStatus.setData(SocketStatus.SOCKET_CONNECTION_SUCCESS)
                logger("局域网联接成功")
                sendFiles(sendFilesList)
            }

            override fun onSocketConnectionFailed(
                info: ConnectionInfo?,
                action: String?,
                e: Exception?
            ) {
                super.onSocketConnectionFailed(info, action, e)
                socketStatus.setData(SocketStatus.SOCKET_CONNECTION_FAIL)
                logger("局域网联接失败,正在尝试重新联接")
            }

            override fun onSocketDisconnection(
                info: ConnectionInfo?,
                action: String?,
                e: Exception?
            ) {
                super.onSocketDisconnection(info, action, e)
                socketStatus.setData(SocketStatus.SOCKET_CONNECTION_DISCONNECT)
                logger("局域网联接断开,正在尝试重新联接")
            }

            override fun onSocketReadResponse(
                info: ConnectionInfo?,
                action: String?,
                data: OriginalData?
            ) {
                super.onSocketReadResponse(info, action, data)
                val response = data?.bodyBytes?.let { String(it, Charsets.UTF_8) }
                if (response == "CMD=SUCCESS") {
                    sendFilesList?.takeIf { it.size > 0 }?.also { it.removeAt(0) }
                    socketStatus.setData(SocketStatus.SOCKET_SEND_SUCCESS)
                } else if (response == "CMD=FAIL") {
                    socketStatus.setData(SocketStatus.SOCKET_SEND_FAIL)
                }
                logger("局域网消息响应 : $response ")
                sendFiles(sendFilesList)
            }

            override fun onSocketWriteResponse(
                info: ConnectionInfo?,
                action: String?,
                data: ISendable?
            ) {
                super.onSocketWriteResponse(info, action, data)
                socketStatus.setData(SocketStatus.SOCKET_SEND_COMPLETED_WAIT_RESPONSE)
                logger("发送文件输入完成，等待客户端响应")
            }
        }
        mConnectionManager?.registerReceiver(socketActionAdapter)
        mConnectionManager?.connect()
    }

    fun sendFiles(files: MutableList<LocalMedia>?) {
        if (sendFilesList == null) {
            sendFilesList = files?.subList(0, files.size)
        }
        if (sendFilesList != null && sendFilesList!!.size > 0) {
            socketStatus.setData(SocketStatus.SOCKET_SEND_START)
            logger("发送文件中，还剩余${sendFilesList!!.size}个文件未发送")
        }
        if (sendFilesList != null && sendFilesList!!.size > 0) {
            sendFilesList?.get(0)?.path?.also {
                sendFile(it)
            }
        }
        if (sendFilesList != null && sendFilesList!!.size == 0) {
            logger("文件发送完成")
            socketStatus.setData(SocketStatus.SOCKET_SEND_COMPLETED)
            sendFilesList = null
        }
    }

    private fun sendFile(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            mConnectionManager?.send(SendFileData(File(filePath)))
        }
    }

    override fun onCleared() {
        super.onCleared()
        mConnectionManager?.disconnect()
        mConnectionManager?.unRegisterReceiver(socketActionAdapter)
        socketActionAdapter = null
    }

    private fun logger(msg: String?) {
        Logger.t("Socket").d("$msg")
        message.setData(msg)
    }

    private inner class SendFileData(private val file: File) : ISendable {
        override fun parse(): ByteArray = file.readBytes().let {
            ByteBuffer.allocate(4 + it.size).apply {
                order(ByteOrder.BIG_ENDIAN)
                putInt(it.size)
                put(it)
            }.array()
        }
    }

}
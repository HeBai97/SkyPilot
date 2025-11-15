package com.brainai.skypilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brainai.skypilot.dji.DjiPipelineManager
import com.brainai.skypilot.model.ConnectionState
import com.brainai.skypilot.model.Message
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DJI SDK 互联互通 ViewModel
 */
class DjiViewModel(application: Application) : AndroidViewModel(application) {

    private val pipelineManager = DjiPipelineManager()

    // 通道 ID
    private val _channelId = MutableStateFlow(1)
    val channelId: StateFlow<Int> = _channelId.asStateFlow()

    // 传输类型
    private val _transmissionType = MutableStateFlow(TransmissionControlType.STABLE)
    val transmissionType: StateFlow<TransmissionControlType> = _transmissionType.asStateFlow()

    // 连接状态
    val connectionState: StateFlow<ConnectionState> = pipelineManager.connectionState

    // 消息列表
    val messages: StateFlow<List<Message>> = pipelineManager.messages

    // 输入的消息文本
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    /**
     * 更新通道 ID
     */
    fun updateChannelId(id: Int) {
        if (id in 1..65535) {
            _channelId.value = id
        }
    }

    /**
     * 更新传输类型
     */
    fun updateTransmissionType(type: TransmissionControlType) {
        _transmissionType.value = type
    }

    /**
     * 连接到通道
     */
    fun connect() {
        viewModelScope.launch {
            pipelineManager.connect(_channelId.value, _transmissionType.value)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            pipelineManager.disconnect()
        }
    }

    /**
     * 切换连接状态
     */
    fun toggleConnection() {
        when (connectionState.value) {
            ConnectionState.CONNECTED -> disconnect()
            ConnectionState.DISCONNECTED -> connect()
            ConnectionState.ERROR -> {
                // 错误状态下重试连接
                disconnect()
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500)
                    connect()
                }
            }
            else -> {} // 连接中不做处理
        }
    }

    /**
     * 更新消息输入
     */
    fun updateMessageInput(text: String) {
        _messageInput.value = text
    }

    /**
     * 发送消息
     */
    fun sendMessage() {
        val message = _messageInput.value.trim()
        if (message.isNotEmpty()) {
            viewModelScope.launch {
                val success = pipelineManager.sendData(message)
                if (success) {
                    _messageInput.value = ""
                }
            }
        }
    }

    /**
     * 发送指令（快捷方法）
     */
    fun sendCommand(command: String) {
        viewModelScope.launch {
            pipelineManager.sendData(command)
        }
    }

    /**
     * 清除所有消息
     */
    fun clearMessages() {
        viewModelScope.launch {
            pipelineManager.clearMessages()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}


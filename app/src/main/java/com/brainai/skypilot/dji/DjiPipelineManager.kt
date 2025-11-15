package com.brainai.skypilot.dji

import android.util.Log
import com.brainai.skypilot.model.ConnectionState
import com.brainai.skypilot.model.Message
import com.brainai.skypilot.model.MessageType
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import dji.v5.manager.mop.Pipeline
import dji.v5.manager.mop.PipelineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * DJI Pipeline 管理器
 * 负责 MSDK 与 PSDK 之间的通信
 */
class DjiPipelineManager {

    private val TAG = "DjiPipelineManager"

    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 接收到的消息
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // 最新消息（用于触发通知）
    private val _latestMessage = MutableStateFlow<Message?>(null)
    val latestMessage: StateFlow<Message?> = _latestMessage.asStateFlow()

    private var pipeline: Pipeline? = null
    private var channelId: Int = 1 // 默认通道 ID
    private var transmissionType: TransmissionControlType = TransmissionControlType.STABLE
    private var readJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    // SDK 初始化状态
    private var isSdkInitialized = false
    private var isSdkRegistered = false

    init {
        checkSdkStatus()
    }

    /**
     * 检查 SDK 状态
     */
    private fun checkSdkStatus() {
        try {
            val pipelineManager = PipelineManager.getInstance()
            pipelineManager.init()
            isSdkInitialized = true
            isSdkRegistered = true
            Log.d(TAG, "PipelineManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PipelineManager", e)
            isSdkInitialized = false
            isSdkRegistered = false
            _connectionState.value = ConnectionState.ERROR
            addSystemMessage(
                "Pipeline 管理器初始化失败: ${e.message}",
                MessageType.ERROR
            )
        }
    }

    /**
     * 连接到指定的通道
     * @param id 通道 ID
     * @param type 传输类型（可靠传输或不可靠传输）
     */
    fun connect(id: Int = 1, type: TransmissionControlType = TransmissionControlType.STABLE) {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) {
            Log.w(TAG, "Already connected or connecting")
            return
        }

        checkSdkStatus()
        if (!isSdkInitialized || !isSdkRegistered) {
            _connectionState.value = ConnectionState.ERROR
            addSystemMessage("SDK 未就绪，无法连接", MessageType.ERROR)
            return
        }

        channelId = id
        transmissionType = type

        Log.d(TAG, "Connecting to channel $channelId with type $type")
        _connectionState.value = ConnectionState.CONNECTING
        addSystemMessage("正在连接通道 $channelId...", MessageType.INFO)

        try {
            val pipelineManager = PipelineManager.getInstance()
            val error = pipelineManager.connectPipeline(
                channelId,
                PipelineDeviceType.PAYLOAD,
                transmissionType
            )

            if (error != null && error.errorCode().isNotEmpty()) {
                Log.e(TAG, "Pipeline connection failed: ${error.description()}")
                _connectionState.value = ConnectionState.ERROR
                addSystemMessage(
                    "连接失败: ${error.description()}\n提示：请确保 PSDK 端已创建通道并绑定相同的通道 ID",
                    MessageType.ERROR
                )
                return
            }

            pipeline = pipelineManager.pipelines[channelId]
            if (pipeline != null) {
                Log.d(TAG, "Pipeline connected successfully, channelId=$channelId, type=$transmissionType")
                _connectionState.value = ConnectionState.CONNECTED
                addSystemMessage(
                    "已连接到通道 $channelId (${if (transmissionType == TransmissionControlType.STABLE) "可靠传输" else "不可靠传输"})",
                    MessageType.SUCCESS
                )
                startReading()
            } else {
                Log.e(TAG, "Pipeline is null after connection")
                _connectionState.value = ConnectionState.ERROR
                addSystemMessage("连接成功但无法获取 Pipeline 对象", MessageType.ERROR)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while connecting pipeline", e)
            _connectionState.value = ConnectionState.ERROR
            addSystemMessage("连接异常: ${e.message}", MessageType.ERROR)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting pipeline")
        stopReading()

        try {
            val pipelineManager = PipelineManager.getInstance()
            val error = pipelineManager.disconnectPipeline(
                channelId,
                PipelineDeviceType.PAYLOAD,
                transmissionType
            )

            if (error != null && error.errorCode().isNotEmpty()) {
                Log.w(TAG, "Pipeline disconnect reported error: ${error.description()}")
                addSystemMessage("断开连接时出错: ${error.description()}", MessageType.WARNING)
            } else {
                addSystemMessage("已断开连接", MessageType.INFO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while disconnecting pipeline", e)
        } finally {
            _connectionState.value = ConnectionState.DISCONNECTED
            pipeline = null
        }
    }

    /**
     * 发送数据
     * 根据文档：int writeData(byte[] data)
     * 建议每次传输的数据不超过 1KB
     * 
     * @param data 要发送的数据（字符串）
     * @return 是否发送成功
     */
    fun sendData(data: String): Boolean {
        val p = pipeline ?: run {
            Log.w(TAG, "Pipeline is null, cannot send data")
            addSystemMessage("未连接，无法发送数据", MessageType.WARNING)
            return false
        }

        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected, cannot send data")
            addSystemMessage("未连接，无法发送数据", MessageType.WARNING)
            return false
        }

        return try {
            val bytes = data.toByteArray(Charsets.UTF_8)
            
            // 根据文档建议，每次传输的数据不超过 1KB
            if (bytes.size > 1024) {
                Log.w(TAG, "Data size (${bytes.size} bytes) exceeds 1KB limit")
                addSystemMessage("数据大小超过 1KB 限制，请分批发送", MessageType.WARNING)
                return false
            }
            
            // 根据文档：int writeData(byte[] data)
            // 返回实际写入的字节数
            val result = p.writeData(bytes)
            val error = result.error
            if (error == null || error.errorCode().isNullOrEmpty()) {
                Log.d(TAG, "Sent ${result.length}/${bytes.size} bytes: $data")
                // 添加发送的消息到列表
                addMessage(
                    Message(
                        title = "发送",
                        content = data,
                        type = MessageType.INFO
                    )
                )
                true
            } else {
                Log.w(TAG, "Failed to send data, written: ${result.length}")
                addSystemMessage("发送失败（写入字节数: ${result.length}）", MessageType.ERROR)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending data", e)
            addSystemMessage("发送异常: ${e.message}", MessageType.ERROR)
            false
        }
    }

    /**
     * 开始读取数据
     * 根据文档，使用阻塞回调的方式接收传输的数据
     */
    private fun startReading() {
        stopReading() // 先停止之前的读取任务

        readJob = coroutineScope.launch {
            // 根据文档建议，每次传输的数据不超过 1KB
            val buffer = ByteArray(1024)
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val p = pipeline
                    if (p == null) {
                        delay(100)
                        continue
                    }

                    // 根据文档：int readData(byte[] buff, int length)
                    // 返回实际读取的字节数，如果没有数据可读，返回 0 或负数
                    val result = p.readData(buffer)
                    val readLength = result.length
                    if (readLength > 0) {
                        val data = String(buffer, 0, readLength, Charsets.UTF_8)
                        Log.d(TAG, "Received $readLength bytes: $data")
                        handleReceivedData(data)
                    } else if (readLength == 0) {
                        // 没有数据，稍等片刻再读（避免 CPU 占用过高）
                        delay(100)
                    } else {
                        // 读取错误或连接断开
                        val error = result.error
                        if (error != null && error.errorCode().isNotEmpty()) {
                            Log.w(TAG, "Read data error: ${error.description()}")
                            addSystemMessage("读取数据失败: ${error.description()}", MessageType.WARNING)
                        } else {
                            Log.w(TAG, "Read data returned negative value: $readLength")
                        }
                        delay(500)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while reading data", e)
                    if (isActive) {
                        // 出错后等待更长时间再重试
                        delay(1000)
                    }
                }
            }
            Log.d(TAG, "Reading loop ended")
        }
    }

    /**
     * 停止读取数据
     */
    private fun stopReading() {
        readJob?.cancel()
        readJob = null
    }

    /**
     * 处理接收到的数据
     */
    private fun handleReceivedData(data: String) {
        try {
            // 尝试解析为 JSON 对象
            val messageData = gson.fromJson(data, MessageData::class.java)
            val message = Message(
                title = messageData.title ?: "新消息",
                content = messageData.content ?: data,
                type = parseMessageType(messageData.type)
            )
            addMessage(message)
        } catch (e: JsonSyntaxException) {
            // 如果不是 JSON 格式，直接作为文本消息处理
            val message = Message(
                title = "接收",
                content = data
            )
            addMessage(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
            val message = Message(
                title = "接收",
                content = data
            )
            addMessage(message)
        }
    }

    /**
     * 添加消息到列表
     */
    private fun addMessage(message: Message) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(message.copy(id = UUID.randomUUID().toString()))
        _messages.value = currentMessages
        _latestMessage.value = message
    }

    /**
     * 添加系统消息
     */
    private fun addSystemMessage(content: String, type: MessageType) {
        val message = Message(
            title = "系统消息",
            content = content,
            type = type
        )
        addMessage(message)
    }

    /**
     * 解析消息类型
     */
    private fun parseMessageType(type: String?): MessageType {
        return when (type?.uppercase()) {
            "WARNING" -> MessageType.WARNING
            "ERROR" -> MessageType.ERROR
            "SUCCESS" -> MessageType.SUCCESS
            else -> MessageType.INFO
        }
    }

    /**
     * 清空消息列表
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * 服务器消息数据格式
     */
    data class MessageData(
        val title: String?,
        val content: String?,
        val type: String?
    )
}


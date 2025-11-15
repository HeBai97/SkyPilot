package com.brainai.skypilot.model

/**
 * 消息数据模型
 */
data class Message(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.INFO,
    val isRead: Boolean = false
)

/**
 * 消息类型
 */
enum class MessageType {
    INFO,      // 普通信息
    WARNING,   // 警告
    ERROR,     // 错误
    SUCCESS    // 成功
}

/**
 * 通信连接状态
 */
enum class ConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 连接中
    CONNECTED,      // 已连接
    RECONNECTING,   // 重连中
    ERROR           // 连接错误
}


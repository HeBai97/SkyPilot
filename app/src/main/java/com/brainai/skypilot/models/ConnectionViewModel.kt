package com.brainai.skypilot.models

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.brainai.skypilot.R
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.PairingState
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

/**
 * 连接视图模型
 * 负责管理与DJI SDK的连接状态和设备配对
 */
class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ConnectionViewModel"

    // 注册状态LiveData
    val registerState = MutableLiveData<String>()
    
    // SDK新闻LiveData
    val sdkNews = MutableLiveData<SDKNews>()

    /**
     * 注册DJI SDK
     * @param context 上下文
     * @param callback SDK管理器回调
     */
    fun registerApp(context: Context, @NonNull callback: SDKManagerCallback) {
        SDKManager.getInstance().init(context, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                Log.d(TAG, "SDK注册成功")
                callback.onRegisterSuccess()
                registerState.postValue("registered")
            }

            override fun onRegisterFailure(error: IDJIError?) {
                val errorMsg = "SDK注册失败: ${error?.description()}"
                Log.e(TAG, errorMsg)
                callback.onRegisterFailure(error)
                registerState.postValue("unregistered: ${error?.description()}")
            }

            override fun onProductDisconnect(product: Int) {
                Log.d(TAG, "设备断开连接，产品ID: $product")
                callback.onProductDisconnect(product)
            }

            override fun onProductConnect(product: Int) {
                Log.d(TAG, "设备已连接，产品ID: $product")
                callback.onProductConnect(product)
            }

            override fun onProductChanged(product: Int) {
                Log.d(TAG, "设备已变更，新产品ID: $product")
                callback.onProductChanged(product)
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                Log.d(TAG, "初始化进度: ${event?.name} ($totalProcess)")
                callback.onInitProcess(event, totalProcess)
                
                // 初始化完成后注册应用
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    Log.d(TAG, "初始化完成，开始注册应用")
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                val progress = if (total > 0) (current * 100 / total) else 0
                Log.d(TAG, "数据库下载进度: $progress% ($current/$total)")
                callback.onDatabaseDownloadProgress(current, total)
            }
        })
    }

    /**
     * 释放SDK回调
     * 安全地释放SDK资源，处理可能出现的异常
     */
    fun releaseSDKCallback() {
        try {
            // 检查 SDK 是否已初始化
            val sdkManager = SDKManager.getInstance()
            if (sdkManager.isRegistered) {
                try {
                    sdkManager.destroy()
                    Log.d(TAG, "已释放SDK资源")
                } catch (e: IllegalArgumentException) {
                    // 处理 Receiver not registered 异常
                    // 这通常发生在Receiver已经被释放或从未注册的情况下
                    if (e.message?.contains("Receiver not registered") == true) {
                        Log.w(TAG, "释放SDK资源时出现异常（Receiver未注册或已释放）: ${e.message}")
                    } else {
                        // 其他类型的IllegalArgumentException，重新抛出
                        throw e
                    }
                }
            } else {
                Log.d(TAG, "SDK未初始化，无需释放资源")
            }
        } catch (e: IllegalArgumentException) {
            // 处理 Receiver not registered 异常
            Log.w(TAG, "释放SDK资源时出现异常（可能是Receiver未注册）: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "释放SDK资源时出错", e)
        }
    }

    /**
     * 更新SDK新闻
     * 注意：当前为示例实现，需要根据实际需求完善
     */
    fun updateNews() {
        // TODO: 实现从服务器获取SDK新闻的逻辑
        sdkNews.postValue(SDKNews(
            title = R.string.app_name, 
            description = R.string.app_name,
            date = "2023-11-16"
        ))
    }

    /**
     * 执行遥控器配对操作
     * @param callback 配对结果回调
     */
    fun doPairing(callback: ((String) -> Unit)? = null) {
        if (!SDKManager.getInstance().isRegistered) {
            callback?.invoke("SDK未注册，请先初始化SDK")
            Log.e(TAG, "SDK未注册，无法执行配对操作")
            return
        }

        RemoteControllerKey.KeyPairingStatus.create().get(
            onSuccess = { pairingState ->
                when (pairingState) {
                    PairingState.PAIRING -> {
                        // 如果正在配对，则停止配对
                        RemoteControllerKey.KeyStopPairing.create().action(
                            onSuccess = {
                                val message = "已停止配对"
                                Log.d(TAG, message)
                                callback?.invoke(message)
                            },
                            onFailure = { error ->
                                val errorMsg = "停止配对失败: ${error?.description()}"
                                Log.e(TAG, errorMsg)
                                callback?.invoke(errorMsg)
                            }
                        )
                    }
                    else -> {
                        // 开始配对
                        RemoteControllerKey.KeyRequestPairing.create().action(
                            onSuccess = {
                                val message = "配对请求已发送"
                                Log.d(TAG, message)
                                callback?.invoke(message)
                            },
                            onFailure = { error ->
                                val errorMsg = "配对请求失败: ${error?.description()}"
                                Log.e(TAG, errorMsg)
                                callback?.invoke(errorMsg)
                            }
                        )
                    }
                }
            },
            onFailure = { error ->
                val errorMsg = "获取配对状态失败: ${error?.description()}"
                Log.e(TAG, errorMsg)
                callback?.invoke(errorMsg)
            }
        )
    }

    /**
     * SDK新闻数据类
     * @property title 标题资源ID
     * @property description 描述资源ID
     * @property date 日期字符串
     */
    data class SDKNews(
        val title: Int,
        val description: Int,
        val date: String
    )
}

package com.brainai.skypilot

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDex
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.network.DJINetworkManager
import com.brainai.skypilot.util.ToastUtils


class MyApplication : Application() {

    companion object {
        private val lvRegisterState = MutableLiveData<Pair<Boolean, IDJIError?>>()
        private val lvProductConnectionState = MutableLiveData<Pair<Boolean, Int>>()
        private val lvProductChanges = MutableLiveData<Int>()
        private val lvInitProcess = MutableLiveData<Pair<DJISDKInitEvent, Int>>()
        private val lvDBDownloadProgress = MutableLiveData<Pair<Long, Long>>()
        private var isInit = false
        
        fun getRegisterState(): LiveData<Pair<Boolean, IDJIError?>> = lvRegisterState
        fun getProductConnectionState(): LiveData<Pair<Boolean, Int>> = lvProductConnectionState
        fun getProductChanges(): LiveData<Int> = lvProductChanges
        fun getInitProcess(): LiveData<Pair<DJISDKInitEvent, Int>> = lvInitProcess
        fun getDBDownloadProgress(): LiveData<Pair<Long, Long>> = lvDBDownloadProgress
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        ToastUtils.initialize(this)

        // 在主线程延迟初始化
        Handler(Looper.getMainLooper()).postDelayed({
            initMobileSDK()
        }, 3000) // 延迟 3 秒
    }

    private fun initMobileSDK() {
        // 确保在主线程执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                doInitMobileSDK()
            }
            return
        }
        doInitMobileSDK()
    }

    private fun doInitMobileSDK() {
        try {
            android.util.Log.d("DJI_SDK", "Starting DJI SDK initialization on main thread...")

            SDKManager.getInstance().init(this, object : SDKManagerCallback {
                override fun onRegisterSuccess() {
                    android.util.Log.d("DJI_SDK", "DJI SDK registered successfully")
                    lvRegisterState.postValue(Pair(true, null))
                }

                override fun onRegisterFailure(error: IDJIError) {
                    android.util.Log.e("DJI_SDK", "DJI SDK registration failed: ${error.description()}")
                    lvRegisterState.postValue(Pair(false, error))
                }

                override fun onProductDisconnect(productId: Int) {
                    lvProductConnectionState.postValue(Pair(false, productId))
                }

                override fun onProductConnect(productId: Int) {
                    lvProductConnectionState.postValue(Pair(true, productId))
                }

                override fun onProductChanged(productId: Int) {
                    lvProductChanges.postValue(productId)
                }

                override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                    android.util.Log.d("DJI_SDK", "DJI SDK init process: $event, $totalProcess")
                    lvInitProcess.postValue(Pair(event, totalProcess))
                    if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                        isInit = true
                        SDKManager.getInstance().registerApp()
                    }
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    lvDBDownloadProgress.postValue(Pair(current, total))
                }
            })

            DJINetworkManager.getInstance().addNetworkStatusListener { isAvailable ->
                if (isInit && isAvailable && !SDKManager.getInstance().isRegistered) {
                    SDKManager.getInstance().registerApp()
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("DJI_SDK", "DJI SDK initialization failed", e)
            e.printStackTrace()
        }
    }

    fun destroyMobileSDK() {
        SDKManager.getInstance().destroy()
    }
    
    // Add public getter methods
    fun getRegisterState(): LiveData<Pair<Boolean, IDJIError?>> = lvRegisterState
    fun getProductConnectionState(): LiveData<Pair<Boolean, Int>> = lvProductConnectionState
    fun getProductChanges(): LiveData<Int> = lvProductChanges
    fun getInitProcess(): LiveData<Pair<DJISDKInitEvent, Int>> = lvInitProcess
    fun getDBDownloadProgress(): LiveData<Pair<Long, Long>> = lvDBDownloadProgress
}
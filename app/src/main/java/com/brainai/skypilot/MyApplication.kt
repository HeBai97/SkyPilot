package com.brainai.skypilot

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class MyApplication : Application() {

    private val TAG = "MyApplication"

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base ?: this)
        // 启用 MultiDex 支持（DJI SDK 可能需要）
        try {
            MultiDex.install(this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to install MultiDex", e)
        }
        // 注意：加固包类名可能因版本而异！
        // 如果报错找不到 com.cySdkyc.clx.Helper，请查看 MSDK Sample 中实际使用的类名
        try {
            val helperClass = Class.forName("com.cySdkyc.clx.Helper")
            val installMethod = helperClass.getMethod("install", Context::class.java)
            installMethod.invoke(null, base ?: this)
            Log.d(TAG, "Helper installed successfully")
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Helper class not found, skipping (this is OK if not using obfuscated SDK)", e)
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "Helper.install method not found, trying alternative method", e)
            // 尝试其他可能的方法签名
            try {
                val helperClass = Class.forName("com.cySdkyc.clx.Helper")
                val methods = helperClass.declaredMethods
                for (method in methods) {
                    if (method.name == "install" && method.parameterTypes.size == 1) {
                        method.isAccessible = true
                        method.invoke(null, base ?: this)
                        Log.d(TAG, "Helper installed using alternative method")
                        return
                    }
                }
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to find alternative install method", e2)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to install Helper, but continuing (this is OK if not using obfuscated SDK)", e)
            // 不抛出异常，允许应用继续启动
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // 检查SDK类是否存在
        try {
            Class.forName("dji.v5.manager.SDKManager")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "DJI SDK classes not found! Please check dependencies.", e)
            Log.e(TAG, "Make sure 'com.dji:dji-sdk-v5-aircraft' is properly added to build.gradle")
            // 不抛出异常，允许应用继续运行（但SDK功能将不可用）
            return
        }
        
        // 初始化DJI SDK
        try {
            SDKManager.getInstance().init(this, object : SDKManagerCallback {
                override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                    Log.d(TAG, "SDK Init Process: $event, Progress: $totalProcess")
                    if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                        try {
                            SDKManager.getInstance().registerApp()
                            Log.d(TAG, "SDK initialization complete, registering app...")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to register app", e)
                        }
                    }
                }

                override fun onRegisterSuccess() {
                    Log.i(TAG, "DJI SDK Registered Successfully")
                }

                override fun onRegisterFailure(error: IDJIError?) {
                    Log.e(TAG, "DJI SDK Register Failed: ${error?.description()}")
                    // 不抛出异常，允许应用继续运行
                }

                override fun onProductConnect(productId: Int) {
                    Log.i(TAG, "Product Connected: $productId")
                }

                override fun onProductDisconnect(productId: Int) {
                    Log.i(TAG, "Product Disconnected: $productId")
                }

                override fun onProductChanged(productId: Int) {
                    Log.i(TAG, "Product Changed: $productId")
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    Log.d(TAG, "DB Download: $current / $total")
                }
            })
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "DJI SDK classes not found at runtime! Check ProGuard rules and dependencies.", e)
            // 不抛出异常，允许应用继续运行
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DJI SDK", e)
            // 不抛出异常，允许应用继续运行
        }
    }
}


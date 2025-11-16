package com.brainai.skypilot

import android.app.Application
import android.content.Context

/**
 * 应用程序的Application类
 * 
 * 负责应用程序的全局初始化和配置
 * 主要功能：
 * 1. 初始化应用程序的全局上下文
 * 2. 安装和初始化Secneo SDK用于DJI SDK的加载
 * 3. 提供全局的应用程序配置
 * 
 * 使用说明：
 * 1. 在AndroidManifest.xml中配置此Application类
 * 2. 所有全局初始化代码应放在此类中
 * 
 * @property instance 应用程序的单例实例
 */
class MyApplication : Application() {
    
    companion object {
        /** 应用程序实例 */
        @Volatile
        private var instance: MyApplication? = null
        
        /**
         * 获取应用程序实例
         * @return 应用程序实例
         */
        @JvmStatic
        fun getInstance(): MyApplication {
            return instance ?: synchronized(this) {
                instance ?: MyApplication().also { instance = it }
            }
        }
    }
    
    /**
     * 当应用程序创建时调用
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        // 在这里添加其他全局初始化代码
    }
    
    /**
     * 在应用程序附加到上下文时调用
     * 用于执行一些需要在应用程序启动前初始化的操作
     * 
     * @param base 上下文基础
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 初始化Secneo SDK，用于DJI SDK的加载
        com.secneo.sdk.Helper.install(this)
    }
    
    /**
     * 当应用程序终止时调用
     * 用于释放资源和清理工作
     */
    override fun onTerminate() {
        // 在这里添加资源释放代码
        super.onTerminate()
    }
}
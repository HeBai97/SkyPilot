package com.brainai.skypilot.models

import dji.v5.common.register.PackageProductCategory
import dji.v5.utils.inner.SDKConfig

/**
 * MSDK信息模型类
 * 单例对象，用于获取DJI MSDK的配置和版本信息
 */
object MSDKInfoModel {
    
    /**
     * 检查SDK是否处于调试模式
     * @return 如果SDK处于调试模式返回true，否则返回false
     */
    fun isDebug(): Boolean = SDKConfig.getInstance().isDebug

    /**
     * 获取产品类别信息
     * @return 产品类别枚举值，表示当前连接的产品类型
     */
    fun getPackageProductCategory(): PackageProductCategory = 
        SDKConfig.getInstance().packageProductCategory

    /**
     * 获取SDK版本号
     * @return 返回SDK的版本号字符串
     */
    fun getSDKVersion(): String = 
        SDKConfig.getInstance().registrationSDKVersion

    /**
     * 获取构建版本号
     * @return 返回SDK的构建版本号字符串
     */
    fun getBuildVersion(): String = 
        SDKConfig.getInstance().buildVersion

    /**
     * 获取核心配置信息
     * @return 返回包含SDK核心配置信息的CoreInfo对象
     */
    fun getCoreInfo(): SDKConfig.CoreInfo = 
        SDKConfig.getInstance().coreInfo
}
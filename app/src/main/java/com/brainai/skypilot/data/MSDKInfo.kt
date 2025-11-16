package com.brainai.skypilot.data

import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.common.register.PackageProductCategory
import dji.v5.utils.inner.SDKConfig

/**
 * 默认字符串常量，用于初始化字符串属性
 */
const val DEFAULT_STR = "null string"

/**
 * MSDK信息数据类
 *
 * 用于存储和管理MSDK相关的配置和状态信息
 *
 * @property SDKVersion MSDK版本号
 * @property buildVer 构建版本号
 * @property isDebug 是否为调试模式
 * @property packageProductCategory 产品类别
 * @property productType 产品类型
 * @property networkInfo 网络信息
 * @property countryCode 国家/地区代码
 * @property firmwareVer 固件版本
 * @property isLDMLicenseLoaded LDM许可证是否已加载
 * @property isLDMEnabled LDM功能是否启用
 * @property coreInfo 核心配置信息
 */
data class MSDKInfo(
    val SDKVersion: String = DEFAULT_STR,
    var buildVer: String = DEFAULT_STR,
    var isDebug: Boolean = false,
    var packageProductCategory: PackageProductCategory? = null,
    var productType: ProductType = ProductType.UNKNOWN,
    var networkInfo: String = DEFAULT_STR,
    var countryCode: String = DEFAULT_STR,
    var firmwareVer: String = DEFAULT_STR,
    var isLDMLicenseLoaded: String = DEFAULT_STR,
    var isLDMEnabled: String = DEFAULT_STR,
    var coreInfo: SDKConfig.CoreInfo? = null
)
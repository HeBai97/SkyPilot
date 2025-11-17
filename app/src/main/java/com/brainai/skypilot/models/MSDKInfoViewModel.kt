package com.brainai.skypilot.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.brainai.skypilot.data.DEFAULT_STR
import com.brainai.skypilot.data.MSDKInfo
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.ProductKey
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.manager.areacode.AreaCodeChangeListener
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.manager.ldm.LDMManager
import dji.v5.network.DJINetworkManager
import dji.v5.network.IDJINetworkStatusListener
import dji.v5.utils.common.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MSDK信息视图模型
 * 负责管理DJI MSDK相关的信息，包括：
 * 1. SDK版本信息
 * 2. 固件版本
 * 3. 网络状态
 * 4. 区域码信息
 * 5. LDM(本地数据管理)状态
 */
class MSDKInfoViewModel : ViewModel() {

    private val tag: String = LogUtils.getTag(this)

    // LiveData 用于观察MSDK信息变化
    val msdkInfo = MutableLiveData<MSDKInfo>()

    // 主标题LiveData
    val mainTitle = MutableLiveData<String>()

    // 标记是否已初始化监听器
    private val isInited = AtomicBoolean(false)

    // MSDK信息模型
    private val msdkInfoModel: MSDKInfoModel = MSDKInfoModel

    // 区域码变更监听器
    private lateinit var areaCodeChangeListener: AreaCodeChangeListener

    // 网络状态监听器
    private lateinit var netWorkStatusListener: IDJINetworkStatusListener

    // 网络状态常量
    companion object {
        private const val ONLINE_STR = "online"
        private const val NO_NETWORK_STR = "no_network"
        private const val INNER_NETWORK_STR = "inner_network"
        private const val OUTER_NETWORK_STR = "outer_network"
    }

    init {
        initializeMSDKInfo()
        setupListeners()
    }

    /**
     * 初始化MSDK信息
     */
    private fun initializeMSDKInfo() {
        msdkInfo.value = MSDKInfo(msdkInfoModel.getSDKVersion()).apply {
            buildVer = msdkInfoModel.getBuildVersion()
            isDebug = msdkInfoModel.isDebug()
            packageProductCategory = msdkInfoModel.getPackageProductCategory()
            isLDMEnabled = LDMManager.getInstance().isLDMEnabled.toString()
            isLDMLicenseLoaded = LDMManager.getInstance().isLDMLicenseLoaded.toString()
            coreInfo = msdkInfoModel.getCoreInfo()
        }
        refreshMSDKInfo()
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 区域码变更监听
        areaCodeChangeListener = AreaCodeChangeListener { _, changed ->
            msdkInfo.value?.countryCode = changed?.areaCode ?: DEFAULT_STR
            LogUtils.i(tag, "区域码变更为: ${msdkInfo.value?.countryCode}")
            refreshMSDKInfo()
        }

        // 网络状态变更监听
        netWorkStatusListener = IDJINetworkStatusListener { isAvailable ->
            updateNetworkInfo(isAvailable)
            refreshMSDKInfo()
        }
    }

    /**
     * 当ViewModel被清除时调用，用于释放资源
     */
    override fun onCleared() {
        super.onCleared()
        removeListener()
    }

    /**
     * 刷新MSDK信息
     */
    fun refreshMSDKInfo() {
        msdkInfo.value?.let { info ->
            msdkInfo.postValue(info)
        }
    }

    /**
     * 初始化监听器
     * 注意：需要在SDK注册成功后调用
     */
    fun initListener() {
        if (!SDKManager.getInstance().isRegistered) {
            LogUtils.w(tag, "SDK未注册，无法初始化监听器")
            return
        }

        if (isInited.getAndSet(true)) {
            LogUtils.d(tag, "监听器已初始化，跳过重复初始化")
            return
        }

        try {
            // 监听飞行控制器连接状态
            FlightControllerKey.KeyConnection.create().listen(this) { isConnected ->
                LogUtils.i(tag, "飞行控制器连接状态: $isConnected")
                if (isConnected == true) {
                    updateProductType()
                    updateFirmwareVersion()
                }
            }

            // 添加区域码变更监听
            AreaCodeManager.getInstance().addAreaCodeChangeListener(areaCodeChangeListener)

            // 添加网络状态监听
            DJINetworkManager.getInstance().addNetworkStatusListener(netWorkStatusListener)

            // 监听产品类型变化
            ProductKey.KeyProductType.create().listen(this) { productType ->
                LogUtils.i(tag, "产品类型变更为: $productType")
                productType?.let {
                    msdkInfo.value?.productType = it
                    refreshMSDKInfo()
                }
            }

            // 主动获取当前产品类型（如果设备已连接）
            updateProductType()

            LogUtils.i(tag, "MSDK信息监听器初始化完成")
        } catch (e: Exception) {
            LogUtils.e(tag, "初始化监听器失败", e)
            isInited.set(false) // 发生异常时重置初始化状态
        }
    }

    /**
     * 移除所有监听器
     */
    private fun removeListener() {
        try {
            // 取消所有通过KeyManager注册的监听
            KeyManager.getInstance().cancelListen(this)

            // 移除区域码变更监听
            AreaCodeManager.getInstance().removeAreaCodeChangeListener(areaCodeChangeListener)

            // 移除网络状态监听
            DJINetworkManager.getInstance().removeNetworkStatusListener(netWorkStatusListener)

            LogUtils.i(tag, "已移除所有监听器")
        } catch (e: Exception) {
            LogUtils.e(tag, "移除监听器时出错", e)
        } finally {
            isInited.set(false)
        }
    }

    /**
     * 更新网络信息
     * @param isAvailable 网络是否可用
     */
    private fun updateNetworkInfo(isAvailable: Boolean) {
        msdkInfo.value?.networkInfo = if (isAvailable) ONLINE_STR else NO_NETWORK_STR

        // 如果需要检测内网/外网状态，可以取消下面的注释
        /*
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isInInnerNetwork = SDKConfig.getInstance().isInInnerNetwork
                msdkInfo.postValue(msdkInfo.value?.apply {
                    networkInfo = if (isInInnerNetwork) INNER_NETWORK_STR else OUTER_NETWORK_STR
                })
            } catch (e: Exception) {
                LogUtils.e(tag, "获取网络状态失败", e)
            }
        }
        */
    }

    /**
     * 更新产品类型信息
     */
    private fun updateProductType() {
        ProductKey.KeyProductType.create().get(
            onSuccess = { productType ->
                LogUtils.i(tag, "产品类型获取成功: $productType")
                productType?.let {
                    msdkInfo.value?.productType = it
                    refreshMSDKInfo()
                }
            },
            onFailure = { error ->
                LogUtils.e(tag, "获取产品类型失败: $error")
                // 如果获取失败，保持当前值不变
            }
        )
    }

    /**
     * 更新固件版本信息
     */
    private fun updateFirmwareVersion() {
        ProductKey.KeyFirmwareVersion.create().get(
            onSuccess = { version ->
                LogUtils.i(tag, "固件版本获取成功: $version")
                msdkInfo.value?.firmwareVer = version ?: DEFAULT_STR
                refreshMSDKInfo()
            },
            onFailure = { error ->
                LogUtils.e(tag, "获取固件版本失败: $error")
                msdkInfo.value?.firmwareVer = DEFAULT_STR
                refreshMSDKInfo()
            }
        )
    }

    /**
     * 更新LDM(本地数据管理)状态
     */
    fun updateLDMStatus() {
        try {
            val ldmManager = LDMManager.getInstance()
            msdkInfo.value?.apply {
                isLDMEnabled = ldmManager.isLDMEnabled.toString()
                isLDMLicenseLoaded = ldmManager.isLDMLicenseLoaded.toString()
                refreshMSDKInfo()
            }
            LogUtils.d(tag, "LDM状态已更新: enabled=${msdkInfo.value?.isLDMEnabled}, licenseLoaded=${msdkInfo.value?.isLDMLicenseLoaded}")
        } catch (e: Exception) {
            LogUtils.e(tag, "更新LDM状态失败", e)
        }
    }
}
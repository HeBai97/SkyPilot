package com.brainai.skypilot.pages

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.brainai.skypilot.MainActivity
import com.brainai.skypilot.R
import com.brainai.skypilot.models.ConnectionViewModel
import com.brainai.skypilot.models.MSDKInfoViewModel
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.StringUtils
import dji.v5.utils.common.ToastUtils

/**
 * 设备连接活动
 * 主要功能：
 * 1. 初始化DJI SDK
 * 2. 处理设备连接状态
 * 3. 显示设备信息和连接状态
 * 4. 提供配网功能
 */
class ConnectionActivity : AppCompatActivity() {
    // UI组件
    private lateinit var mTextConnectionStatus: TextView  // 连接状态显示
    private lateinit var mTextProduct: TextView           // 产品信息显示
    private lateinit var mTextModelAvailable: TextView    // 型号可用性显示
    private lateinit var mBtnOpen: Button                 // 打开主界面按钮
    private lateinit var mBtnPair: Button                 // 配网按钮
    private lateinit var mVersionTv: TextView             // 版本信息显示

    // ViewModel
    private val model: ConnectionViewModel by viewModels()
    private val msdkInfoVm: MSDKInfoViewModel by viewModels()
    
    // Handler用于主线程操作
    private val handler: Handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ConnectionActivity"
        
        /**
         * 启动ConnectionActivity的便捷方法
         * @param context 上下文
         */
        fun start(context: Context) {
            val intent = Intent(context, ConnectionActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        // 请求必要的权限
        requestRequiredPermissions()
        
        // 初始化UI
        initUI()
        
        // 注册DJI SDK
        registerApp()
        
        // 初始化MSDK信息显示
        initMSDKInfoView()
    }

    /**
     * 请求应用所需的权限
     */
    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_PHONE_STATE
            ),
            1
        )
    }

    /**
     * 初始化UI组件
     */
    private fun initUI() {
        // 初始化视图引用
        mTextConnectionStatus = findViewById(R.id.text_connection_status)
        mTextModelAvailable = findViewById(R.id.text_model_available)
        mTextProduct = findViewById(R.id.text_product_info)
        mBtnOpen = findViewById(R.id.btn_open)
        mVersionTv = findViewById(R.id.textView2)
        mBtnPair = findViewById(R.id.btn_pair)
        
        // 设置打开按钮点击事件
        mBtnOpen.isEnabled = false
        mBtnOpen.setOnClickListener {
            MainActivity.start(this)
            finish()
        }
        
        // 设置配网按钮点击事件
        mBtnPair.setOnClickListener {
            startPairing()
        }
    }

    /**
     * 开始配网流程
     */
    private fun startPairing() {
        model.doPairing { message ->
            runOnUiThread {
                ToastUtils.showToast(message)
                // 配网成功后启用打开按钮
                mBtnOpen.isEnabled = true
            }
        }
    }

    /**
     * 初始化MSDK信息显示
     */
    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        // 初始化Toast工具
        ToastUtils.init(this)
        
        // 观察MSDK信息变化
        msdkInfoVm.msdkInfo.observe(this) { info ->
            mVersionTv.text = StringUtils.getResStr(
                R.string.sdk_version, 
                "${info.SDKVersion} ${info.buildVer}"
            )
            mTextProduct.text = StringUtils.getResStr(
                R.string.product_information, 
                info.productType.name
            )
        }
        
        // 观察注册状态变化
        model.registerState.observe(this) { state ->
            mTextConnectionStatus.text = state
            // 注册成功时启用打开按钮
            if (state == getString(R.string.registration_success)) {
                mBtnOpen.isEnabled = true
            }
        }
        
        // 显示当前注册状态
        model.registerState.value?.let {
            ToastUtils.showToast(it)
        }
    }

    /**
     * 注册DJI SDK
     */
    private fun registerApp() {
        model.registerApp(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                runOnUiThread {
                    ToastUtils.showToast("SDK注册成功")
                    msdkInfoVm.initListener()
                    // 注册成功后可以在这里添加其他初始化操作
                }
            }

            override fun onRegisterFailure(error: IDJIError?) {
                runOnUiThread {
                    val errorMsg = "SDK注册失败: (错误码: ${error?.errorCode()}, 描述: ${error?.description()})"
                    ToastUtils.showToast(errorMsg)
                    mTextConnectionStatus.text = errorMsg
                }
            }

            override fun onProductDisconnect(product: Int) {
                runOnUiThread {
                    val message = "设备已断开连接, 产品ID: $product"
                    ToastUtils.showToast(message)
                    mTextConnectionStatus.text = message
                    mBtnOpen.isEnabled = false
                }
            }

            override fun onProductConnect(product: Int) {
                runOnUiThread {
                    val message = "设备已连接, 产品ID: $product"
                    ToastUtils.showToast(message)
                    mTextConnectionStatus.text = message
                    mBtnOpen.isEnabled = true
                }
            }

            override fun onProductChanged(product: Int) {
                runOnUiThread {
                    val message = "设备已变更, 新产品ID: $product"
                    ToastUtils.showToast(message)
                    mTextConnectionStatus.text = message
                }
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                runOnUiThread {
                    val message = "初始化进度: ${event?.name} ($totalProcess)"
                    mTextConnectionStatus.text = message
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                runOnUiThread {
                    val progress = (current * 100 / total).toInt()
                    mTextConnectionStatus.text = "数据库下载中: $progress% ($current/$total)"
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        model.releaseSDKCallback()
        handler.removeCallbacksAndMessages(null)
        ToastUtils.destroy()
    }

    override fun onResume() {
        super.onResume()
        // 重新连接SDK回调
        model.releaseSDKCallback()
    }
}
package com.brainai.skypilot

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.brainai.skypilot.data.MSDKInfo
import com.brainai.skypilot.databinding.ActivityMainBinding
import com.brainai.skypilot.util.MSDKInfoModel
import com.brainai.skypilot.util.ToastUtils
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.PermissionUtil
import io.reactivex.rxjava3.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private val tag: String = LogUtils.getTag(this)
    private val permissionArray = arrayListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.KILL_BACKGROUND_PROCESSES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    init {
        permissionArray.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // add(Manifest.permission.READ_MEDIA_IMAGES)
                // add(Manifest.permission.READ_MEDIA_VIDEO)
                // add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private lateinit var binding: ActivityMainBinding
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()
    private val msdkInfoModel = MSDKInfoModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Display SDK information
        displaySDKInfo()

        // Observe SDK registration state
        observeSDKRegistration()

        // Check and request permissions
        checkPermissionAndRequest()

        // Set click listener for the button
        binding.btnGoToPayload.setOnClickListener {
            // TODO: Implement navigation to payload connection activity
            ToastUtils.showToast("Navigating to Payload Connection...")
        }
    }

    private fun displaySDKInfo() {
        try {
            val msdkInfo = MSDKInfo(msdkInfoModel.getSDKVersion())
            msdkInfo.buildVer = msdkInfoModel.getBuildVersion()
            msdkInfo.isDebug = msdkInfoModel.isDebug()
            msdkInfo.packageProductCategory = msdkInfoModel.getPackageProductCategory().toString()

            binding.textView.text =
                "MSDK Integrated!\n\n" + "SDK Version: ${msdkInfo.SDKVersion}\n" + "Build Version: ${msdkInfo.buildVer}\n" + "Debug Mode: ${msdkInfo.isDebug}\n" + "Product Category: ${msdkInfo.packageProductCategory}\n\n" + "Click the button below to enter DJI Payload connection interface"
        } catch (e: Exception) {
            LogUtils.e(tag, "Error displaying SDK info", e)
            binding.textView.text = "MSDK Integrated!\n\nError retrieving SDK information"
        }
    }

    private fun observeSDKRegistration() {
        // Observe registration state
        (application as MyApplication).getRegisterState().observe(this, Observer { result ->
            if (result.first) {
                ToastUtils.showToast("SDK Registered Successfully")
                // Update UI to show registered status
                binding.textView.append("\n\nStatus: Registered")
            } else {
                ToastUtils.showToast("SDK Registration Failed: ${result.second?.description()}")
                // Update UI to show registration failure
                binding.textView.append("\n\nStatus: Registration Failed")
            }
        })

        // Observe product connection state
        (application as MyApplication).getProductConnectionState().observe(this, Observer { result ->
            val connected = if (result.first) "Connected" else "Disconnected"
            ToastUtils.showToast("Product ${result.second} $connected")
            // Update UI with product connection status
            binding.textView.append("\nProduct ${result.second}: $connected")
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    private fun handleAfterPermissionPermitted() {
        // Handle post-permission logic here
        ToastUtils.showToast("Permissions granted")
    }

    private fun showToast(content: String) {
        ToastUtils.showToast(content)
    }

    private fun checkPermissionAndRequest() {
        if (!checkPermission()) {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        for (i in permissionArray.indices) {
            if (!PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                return false
            }
        }
        return true
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result?.entries?.forEach {
            if (!it.value) {
                requestPermission()
                return@forEach
            }
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissionArray.toArray(arrayOf()))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()
    }
}
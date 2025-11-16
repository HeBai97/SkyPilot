package com.brainai.skypilot.pages

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.brainai.skypilot.R
import com.brainai.skypilot.models.WayPointObject
import com.dji.flysafe.mapkit.core.core.models.DJIBitmapDescriptor
import com.dji.flysafe.mapkit.core.core.models.DJILatLng
import com.dji.flysafe.mapkit.core.core.models.annotations.DJIMarkerOptions
import com.dji.flysafe.mapkit.core.core.models.annotations.DJIPolylineOptions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import dji.sdk.wpmz.jni.JNIWPMZManager
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.waypoint3.WPMZParserManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DiskUtil
import dji.v5.utils.common.FileUtils
import dji.v5.utils.common.ToastUtils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 航点任务主活动类
 * 
 * 负责管理航点任务的创建、编辑、上传和执行
 * 主要功能：
 * 1. 显示地图界面和航点标记
 * 2. 管理航点任务的创建和编辑
 * 3. 处理航点任务的上传和执行控制
 * 4. 显示任务执行状态和飞行器位置
 * 
 * 实现接口：
 * - View.OnClickListener: 处理界面点击事件
 * - OnMapReadyCallback: 地图初始化完成回调
 */
class WayPointV3 : View.OnClickListener, AppCompatActivity(), OnMapReadyCallback {

    companion object {
        /** 默认航点任务文件名 */
        private const val WAYPOINT_SAMPLE_FILE_NAME = "waypointsample.kmz"
        
        /** 航点任务文件存储目录 */
        private const val WAYPOINT_SAMPLE_FILE_DIR = "waypoint/"
        
        /** 航点任务缓存目录 */
        private const val WAYPOINT_SAMPLE_FILE_CACHE_DIR = "waypoint/cache/"
        
        /** 航点任务文件扩展名 */
        private const val WAYPOINT_FILE_TAG = ".kmz"
        
        /** 解压临时目录 */
        private const val UNZIP_CHILD_DIR = "temp/"
        
        /** 解压目录 */
        private const val UNZIP_DIR = "wpmz/"
        
        /** 文件选择器请求码 */
        private const val OPEN_FILE_CHOOSER = 0
        
        /** 目录选择器请求码 */
        private const val OPEN_DOCUMENT_TREE = 1
    }
    
    /** 异步操作可取消对象 */
    private var mDisposable: Disposable? = null

    var curMissionPath: String = DiskUtil.getExternalCacheDirPath(
        ContextUtil.getContext(),
        WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME
    )
    val rootDir = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR)
    var validLenth: Int = 2
    var curMissionExecuteState: WaypointMissionExecuteState? = null
    var selectWaylines: ArrayList<Int> = ArrayList()

    private lateinit var locate: Button
    private lateinit var start: Button
    private lateinit var stop: Button
    private lateinit var config: Button
    private lateinit var upload: Button
    private lateinit var pause: Button
    private lateinit var resume: Button
    private lateinit var select: Button
    private lateinit var kmz_btn: Button

    private lateinit var showTrack : Button
    private lateinit var mTextGPS: TextView
    private lateinit var clearWaypoints : Button

    private var mapboxMap: MapboxMap? = null
    private var droneMarker: Marker? = null
    private val markers: MutableMap<Int, Marker> = ConcurrentHashMap<Int, Marker>()

    /**
     * 初始化UI组件
     */
    private fun initUi() {
        locate = findViewById(R.id.locate)
        start = findViewById(R.id.start)
        stop = findViewById(R.id.stop)
        config = findViewById(R.id.start)
        upload = findViewById(R.id.stop)
        pause = findViewById(R.id.pause)
        resume = findViewById(R.id.resume)
        select = findViewById(R.id.select)
        kmz_btn = findViewById(R.id.kmz_btn)

        mTextGPS = findViewById(R.id.GPSTextView)
        showTrack = findViewById(R.id.showTrack)
        clearWaypoints = findViewById(R.id.clearWaypoints)
    }

    /**
     * 当地图准备就绪时调用
     * @param mapboxMap Mapbox地图实例
     */
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { 
            // 地图样式加载完成后的回调
        }
    }

    /**
     * 当活动创建时调用
     * @param savedInstanceState 保存的活动状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化Mapbox地图
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_waypoint1)

        // 初始化UI组件
        initUi()

        // 设置地图片段
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.onCreate(savedInstanceState)
        mapFragment.getMapAsync(this)

        // 准备任务数据
        prepareMissionData()
        
        // 初始化视图和数据
        initView(savedInstanceState)
        initData()
    }

    /**
     * 标记航点
     * @param point 航点坐标
     */
    private fun markWaypoint(point: LatLng) {
        val markerOptions = MarkerOptions()
            .position(point)
        mapboxMap?.let {
            val marker = it.addMarker(markerOptions)
            markers.put(markers.size, marker)
        }
    }

    /**
     * 准备任务数据
     * 创建必要的目录结构并复制默认航点任务文件
     */
    private fun prepareMissionData() {
        // 创建主目录
        val dir = File(rootDir).apply {
            if (!exists()) mkdirs()
        }

        // 创建缓存目录
        val cacheDir = File(
            DiskUtil.getExternalCacheDirPath(
                ContextUtil.getContext(),
                WAYPOINT_SAMPLE_FILE_CACHE_DIR
            )
        ).apply {
            if (!exists()) mkdirs()
        }

        // 复制默认航点任务文件
        val destPath = "$rootDir$WAYPOINT_SAMPLE_FILE_NAME"
        if (!File(destPath).exists()) {
            FileUtils.copyAssetsFile(
                ContextUtil.getContext(),
                WAYPOINT_SAMPLE_FILE_NAME,
                destPath
            )
        }
    }

    /**
     * 初始化视图和数据
     * @param savedInstanceState 保存的活动状态
     */
    private fun initView(savedInstanceState: Bundle?) {
        WayPointObject.addMissionStateListener() {
            curMissionExecuteState = it
        }
        
        WayPointObject.addWaylineExecutingInfoListener(object : WaylineExecutingInfoListener {
            override fun onWaylineExecutingInfoUpdate(it: WaylineExecutingInfo) {
                // 可在此处更新UI显示航迹执行信息
            }

            override fun onWaylineExecutingInterruptReasonUpdate(error: IDJIError?) {
                if (error != null) {
                    // 可在此处处理航迹执行中断错误
                }
            }
        })

        WayPointObject.addWaypointActionListener(object : WaypointActionListener {
            override fun onExecutionStart(actionId: Int) {
                // 可在此处处理动作开始执行事件
            }

            override fun onExecutionFinish(actionId: Int, error: IDJIError?) {
                // 可在此处处理动作执行完成事件
            }
        })

        upload.setOnClickListener {
            val waypointFile = File(curMissionPath)
            if (waypointFile.exists()) {
                WayPointObject.pushKMZFileToAircraft(curMissionPath)
            } else {
                ToastUtils.showToast("Mission file not found!")
            }
            markWaypoints()
        }

        start.setOnClickListener {
            WayPointObject.startMission(
                FileUtils.getFileName(curMissionPath, WAYPOINT_FILE_TAG),
                selectWaylines,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("startMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("startMission Failed " + getErroMsg(error))
                    }
                })
        }

        pause.setOnClickListener {
            WayPointObject.pauseMission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("pauseMission Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("pauseMission Failed " + getErroMsg(error))
                }
            })
        }

        resume.setOnClickListener {
            WayPointObject.resumeMission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("resumeMission Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("resumeMission Failed " + getErroMsg(error))
                }
            })
        }

        select.setOnClickListener {
            if (curMissionPath == null) {
                ToastUtils.showToast("please upload mission")
                return@setOnClickListener
            }
            selectWaylines.clear()
            var waylineids = WayPointObject.getAvailableWaylineIDs(curMissionPath)
            // 可在此处显示航迹选择对话框
        }

        kmz_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, "Select KMZ File"), OPEN_FILE_CHOOSER
            )
        }

        stop.setOnClickListener {
            if (curMissionExecuteState == WaypointMissionExecuteState.READY) {
                ToastUtils.showToast("Mission not start")
                return@setOnClickListener
            }
            WayPointObject.stopMission(
                FileUtils.getFileName(curMissionPath, WAYPOINT_FILE_TAG),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("stopMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("stopMission Failed " + getErroMsg(error))
                    }
                })
        }
    }

    /**
     * 更新WPML文件内容
     * @param newContent 新的文件内容
     */
    private fun updateWPML(newContent: String) {
        val waylineFile = File(rootDir + UNZIP_CHILD_DIR + UNZIP_DIR, WPMZParserManager.WAYLINE_FILE)

        Single.fromCallable {
            FileUtils.writeFile(waylineFile.path, newContent, false)
            // 将修改后的waylines.wpml重新压缩打包成 kmz
            val zipFiles = mutableListOf<String>()
            val cacheFolder = File(rootDir, UNZIP_CHILD_DIR + UNZIP_DIR)
            var zipFile = File(rootDir + UNZIP_CHILD_DIR + "waypoint.kmz")
            if (waylineFile.exists()) {
                zipFiles.add(cacheFolder.path)
                zipFile.createNewFile()
                WPMZParserManager.zipFiles(ContextUtil.getContext(), zipFiles, zipFile.path)
            }
            // 将用户选择的kmz用修改的后的覆盖
            FileUtils.copyFileByChannel(zipFile.path, curMissionPath)
        }.subscribeOn(Schedulers.io()).subscribe()
    }

    /**
     * 初始化数据
     */
    private fun initData() {
       WayPointObject.listenFlightControlState()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        WayPointObject.cancelListenFlightControlState()
        WayPointObject.removeAllMissionStateListener()
        WayPointObject.clearAllWaylineExecutingInfoListener()
        WayPointObject.clearAllWaypointActionListener()

        mDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    /**
     * 获取错误信息
     * @param error 错误对象
     * @return 错误描述信息
     */
    fun getErroMsg(error: IDJIError): String {
        if (!TextUtils.isEmpty(error.description())) {
            return error.description()
        }
        return error.errorCode()
    }

    /**
     * 标记所有航点
     */
    fun markWaypoints() {
        // version参数实际未用到
        var waypoints: ArrayList<WaylineExecuteWaypoint> = ArrayList<WaylineExecuteWaypoint>()
        val parseInfo = JNIWPMZManager.getWaylines("1.0.0", curMissionPath)
        var waylines = parseInfo.waylines
        waylines.forEach() {
            waypoints.addAll(it.waypoints)
            markLine(it.waypoints)
        }
        waypoints.forEach() {
            markWaypoint(DJILatLng(it.location.latitude, it.location.longitude), it.waypointIndex)
        }
    }

    /**
     * 标记单个航点
     * @param latlong 航点经纬度
     * @param waypointIndex 航点索引
     */
    fun markWaypoint(latlong: DJILatLng, waypointIndex: Int) {
        var markOptions = DJIMarkerOptions()
        markOptions.position(latlong)
        markOptions.icon(DJIBitmapDescriptor(R.drawable.aircraft))
        markOptions.title(waypointIndex.toString())
        markOptions.isInfoWindowEnable = true
        // 可在此处添加标记到地图
    }

    /**
     * 绘制航迹线
     * @param waypoints 航点列表
     */
    fun markLine(waypoints: List<WaylineExecuteWaypoint>) {
        var djiwaypoints = waypoints.filter {
            true
        }.map {
            DJILatLng(it.location.latitude, it.location.longitude)
        }
        var lineOptions = DJIPolylineOptions()
        lineOptions.width(5f)
        lineOptions.color(Color.GREEN)
        lineOptions.addAll(djiwaypoints)
        // 可在此处添加航线到地图
    }

    override fun onClick(v: View?) {
        // 可在此处处理点击事件
    }
}
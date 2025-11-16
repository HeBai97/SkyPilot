package com.brainai.skypilot.models

import androidx.lifecycle.MutableLiveData
import com.brainai.skypilot.data.FlightControlState
import com.brainai.skypilot.data.MissionUploadStateInfo
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.RxUtil
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable

/**
 * 航点任务管理类
 * 单例对象，负责管理航点任务的上传、执行、暂停、恢复和停止等操作
 * 同时处理飞行器状态监听和位置信息更新
 */
object WayPointObject {
    /** 弧度到角度的转换系数 */
    private const val RadToDeg = 57.295779513082321
    
    /** 任务上传状态LiveData，用于观察任务上传进度和结果 */
    val missionUploadState = MutableLiveData<MissionUploadStateInfo>()

    /** 飞行控制状态LiveData，包含飞行器的实时状态信息 */
    val flightControlState = MutableLiveData<FlightControlState>()
    
    /** 指南针航向键，用于获取飞行器航向角 */
    private val compassHeadKey: DJIKey<Double> = FlightControllerKey.KeyCompassHeading.create()
    
    /** 高度键，用于获取飞行器当前高度 */
    private val altitudeKey: DJIKey<Double> = FlightControllerKey.KeyAltitude.create()


    /**
     * 上传KMZ文件到飞行器
     * @param missionPath KMZ文件在设备上的路径
     */
    fun pushKMZFileToAircraft(missionPath: String) {
        WaypointMissionManager.getInstance().pushKMZFileToAircraft(missionPath, object :
            CommonCallbacks.CompletionCallbackWithProgress<Double> {
            
            /** 上传进度更新回调 */
            override fun onProgressUpdate(progress: Double) {
                missionUploadState.value = MissionUploadStateInfo(updateProgress = progress)
                refreshMissionState()
            }

            /** 上传成功回调 */
            override fun onSuccess() {
                missionUploadState.value = MissionUploadStateInfo(tips = "任务上传成功")
                refreshMissionState()
            }

            /** 上传失败回调 */
            override fun onFailure(error: IDJIError) {
                missionUploadState.value = MissionUploadStateInfo(error = error, tips = "任务上传失败")
                refreshMissionState()
            }
        })
    }

    /**
     * 刷新任务状态
     * 通过postValue确保在主线程更新UI
     */
    private fun refreshMissionState() {
        missionUploadState.postValue(missionUploadState.value)
    }

    /**
     * 开始执行航点任务
     * @param missionId 任务ID
     * @param waylineIDs 航点ID列表
     * @param callback 执行结果回调
     */
    fun startMission(missionId: String, waylineIDs: List<Int>, callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().startMission(missionId, waylineIDs, callback)
    }

    /**
     * 暂停当前正在执行的任务
     * @param callback 暂停操作结果回调
     */
    fun pauseMission(callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().pauseMission(callback)
    }

    /**
     * 恢复暂停的任务
     * @param callback 恢复操作结果回调
     */
    fun resumeMission(callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().resumeMission(callback)
    }

    /**
     * 停止当前任务
     * @param missionID 要停止的任务ID
     * @param callback 停止操作结果回调
     */
    fun stopMission(missionID: String, callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().stopMission(missionID, callback)
    }

    /**
     * 添加任务状态监听器
     * @param listener 任务状态监听器
     */
    fun addMissionStateListener(listener: WaypointMissionExecuteStateListener) {
        WaypointMissionManager.getInstance().addWaypointMissionExecuteStateListener(listener)
    }

    /**
     * 移除任务状态监听器
     * @param listener 要移除的监听器
     */
    fun removeMissionStateListener(listener: WaypointMissionExecuteStateListener) {
        WaypointMissionManager.getInstance().removeWaypointMissionExecuteStateListener(listener)
    }

    /**
     * 移除所有任务状态监听器
     */
    fun removeAllMissionStateListener() {
        WaypointMissionManager.getInstance().clearAllWaypointMissionExecuteStateListener()
    }

    /**
     * 添加航点执行信息监听器
     * @param listener 航点执行信息监听器
     */
    fun addWaylineExecutingInfoListener(listener: WaylineExecutingInfoListener) {
        WaypointMissionManager.getInstance().addWaylineExecutingInfoListener(listener)
    }

    /**
     * 移除航点执行信息监听器
     * @param listener 要移除的监听器
     */
    fun removeWaylineExecutingInfoListener(listener: WaylineExecutingInfoListener) {
        WaypointMissionManager.getInstance().removeWaylineExecutingInfoListener(listener)
    }

    /**
     * 清除所有航点执行信息监听器
     */
    fun clearAllWaylineExecutingInfoListener() {
        WaypointMissionManager.getInstance().clearAllWaylineExecutingInfoListener()
    }

    /**
     * 添加航点动作监听器
     * @param listener 航点动作监听器
     */
    fun addWaypointActionListener(listener: WaypointActionListener) {
        WaypointMissionManager.getInstance().addWaypointActionListener(listener)
    }

    /**
     * 清除所有航点动作监听器
     */
    fun clearAllWaypointActionListener() {
        WaypointMissionManager.getInstance().clearAllWaypointActionListener()
    }

    /**
     * 监听飞行控制状态
     * @return 返回可销毁的Disposable对象，用于取消订阅
     */
    fun listenFlightControlState(): Disposable {
        return Flowable.combineLatest(
            RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), this)
                .observeOn(AndroidSchedulers.mainThread()),
            RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), this)
                .observeOn(AndroidSchedulers.mainThread())
        ) { homelocation: LocationCoordinate2D?, aircraftLocation: LocationCoordinate2D? ->
            if (homelocation == null || aircraftLocation == null) {
                return@combineLatest
            }
            val height = getHeight()
            val distance = calculateDistance(
                homelocation.latitude, 
                homelocation.longitude, 
                aircraftLocation.latitude, 
                aircraftLocation.longitude
            )
            val heading = getHeading()
            flightControlState.value = FlightControlState(
                longitude = aircraftLocation.longitude,
                latitude = aircraftLocation.latitude,
                distance = distance,
                height = height,
                head = heading,
                homeLocation = homelocation
            )
            refreshFlightControlState()
        }.subscribe()
    }

    /**
     * 检查坐标是否有效
     * @param latitude 纬度
     * @param longitude 经度
     * @return 如果坐标有效返回true，否则返回false
     */
    fun isLocationValid(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && 
               longitude in -180.0..180.0 && 
               latitude != 0.0 && 
               longitude != 0.0
    }

    /**
     * 取消飞行控制状态监听
     */
    fun cancelListenFlightControlState() {
        KeyManager.getInstance().cancelListen(this)
    }
    /**
     * 获取可用的航点ID列表
     * @param missionPath 任务文件路径
     * @return 可用的航点ID列表
     */
    fun getAvailableWaylineIDs(missionPath: String): List<Int> {
        return WaypointMissionManager.getInstance().getAvailableWaylineIDs(missionPath)
    }

    /**
     * 刷新飞行控制状态
     * 通过postValue确保在主线程更新UI
     */
    private fun refreshFlightControlState() {
        flightControlState.postValue(flightControlState.value)
    }


    /**
     * 计算两个经纬度坐标之间的距离（米）
     * 使用球面余弦定律计算大圆距离
     * 
     * @param latA 第一个点的纬度
     * @param lngA 第一个点的经度
     * @param latB 第二个点的纬度
     * @param lngB 第二个点的经度
     * @return 两点之间的距离（米）
     */
    fun calculateDistance(
        latA: Double,
        lngA: Double,
        latB: Double,
        lngB: Double
    ): Double {
        // 地球半径（米）
        val earthRadius = 6_371_000.0
        
        // 将角度转换为弧度
        val lat1Rad = Math.toRadians(latA)
        val lat2Rad = Math.toRadians(latB)
        val deltaLngRad = Math.toRadians(lngB - lngA)
        
        // 使用球面余弦定律计算距离
        val cosAngle = sin(lat1Rad) * sin(lat2Rad) +
                      cos(lat1Rad) * cos(lat2Rad) * cos(deltaLngRad)
        
        // 处理计算精度问题
        val clampedCos = cosAngle.coerceIn(-1.0, 1.0)
        
        return acos(clampedCos) * earthRadius
    }
    
    // Kotlin数学函数别名，提高可读性
    private fun sin(radians: Double) = Math.sin(radians)
    private fun cos(radians: Double) = Math.cos(radians)
    private fun acos(value: Double) = Math.acos(value)

    /**
     * 获取当前航向角
     * @return 航向角（度），范围0-360
     */
    private fun getHeading(): Float = compassHeadKey.get(0.0).toFloat()

    /**
     * 获取当前高度
     * @return 高度值（米）
     */
    private fun getHeight(): Double = altitudeKey.get(0.0)

}
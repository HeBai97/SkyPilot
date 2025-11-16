package com.brainai.skypilot.data

import dji.sdk.keyvalue.value.common.LocationCoordinate2D

/**
 * 飞机飞行控制状态数据类
 *
 * 用于表示飞机的实时状态信息，包括位置、高度、航向等关键飞行数据
 *
 * @property longitude 飞机当前经度坐标
 * @property latitude 飞机当前纬度坐标
 * @property head 飞机当前航向角度（单位：度，0-360）
 * @property height 飞机当前相对高度（单位：米）
 * @property distance 飞机与返航点的水平距离（单位：米）
 * @property homeLocation 返航点坐标
 */
data class FlightControlState(
    val longitude: Double = 0.0,
    val latitude: Double = 0.0,
    val head: Float = 0.0f,
    val height: Double = 0.0,
    val distance: Double = 0.0,
    val homeLocation: LocationCoordinate2D
)
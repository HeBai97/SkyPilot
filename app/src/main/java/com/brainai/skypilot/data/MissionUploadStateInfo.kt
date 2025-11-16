package com.brainai.skypilot.data

import dji.v5.common.error.IDJIError

/**
 * 任务上传状态信息数据类
 *
 * 用于表示任务上传过程中的状态信息，包括上传进度、提示信息和可能发生的错误
 *
 * @property tips 上传过程中的提示信息
 * @property updateProgress 上传进度，范围0.0到1.0
 * @property error 上传过程中发生的错误，如果为null表示没有错误
 */
data class MissionUploadStateInfo(
    val tips: String = "",
    val updateProgress: Double = 0.0,
    val error: IDJIError? = null
) {

}
package com.brainai.skypilot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.brainai.skypilot.pages.WayPointV3

/**
 * 主活动类，作为应用程序的入口点
 * 主要功能：
 * 1. 显示主界面UI
 * 2. 处理导航到其他功能模块（如航点任务）
 */
class MainActivity : AppCompatActivity() {

    /**
     * 点击事件监听器，处理界面上的按钮点击
     */
    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.btn_waypoint1 -> navigateToActivity(WayPointV3::class.java)
            // 可以在这里添加更多按钮的点击事件处理
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
    }

    /**
     * 初始化视图组件
     */
    private fun initializeViews() {
        // 设置航点任务按钮的点击监听
        findViewById<View>(R.id.btn_waypoint1).setOnClickListener(clickListener)
    }

    /**
     * 导航到指定活动
     * @param context 上下文
     * @param targetActivity 目标Activity的Class对象
     */
    private fun navigateToActivity(targetActivity: Class<*>) {
        startActivity(Intent(this, targetActivity))
    }

    companion object {
        /**
         * 启动MainActivity的便捷方法
         * @param context 上下文
         */
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
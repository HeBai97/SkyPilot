package com.brainai.skypilot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MainActivity : AppCompatActivity() {
    
    private val TAG = "MainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
        setContentView(R.layout.activity_main)
            Log.d(TAG, "MainActivity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MainActivity", e)
            // 即使布局加载失败，也不让应用崩溃
            finish()
        }
    }
}
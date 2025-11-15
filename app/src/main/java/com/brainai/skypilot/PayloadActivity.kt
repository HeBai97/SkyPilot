package com.brainai.skypilot

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.brainai.skypilot.adapter.MessageAdapter
import com.brainai.skypilot.databinding.ActivityPayloadBinding
import com.brainai.skypilot.model.ConnectionState
import com.brainai.skypilot.model.MessageType
import com.brainai.skypilot.viewmodel.DjiViewModel
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * DJI Payload 连接界面
 * 用于连接 DJI payload 并接收消息和发送指令
 */
class PayloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPayloadBinding
    private lateinit var viewModel: DjiViewModel
    private lateinit var messageAdapter: MessageAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[DjiViewModel::class.java]

        // 设置标题
        supportActionBar?.title = "DJI Payload 连接"

        // 初始化 RecyclerView
        setupRecyclerView()

        // 设置监听器
        setupListeners()

        // 观察数据变化
        observeData()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@PayloadActivity)
            adapter = messageAdapter
        }
    }

    private fun setupListeners() {
        // 连接/断开按钮
        binding.btnConnect.setOnClickListener {
            viewModel.toggleConnection()
        }

        // 发送按钮
        binding.btnSend.setOnClickListener {
            viewModel.sendMessage()
        }

        // 清空消息按钮
        binding.btnClear.setOnClickListener {
            viewModel.clearMessages()
        }

        // 通道ID输入
        binding.editChannelId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val id = s?.toString()?.toIntOrNull() ?: 1
                if (id in 1..65535) {
                    viewModel.updateChannelId(id)
                }
            }
        })

        // 传输类型切换
        binding.switchTransmissionType.setOnCheckedChangeListener { _, isChecked ->
            val type = if (isChecked) {
                TransmissionControlType.STABLE
            } else {
                TransmissionControlType.UNRELIABLE
            }
            viewModel.updateTransmissionType(type)
        }

        // 消息输入框
        binding.editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateMessageInput(s?.toString() ?: "")
            }
        })

        // 快捷指令按钮
        binding.btnCommand1.setOnClickListener {
            viewModel.sendCommand("COMMAND_1")
        }
        binding.btnCommand2.setOnClickListener {
            viewModel.sendCommand("COMMAND_2")
        }
        binding.btnCommand3.setOnClickListener {
            viewModel.sendCommand("COMMAND_3")
        }
    }

    private fun observeData() {
        // 观察连接状态
        viewModel.connectionState
            .onEach { state ->
                updateConnectionState(state)
            }
            .launchIn(coroutineScope)

        // 观察消息列表
        viewModel.messages
            .onEach { messages ->
                messageAdapter.submitList(messages)
                // 自动滚动到底部
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.post {
                        binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
            .launchIn(coroutineScope)

        // 观察消息输入
        viewModel.messageInput
            .onEach { text ->
                binding.editMessage.setText(text)
                binding.editMessage.setSelection(text.length)
            }
            .launchIn(coroutineScope)

        // 观察通道ID
        viewModel.channelId
            .onEach { id ->
                if (binding.editChannelId.text.toString() != id.toString()) {
                    binding.editChannelId.setText(id.toString())
                }
            }
            .launchIn(coroutineScope)

        // 观察传输类型
        viewModel.transmissionType
            .onEach { type ->
                val isStable = type == TransmissionControlType.STABLE
                if (binding.switchTransmissionType.isChecked != isStable) {
                    binding.switchTransmissionType.isChecked = isStable
                }
            }
            .launchIn(coroutineScope)
    }

    private fun updateConnectionState(state: ConnectionState) {
        when (state) {
            ConnectionState.DISCONNECTED -> {
                binding.btnConnect.text = "连接"
                binding.btnConnect.isEnabled = true
                binding.textConnectionStatus.text = "未连接"
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
                binding.btnSend.isEnabled = false
                binding.editMessage.isEnabled = false
            }
            ConnectionState.CONNECTING -> {
                binding.btnConnect.text = "连接中..."
                binding.btnConnect.isEnabled = false
                binding.textConnectionStatus.text = "连接中..."
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))
                binding.btnSend.isEnabled = false
                binding.editMessage.isEnabled = false
            }
            ConnectionState.CONNECTED -> {
                binding.btnConnect.text = "断开"
                binding.btnConnect.isEnabled = true
                binding.textConnectionStatus.text = "已连接"
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                binding.btnSend.isEnabled = true
                binding.editMessage.isEnabled = true
            }
            ConnectionState.ERROR -> {
                binding.btnConnect.text = "重试"
                binding.btnConnect.isEnabled = true
                binding.textConnectionStatus.text = "连接错误"
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
                binding.btnSend.isEnabled = false
                binding.editMessage.isEnabled = false
            }
            ConnectionState.RECONNECTING -> {
                binding.btnConnect.text = "重连中..."
                binding.btnConnect.isEnabled = false
                binding.textConnectionStatus.text = "重连中..."
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))
                binding.btnSend.isEnabled = false
                binding.editMessage.isEnabled = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}


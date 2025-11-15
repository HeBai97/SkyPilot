package com.brainai.skypilot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brainai.skypilot.R
import com.brainai.skypilot.model.Message
import com.brainai.skypilot.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 消息列表适配器
 */
class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.textTitle)
        private val contentText: TextView = itemView.findViewById(R.id.textContent)
        private val timeText: TextView = itemView.findViewById(R.id.textTime)
        private val typeIndicator: View = itemView.findViewById(R.id.viewTypeIndicator)

        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(message: Message) {
            titleText.text = message.title
            contentText.text = message.content
            timeText.text = dateFormat.format(Date(message.timestamp))

            // 根据消息类型设置颜色
            val color = when (message.type) {
                MessageType.INFO -> ContextCompat.getColor(itemView.context, R.color.info)
                MessageType.SUCCESS -> ContextCompat.getColor(itemView.context, R.color.success)
                MessageType.WARNING -> ContextCompat.getColor(itemView.context, R.color.warning)
                MessageType.ERROR -> ContextCompat.getColor(itemView.context, R.color.error)
            }
            typeIndicator.setBackgroundColor(color)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}


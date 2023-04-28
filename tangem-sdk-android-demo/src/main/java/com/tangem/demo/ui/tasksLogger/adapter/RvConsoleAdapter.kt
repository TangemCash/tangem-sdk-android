package com.tangem.demo.ui.tasksLogger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tangem.Log
import com.tangem.TangemSdkLogger
import com.tangem.demo.postUi
import com.tangem.tangem_demo.R

/**
 * Created by Anton Zhilenkov on 25/12/2020.
 */
class RvConsoleAdapter : RecyclerView.Adapter<ConsoleVH>(), TangemSdkLogger {
    private val itemList = mutableListOf<ConsoleMessage>()

    var onItemCountChanged: ((Int) -> Unit)? = null

    fun addItem(item: ConsoleMessage) {
        postUi {
            itemList.add(item)
            notifyDataSetChanged()
            onItemCountChanged?.invoke(itemCount)
        }
    }

    fun clear() {
        postUi {
            itemList.clear()
            notifyDataSetChanged()
            onItemCountChanged?.invoke(itemCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsoleVH {
        val inflater = LayoutInflater.from(parent.context)
        val vhView = inflater.inflate(R.layout.item_console_message_rv, parent, false)
        return ConsoleVH(vhView)
    }

    override fun onBindViewHolder(holder: ConsoleVH, position: Int) {
        holder.bindData(itemList[position])
    }

    override fun getItemCount(): Int = itemList.size

    override fun log(message: () -> String, level: Log.Level) {
        addItem(ConsoleMessage(message, level))
    }

    fun onDestroy() {
        onItemCountChanged = null
    }
}

data class ConsoleMessage(val message: () -> String, val level: Log.Level)

class ConsoleVH(view: View) : RecyclerView.ViewHolder(view) {

    private val tvLogMessage = view.findViewById<TextView>(R.id.tvLogMessage)
    private val bottomSeparator = view.findViewById<View>(R.id.bottomSeparator)

    fun bindData(data: ConsoleMessage) {
        val context = tvLogMessage.context
        bottomSeparator.visibility = View.GONE

        tvLogMessage.setTextColor(ContextCompat.getColor(context, getColorResource(data.level)))
        tvLogMessage.text = data.message()
    }

    private fun getColorResource(level: Log.Level): Int {
        return when (level) {
            Log.Level.Debug -> R.color.log_debug
            Log.Level.Warning -> R.color.log_warning
            Log.Level.Error -> R.color.log_error
            Log.Level.Tlv -> R.color.log_tlv
            else -> R.color.log
        }
    }
}

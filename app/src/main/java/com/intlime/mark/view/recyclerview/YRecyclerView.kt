package com.intlime.mark.view.recyclerview

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView
import java.util.*

/**
 * Created by root on 15-12-28.
 */
open class YRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : RecyclerView(context, attrs) {
    private var recyclerItemListener: RecyclerItemListener? = null
    private var emptyView: View? = null
    private var innerDataObserver: EmptyAdapterDataObserver? = null
    private var dataObservers: MutableList<RecyclerView.AdapterDataObserver>? = null
    var inVisible = GONE

    fun addDataObserver(dataObserver: RecyclerView.AdapterDataObserver) {
        dataObservers!!.add(dataObserver)
    }

    fun setRecyclerItemListener(recyclerItemListener: RecyclerItemListener?) {
        this.recyclerItemListener = recyclerItemListener
        if (recyclerItemListener != null) {
            recyclerItemListener.recyclerView = this
        }
        if (adapter != null) {
            (adapter as RecyclerAdapter).recyclerItemListener = recyclerItemListener
        }
    }

    init {
        dataObservers = ArrayList<AdapterDataObserver>()
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<ViewHolder>?) {
        if (adapter != null && adapter !is RecyclerAdapter) {
            throw RuntimeException("Unsupported Adapter used. Valid one is RecyclerAdapter！")
        }
        if (innerDataObserver == null) {
            innerDataObserver = EmptyAdapterDataObserver()
        }
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(innerDataObserver)
        super.setAdapter(adapter)
        if (adapter != null) {
            adapter.registerAdapterDataObserver(innerDataObserver)
            (adapter as RecyclerAdapter).recyclerView = this
            adapter.recyclerItemListener = recyclerItemListener
        }

        checkIfEmpty()
    }

    fun registerEmptyView(emptyView: View?) {
        this.emptyView = emptyView
        checkIfEmpty()
    }

    private fun checkIfEmpty() {
        if (emptyView != null && adapter != null) {
            val emptyViewVisible = (adapter as RecyclerAdapter).normalItemCount == 0
            emptyView!!.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
            visibility = if (emptyViewVisible) inVisible else View.VISIBLE
        }
    }

    private inner class EmptyAdapterDataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty()
            for (i in dataObservers!!.indices) {
                dataObservers!![i].onChanged()
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
            for (i in dataObservers!!.indices) {
                dataObservers!![i].onItemRangeInserted(positionStart, itemCount)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
            for (i in dataObservers!!.indices) {
                dataObservers!![i].onItemRangeRemoved(positionStart, itemCount)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            for (i in dataObservers!!.indices) {
                dataObservers!![i].onItemRangeChanged(positionStart, itemCount)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            for (i in dataObservers!!.indices) {
                dataObservers!![i].onItemRangeChanged(positionStart, itemCount, payload)
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            for (i in dataObservers!!.indices) {
                dataObservers!![i].onItemRangeMoved(fromPosition, toPosition, itemCount)
            }
        }
    }
}

inline fun ViewManager.yRecyclerView(theme: Int = 0, init: YRecyclerView.() -> Unit) = ankoView({ YRecyclerView(it) }, theme, init)

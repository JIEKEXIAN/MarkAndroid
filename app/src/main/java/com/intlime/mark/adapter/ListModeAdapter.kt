package com.intlime.mark.adapter

import android.app.Dialog
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.tools.MResource
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.glide.MovieCoverKey
import com.intlime.mark.view.MovieTodoView
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.RecyclerAdapter
import com.intlime.mark.view.widget.PercentRatingBar
import com.intlime.mark.view.widget.percentRatingBar
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableRelativeLayout
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by root on 16-3-14.
 * @param mode 1:想看 2:已看
 */
class ListModeAdapter(val mode: Int = 1) : RecyclerAdapter() {
    companion object {
        val pool = RecyclerView.RecycledViewPool()
        private val placeholder = ColorDrawable(Color.parseColor("#e1e1e1"))
        private val error = EmptyDrawable(50f, 50f)
    }

    var cCursor: Cursor = MovieTodoView.getMyCursor()

    var canReloadGroupMap = true
    val groupMap = LinkedHashMap<String, Int>()
    var pinGroupText: TextView? = null
    var type = 1//1,2,3,4个tab
    var multiSelect = false
    var lastPos = Int.MAX_VALUE

    fun setMultiSelect(multiSelect: Boolean, notify: Boolean = false) {
        this.multiSelect = multiSelect
        if (multiSelect) {
            recyclerItemListener.createContextMenuable = false
        } else {
            recyclerItemListener.createContextMenuable = true
            multiSelectList.clear()
        }
        if (notify) {
            lastPos = Int.MAX_VALUE
            notifyDataSetChanged()
        }
    }

    val multiSelectList = ArrayList<Int>()
    var outMultiSelectListener: View.OnClickListener? = null
    private var multiSelectListener: View.OnClickListener = View.OnClickListener { v ->
        val position = recyclerView.getChildLayoutPosition(v)
        val holder = recyclerView.findViewHolderForLayoutPosition(position) as Holder
        if (multiSelectList.contains(position)) {
            multiSelectList.remove(position.toInt())
            holder.checkBox.isChecked = false
        } else {
            multiSelectList.add(position)
            holder.checkBox.isChecked = true
        }
        outMultiSelectListener?.onClick(v)
    }

    init {
        onDataChanged()
    }

    override fun getNormalItemCount(): Int {
        return cCursor.count
    }

    fun getItem(position: Int): MovieBean? {
        val bool = cCursor.moveToPosition(position - headerSize)
        if (!bool) return null
        return MovieDbManager.getInstance().getItemByCursor(cCursor)
    }

    fun onDataChanged() {
        pinGroupText?.text = ""
        lastPos = Int.MAX_VALUE
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        if (viewHolder is Holder) {
            handleGroup(position, viewHolder)
            if (cCursor.position != position) cCursor.moveToPosition(position)
            val pubdate = cCursor.getString(MovieDbManager.PUBDATE_P)
            var name = cCursor.getString(MovieDbManager.NAME_P)
            val imgUrl = cCursor.getString(MovieDbManager.IMAGE_P)
            val duration = cCursor.getString(MovieDbManager.DURATION_P)
            val movieType = cCursor.getString(MovieDbManager.MOVIE_TYPE_P)
            val rating = if (mode == 2) cCursor.getFloat(MovieDbManager.M_RATING_P) else cCursor.getFloat(MovieDbManager.DB_RATING_P)

            if (pinGroupText?.text.isNullOrEmpty()) pinGroupText!!.text = viewHolder.group.text
            viewHolder.nameText.text = if (name.isNullOrEmpty()) "-" else name
            if (mode == 2 && type == 1) {
                val watchTime = cCursor.getInt(MovieDbManager.WATCHTIME_P)
                if (watchTime == 0) {
                    viewHolder.pubdate.text = "暂无观影时间"
                } else {
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                    val date = Date(watchTime * 1000L)
                    viewHolder.pubdate.text = "${formatter.format(date)} (观影时间)"
                }
            } else {
                viewHolder.pubdate.text = if (pubdate.isNullOrEmpty()) "-" else pubdate
            }
            viewHolder.duration_movieType.text = ""

            val sb = StringBuilder()
            val movieTypes = movieType!!.split("/")
            if (movieTypes.size != 0) {
                for (i in movieTypes.indices) {
                    val tag = movieTypes[i]
                    if (!TextUtils.isEmpty(tag)) {
                        if (i != 0) {
                            sb.append(" | ")
                        }
                        sb.append(tag)
                    }
                }
            }
            if (duration.isNullOrEmpty() && sb.isNullOrEmpty()) {
                viewHolder.duration_movieType.text = "-"
            } else if (duration.isNullOrEmpty()) {
                viewHolder.duration_movieType.text = sb
            } else if (sb.isNullOrEmpty()) {
                viewHolder.duration_movieType.text = duration
            } else {
                viewHolder.duration_movieType.text = "$duration - $sb"
            }

            if (mode == 2) {
                viewHolder.rating.text = "${rating}分(我的评分)"
            } else {
                viewHolder.rating.text = "${rating}分(豆瓣)"
            }

            Glide.with(viewHolder.imageView.context)
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .signature(MovieCoverKey(imgUrl))
                    .placeholder(placeholder)
                    .error(error)
                    .into(viewHolder.imageView)

            viewHolder.ratingBar.setRating(rating)

            if (multiSelect) {
                viewHolder.checkBox.visibility = View.VISIBLE
                if (multiSelectList.contains(position)) {
                    viewHolder.checkBox.isChecked = true
                } else {
                    viewHolder.checkBox.isChecked = false
                }
                viewHolder.itemView.setOnClickListener(multiSelectListener)
            } else {
                viewHolder.checkBox.visibility = View.GONE
            }
            handleLongPressHint()
        }
    }

    private fun handleLongPressHint() {
        if (SettingManager.getInstance().canShowLongPressHint()) {
            SettingManager.getInstance().setCanShowLongPressHint(false)
            val dialog = Dialog(WWindowManager.getInstance().currentActivity, R.style.mydialog)
            val imageView = ImageView(AppEngine.getContext())
            imageView.setImageResource(R.drawable.long_press_hint)
            imageView.setOnClickListener { dialog.dismiss() }
            dialog.setContentView(imageView)
            val window = dialog.window
            val lp = window.attributes
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.x = WWindowManager.getInstance().width / -20
            lp.y = WWindowManager.getInstance().width / -5
            lp.dimAmount = 0f
            window.attributes = lp
            dialog.show()
            ThreadManager.getInstance().postDelayed({
                try {
                    if (dialog.isShowing) dialog.dismiss()
                } catch (ignore: Exception) {
                }
            }, 5000)
        }
    }


    private fun handleGroup(position: Int, viewHolder: Holder) {
        if (position < lastPos && position + 1 >= 0 && position + 1 < itemCount) {
            cCursor.moveToPosition(position + 1)
            val triple = getCondition(position)
            val groupText = triple.first
            val t1 = triple.second
            val t2 = triple.third

            if (t2.equals(t1)) {
                val holder = recyclerView.findViewHolderForLayoutPosition(position + 1)
                if (holder is Holder) {
                    ThreadManager.getInstance().post(holder)
                }
            }
            viewHolder.group.text = groupText
            viewHolder.group.visibility = View.VISIBLE
            viewHolder.line.visibility = View.INVISIBLE
        } else if (position - 1 >= 0 && position - 1 < itemCount) {
            cCursor.moveToPosition(position - 1)
            val triple = getCondition(position)
            val groupText = triple.first
            val t1 = triple.second
            val t2 = triple.third

            if (t2.equals(t1)) {
                viewHolder.group.visibility = View.GONE
                viewHolder.line.visibility = View.VISIBLE
            } else {
                viewHolder.group.visibility = View.VISIBLE
                viewHolder.line.visibility = View.INVISIBLE
            }
            viewHolder.group.text = groupText
        } else if (itemCount == 1 && position == 0) {
            cCursor.moveToPosition(position)
            val triple = getCondition(position)
            val groupText = triple.first

            viewHolder.group.visibility = View.VISIBLE
            viewHolder.line.visibility = View.INVISIBLE
            viewHolder.group.text = groupText
        }
        if (Math.abs(position - lastPos) == 2) {
            ThreadManager.getInstance().post {
                lastPos = Int.MAX_VALUE
                notifyDataSetChanged()
            }
        }
        lastPos = position
    }

    private fun getCondition(position: Int): Triple<String, Any, Any> {
        var groupText: String
        var t1: Any
        var t2: Any
        when (type) {
            1 -> {
                if (mode == 1) {
                    t1 = cCursor.getString(MovieDbManager.GROUP_UPDATE_TIME_P)
                    cCursor.moveToPosition(position)
                    t2 = cCursor.getString(MovieDbManager.GROUP_UPDATE_TIME_P)
                    groupText = t2
                } else {
                    t1 = cCursor.getString(MovieDbManager.GROUP_WATCH_TIME_P)
                    cCursor.moveToPosition(position)
                    t2 = cCursor.getString(MovieDbManager.GROUP_WATCH_TIME_P)
                    groupText = t2
                }
            }
            2 -> {
                t1 = cCursor.getString(MovieDbManager.GROUP_PUBDATE_P)
                cCursor.moveToPosition(position)
                t2 = cCursor.getString(MovieDbManager.GROUP_PUBDATE_P)
                groupText = t2
            }
            3 -> {
                if (mode == 1) {
                    t1 = cCursor.getFloat(MovieDbManager.DB_RATING_P).toInt()
                    cCursor.moveToPosition(position)
                    t2 = cCursor.getFloat(MovieDbManager.DB_RATING_P).toInt()
                    groupText = t2.toString()
                } else {
                    t1 = cCursor.getFloat(MovieDbManager.M_RATING_P).toInt()
                    cCursor.moveToPosition(position)
                    t2 = cCursor.getFloat(MovieDbManager.M_RATING_P).toInt()
                    groupText = t2.toString()
                }
            }
            else -> {
                t1 = cCursor.getString(MovieDbManager.PINYIN_P)[0]
                cCursor.moveToPosition(position)
                t2 = cCursor.getString(MovieDbManager.PINYIN_P)[0]
                if (t1.equals('~')) t1 = '#'
                if (t2.equals('~')) t2 = '#'
                groupText = t2.toString()
                updateGroupMap(groupText, position)
            }
        }
        return Triple(groupText, t1, t2)
    }

    private fun updateGroupMap(s: String, position: Int) {
        if (groupMap.containsKey(s)) {
            if (position < groupMap[s]!!) {
                groupMap.put(s, position)
            }
        } else {
            groupMap.put(s, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var viewHolder = super.onCreateViewHolder(parent, viewType)
        if (viewHolder == null) {
            viewHolder = Holder(LinearLayout(AppEngine.getContext()))
        }
        return viewHolder
    }

    inner class Holder(view: LinearLayout) : RecyclerView.ViewHolder(view), Runnable {
        lateinit var checkBox: CheckBox
        lateinit var imageView: ImageView
        lateinit var nameText: TextView
        lateinit var pubdate: TextView
        lateinit var duration_movieType: TextView
        lateinit var ratingBar: PercentRatingBar
        lateinit var rating: TextView
        lateinit var line: View
        lateinit var group: TextView

        override fun run() {
            group.visibility = View.GONE
            line.visibility = View.VISIBLE
        }

        init {
            view.apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RecyclerView.LayoutParams(matchParent, wrapContent)
                group = textView {
                    textColor = Color.parseColor("#6f93a2")
                    textSize = 13f
                    singleLine = true
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(23))
                    backgroundColor = Color.parseColor("#ebebeb")
                    leftPadding = dip(12.5f)
                }
                pressableRelativeLayout {
                    PressableUtils.setPressableDrawable(this, MResource.getColor(R.color.black_pressed_color))
                    backgroundResource = R.color.bg
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(107))
                    checkBox = checkBox {
                        id = 1
                        val lp = RelativeLayout.LayoutParams(dip(20), dip(20))
                        lp.centerVertically()
                        lp.leftMargin = dip(15.6f)
                        lp.rightMargin = dip(7.1f)
                        layoutParams = lp
                        buttonDrawable = ColorDrawable()
                        backgroundResource = R.drawable.selector_list_multi_checkicon
                    }
                    imageView = imageView {
                        id = 2
                        val lp = RelativeLayout.LayoutParams(dip(55), dip(80))
                        lp.centerVertically()
                        lp.leftMargin = dip(15)
                        lp.rightMargin = dip(15)
                        lp.rightOf(checkBox)
                        layoutParams = lp
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    nameText = textView {
                        id = 3
                        textSize = 16f
                        textColor = resources.getColor(R.color.a_main_text_color)
                        singleLine = true
                        ellipsize = TextUtils.TruncateAt.END
                        rightPadding = dip(15)
                        val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                        lp.topMargin = dip(10)
                        lp.rightOf(imageView)
                        layoutParams = lp
                    }
                    pubdate = textView {
                        id = 4
                        textSize = 12f
                        textColor = Color.parseColor("#818c91")
                        singleLine = true
                        ellipsize = TextUtils.TruncateAt.END
                        rightPadding = dip(18)
                        val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                        lp.topMargin = dip(9)
                        lp.rightOf(imageView)
                        lp.below(nameText)
                        layoutParams = lp
                    }
                    duration_movieType = textView {
                        id = 5
                        textSize = 12f
                        textColor = Color.parseColor("#818c91")
                        singleLine = true
                        ellipsize = TextUtils.TruncateAt.END
                        rightPadding = dip(15)
                        val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                        lp.topMargin = dip(2)
                        lp.rightOf(imageView)
                        lp.below(pubdate)
                        layoutParams = lp
                    }
                    ratingBar = percentRatingBar {
                        id = 6
                        val lp = RelativeLayout.LayoutParams(dip(86.9f), dip(11.7f))
                        lp.topMargin = dip(7)
                        lp.rightOf(imageView)
                        lp.below(duration_movieType)
                        layoutParams = lp
                    }
                    rating = textView {
                        id = 7
                        textSize = 12f
                        textColor = Color.parseColor("#818c91")
                        singleLine = true
                        ellipsize = TextUtils.TruncateAt.END
                        rightPadding = dip(15)
                        val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                        lp.topMargin = dip(4.5f)
                        lp.leftMargin = dip(8.9f)
                        lp.rightOf(ratingBar)
                        lp.below(duration_movieType)
                        layoutParams = lp
                    }
                    line = view {
                        id = 8
                        val lp = RelativeLayout.LayoutParams(matchParent, dip(1))
                        lp.alignParentTop()
                        lp.leftMargin = dip(12.2f)
                        layoutParams = lp
                        backgroundColor = Color.parseColor("#e1e1e1")
                    }
                }
            }
        }
    }
}

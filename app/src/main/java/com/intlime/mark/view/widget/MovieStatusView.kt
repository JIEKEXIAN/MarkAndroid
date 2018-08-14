package com.intlime.mark.view.widget

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.activitys.BaseActivity
import com.intlime.mark.activitys.MovieCardShareActivity
import com.intlime.mark.activitys.SingleAccessEditActivity
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.SingleAccessBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.CryptTool
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.FastBlurTool
import com.intlime.mark.tools.ZhugeTool
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableTextView
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by wtuadn on 16/05/05.
 */
object MovieStatusView : View.OnClickListener {
    private lateinit var name: TextView
    private lateinit var year: TextView
    private lateinit var todo: TextView
    private lateinit var done: TextView
    private lateinit var like: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var deleteSingle: TextView
    private lateinit var deleteSingleLine: View
    private lateinit var single: TextView
    private lateinit var share: TextView
    private lateinit var multi: TextView
    private lateinit var multiLine: View
    private lateinit var delete: TextView
    private val rootView = applyUI()

    private var activity: BaseActivity? = null
    private var dialog: Dialog? = null
    private var bean: MovieBean? = null
    private var multiClickListener: OnMultiClickListener? = null
    private var deleteSingleClickListener: OnDeleteSingleClickListener? = null
    private var statusChangeListener: OnStatusChangeListener? = null

    private var type = 0 //0为本地有的,1为本地没有的
    private var isDone = 0
    private var isLiked = false

    private fun applyUI(): View {
        return AppEngine.getContext().verticalLayout {
            onClick {
                dialog?.dismiss()
            }
            padding = dip(20)
            gravity = Gravity.BOTTOM
            lparams(matchParent, wrapContent)
            this@MovieStatusView.name = textView {
                textSize = 18f
                textColor = Color.WHITE
                gravity = Gravity.CENTER
                setSingleLine(true)
                lines = 1
                ellipsize = TextUtils.TruncateAt.END
            }.lparams(matchParent, wrapContent)
            year = textView {
                textSize = 15f
                textColor = Color.parseColor("#818c91")
                gravity = Gravity.CENTER
                setSingleLine(true)
                lines = 1
                ellipsize = TextUtils.TruncateAt.END
            }.lparams(matchParent, wrapContent) {
                topMargin = dip(6.7f)
                bottomMargin = dip(12.7f)
            }
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                val r = dip(4).toFloat()
                itemBg.setCornerRadii(floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f))
                backgroundDrawable = itemBg
                todo = pressableTextView {
                    text = "想看"
                    textSize = 17f
                    textColor = resources.getColor(R.color.dark_blue)
                    gravity = Gravity.CENTER_HORIZONTAL
                    compoundDrawablePadding = dip(12.5f)
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    setOnClickListener(this@MovieStatusView)
                    topPadding = dip(18)
                    bottomPadding = dip(11)
                }.lparams(0, wrapContent) {
                    weight = 1f
                }
                done = pressableTextView {
                    text = "已看"
                    textSize = 17f
                    textColor = resources.getColor(R.color.dark_blue)
                    gravity = Gravity.CENTER_HORIZONTAL
                    compoundDrawablePadding = dip(12.5f)
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    setOnClickListener(this@MovieStatusView)
                    topPadding = dip(18)
                    bottomPadding = dip(11)
                }.lparams(0, wrapContent) {
                    weight = 1f
                }
                like = pressableTextView {
                    text = "喜欢"
                    textSize = 17f
                    textColor = resources.getColor(R.color.dark_blue)
                    gravity = Gravity.CENTER_HORIZONTAL
                    compoundDrawablePadding = dip(12.5f)
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    setOnClickListener(this@MovieStatusView)
                    topPadding = dip(18)
                    bottomPadding = dip(11)
                }.lparams(0, wrapContent) {
                    weight = 1f
                }
            }
            line()
            verticalLayout {
                isClickable = true
                lparams(matchParent, dip(76))
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                backgroundDrawable = itemBg
                gravity = Gravity.CENTER
                textView("评分") {
                    textSize = 17f
                    textColor = resources.getColor(R.color.dark_blue)
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    lines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                ratingBar = ratingBar {
                    unChecked = resources.getDrawable(R.drawable.movie_status_rating_off)
                    setOnRatingBarChangeListener(getRatingBarChangeListener())
                }.lparams(dip(155), dip(21.5f)) {
                    topMargin = dip(10)
                }
            }
            line()
            deleteSingle = pressableTextView {
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                backgroundDrawable = itemBg
                text = "从该影单移除"
                textSize = 17f
                textColor = resources.getColor(R.color.dark_blue)
                gravity = Gravity.CENTER
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                setOnClickListener(this@MovieStatusView)
            }.lparams(matchParent, dip(45))
            deleteSingleLine = line()
            single = pressableTextView {
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                backgroundDrawable = itemBg
                text = "加入影单"
                textSize = 17f
                textColor = resources.getColor(R.color.dark_blue)
                gravity = Gravity.CENTER
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                setOnClickListener(this@MovieStatusView)
            }.lparams(matchParent, dip(45))
            line()
            share = pressableTextView {
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                backgroundDrawable = itemBg
                text = "分享"
                textSize = 17f
                textColor = resources.getColor(R.color.dark_blue)
                gravity = Gravity.CENTER
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                setOnClickListener(this@MovieStatusView)
            }.lparams(matchParent, dip(45))
            line()
            multi = pressableTextView {
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                backgroundDrawable = itemBg
                text = "多选"
                textSize = 17f
                textColor = resources.getColor(R.color.dark_blue)
                gravity = Gravity.CENTER
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                setOnClickListener(this@MovieStatusView)
            }.lparams(matchParent, dip(45))
            multiLine = line()
            delete = pressableTextView {
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                val r = dip(4).toFloat()
                itemBg.setCornerRadii(floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r))
                backgroundDrawable = itemBg
                text = "删除"
                textSize = 17f
                gravity = Gravity.CENTER
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                setOnClickListener(this@MovieStatusView)
            }.lparams(matchParent, dip(45))
            pressableTextView {
                val itemBg = GradientDrawable()
                itemBg.setColor(Color.parseColor("#e5f3f3f3"))
                val r = dip(4).toFloat()
                itemBg.setCornerRadius(r)
                backgroundDrawable = itemBg
                text = "完成"
                textSize = 17f
                textColor = resources.getColor(R.color.dark_blue)
                gravity = Gravity.CENTER
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                setOnClickListener(this@MovieStatusView)
            }.lparams(matchParent, dip(45)) {
                topMargin = dip(6)
            }
        }
    }

    private fun _LinearLayout.line(): View {
        return view {
            backgroundColor = Color.parseColor("#cbcbcb")
        }.lparams(matchParent, dip(1))
    }

    fun show(bean: MovieBean?, type: Int = 0, multiClickListener: OnMultiClickListener? = null,
             deleteSingleClickListener: OnDeleteSingleClickListener? = null, statusChangeListener: OnStatusChangeListener? = null) {
        bean ?: return
        isLiked = MovieSingleDbManager.isInSingle(0, bean.id)
        this.bean = bean
        this.type = type
        isDone = bean.done
        if (isDone < 0 || isDone > 1) isDone = 0
        this.multiClickListener = multiClickListener
        this.deleteSingleClickListener = deleteSingleClickListener
        this.statusChangeListener = statusChangeListener
        if (multiClickListener == null) {
            multi.visibility = View.GONE
            multiLine.visibility = View.GONE
        } else {
            multi.visibility = View.VISIBLE
            multiLine.visibility = View.VISIBLE
        }
        if (deleteSingleClickListener == null) {
            deleteSingle.visibility = View.GONE
            deleteSingleLine.visibility = View.GONE
        } else {
            deleteSingle.visibility = View.VISIBLE
            deleteSingleLine.visibility = View.VISIBLE
        }
        update(bean)
        if (rootView.parent != null) {
            (rootView.parent as ViewGroup).removeAllViews()
        }
        activity = WWindowManager.getInstance().currentActivity
        dialog = Dialog(activity, R.style.mydialog)
        activity!!.rootView.isDrawingCacheEnabled = true
        val blurBg = BitmapDrawable(FastBlurTool.blur(activity!!.rootView.drawingCache, activity!!.rootView))
        blurBg.setColorFilter(Color.parseColor("#a6000000"), PorterDuff.Mode.SRC_ATOP)
        rootView.backgroundDrawable = blurBg
        activity!!.rootView.isDrawingCacheEnabled = false
        dialog!!.setContentView(rootView)
        val window = dialog!!.window
        val lp = window.attributes
        lp.width = matchParent
        lp.height = matchParent
        lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        window.attributes = lp
        dialog!!.show()
        dialog!!.setOnDismissListener {
            this.bean = null
            this.multiClickListener = null
            this.deleteSingleClickListener = null
            this.statusChangeListener = null
            if (dialog != null) {
                (dialog!!.window.decorView as ViewGroup).removeAllViews()
            }
            dialog = null
            activity = null
        }
    }

    private fun update(bean: MovieBean) {
        val sdf = SimpleDateFormat("yyyy", Locale.CHINA)
        if (bean.pubdateTimestamp == 0L) {
            year.visibility = View.INVISIBLE
        } else if (bean.pubdateTimestamp < -2398320000L) {
            year.visibility = View.VISIBLE
            year.text = "暂无日期"
        } else {
            year.visibility = View.VISIBLE
            year.text = sdf.format(Date(bean.pubdateTimestamp * 1000L))
        }
        name.text = bean.name
        updateDoneStatus(bean)
        updateLikeStatus()
        ratingBar.setRating((bean.mark_rating / 2).toInt())
        if (type == 0) {
            delete.isEnabled = true
            delete.textColor = delete.resources.getColor(R.color.dark_blue)
        } else {
            delete.isEnabled = false
            delete.textColor = delete.resources.getColor(R.color.gray_text_color)
        }
    }

    private fun updateLikeStatus() {
        if (isLiked) {
            like.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_like_checked, 0, 0)
        } else {
            like.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_like_unchecked, 0, 0)
        }
    }

    private fun updateDoneStatus(bean: MovieBean) {
        if (type == 1) {
            todo.isEnabled = true
            todo.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_todo_unchecked, 0, 0)
            done.isEnabled = true
            done.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_done_unchecked, 0, 0)
        } else if (bean.done == 0) {
            todo.isEnabled = false
            todo.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_todo_checked, 0, 0)
            done.isEnabled = true
            done.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_done_unchecked, 0, 0)
        } else {
            todo.isEnabled = true
            todo.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_todo_unchecked, 0, 0)
            done.isEnabled = false
            done.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_status_done_checked, 0, 0)
        }
    }

    override fun onClick(v: View?) {
        if (checkArgs()) return
        when (v) {
            todo -> {
                isDone = 0
                handleTopThreeClick(false)
                ZhugeTool.track("电影想看", ZhugeTool.getTrackArg(Pair("电影名称", bean!!.name)))
            }
            done -> {
                isDone = 1
                handleTopThreeClick(false)
                ZhugeTool.track("电影已看", ZhugeTool.getTrackArg(Pair("电影名称", bean!!.name)))
            }
            like -> {
                handleTopThreeClick(true)
                if (!isLiked) {
                    ZhugeTool.track("电影喜欢", ZhugeTool.getTrackArg(Pair("电影名称", bean!!.name)))
                }
            }
            deleteSingle -> {
                //                ZhugeTool.track("从影单移除", ZhugeTool.getTrackArg(Pair("电影名称", bean!!.name)))
                deleteSingleClickListener?.onDeleteSingleClicked(deleteSingle)
                dialog?.dismiss()
            }
            single -> {
                ZhugeTool.track("电影加入影单", ZhugeTool.getTrackArg(Pair("电影名称", bean!!.name)))
                activity?.startActivity<SingleAccessEditActivity>(BaseActivity.BEAN to bean!!, "type" to type)
                dialog?.dismiss()
            }
            share -> {
                activity?.startActivity<MovieCardShareActivity>(BaseActivity.BEAN to bean!!)
                dialog?.dismiss()
            }
            multi -> {
                ZhugeTool.track("电影多选")
                multiClickListener?.onMultiClicked(multi)
                dialog?.dismiss()
            }
            delete -> {
                val bean = this.bean!!
                val statusChangeListener = this.statusChangeListener
                dialog?.dismiss()
                val confirmDialog = DialogTool.getConfirmDialog("确认删除", "确定", "取消")
                confirmDialog.confirm_agree.setOnClickListener {
                    //                    ZhugeTool.track("电影删除", ZhugeTool.getTrackArg(Pair("电影名称", bean.name)))
                    confirmDialog.dismiss()
                    val callback = object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<Any, Any>) {
                            MovieDbManager.getInstance().delete(bean)
                            bean.done = -1
                            bean.cursorPosition = -1
                            bean.mark_rating = 0f
                            bean.watchTime = 0
                            val intent = Intent(BaseActivity.RELOAD_ALL_ACTION)
                            intent.putExtra(BaseActivity.BEAN, bean)
                            intent.putExtra("type", 1)
                            AppEngine.getContext().sendBroadcast(intent)
                            bean.id = 0
                            bean.done = 0
                            ThreadManager.getInstance().post {
                                statusChangeListener?.onStatusChange(bean)
                            }
                        }
                    }
                    DialogTool.showWaitDialog("正在删除，请稍等", DialogTool.CANCEL_ON_BACK, callback)
                    NetManager.getInstance().deleteMovie(Integer.toString(bean.id), callback)
                }
                confirmDialog.confirm_disagree.setOnClickListener { confirmDialog.dismiss() }
                confirmDialog.show()
            }
            else -> {
                dialog?.dismiss()
            }
        }
    }

    private fun checkArgs(): Boolean {
        if (bean == null || activity == null) {
            dialog?.dismiss()
            return true
        }
        return false
    }

    private fun handleTopThreeClick(isLikeClick: Boolean, callBack: NetRequestCallBack? = null) {
        val callBack1 = object : NetRequestCallBack() {
            override fun onDefault() {
                if (callBack == null) DialogTool.dismissWaitDialog()
                callBack?.onDefault()
            }

            override fun onFail(result: ArrayMap<*, *>?, error_code: Int) {
                bean ?: return
                isDone = bean!!.done
                callBack?.onFail(result, error_code)
            }

            override fun onSuccess(result: ArrayMap<Any, Any>) {
                val time = result["time"] as Long
                if (type == 1) {
                    val temp = result["bean"] as MovieBean
                    bean!!.id = temp.id
                    bean!!.done = isDone
                    bean!!.db_rating = temp.db_rating
                    bean!!.pubdate = temp.pubdate
                    bean!!.duration = temp.duration
                    bean!!.movieType = temp.movieType
                    bean!!.update_time = temp.update_time
                    bean!!.pubdateTimestamp = temp.pubdateTimestamp
                    if (bean!!.done == 1) {
                        bean!!.watchTime = bean!!.update_time
                    } else {
                        bean!!.watchTime = 0
                    }
                    MovieDbManager.getInstance().insert(bean)
                    val intent = Intent(BaseActivity.RELOAD_ALL_ACTION)
                    intent.putExtra(BaseActivity.BEAN, bean!!)
                    intent.putExtra("type", 0)
                    AppEngine.getContext().sendBroadcast(intent)
                    type = 0
                } else if (isDone != bean!!.done) {
                    bean!!.done = isDone
                    bean!!.update_time = time
                    if (bean!!.done == 1) {
                        bean!!.watchTime = bean!!.update_time
                    } else {
                        bean!!.watchTime = 0
                    }
                    MovieDbManager.getInstance().update(bean)
                    val intent = Intent(BaseActivity.RELOAD_ALL_ACTION)
                    intent.putExtra(BaseActivity.BEAN, bean!!)
                    AppEngine.getContext().sendBroadcast(intent)
                }
                if (isLikeClick) {
                    if (isLiked) {
                        MovieSingleDbManager.deleteAccess(SingleAccessBean(0, bean!!.id, time))
                    } else {
                        MovieSingleDbManager.insertAccess(SingleAccessBean(0, bean!!.id, time))
                    }
                    isLiked = !isLiked
                    AppEngine.getContext().sendBroadcast(Intent(BaseActivity.RELOAD_SINGLE_ACTION))
                }
                ThreadManager.getInstance().post {
                    bean ?: return@post
                    update(bean!!)
                    statusChangeListener?.onStatusChange(bean!!)
                }
                callBack?.onSuccess(result)
            }
        }
        if (callBack == null) DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callBack1)
        val params: MutableList<NameValuePair> = ArrayList()
        params.add(BasicNameValuePair("db_num", bean!!.db_num))
        params.add(BasicNameValuePair("is_done", isDone.toString()))
        if (isLikeClick) {
            params.add(BasicNameValuePair("add_ids", if (isLiked) CryptTool.encrypt("") else CryptTool.encrypt("0")))
            params.add(BasicNameValuePair("delete_ids", if (isLiked) CryptTool.encrypt("0") else CryptTool.encrypt("")))
        } else {
            params.add(BasicNameValuePair("add_ids", CryptTool.encrypt("")))
            params.add(BasicNameValuePair("delete_ids", CryptTool.encrypt("")))
        }
        NetManager.getInstance().addSingleAccess(params, callBack1)
    }

    private fun getRatingBarChangeListener(): RatingBar.OnRatingBarChangeListener {
        return object : RatingBar.OnRatingBarChangeListener {
            override fun onRatingChanged(ratingBar: RatingBar, rating: Int) {
                if (checkArgs()) return
                if (rating <= 0) {
                    ratingBar.setRating((bean!!.mark_rating / 2).toInt())
                    return
                }
                val trulyRating = rating * 2
                if (trulyRating == bean!!.mark_rating.toInt()) return
                if (type == 1) {
                    isDone = 1
                    handleTopThreeClick(false, object : NetRequestCallBack() {
                        override fun onSuccess(result: ArrayMap<Any, Any>) {
                            rate(ratingBar, bean!!, trulyRating)
                        }

                        override fun onFail(result: ArrayMap<Any, Any>?, error_code: Int) {
                            bean ?: return
                            ThreadManager.getInstance().post { ratingBar.setRating((bean!!.mark_rating / 2).toInt()) }
                        }
                    })
                } else {
                    rate(ratingBar, bean!!, trulyRating)
                }
            }

            private fun rate(ratingBar: RatingBar, bean: MovieBean, trulyRating: Int) {
                ZhugeTool.track("电影评分", ZhugeTool.getTrackArg(Pair("电影名称", bean.name)))
                val params = ArrayList<NameValuePair>()
                params.add(BasicNameValuePair("rating", Integer.toString(trulyRating)))
                NetManager.getInstance().rateMovie(bean.id, params, object : NetRequestCallBack() {
                    override fun onDefault() {
                        DialogTool.dismissWaitDialog()
                    }

                    override fun onSuccess(result: ArrayMap<Any, Any>) {
                        if (bean.done == 0) {
                            bean.done = 1
                            isDone = 1
                            bean.update_time = result["time"] as Long
                            if (bean.done == 1) {
                                bean.watchTime = bean.update_time
                            } else {
                                bean.watchTime = 0
                            }
                        }
                        bean.mark_rating = trulyRating.toFloat()
                        ThreadManager.getInstance().post {
                            update(bean)
                            statusChangeListener?.onStatusChange(bean)
                        }
                        MovieDbManager.getInstance().update(bean)
                        val intent = Intent(BaseActivity.RELOAD_ALL_ACTION)
                        intent.putExtra(BaseActivity.BEAN, MovieStatusView.bean!!)
                        intent.putExtra("type", 0)
                        AppEngine.getContext().sendBroadcast(intent)
                    }

                    override fun onFail(result: ArrayMap<Any, Any>?, error_code: Int) {
                        ThreadManager.getInstance().post { ratingBar.setRating((bean.mark_rating / 2).toInt()) }
                    }
                })
            }
        }
    }

    interface OnMultiClickListener {
        fun onMultiClicked(v: View)
    }

    interface OnDeleteSingleClickListener {
        fun onDeleteSingleClicked(v: View)
    }

    interface OnStatusChangeListener {
        fun onStatusChange(bean: MovieBean)
    }
}
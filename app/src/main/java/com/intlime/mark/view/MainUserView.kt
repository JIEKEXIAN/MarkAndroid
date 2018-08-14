package com.intlime.mark.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.activitys.*
import com.intlime.mark.application.Session
import com.intlime.mark.application.SettingManager
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.tools.UmengTool
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.tools.glide.CircleTransform
import com.umeng.socialize.ShareAction
import com.umeng.socialize.bean.SHARE_MEDIA
import com.umeng.socialize.media.UMImage
import kotlinx.android.synthetic.main.user_center_layout.view.*
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.startActivity

/**
 * Created by root on 16-3-12.
 */
class MainUserView(val context: Activity) : ScrollView(context), View.OnClickListener {

    init {
        isFillViewport = true
        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
        View.inflate(context, R.layout.user_center_layout, this)
        initOther()
    }

    private fun initOther() {
        setClick()
        updateLayout()
        updateNewsCount()

        version.text = "v${SettingManager.getInstance().versionName} (${SettingManager.getInstance().versionCode})"
    }

    fun updateLayout() {
        val nicknameStr = SettingManager.getInstance().nickname
        val headUrl = SettingManager.getInstance().userHeadImgUrl
        if (TextUtils.isEmpty(nicknameStr)) {
            nickname.text = "设置昵称"
        } else {
            nickname.text = nicknameStr
        }
        if (TextUtils.isEmpty(headUrl)) {
            head.setImageResource(R.drawable.setting_header_icon)
        } else {
            Glide.with(context)
                    .load(headUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .transform(CircleTransform(context))
                    .error(R.drawable.setting_header_icon)
                    .into(head)
        }
    }

    fun updateNewsCount() {
        var count = SettingManager.getInstance().commentsCount + SettingManager.getInstance().notifyCount
        if (count > 0) {
            my_news_count.visibility = View.VISIBLE
            my_news_count.text = Integer.toString(count)
        } else {
            my_news_count.visibility = View.GONE
        }
    }

    private fun setClick() {
        head_layout.setOnClickListener(this)
        share.setOnClickListener(this)
        evaluate.setOnClickListener(this)
        feedback.setOnClickListener(this)
        about.setOnClickListener(this)
        exit.setOnClickListener(this)
        my_news.setOnClickListener(this)
        favorite_movielists.setOnClickListener(this)
        writer.setOnClickListener(this)
        favorite_cards.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.head_layout -> {
                context.startActivity<UserSettingActivity>()
            }
            R.id.my_news -> {
                context.startActivity<MyNewsActivity>()
                SettingManager.getInstance().commentsCount = 0
                context.sendBroadcast(Intent(BaseActivity.NOTIFY_COUNT_ACTION))
            }
            R.id.favorite_movielists -> {
                context.startActivity<MovieListFavoriteActivity>()
            }
            R.id.writer -> {
                context.startActivity<WriterActivity>()
            }
            R.id.favorite_cards -> {
                context.startActivity<LikedCardsActivity>()
            }
            R.id.share -> {
                if (UmengTool.checkPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                    DialogTool.showShareDialog(context, ShareListener())
                }
            }
            R.id.evaluate -> try {
                val uri = Uri.parse("market://details?id=" + context.packageName)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastTool.show("没有安装应用市场")
            }

            R.id.feedback -> {
                val intent = Intent(context, WebActivity::class.java)
                intent.putExtra("title", "意见反馈")
                intent.putExtra("url", "http://mark.intlime.com/Index/feedback")
                context.startActivity(intent)
            }
            R.id.about -> {
                val intent = Intent(context, AboutActivity::class.java)
                context.startActivity(intent)
            }
            R.id.exit -> {
                val dialog = DialogTool.getConfirmDialog("确定退出当前账号吗？", null, null)
                dialog.confirm_agree.setOnClickListener {
                    dialog.dismiss()
                    Session.uid = 0
                    Session.mUid = ""
                    SettingManager.getInstance().clear()
                    MovieDbManager.getInstance().clear()
                    MovieSingleDbManager.clearSingle()
                    MovieSingleDbManager.clearAccess()
                    context.startActivity<SplashActivity>()
                }
                dialog.show()
            }
        }
    }

    fun onRequestPermissionsSuccess() {
        DialogTool.showShareDialog(context, ShareListener())
    }

    private inner class ShareListener : View.OnClickListener {
        override fun onClick(v: View) {
            try {
                UmengTool.getInstance()

                val shareUrl = "http://mark.intlime.com/Index/download"
                val shareTitle = "Mark-我的电影清单"
                val shareText = "我在用Mark记录和发现电影，感觉还不错，推荐你试试"
                val shareImg = UMImage(context, R.mipmap.push)
                val shareAction = ShareAction(context)
                when (v.id) {
                    R.id.weixin -> shareAction.setPlatform(SHARE_MEDIA.WEIXIN).withMedia(shareImg).withTitle(shareTitle).withText(shareText).withTargetUrl(shareUrl).share()
                    R.id.weixin_circle -> shareAction.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE).withMedia(shareImg).withTitle(shareText).withText(shareText).withTargetUrl(shareUrl).share()
                    R.id.qq -> shareAction.setPlatform(SHARE_MEDIA.QQ).withMedia(shareImg).withTitle(shareTitle).withText(shareText).withTargetUrl(shareUrl).share()
                    R.id.qq_zone -> shareAction.setPlatform(SHARE_MEDIA.QZONE).withMedia(shareImg).withTitle(shareTitle).withText(shareText).withTargetUrl(shareUrl).share()
                    R.id.weibo -> shareAction.setPlatform(SHARE_MEDIA.SINA).withMedia(UMImage(context, R.drawable.weibo_share_img))
                            .withText("分享一个电影APP：Mark-我的电影清单，还挺好用的").withTargetUrl(shareUrl).share()
                    R.id.copy_link -> {
                        context.clipboardManager.text = shareText + " " + shareUrl
                        ToastTool.show("已复制到剪切板")
                    }
                    R.id.more -> {
                        val intent = Intent()
                        intent.action = Intent.ACTION_SEND
                        intent.putExtra(Intent.EXTRA_TEXT, shareText + " " + shareUrl)
                        intent.type = "text/*"
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ToastTool.show("分享失败")
            }
        }
    }
}

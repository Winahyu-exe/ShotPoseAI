package com.shotposeai.ui.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import com.shotposeai.analysis.TechniqueVideos

/** OS WebView with an app-identity Referer as required by the YouTube embedded player. */
class TechniquePreview(private val activity: Activity, private val web: WebView, private val status: TextView) {
    @SuppressLint("SetJavaScriptEnabled")
    fun load(video: TechniqueVideos.Video) {
        status.text="${video.title}\nPreview YouTube membutuhkan koneksi internet."
        web.settings.apply {
            javaScriptEnabled=true;domStorageEnabled=true;mediaPlaybackRequiresUserGesture=true
            allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        web.webChromeClient=object:WebChromeClient(){
            override fun onReceivedTitle(view:WebView?,title:String?){
                when {
                    title=="preview_ready" -> status.text="${video.title}\nKetuk putar untuk menonton."
                    title?.startsWith("preview_error_")==true -> status.text="Video belum bisa diputar di aplikasi. Coba lagi atau buka di YouTube."
                }
            }
        }
        web.webViewClient=object:WebViewClient(){
            override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest):Boolean {
                if(!request.isForMainFrame)return false
                val uri=request.url
                if(uri.scheme=="https"&&(uri.host=="www.youtube.com"||uri.host=="m.youtube.com"||uri.host=="youtu.be"))open(uri.toString())
                return true
            }
            override fun onReceivedError(view:WebView?,request:WebResourceRequest,error:WebResourceError?){
                if(request.isForMainFrame) status.text="Preview tidak dapat dimuat. Periksa internet lalu ketuk Coba Lagi. Instruksi latihan tetap dapat digunakan."
            }
        }
        val origin="https://${activity.packageName}"
        val html="""
            <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="referrer" content="strict-origin-when-cross-origin">
            <style>html,body{margin:0;width:100%;height:100%;background:#14171c}#player{width:100%;height:100%}</style></head>
            <body><div id="player"></div><script>
            var player;
            function onYouTubeIframeAPIReady(){player=new YT.Player('player',{width:'100%',height:'100%',videoId:'${video.id}',
                playerVars:{playsinline:1,autoplay:0,fs:0,origin:'$origin'},
                events:{onReady:function(){document.title='preview_ready'},onError:function(e){document.title='preview_error_'+e.data}}});}
            </script><script src="https://www.youtube.com/iframe_api" onerror="document.title='preview_error_network'"></script></body></html>
        """.trimIndent()
        web.loadDataWithBaseURL("$origin/",html,"text/html","UTF-8",null)
    }
    fun open(url:String){
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }
            .onFailure { Toast.makeText(activity,"Aplikasi pembuka YouTube tidak tersedia",Toast.LENGTH_SHORT).show() }
    }
    fun pause(){web.evaluateJavascript("if(window.player && player.pauseVideo){player.pauseVideo();}",null);web.onPause()}
    fun resume(){web.onResume()}
    fun destroy(){web.stopLoading();(web.parent as? android.view.ViewGroup)?.removeView(web);web.destroy()}
}

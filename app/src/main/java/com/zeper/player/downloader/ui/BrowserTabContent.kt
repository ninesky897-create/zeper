package com.zeper.player.downloader.ui

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class ZeperWebInterface(private val onDownload: (String) -> Unit) {
    @JavascriptInterface
    fun downloadVideo(videoUrl: String) {
        Handler(Looper.getMainLooper()).post {
            onDownload(videoUrl)
        }
    }
}

@Composable
fun BrowserTabContent(
    url: String,
    onUrlDetected: (String) -> Unit
) {
    var currentUrl by remember { mutableStateOf(url) }
    var canDownload by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    val zeperInterface = remember { ZeperWebInterface(onUrlDetected) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                    
                    addJavascriptInterface(zeperInterface, "ZeperApp")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            currentUrl = url ?: ""
                            canDownload = isDownloadable(currentUrl)
                            canGoBack = view?.canGoBack() ?: false
                            
                            injectZeperScripts(view)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = canDownload,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = { onUrlDetected(currentUrl) },
                containerColor = Color(0xFFF4B400),
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
        }
    }
}

private fun injectZeperScripts(webView: WebView?) {
    val js = """
        (function() {
            if (window.zeperInjected) return;
            window.zeperInjected = true;

            // 1. Hide the original 3-dots and login banners
            const style = document.createElement('style');
            style.innerHTML = `
                ytm-menu, .media-item-menu, [aria-label="Action menu"], .ytm-menu, .icon-button.ytm-media-item-menu-button,
                .signin-container, .signup-container, .fb_customer_chat_icon { 
                    display: none !important; 
                }
                .zeper-download-btn {
                    background: #F4B400 !important;
                    border-radius: 50%;
                    width: 36px;
                    height: 36px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-left: 8px;
                    cursor: pointer;
                    box-shadow: 0 4px 8px rgba(0,0,0,0.5);
                    flex-shrink: 0;
                    border: 2px solid white;
                }
                .zeper-download-btn svg {
                    fill: black;
                    width: 22px;
                    height: 22px;
                }
                /* YouTube Music specific tweaks */
                ytmusic-menu-renderer {
                    display: none !important;
                }
            `;
            document.head.appendChild(style);

            function addButtons() {
                const currentUrl = window.location.href;

                // --- YouTube Mobile ---
                const items = document.querySelectorAll('ytm-media-item, ytm-video-with-context-renderer, ytm-compact-video-renderer');
                items.forEach(item => {
                    if (item.querySelector('.zeper-download-btn')) return;
                    const details = item.querySelector('.details, .ytm-media-item-detail, .item-details, .compact-media-item-metadata');
                    if (details) {
                        const btn = document.createElement('div');
                        btn.className = 'zeper-download-btn';
                        btn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>';
                        btn.onclick = (e) => {
                            e.preventDefault(); e.stopPropagation();
                            const link = item.querySelector('a')?.href;
                            if (link) ZeperApp.downloadVideo(link);
                        };
                        details.appendChild(btn);
                    }
                });

                // --- Facebook Videos ---
                if (currentUrl.includes('facebook.com')) {
                    const fbVideos = document.querySelectorAll('div[data-sigil="m-video-play-button"], article, .story_body_container');
                    fbVideos.forEach(item => {
                        if (item.querySelector('.zeper-download-btn')) return;
                        const btn = document.createElement('div');
                        btn.className = 'zeper-download-btn';
                        btn.style.margin = '10px';
                        btn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>';
                        btn.onclick = (e) => {
                            e.preventDefault(); e.stopPropagation();
                            ZeperApp.downloadVideo(currentUrl);
                        };
                        item.appendChild(btn);
                    });
                }

                // --- Instagram Reels/Posts ---
                if (currentUrl.includes('instagram.com')) {
                    const igPosts = document.querySelectorAll('article, .x9f619');
                    igPosts.forEach(item => {
                        if (item.querySelector('.zeper-download-btn')) return;
                        const btn = document.createElement('div');
                        btn.className = 'zeper-download-btn';
                        btn.style.position = 'absolute';
                        btn.style.bottom = '60px';
                        btn.style.right = '10px';
                        btn.style.zIndex = '999';
                        btn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>';
                        btn.onclick = (e) => {
                            e.preventDefault(); e.stopPropagation();
                            ZeperApp.downloadVideo(currentUrl);
                        };
                        item.appendChild(btn);
                    });
                }

                // --- Inside Video Player Overlay ---
                const player = document.querySelector('.player-control-container, #player-container-id, .ytm-video-player-overlay, .player-controls-bottom');
                if (player && !player.querySelector('.zeper-player-download-btn')) {
                    const btn = document.createElement('div');
                    btn.className = 'zeper-download-btn zeper-player-download-btn';
                    btn.style.position = 'absolute';
                    btn.style.top = '15px';
                    btn.style.right = '70px';
                    btn.style.zIndex = '9999';
                    btn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>';
                    btn.onclick = (e) => {
                        e.preventDefault(); e.stopPropagation();
                        ZeperApp.downloadVideo(currentUrl);
                    };
                    player.appendChild(btn);
                }
            }

            setInterval(addButtons, 1500);
            addButtons();
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

private fun isDownloadable(url: String): Boolean {
    return url.contains("youtube.com/watch") || 
           url.contains("youtu.be/") || 
           url.contains("music.youtube.com/watch") ||
           url.contains("facebook.com") || 
           url.contains("fb.watch") ||
           url.contains("instagram.com") ||
           url.contains("tiktok.com") ||
           url.contains("vimeo.com") ||
           url.contains("dailymotion.com")
}

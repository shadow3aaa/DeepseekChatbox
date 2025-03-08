package com.shadow3.deepseekchatbox.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MessageText(
    content: String?,
    reasoningContent: String?,
    modifier: Modifier = Modifier
) {
    var isPageLoaded by remember { mutableStateOf(false) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isPageLoaded = true
                    }
                }

                loadUrl("file:///android_asset/markdown.html")
            }
        },
        update = {
            if (isPageLoaded) {
                renderContent(it, content ?: "", reasoningContent ?: "")
            }
        },
        modifier = modifier
    )
}

private fun renderContent(
    webView: WebView?,
    content: String,
    reasoningContent: String
) {
    val escapedReasoningMarkdown = reasoningContent.replace("'", "\\'").replace("\n", "\\n")
    webView?.evaluateJavascript("renderReasoningMarkdown('$escapedReasoningMarkdown');") {
        if (it != "null")
            Log.e("MessageText", "Failed to render reasoning markdown: $it")
    }

    val escapedMarkdown = content.replace("'", "\\'").replace("\n", "\\n")
    webView?.evaluateJavascript("renderMarkdown('$escapedMarkdown');") {
        if (it != "null")
            Log.e("MessageText", "Failed to render markdown: $it")
    }
}
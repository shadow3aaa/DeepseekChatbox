package com.shadow3.deepseekchatbox.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MessageText(
    content: String?,
    reasoningContent: String?,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isPageLoaded = true
                    }
                }
                loadUrl("file:///android_asset/markdown.html")
                webView = this
            }
        },
        modifier = modifier
    )

    LaunchedEffect(content, reasoningContent, isPageLoaded) {
        if (isPageLoaded) {
            renderContent(webView, content ?: "", reasoningContent ?: "")
        }
    }
}

private fun renderContent(
    webView: WebView?,
    content: String,
    reasoningContent: String
) {
    val escapedReasoningMarkdown = reasoningContent.replace("'", "\\'").replace("\n", "\\n")
    webView?.evaluateJavascript("renderReasoningMarkdown('$escapedReasoningMarkdown');") {

    }

    val escapedMarkdown = content.replace("'", "\\'").replace("\n", "\\n")
    webView?.evaluateJavascript("renderMarkdown('$escapedMarkdown');") {}
}
package com.shadow3.deepseekchatbox.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLFile

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkDown(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val state = rememberWebViewStateWithHTMLFile("markdown.html")
    val navigator = rememberWebViewNavigator()

    WebView(
        modifier = modifier,
        state = state,
        navigator = navigator,
        captureBackPresses = false
    )

    if (state.loadingState == LoadingState.Finished) {
        renderMarkdownContent(markdown, navigator)
    }

    LaunchedEffect(markdown) {
        if (state.loadingState == LoadingState.Finished) {
            renderMarkdownContent(markdown, navigator)
        }
    }
}

private fun renderMarkdownContent(markdown: String, navigator: WebViewNavigator) {
    val escapedMarkdown = markdown.replace("'", "\\'").replace("\n", "\\n")
    val jsCode = "renderMarkdown('$escapedMarkdown');"
    navigator.evaluateJavaScript(jsCode) {
        if (it != "null") {
            Log.e("MarkDown", it)
            renderMarkdownContent(markdown, navigator)
        }
    }
}
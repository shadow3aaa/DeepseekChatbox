package com.shadow3.deepseekchatbox.components

import android.annotation.SuppressLint
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
fun MessageText(
    content: String?,
    reasoningContent: String?,
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
        renderMarkdownContent(content ?: "", reasoningContent ?: "", navigator)
    }

    LaunchedEffect(content) {
        if (state.loadingState == LoadingState.Finished) {
            renderMarkdownContent(content ?: "", reasoningContent ?: "", navigator)
        }
    }
}

private fun renderMarkdownContent(
    content: String,
    reasoningContent: String,
    navigator: WebViewNavigator
) {
    val escapedReasoningMarkdown = reasoningContent.replace("'", "\\'").replace("\n", "\\n")
    navigator.evaluateJavaScript("renderReasoningMarkdown('$escapedReasoningMarkdown');")

    val escapedMarkdown = content.replace("'", "\\'").replace("\n", "\\n")
    navigator.evaluateJavaScript("renderMarkdown('$escapedMarkdown');")
}
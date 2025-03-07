package com.shadow3.deepseekchatbox.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shadow3.deepseekchatbox.Message
import com.shadow3.deepseekchatbox.R

@Composable
fun MessageCard(
    role: String,
    msg: Message,
    chatHistoryLength: Int,
    index: Int,
    regenerateResponse: () -> Unit,
    isWaiting: Boolean
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val fontSize = textStyle.fontSize
    val fontSizeDp = with(density) { fontSize.toDp() }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = msg.error?.let {
            MaterialTheme.colorScheme.errorContainer
        } ?: CardDefaults.cardColors().containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = role,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (msg.reasoningContent?.isNotBlank() == true) {
                var isExpanded by remember { mutableStateOf(false) }

                val content = @Composable {
                    Spacer(modifier = Modifier.height(4.dp))

                    MarkDown(
                        modifier = Modifier.fillMaxWidth().let {
                            if (!isExpanded) {
                                it.heightIn(min = 0.dp, max = 100.dp)
                            } else {
                                it
                            }
                        },
                        markdown = msg.reasoningContent!!,
                    )

                    AnimatedVisibility(!isExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            modifier = Modifier.clickable {
                                isExpanded = true
                            },
                            text = "Show more",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    AnimatedVisibility(isExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            modifier = Modifier.clickable {
                                isExpanded = false
                            },
                            text = "Show less",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isExpanded) {
                    Card(
                        modifier = Modifier.wrapContentSize(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        content()
                    }
                } else {
                    Card(modifier = Modifier.wrapContentSize(),
                        onClick = {
                            isExpanded = true
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                        content()
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (msg.content?.isNotBlank() == true) {
                MarkDown(
                    modifier = Modifier.fillMaxWidth(),
                    markdown = msg.content!!,
                )
            }

            if (msg.error?.isNotBlank() == true) {
                Text(
                    text = msg.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                if (index == chatHistoryLength - 1) {
                    if (role == "assistant") {
                        MessageCardIconButton(
                            modifier = Modifier.size(fontSizeDp),
                            onClick = { regenerateResponse() },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_autorenew),
                                contentDescription = "Regenerate"
                            )
                        }
                    }

                    AnimatedVisibility(isWaiting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(fontSizeDp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCardIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) = IconButton(modifier = modifier, onClick = onClick, enabled = enabled, content = content)

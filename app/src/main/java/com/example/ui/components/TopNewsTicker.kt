package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NewsItem

@Composable
fun TopNewsTicker(
    news: List<NewsItem>,
    modifier: Modifier = Modifier
) {
    if (news.isEmpty()) return

    val fullText = remember(news) {
        news.joinToString("   ◆   ") { it.text }
    }

    val annotatedString = remember(fullText) {
        buildAnnotatedString {
            val parts = fullText.split("**")
            var isBold = false
            for (part in parts) {
                if (isBold) {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            color = Color(0xFFB91C1C)
                        )
                    ) {
                        append(part)
                    }
                } else {
                    withStyle(SpanStyle(color = Color(0xFF991B1B))) {
                        append(part)
                    }
                }
                isBold = !isBold
            }
        }
    }

    val scrollState = rememberScrollState()

    // Smooth continuous auto-scroll ticker effect
    LaunchedEffect(fullText) {
        while (true) {
            val maxScroll = scrollState.maxValue
            if (maxScroll > 0) {
                scrollState.animateScrollTo(
                    value = maxScroll,
                    animationSpec = tween(
                        durationMillis = (maxScroll * 25).coerceAtLeast(4000),
                        easing = LinearEasing
                    )
                )
                scrollState.scrollTo(0)
            } else {
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEEED))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = "Notice Ticker",
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState, enabled = false)
        ) {
            Text(
                text = annotatedString,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

package com.kachat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.kachat.app.models.KaspaExplorer
import com.kachat.app.services.LinkPreviewData
import com.kachat.app.services.LinkPreviewService
import com.kachat.app.ui.theme.LocalAppColors

private val VIDEO_HOSTS = setOf("youtube.com", "www.youtube.com", "youtu.be", "m.youtube.com")

/** Rich link-preview card shown below a chat bubble's text when the message contains a link -
 *  mirrors iMessage. Renders nothing while the fetch is in flight, rather than a placeholder that
 *  could flash or look broken. If no preview data is found (a bare/broken link, or a site with no
 *  Open Graph tags), falls back to [fallbackText] if given, else renders nothing. Used by
 *  [MessageBubble] and `GroupMessageBubble` only - broadcast rooms never call this.
 *
 *  [txId] is the owning message's transaction id, for the "View in Explorer" long-press action -
 *  matches every other bubble type's identical action ([MessageBubble]'s
 *  `kaspaExplorer.txUrl(message.id)` call site). */
@Composable
fun LinkPreviewCard(
    url: String,
    txId: String,
    kaspaExplorer: KaspaExplorer = KaspaExplorer.default,
    /** Non-null only when this card is standing in for the *entire* message (nothing but a bare
     *  link, no separate text bubble shown alongside it) - shown as a plain tappable-link bubble
     *  if the fetch finds no preview data, so the message doesn't render as nothing at all. Null
     *  when used alongside a real text bubble, where showing nothing on failure is correct since
     *  the message's own text is already visible. Mirrors iOS's `LinkPreviewCardView.fallbackText`. */
    fallbackText: String? = null
) {
    var preview by remember(url) { mutableStateOf<LinkPreviewData?>(null) }
    var hasFinishedLoading by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        hasFinishedLoading = false
        preview = LinkPreviewService.fetchPreview(url)
        hasFinishedLoading = true
    }

    if (hasFinishedLoading && preview != null) {
        LinkPreviewCardContent(data = preview!!, url = url, txId = txId, kaspaExplorer = kaspaExplorer)
    } else if (hasFinishedLoading && fallbackText != null) {
        LinkPreviewFallbackBubble(text = fallbackText, url = url, txId = txId, kaspaExplorer = kaspaExplorer)
    }
}

@Composable
private fun LinkPreviewFallbackBubble(text: String, url: String, txId: String, kaspaExplorer: KaspaExplorer) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Offset.Zero) }

    Text(
        text,
        color = LocalAppColors.current.textPrimary,
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAppColors.current.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .onGloballyPositioned { coords ->
                menuAnchor = coords.positionInWindow() + Offset(0f, coords.size.height.toFloat())
            }
            .pointerInput(url) {
                detectTapGestures(
                    onLongPress = { showMenu = true },
                    onTap = { uriHandler.openUri(url) }
                )
            }
    )

    if (showMenu) {
        CenteredOptionsMenu(onDismissRequest = { showMenu = false }, anchor = menuAnchor) {
            PopupMenuRow(Icons.Default.ContentCopy, "Copy Link") {
                clipboardManager.setText(AnnotatedString(url))
                showMenu = false
            }
            HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
            PopupMenuRow(Icons.Default.Tag, "View in Explorer") {
                uriHandler.openUri(kaspaExplorer.txUrl(txId))
                showMenu = false
            }
        }
    }
}

@Composable
private fun LinkPreviewCardContent(data: LinkPreviewData, url: String, txId: String, kaspaExplorer: KaspaExplorer) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val isVideoLink = remember(url) {
        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() in VIDEO_HOSTS
    }

    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LocalAppColors.current.surface)
            .onGloballyPositioned { coords ->
                menuAnchor = coords.positionInWindow() + Offset(0f, coords.size.height.toFloat())
            }
            .pointerInput(url) {
                detectTapGestures(
                    onLongPress = { showMenu = true },
                    onTap = { uriHandler.openUri(url) }
                )
            }
    ) {
        if (data.imageUrl != null) {
            Box {
                SubcomposeAsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                if (isVideoLink) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            if (!data.title.isNullOrEmpty()) {
                Text(
                    data.title,
                    color = LocalAppColors.current.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!data.description.isNullOrEmpty()) {
                Text(
                    data.description,
                    color = LocalAppColors.current.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!data.siteName.isNullOrEmpty()) {
                Text(
                    data.siteName.uppercase(),
                    color = LocalAppColors.current.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showMenu) {
        CenteredOptionsMenu(onDismissRequest = { showMenu = false }, anchor = menuAnchor) {
            PopupMenuRow(Icons.Default.ContentCopy, "Copy Link") {
                clipboardManager.setText(AnnotatedString(url))
                showMenu = false
            }
            HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
            PopupMenuRow(Icons.Default.Tag, "View in Explorer") {
                uriHandler.openUri(kaspaExplorer.txUrl(txId))
                showMenu = false
            }
        }
    }
}

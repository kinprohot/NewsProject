// File: com/gk/news_pro/page/screen/detail_screen/NewsDetailScreen.kt
package com.gk.news_pro.page.screen.detail_screen

import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // Thêm nếu bạn dùng trong EnhancedMarkdownText cho link
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.gk.news_pro.data.model.News
import com.gk.news_pro.data.model.Comment
import com.gk.news_pro.data.repository.GeminiRepository
import com.gk.news_pro.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NewsDetailScreen(
    navController: NavController,
    news: News,
    geminiRepository: GeminiRepository,
    userRepository: UserRepository
) {
    val viewModel: NewsDetailViewModel = remember(geminiRepository, userRepository, news.article_id) {
        NewsDetailViewModel(geminiRepository, userRepository)
    }

    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentText by viewModel.commentText.collectAsState()
    val isSubmittingComment by viewModel.isSubmittingComment.collectAsState()
    val commentError by viewModel.commentError.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var expandedAiAnalysis by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = news.article_id) {
        if (news.article_id.isNotEmpty()) {
            Log.d("NewsDetailScreen", "Article ID: ${news.article_id}. Loading comments.")
            viewModel.loadComments(news.article_id)
        } else {
            Log.w("NewsDetailScreen", "Article ID is empty. Cannot load comments.")
        }
    }

    LaunchedEffect(key1 = news.link) {
        if (news.link.isNotEmpty()) {
            scope.launch {
                Log.d("NewsDetailScreen", "Analyzing news from URL: ${news.link}")
                viewModel.analyzeNewsFromUrl(news.link)
            }
        }
    }

    LaunchedEffect(key1 = commentError) {
        commentError?.let {
            Log.d("NewsDetailScreen", "Displaying comment error: $it")
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearCommentError()
        }
    }

    Scaffold(
        topBar = {
            NewsDetailAppBar(
                navController = navController,
                news = news,
                onShare = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_SUBJECT, "Bài báo: ${news.title}")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            """
                            ${news.title}
                            
                            PHÂN TÍCH AI:
                            ${aiAnalysis ?: "Chưa có phân tích"}
                            
                            Đọc bài viết gốc: ${news.link}
                            """.trimIndent()
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ bài báo"))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 72.dp), // Nâng lên để không bị che
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp), // Padding cho từng snackbar
                        shape = RoundedCornerShape(8.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        action = {
                            data.visuals.actionLabel?.let { actionLabel ->
                                TextButton(onClick = { data.performAction() }) {
                                    Text(
                                        text = actionLabel,
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    )
                                }
                            }
                        }
                    ) {
                        Text(text = data.visuals.message)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // PHẦN 1: NỘI DUNG BÀI BÁO
            item {
                if (!news.image_url.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = news.image_url,
                                error = rememberAsyncImagePainter(model = "https://via.placeholder.com/800x400?text=Image+not+available")
                            ),
                            contentDescription = news.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        ),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                )
                        )
                        if (news.source_name.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = news.source_name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (news.image_url.isNullOrEmpty()) 16.dp else 8.dp)
                ) {
                    Text(
                        text = news.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    NewsMetaInfo(news)
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    if (!news.description.isNullOrEmpty()) {
                        Text(
                            text = news.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                textAlign = TextAlign.Justify,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    if (news.link.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { uriHandler.openUri(news.link) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.5.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Đọc bài viết gốc",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = aiAnalysis != null || isLoading || error != null,
                    enter = fadeIn(animationSpec = tween(300)) +
                            expandVertically(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(300)) +
                            shrinkVertically(animationSpec = tween(500))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                        shape = RoundedCornerShape(0.dp), // Hoặc bo tròn nhẹ nếu muốn
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Màu nền khác biệt
                        tonalElevation = 2.dp // Thêm chút độ nổi
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "AI",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Phân tích AI",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { expandedAiAnalysis = !expandedAiAnalysis },
                                    enabled = aiAnalysis != null && !isLoading && error == null
                                ) {
                                    Icon(
                                        imageVector = if (expandedAiAnalysis) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (expandedAiAnalysis) "Thu gọn" else "Mở rộng",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            AnimatedVisibility(visible = expandedAiAnalysis) {
                                when {
                                    isLoading -> LoadingIndicator()
                                    error != null -> ErrorDisplay(error ?: "Lỗi không xác định")
                                    aiAnalysis != null -> {
                                        EnhancedMarkdownText(
                                            markdown = aiAnalysis!!,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PHẦN 2: BÌNH LUẬN
            item {
                CommentSectionHeader()
            }

            if (currentUser != null) {
                item {
                    CommentInputField(
                        commentText = commentText,
                        onCommentTextChanged = viewModel::onCommentTextChanged,
                        onSubmitComment = { viewModel.submitComment(news.article_id) },
                        isSubmitting = isSubmittingComment,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                item {
                    Text(
                        "Bạn cần đăng nhập để bình luận.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (comments.isEmpty()) {
                if (currentUser != null) {
                    item {
                        Text(
                            "Chưa có bình luận nào. Hãy là người đầu tiên!",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items = comments, key = { comment -> comment.commentId }) { comment ->
                    CommentItem(
                        comment = comment,
                        currentUserIsAdmin = currentUser?.admin == true,
                        onDeleteComment = {
                            viewModel.deleteCommentByAdmin(news.article_id, comment.commentId)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp)) // Spacer ở cuối
            }
        }
    }
}

@Composable
fun CommentSectionHeader(modifier: Modifier = Modifier) {
    Text(
        text = "Bình luận",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommentInputField(
    commentText: String,
    onCommentTextChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentTextChanged,
            modifier = Modifier.weight(1f),
            label = { Text("Viết bình luận...") },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            enabled = !isSubmitting,
            maxLines = 5
        )
        IconButton(
            onClick = {
                if (commentText.isNotBlank() && !isSubmitting) {
                    onSubmitComment()
                    keyboardController?.hide()
                }
            },
            enabled = commentText.isNotBlank() && !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Gửi bình luận",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserIsAdmin: Boolean,
    onDeleteComment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!comment.avatarUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = comment.avatarUrl),
                        contentDescription = "Avatar của ${comment.username}",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar mặc định",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.username.ifEmpty { "Người dùng ẩn danh" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (currentUserIsAdmin) {
                IconButton(onClick = onDeleteComment, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa bình luận",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCommentTimestamp(comment.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun formatCommentTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Vừa xong"
    return try {
        val date = Date(timestamp)
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(date)
    } catch (e: Exception) {
        "Không rõ thời gian"
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Đang phân tích...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ErrorDisplay(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun NewsMetaInfo(news: News) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetaInfoItem(
            icon = Icons.Default.Info,
            text = news.source_name.takeIf { it.isNotEmpty() } ?: "Không xác định",
            modifier = Modifier.weight(1f)
        )
        if (!news.pubDate.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(16.dp))
            MetaInfoItem(
                icon = Icons.Default.DateRange,
                text = formatDate(news.pubDate ?: ""), // Sử dụng hàm formatDate đã có
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetaInfoItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsDetailAppBar(navController: NavController, news: News, onShare: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = news.source_name.takeIf { it.isNotEmpty() } ?: "Bài báo",
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) { // Bỏ modifier size và padding để dùng default
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface // Hoặc primary
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) { // Bỏ modifier size và padding
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Chia sẻ",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface, // Hoặc surfaceColorAtElevation(3.dp)
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun EnhancedMarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val annotatedString = buildAnnotatedString {
        val paragraphs = markdown.split("\n\n")

        paragraphs.forEach { paragraph ->
            when {
                paragraph.startsWith("# ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.25.sp)) {
                        append(paragraph.removePrefix("# ").trim()); append("\n\n")
                    }
                }
                paragraph.startsWith("## ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), letterSpacing = 0.15.sp)) {
                        append(paragraph.removePrefix("## ").trim()); append("\n\n")
                    }
                }
                paragraph.startsWith("### ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))) {
                        append(paragraph.removePrefix("### ").trim()); append("\n\n")
                    }
                }
                paragraph.startsWith("> ") -> {
                    withStyle(style = ParagraphStyle(textIndent = TextIndent(firstLine = 16.sp))) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            val lines = paragraph.split("\n")
                            lines.forEach { line -> append("❝ ${line.removePrefix("> ").trim()}"); append("\n") }
                        }
                    }
                    append("\n")
                }
                paragraph.trim().startsWith("- ") || paragraph.trim().startsWith("* ") -> {
                    val items = paragraph.split("\n")
                    items.forEach { item ->
                        if (item.trim().startsWith("- ") || item.trim().startsWith("* ")) {
                            val cleanItem = item.trim().removePrefix("- ").removePrefix("* ").trim()
                            append("• ")
                            appendStyledText(cleanItem)
                            append("\n")
                        }
                    }
                    append("\n")
                }
                paragraph.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val items = paragraph.split("\n")
                    items.forEachIndexed { index, item ->
                        if (item.trim().matches(Regex("^\\d+\\.\\s.*"))) {
                            val cleanItem = item.replace(Regex("^\\d+\\.\\s"), "").trim()
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)) {
                                append("${index + 1}. ")
                            }
                            appendStyledText(cleanItem)
                            append("\n")
                        }
                    }
                    append("\n")
                }
                paragraph.startsWith("```") && paragraph.endsWith("```") -> {
                    val codeContent = paragraph.removePrefix("```").removePrefix("kotlin").removePrefix("java").removePrefix("swift").removePrefix("js").removePrefix("python").removePrefix("html").removePrefix("css").removePrefix("json").removePrefix("\n").removeSuffix("```").trim()
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = codeContent, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    }
                    append("\n\n")
                }
                else -> {
                    appendStyledText(paragraph)
                    append("\n\n")
                }
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, textAlign = TextAlign.Justify, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onBackground),
        modifier = modifier
    )
}

@Composable
private fun AnnotatedString.Builder.appendStyledText(text: String) {
    val segments = mutableListOf<TextSegment>()
    // Handle bold and italic
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    extractSegments(text, boldRegex, segments, TextSegmentType.BOLD)
    val italicRegex = Regex("\\*(.*?)\\*|_(.*?)_")
    extractSegments(text, italicRegex, segments, TextSegmentType.ITALIC)
    val codeRegex = Regex("`(.*?)`")
    extractSegments(text, codeRegex, segments, TextSegmentType.CODE)
    val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
    extractLinkSegments(text, linkRegex, segments)
    val strikethroughRegex = Regex("~~(.*?)~~")
    extractSegments(text, strikethroughRegex, segments, TextSegmentType.STRIKETHROUGH)

    segments.sortBy { it.startPos }
    var currentPos = 0
    segments.forEach { segment ->
        if (currentPos < segment.startPos) {
            append(text.substring(currentPos, segment.startPos))
        }
        when (segment.type) {
            TextSegmentType.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment.content) }
            TextSegmentType.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(segment.content) }
            TextSegmentType.CODE -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), letterSpacing = 0.sp)) { append(" ${segment.content} ") }
            TextSegmentType.LINK -> {
                pushStringAnnotation(tag = "URL", annotation = segment.url ?: "")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append(segment.content)
                }
                pop()
            }
            TextSegmentType.STRIKETHROUGH -> withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(segment.content) }
        }
        currentPos = segment.endPos
    }
    if (currentPos < text.length) {
        append(text.substring(currentPos))
    }
}

private enum class TextSegmentType { BOLD, ITALIC, CODE, LINK, STRIKETHROUGH }

private data class TextSegment(
    val startPos: Int,
    val endPos: Int,
    val content: String,
    val type: TextSegmentType,
    val url: String? = null
)

private fun extractSegments(text: String, regex: Regex, segments: MutableList<TextSegment>, type: TextSegmentType) {
    regex.findAll(text).forEach { match ->
        val content = when (type) {
            TextSegmentType.ITALIC -> match.groupValues[1].ifEmpty { match.groupValues[2] }
            else -> match.groupValues[1]
        }
        // Kiểm tra xem đoạn này có nằm trong đoạn đã được xử lý khác không (ví dụ code block)
        if (segments.none { it.startPos <= match.range.first && it.endPos >= match.range.last + 1 && it.type != type }) {
            segments.add(TextSegment(startPos = match.range.first, endPos = match.range.last + 1, content = content, type = type))
        }
    }
}

private fun extractLinkSegments(text: String, regex: Regex, segments: MutableList<TextSegment>) {
    regex.findAll(text).forEach { match ->
        val linkText = match.groupValues[1]
        val url = match.groupValues[2]
        if (segments.none { it.startPos <= match.range.first && it.endPos >= match.range.last + 1 }) {
            segments.add(TextSegment(startPos = match.range.first, endPos = match.range.last + 1, content = linkText, type = TextSegmentType.LINK, url = url))
        }
    }
}

private fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return "Không rõ ngày"
    return try {
        // Thử một vài định dạng phổ biến nếu parser gốc thất bại
        val possibleFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Định dạng phổ biến khác
            // Thêm các định dạng khác nếu cần
        )
        var parsedDate: Date? = null
        for (parser in possibleFormats) {
            try {
                parsedDate = parser.parse(dateString)
                if (parsedDate != null) break
            } catch (e: Exception) {
                // Bỏ qua và thử định dạng tiếp theo
            }
        }

        val outputFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        outputFormatter.format(parsedDate ?: return dateString) // Trả về chuỗi gốc nếu không parse được
    } catch (e: Exception) {
        Log.e("NewsDetailScreen", "Error formatting date: $dateString", e)
        dateString // Trả về chuỗi gốc nếu có lỗi
    }
}
package com.bazinox.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SocialFeedScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts = remember { mutableStateListOf<SocialPost>() }
    var caption by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("برای دیدن فید، وارد حساب شو.") }

    fun refresh() {
        scope.launch {
            busy = true
            try {
                posts.clear()
                posts.addAll(SocialRepository.loadFeed(context))
                message = if (posts.isEmpty()) "هنوز پستی منتشر نشده است؛ اولین پست را منتشر کن." else ""
            } catch (e: Exception) {
                message = e.message ?: "دریافت فید ناموفق بود."
            } finally { busy = false }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("اجتماعی بازارینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("پست‌های عمومی کاربران بازارینو")
        OutlinedTextField(
            value = caption, onValueChange = { caption = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("چه چیزی می‌خواهی به اشتراک بگذاری؟") }, minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { imagePicker.launch("image/*") }) {
                Text(if (imageUri == null) "انتخاب تصویر" else "تصویر انتخاب شد")
            }
            Button(enabled = !busy && (caption.isNotBlank() || imageUri != null), onClick = {
                scope.launch {
                    busy = true
                    try {
                        SocialRepository.createPost(context, caption, imageUri)
                        caption = ""
                        imageUri = null
                        message = "پست با موفقیت منتشر شد."
                        posts.clear()
                        posts.addAll(SocialRepository.loadFeed(context))
                    } catch (e: Exception) { message = e.message ?: "انتشار پست ناموفق بود." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "لطفاً صبر کن…" else "انتشار پست") }
            OutlinedButton(enabled = !busy, onClick = { refresh() }) { Text("تازه‌سازی") }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(posts, key = { it.id }) { post ->
                var comment by remember(post.id) { mutableStateOf("") }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("کاربر ${post.authorId.take(8)}", fontWeight = FontWeight.Bold)
                        Text(post.caption)
                        Text(post.createdAt, style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        SocialRepository.toggleLike(context, post)
                                        val index = posts.indexOfFirst { it.id == post.id }
                                        if (index >= 0) posts[index] = post.copy(likedByMe = !post.likedByMe)
                                    } catch (e: Exception) { message = e.message ?: "ثبت پسند ناموفق بود." }
                                }
                            }) { Text(if (post.likedByMe) "♥ پسندیده شد" else "♡ پسند") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        SocialRepository.follow(context, post.authorId)
                                        message = "کاربر را دنبال کردی."
                                    } catch (e: Exception) { message = e.message ?: "دنبال‌کردن ناموفق بود." }
                                }
                            }) { Text("دنبال‌کردن") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextField(value = comment, onValueChange = { comment = it }, modifier = Modifier.weight(1f), placeholder = { Text("نظر…") }, singleLine = true)
                            Button(enabled = comment.isNotBlank(), onClick = {
                                scope.launch {
                                    try {
                                        SocialRepository.addComment(context, post.id, comment)
                                        comment = ""
                                        message = "نظر ثبت شد."
                                    } catch (e: Exception) { message = e.message ?: "ثبت نظر ناموفق بود." }
                                }
                            }) { Text("ارسال") }
                        }
                    }
                }
            }
        }
    }
}

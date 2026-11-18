package com.bazinox.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("bazarino_auth", android.content.Context.MODE_PRIVATE) }
    val currentUserId = prefs.getString("user_id", null).orEmpty()
    var conversations by remember { mutableStateOf<List<BazarinoConversation>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<BazarinoMessage>>(emptyList()) }
    var listingId by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("برای دیدن گفتگوها وارد حساب شو و فهرست را تازه‌سازی کن.") }

    fun refreshConversations() {
        scope.launch {
            busy = true
            try {
                conversations = ChatRepository.conversations(context)
                if (selectedId == null && conversations.isNotEmpty()) selectedId = conversations.first().id
                status = if (conversations.isEmpty()) "هنوز گفت‌وگویی نداری. برای شروع، شناسه آگهی منتشرشده را وارد کن." else "گفت‌وگوها از سرور دریافت شدند."
            } catch (e: Exception) { status = e.message ?: "خطا در دریافت گفتگوها" }
            finally { busy = false }
        }
    }

    LaunchedEffect(Unit) { refreshConversations() }
    LaunchedEffect(selectedId) {
        val id = selectedId ?: return@LaunchedEffect
        try { messages = ChatRepository.messages(context, id) }
        catch (e: Exception) { status = e.message ?: "خطا در دریافت پیام‌ها" }
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("پیام‌رسان بازارینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("گفت‌وگوی خصوصی خریدار و فروشنده؛ پیام‌ها از Supabase دریافت و ذخیره می‌شوند.")
        OutlinedTextField(
            value = listingId, onValueChange = { listingId = it.trim() },
            modifier = Modifier.fillMaxWidth(), label = { Text("شناسه آگهی منتشرشده برای شروع گفتگو") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy && listingId.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    try {
                        selectedId = ChatRepository.startForListing(context, listingId)
                        listingId = ""
                        refreshConversations()
                        status = "گفتگو ایجاد شد."
                    } catch (e: Exception) { status = e.message ?: "ساخت گفتگو ناموفق بود." }
                    finally { busy = false }
                }
            }) { Text("شروع گفتگو") }
            OutlinedButton(enabled = !busy, onClick = { refreshConversations() }) { Text("تازه‌سازی") }
        }
        Text("گفتگوهای من", fontWeight = FontWeight.Bold)
        if (conversations.isEmpty()) Text("گفتگویی برای نمایش وجود ندارد.")
        else {
            LazyColumn(modifier = Modifier.height(116.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(conversations, key = { it.id }) { conversation ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedId = conversation.id }
                    ) {
                        Text(if (selectedId == conversation.id) "● " else "○ " + "گفتگو · ${conversation.id.take(8)}")
                    }
                }
            }
        }
        Text("پیام‌ها", fontWeight = FontWeight.Bold)
        Card(Modifier.weight(1f).fillMaxWidth()) {
            if (selectedId == null) {
                Text("ابتدا یک گفتگو را انتخاب یا ایجاد کن.", modifier = Modifier.padding(12.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val mine = message.senderId == currentUserId
                        Text(
                            text = (if (mine) "شما: " else "طرف گفتگو: ") + message.body,
                            modifier = Modifier.fillMaxWidth(),
                            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                modifier = Modifier.weight(1f), label = { Text("پیام متنی") }, maxLines = 3
            )
            Button(enabled = !busy && selectedId != null && draft.isNotBlank(), onClick = {
                val target = selectedId ?: return@Button
                val text = draft
                scope.launch {
                    busy = true
                    try {
                        ChatRepository.send(context, target, text)
                        draft = ""
                        messages = ChatRepository.messages(context, target)
                        status = "پیام در سرور ذخیره شد."
                    } catch (e: Exception) { status = e.message ?: "ارسال پیام ناموفق بود." }
                    finally { busy = false }
                }
            }) { Text("ارسال") }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(2.dp))
        Text("فعلاً پیام متنی فعال است؛ ارسال تصویر، فایل و اعلان لحظه‌ای در مرحله بعد اضافه می‌شود.", style = MaterialTheme.typography.bodySmall)
    }
}

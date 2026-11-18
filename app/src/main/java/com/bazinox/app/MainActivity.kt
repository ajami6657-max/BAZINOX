package com.bazinox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.OutlinedTextField
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BazarinoApp() }
    }
}

private data class MarketCategory(val title: String, val description: String)

private val categories = listOf(
    MarketCategory("خودرو و وسایل نقلیه", "خرید و فروش خودرو، موتور و قطعات"),
    MarketCategory("املاک", "خانه، آپارتمان، زمین و املاک تجاری"),
    MarketCategory("کالای دیجیتال", "موبایل، رایانه و لوازم الکترونیکی"),
    MarketCategory("خانه و زندگی", "مبلمان، لوازم خانگی و ابزار"),
    MarketCategory("استخدام و کاریابی", "فرصت‌های شغلی و دورکاری"),
    MarketCategory("خدمات", "خدمات فنی، آموزشی و تخصصی"),
    MarketCategory("عمده‌فروشی و تجارت", "تأمین‌کننده و خرید تجاری"),
    MarketCategory("گردشگری و اقامت", "هتل، اقامتگاه و تور"),
    MarketCategory("ورزش و سرگرمی", "لوازم ورزشی و تفریحی"),
    MarketCategory("کشاورزی و صنعت", "تجهیزات و محصولات تخصصی")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BazarinoApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("خانه", "اجتماعی", "جست‌وجو", "ثبت آگهی", "پیام‌ها", "حساب من")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.FavoriteBorder,
        Icons.Default.Search,
        Icons.Default.AddCircle,
        Icons.Default.ChatBubbleOutline,
        Icons.Default.PersonOutline
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("BAZARINO", fontWeight = FontWeight.Bold)
                            Text(
                                "بازار آنلاین سراسر ایران",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(innerPadding))
                1 -> SocialFeedScreen(Modifier.padding(innerPadding))
                2 -> InfoScreen("جست‌وجوی بازار", "جست‌وجوی آگهی‌ها در استان‌ها، شهرها و دسته‌بندی‌های مختلف.", Modifier.padding(innerPadding))
                3 -> PaidListingScreen(Modifier.padding(innerPadding))
                4 -> ChatScreen(Modifier.padding(innerPadding))
                else -> AuthScreen(Modifier.padding(innerPadding))
            }
        }
    }
}


@Composable
private fun MessagesScreen(modifier: Modifier = Modifier) {
    var draft by remember { mutableStateOf("") }
    var sentMessages by remember { mutableStateOf(listOf<String>()) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("پیام‌رسان بازارینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("گفت‌وگوی خریدار و فروشنده؛ هر گفتگو می‌تواند به یک آگهی یا سفارش متصل شود.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("گفت‌وگوی نمونه · فروشنده کالای دیجیتال", fontWeight = FontWeight.Bold)
                Text("سلام، درباره این کالا سؤال داشتم.")
                Text("سلام، بفرمایید در خدمتم.")
                sentMessages.forEach { Text("شما: $it") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f), placeholder = { Text("پیام بنویسید…") }, maxLines = 3)
                    IconButton(onClick = { if (draft.isNotBlank()) { sentMessages = sentMessages + draft.trim(); draft = "" } }) {
                        Icon(Icons.Default.Send, contentDescription = "ارسال پیام")
                    }
                }
            }
        }
        Text("امکانات بعدی: ارسال عکس و فایل، پیام صوتی، وضعیت آنلاین، پیشنهاد قیمت، گزارش و مسدودسازی.")
        Text("این صفحه فعلاً نمونه تعاملی محلی است؛ ارسال واقعی پیام به سرور و اعلان‌ها هنوز متصل نیست.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SocialScreen(modifier: Modifier = Modifier) {
    var liked by remember { mutableStateOf(setOf<Int>()) }
    var followed by remember { mutableStateOf(setOf<Int>()) }
    var comment by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf(listOf<String>()) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("بازارینو اجتماعی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("فروشگاه‌ها و کاربران را دنبال کنید، تجربه‌ها و محصولاتشان را ببینید.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("همه", "دنبال‌شده‌ها", "ویدئوها", "فروشگاه‌ها").forEach { OutlinedButton(onClick = {}) { Text(it) } }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("استوری‌های تازه", fontWeight = FontWeight.Bold)
                Text("◉ فروشگاه‌های محلی   ◉ لوازم دیجیتال   ◉ خانه و زندگی")
                Text("استوری ۲۴ ساعته و ویدئوهای کوتاه در نسخه کامل فعال می‌شوند.", style = MaterialTheme.typography.bodySmall)
            }
        }
        listOf(
            Triple(1, "فروشگاه دیجیتال", "معرفی گوشی و لوازم جانبی؛ برای دیدن جزئیات آگهی روی صفحه محصول بزنید."),
            Triple(2, "خانه و دکور", "ایده‌های تازه برای خانه؛ محصولات قابل خرید به آگهی متصل می‌شوند.")
        ).forEach { post ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(post.second, fontWeight = FontWeight.Bold)
                    Text(post.third)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { liked = if (post.first in liked) liked - post.first else liked + post.first }) {
                            Text(if (post.first in liked) "♥ پسندیدید" else "♡ پسند")
                        }
                        OutlinedButton(onClick = { followed = if (post.first in followed) followed - post.first else followed + post.first }) {
                            Text(if (post.first in followed) "دنبال می‌کنید" else "دنبال‌کردن")
                        }
                        OutlinedButton(onClick = { comment = "نظر برای " + post.second + ": " }) { Text("نظر") }
                    }
                }
            }
        }
        TextField(value = comment, onValueChange = { comment = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("نظر خود را بنویسید…") })
        Button(onClick = { if (comment.isNotBlank()) { comments = comments + comment; comment = "" } }, enabled = comment.isNotBlank()) { Text("ثبت نظر نمونه") }
        comments.forEach { Text("نظر شما: $it") }
        Text("این فید فعلاً نمونه تعاملی محلی است؛ آپلود عکس/ویدئو، فید سروری، استوری و اعلان‌ها هنوز پیاده‌سازی نشده‌اند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("به BAZARINO خوش آمدید", fontWeight = FontWeight.Bold)
                Text("بازارینو؛ بازار خریدوفروش سراسر ایران.")
                Text("محدوده شروع: ایران", style = MaterialTheme.typography.labelMedium)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تعرفه‌های بازارینو", fontWeight = FontWeight.Bold)
                Text("پلن ۱: ۳۰ روزه — ۷۵٬۰۰۰ تومان | ۵ دلار | ۵ یورو")
                Text("پلن ۲: ۶۰ روزه — ۱۵۰٬۰۰۰ تومان | ۱۰ دلار | ۱۰ یورو")
                Text("پلن ۳: ۹۰ روزه — ۲۲۵٬۰۰۰ تومان | ۱۵ دلار | ۱۵ یورو")
                Text("تعرفه‌های دلاری و یورویی ثابت هستند؛ پرداخت ارزی هنوز به درگاه بین‌المللی متصل نشده است.", style = MaterialTheme.typography.bodySmall)
                Text("کارمزد معامله آنلاین: ${RevenuePolicy.COMMISSION_BASIS_POINTS / 100}٪ از مبلغ معامله موفق")
                Text("مثال: از معامله ۱۰٬۰۰۰٬۰۰۰ تومانی، سهم بازارینو ${RevenuePolicy.commissionRial(100_000_000L) / 10} تومان است.")
            }
        }
        Text("دسته‌بندی‌های بازار", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(category.title, fontWeight = FontWeight.SemiBold)
                        Text(category.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaidListingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("کالای دیجیتال") }
    var price by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var planDays by remember { mutableIntStateOf(30) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ثبت آگهی جدید", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("مدت انتشار و تعرفه را انتخاب کن:")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30, 60, 90).forEach { days ->
                val fee = when (days) {
                    30 -> "۷۵ هزار تومان\n۵ دلار / ۵ یورو"
                    60 -> "۱۵۰ هزار تومان\n۱۰ دلار / ۱۰ یورو"
                    else -> "۲۲۵ هزار تومان\n۱۵ دلار / ۱۵ یورو"
                }
                if (planDays == days) {
                    Button(onClick = { planDays = days }, modifier = Modifier.weight(1f)) { Text("$days روز\n$fee") }
                } else {
                    OutlinedButton(onClick = { planDays = days }, modifier = Modifier.weight(1f)) { Text("$days روز\n$fee") }
                }
            }
        }
        Text("آگهی پس از تأیید پرداخت، به مدت $planDays روز منتشر می‌ماند.")
        TextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("عنوان آگهی") }, singleLine = true)
        TextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("توضیحات") }, minLines = 3)
        TextField(value = category, onValueChange = { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("دسته‌بندی") }, singleLine = true)
        TextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("قیمت به تومان (برای رایگان عدد صفر)") }, singleLine = true)
        TextField(value = province, onValueChange = { province = it }, modifier = Modifier.fillMaxWidth(), label = { Text("استان") }, singleLine = true)
        TextField(value = city, onValueChange = { city = it }, modifier = Modifier.fillMaxWidth(), label = { Text("شهر") }, singleLine = true)
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && title.isNotBlank() && description.isNotBlank() &&
                price.isNotBlank() && province.isNotBlank() && city.isNotBlank(),
            onClick = {
                val amount = price.toLongOrNull()
                if (amount == null) {
                    message = "قیمت را به‌صورت عددی وارد کن."
                } else {
                    busy = true
                    message = ""
                    scope.launch {
                        try {
                            val result = ListingRepository.createPendingListing(
                                context = context,
                                title = title,
                                description = description,
                                categorySlug = category.trim().lowercase().replace(" ", "-"),
                                priceToman = amount,
                                planDays = planDays,
                                province = province,
                                city = city
                            )
                            message = result
                            title = ""
                            description = ""
                            price = ""
                        } catch (e: Exception) {
                            message = e.message ?: "خطا در ذخیره آگهی"
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        ) {
            Text(if (busy) "در حال ذخیره…" else "ذخیره آگهی در بازارینو")
        }
        if (message.isNotBlank()) {
            Text(message, color = if (message.contains("موفقیت")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        Text(
            "وضعیت فعلی: آگهی با پلن انتخابی ذخیره می‌شود و در انتظار پرداخت می‌ماند. درگاه پرداخت هنوز وصل نیست؛ هیچ پولی دریافت نمی‌شود و آگهی تا تأیید پرداخت عمومی نخواهد شد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun InfoScreen(title: String, description: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

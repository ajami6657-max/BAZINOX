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
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BazinoxApp() }
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
private fun BazinoxApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("خانه", "جست‌وجو", "ثبت آگهی", "پیام‌ها", "حساب من")
    val icons = listOf(
        Icons.Default.Home,
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
                            Text("BAZINOX", fontWeight = FontWeight.Bold)
                            Text(
                                "بازار جهانی در دستان تو",
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
                1 -> InfoScreen("جست‌وجوی بازار", "جست‌وجوی آگهی‌ها در کشورها و دسته‌بندی‌های مختلف در مرحله توسعه است.", Modifier.padding(innerPadding))
                2 -> InfoScreen("ثبت آگهی", "فرم ثبت آگهی با تصاویر، قیمت، کشور، شهر و ویژگی‌های دسته‌بندی در مرحله توسعه است.", Modifier.padding(innerPadding))
                3 -> InfoScreen("پیام‌ها", "گفت‌وگوی خریدار و فروشنده در مرحله توسعه است.", Modifier.padding(innerPadding))
                else -> InfoScreen("حساب کاربری", "پروفایل، آگهی‌ها، علاقه‌مندی‌ها و کیف پول در مرحله توسعه هستند.", Modifier.padding(innerPadding))
            }
        }
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
                Text("به بازار جهانی BAZINOX خوش آمدید", fontWeight = FontWeight.Bold)
                Text("بازار کشور موردنظر خود را انتخاب کنید و آگهی‌ها را پیدا کنید.")
                Text("انتخاب قاره و کشور به‌زودی فعال می‌شود.", style = MaterialTheme.typography.labelMedium)
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

package app.subradar

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val SUBS_KEY = "subradar_subscriptions"
private const val SETTINGS_KEY = "subradar_settings"
private const val CHANNEL_ID = "renewal_reminders"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent { NativeSubRadar() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Subscription reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "SubRadar"
        val body = intent.getStringExtra("body") ?: "Subscription due today"
        val openIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(intent.getIntExtra("id", title.hashCode()), notification)
        }
    }
}

enum class BillingCycle { Monthly, Quarterly, Yearly, Custom }
enum class CycleUnit { Day, Week, Month, Year }
enum class AppLanguage { En, Zh }
enum class AppThemeMode { Light, Dark, Auto }
enum class Currency { CNY, USD }
enum class DisplayMode { List, Stats }
enum class HomeTab { List, Stats, Attention }
enum class SmartFilter { All, Attention, Due, LowBalance, Active, Archived }
enum class SpendingMode { Fixed, Balance, Metered, Hybrid }
enum class LedgerType { Renewal, Expense, TopUp, Refund, Adjustment, PriceChange }
enum class SubscriptionState { Active, Paused, Archived }

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: LedgerType,
    val amount: Double,
    val date: String,
    val note: String? = null,
    val balanceAfter: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class SubscriptionTemplate(
    val name: String,
    val category: String,
    val price: Double,
    val currency: Currency,
    val cycle: BillingCycle,
    val spendingMode: SpendingMode
)

data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val currency: Currency,
    val cycle: BillingCycle,
    val customCycleDuration: Int? = null,
    val customCycleUnit: CycleUnit? = null,
    val nextBillingDate: String,
    val startDate: String? = null,
    val accountBalance: Double? = null,
    val spendingMode: SpendingMode = SpendingMode.Fixed,
    val state: SubscriptionState = SubscriptionState.Active,
    val ledger: List<LedgerEntry> = emptyList(),
    val category: String = "Other",
    val notes: String? = null,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val notificationsEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.En,
    val theme: AppThemeMode = AppThemeMode.Auto,
    val reminderDaysBefore: Int = 0,
    val primaryCurrency: Currency = Currency.CNY,
    val usdToCnyRate: Double = 7.2,
    val defaultCurrency: Currency = Currency.CNY,
    val defaultCycle: BillingCycle = BillingCycle.Monthly,
    val defaultCategory: String = "Other",
    val defaultSpendingMode: SpendingMode = SpendingMode.Fixed
)

data class Copy(
    val appName: String,
    val totalMonthly: String,
    val settings: String,
    val search: String,
    val notifications: String,
    val notificationsDesc: String,
    val language: String,
    val appearance: String,
    val about: String,
    val noSubsTitle: String,
    val noSubsDesc: String,
    val dueToday: String,
    val overdue: String,
    val inDays: String,
    val newSub: String,
    val editSub: String,
    val serviceName: String,
    val price: String,
    val currency: String,
    val billingCycle: String,
    val nextBillingDate: String,
    val startDate: String,
    val accountBalance: String,
    val notes: String,
    val addImage: String,
    val save: String,
    val delete: String,
    val cancel: String,
    val confirm: String,
    val confirmDelete: String,
    val monthly: String,
    val quarterly: String,
    val yearly: String,
    val custom: String,
    val renew: String,
    val payToday: String
)

private val enCopy = Copy(
    "SubRadar", "/ mo", "Settings", "Search subscriptions", "Notifications", "Alert on due date",
    "Language", "Appearance", "Private subscription tracking stored only on this device.",
    "No subscriptions yet", "Tap + to add your first subscription.", "Due today",
    "Overdue by %d days", "In %d days", "New Subscription", "Edit Subscription", "Service Name",
    "Price", "Currency", "Billing Cycle", "Next Billing Date", "Start Date", "Balance",
    "Notes", "Add image", "Save Subscription", "Delete", "Cancel", "Confirm",
    "Delete this subscription?", "Monthly", "Quarterly", "Yearly", "Custom", "Renew", "PAY TODAY"
)

private val zhCopy = Copy(
    "SubRadar", "/ 月", "设置", "搜索订阅", "续费提醒", "在到期当天提醒",
    "语言", "外观", "隐私优先的订阅管理，数据只保存在本机。",
    "暂无订阅", "点击 + 添加你的第一个订阅。", "今天到期",
    "已逾期 %d 天", "还有 %d 天", "新建订阅", "编辑订阅", "服务名称",
    "金额", "货币", "计费周期", "下次扣费日期", "开始日期", "账户余额",
    "备注", "添加图片", "保存订阅", "删除", "取消", "确认",
    "确定删除这个订阅？", "按月", "按季", "按年", "自定义", "续费", "今天付款"
)

class SubRadarStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("subradar", Context.MODE_PRIVATE)

    fun loadSubscriptions(): List<Subscription> {
        val raw = prefs.getString(SUBS_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toSubscription() }
        }.getOrDefault(emptyList())
    }

    fun saveSubscriptions(items: List<Subscription>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString(SUBS_KEY, array.toString()).apply()
    }

    fun loadSettings(): AppSettings {
        val raw = prefs.getString(SETTINGS_KEY, null) ?: return AppSettings(language = detectLanguage())
        return runCatching {
            val json = JSONObject(raw)
            AppSettings(
                notificationsEnabled = json.optBoolean("notificationsEnabled", false),
                language = json.optString("language", detectLanguage().name).toEnum(AppLanguage.En),
                theme = json.optString("theme", AppThemeMode.Auto.name).toEnum(AppThemeMode.Auto),
                reminderDaysBefore = json.optInt("reminderDaysBefore", 0).coerceIn(0, 30),
                primaryCurrency = json.optString("primaryCurrency", Currency.CNY.name).toEnum(Currency.CNY),
                usdToCnyRate = json.optDouble("usdToCnyRate", 7.2).takeIf { it > 0.0 } ?: 7.2,
                defaultCurrency = json.optString("defaultCurrency", Currency.CNY.name).toEnum(Currency.CNY),
                defaultCycle = json.optString("defaultCycle", BillingCycle.Monthly.name).toEnum(BillingCycle.Monthly),
                defaultCategory = json.optString("defaultCategory", "Other").ifBlank { "Other" },
                defaultSpendingMode = json.optString("defaultSpendingMode", SpendingMode.Fixed.name).toEnum(SpendingMode.Fixed)
            )
        }.getOrDefault(AppSettings(language = detectLanguage()))
    }

    fun saveSettings(settings: AppSettings) {
        val json = JSONObject()
            .put("notificationsEnabled", settings.notificationsEnabled)
            .put("language", settings.language.name)
            .put("theme", settings.theme.name)
            .put("reminderDaysBefore", settings.reminderDaysBefore)
            .put("primaryCurrency", settings.primaryCurrency.name)
            .put("usdToCnyRate", settings.usdToCnyRate)
            .put("defaultCurrency", settings.defaultCurrency.name)
            .put("defaultCycle", settings.defaultCycle.name)
            .put("defaultCategory", settings.defaultCategory)
            .put("defaultSpendingMode", settings.defaultSpendingMode.name)
        prefs.edit().putString(SETTINGS_KEY, json.toString()).apply()
    }

    private fun detectLanguage(): AppLanguage {
        return if (context.resources.configuration.locales[0].language.startsWith("zh")) AppLanguage.Zh else AppLanguage.En
    }
}

private inline fun <reified T : Enum<T>> String.toEnum(default: T): T {
    return enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) } ?: default
}

private fun JSONObject.toSubscription(): Subscription {
    return Subscription(
        id = optString("id", UUID.randomUUID().toString()),
        name = optString("name"),
        price = optDouble("price", 0.0),
        currency = optString("currency", Currency.USD.name).toEnum(Currency.USD),
        cycle = optString("cycle", BillingCycle.Monthly.name).toEnum(BillingCycle.Monthly),
        customCycleDuration = if (has("customCycleDuration")) optInt("customCycleDuration") else null,
        customCycleUnit = optString("customCycleUnit", "").takeIf { it.isNotBlank() }?.toEnum(CycleUnit.Month),
        nextBillingDate = optString("nextBillingDate", LocalDate.now().toString()),
        startDate = optString("startDate", "").ifBlank { null },
        accountBalance = if (has("accountBalance")) optDouble("accountBalance") else null,
        spendingMode = optString("spendingMode", SpendingMode.Fixed.name).toEnum(SpendingMode.Fixed),
        state = optString("state", SubscriptionState.Active.name).toEnum(SubscriptionState.Active),
        ledger = optJSONArray("ledger")?.let { array ->
            List(array.length()) { index -> array.getJSONObject(index).toLedgerEntry() }
        }.orEmpty(),
        category = optString("category", "Other").ifBlank { "Other" },
        notes = optString("notes", "").ifBlank { null },
        imageUri = optString("imageUri", "").ifBlank { optString("image", "").ifBlank { null } },
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )
}

private fun Subscription.toJson(): JSONObject {
    val json = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("price", price)
        .put("currency", currency.name)
        .put("cycle", cycle.name)
        .put("nextBillingDate", nextBillingDate)
        .put("createdAt", createdAt)
    customCycleDuration?.let { json.put("customCycleDuration", it) }
    customCycleUnit?.let { json.put("customCycleUnit", it.name) }
    startDate?.let { json.put("startDate", it) }
    accountBalance?.let { json.put("accountBalance", it) }
    json.put("spendingMode", spendingMode.name)
    json.put("state", state.name)
    val ledgerArray = JSONArray()
    ledger.forEach { ledgerArray.put(it.toJson()) }
    json.put("ledger", ledgerArray)
    json.put("category", category)
    notes?.let { json.put("notes", it) }
    imageUri?.let { json.put("imageUri", it) }
    return json
}

private fun JSONObject.toLedgerEntry(): LedgerEntry {
    return LedgerEntry(
        id = optString("id", UUID.randomUUID().toString()),
        type = optString("type", LedgerType.Adjustment.name).toEnum(LedgerType.Adjustment),
        amount = optDouble("amount", 0.0),
        date = optString("date", LocalDate.now().toString()),
        note = optString("note", "").ifBlank { null },
        balanceAfter = if (has("balanceAfter")) optDouble("balanceAfter") else null,
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )
}

private fun LedgerEntry.toJson(): JSONObject {
    val json = JSONObject()
        .put("id", id)
        .put("type", type.name)
        .put("amount", amount)
        .put("date", date)
        .put("createdAt", createdAt)
    note?.let { json.put("note", it) }
    balanceAfter?.let { json.put("balanceAfter", it) }
    return json
}

private fun addCycle(date: LocalDate, cycle: BillingCycle, duration: Int?, unit: CycleUnit?): LocalDate {
    return when (cycle) {
        BillingCycle.Monthly -> date.plusMonths(1)
        BillingCycle.Quarterly -> date.plusMonths(3)
        BillingCycle.Yearly -> date.plusYears(1)
        BillingCycle.Custom -> when (unit) {
            CycleUnit.Day -> date.plusDays((duration ?: 1).toLong())
            CycleUnit.Week -> date.plusWeeks((duration ?: 1).toLong())
            CycleUnit.Month -> date.plusMonths((duration ?: 1).toLong())
            CycleUnit.Year -> date.plusYears((duration ?: 1).toLong())
            null -> date.plusMonths(1)
        }
    }
}

private fun autoRenew(items: List<Subscription>): List<Subscription> {
    return items.map { sub ->
        val next = parseDate(sub.nextBillingDate)
        if (
            sub.state == SubscriptionState.Active &&
            next != null &&
            ChronoUnit.DAYS.between(next, LocalDate.now()) >= 14
        ) {
            setSubscriptionState(sub, SubscriptionState.Paused)
        } else {
            sub
        }
    }
}

private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

private fun roundMoney(value: Double): Double = (value * 100.0).roundToLong() / 100.0

private fun isDueOrOverdue(sub: Subscription): Boolean {
    if (sub.state != SubscriptionState.Active) return false
    val next = parseDate(sub.nextBillingDate) ?: return true
    return !next.isAfter(LocalDate.now())
}

private fun isStalePending(sub: Subscription): Boolean {
    if (sub.state != SubscriptionState.Active) return false
    val next = parseDate(sub.nextBillingDate) ?: return false
    return ChronoUnit.DAYS.between(next, LocalDate.now()) >= 7
}

private fun isLowBalance(sub: Subscription): Boolean {
    if (sub.state != SubscriptionState.Active) return false
    val balance = sub.accountBalance ?: return false
    return balance < sub.price
}

private fun needsAttention(sub: Subscription): Boolean {
    if (sub.state != SubscriptionState.Active) return false
    return isDueOrOverdue(sub) || isLowBalance(sub) || isStalePending(sub) || hasSuspiciousDate(sub)
}

private fun hasSuspiciousDate(sub: Subscription): Boolean {
    val next = parseDate(sub.nextBillingDate) ?: return true
    val days = ChronoUnit.DAYS.between(LocalDate.now(), next)
    return days > 730
}

private fun matchesSmartFilter(sub: Subscription, filter: SmartFilter): Boolean {
    return when (filter) {
        SmartFilter.All -> sub.state != SubscriptionState.Archived
        SmartFilter.Attention -> needsAttention(sub)
        SmartFilter.Due -> isDueOrOverdue(sub)
        SmartFilter.LowBalance -> isLowBalance(sub)
        SmartFilter.Active -> sub.state == SubscriptionState.Active
        SmartFilter.Archived -> sub.state == SubscriptionState.Archived
    }
}

private fun issueLabels(sub: Subscription, language: AppLanguage): List<String> {
    val labels = mutableListOf<String>()
    if (isDueOrOverdue(sub)) labels += if (language == AppLanguage.Zh) "待确认续费" else "Renewal pending"
    if (isLowBalance(sub)) labels += if (language == AppLanguage.Zh) "余额不足" else "Low balance"
    if (isStalePending(sub)) labels += if (language == AppLanguage.Zh) "逾期超过 7 天" else "Overdue 7+ days"
    if (hasSuspiciousDate(sub)) labels += if (language == AppLanguage.Zh) "日期异常" else "Date looks unusual"
    return labels
}

private fun duplicateIds(items: List<Subscription>): Set<String> {
    return items
        .filter { it.state != SubscriptionState.Archived }
        .groupBy { it.name.trim().lowercase() }
        .filterKeys { it.isNotBlank() }
        .filterValues { it.size > 1 }
        .values
        .flatten()
        .map { it.id }
        .toSet()
}

private fun applyRenewal(sub: Subscription, paidDate: LocalDate, amount: Double, note: String?): Subscription {
    val previousBalance = sub.accountBalance
    val newBalance = previousBalance?.let { balance -> roundMoney(balance - amount) }
    val entry = LedgerEntry(
        type = LedgerType.Renewal,
        amount = amount,
        date = paidDate.toString(),
        note = note?.ifBlank { null },
        balanceAfter = newBalance
    )
    return sub.copy(
        state = SubscriptionState.Active,
        nextBillingDate = addCycle(paidDate, sub.cycle, sub.customCycleDuration, sub.customCycleUnit).toString(),
        accountBalance = newBalance,
        ledger = listOf(entry) + sub.ledger
    )
}

private fun applyLedger(sub: Subscription, type: LedgerType, amount: Double, date: LocalDate, note: String?): Subscription {
    val currentBalance = sub.accountBalance ?: 0.0
    val nextBalance = when (type) {
        LedgerType.Expense -> currentBalance - amount
        LedgerType.TopUp -> currentBalance + amount
        LedgerType.Refund -> currentBalance + amount
        LedgerType.Adjustment -> amount
        LedgerType.Renewal -> currentBalance - amount
        LedgerType.PriceChange -> currentBalance
    }.let(::roundMoney)
    val entry = LedgerEntry(
        type = type,
        amount = amount,
        date = date.toString(),
        note = note?.ifBlank { null },
        balanceAfter = nextBalance
    )
    val mode = if (sub.spendingMode == SpendingMode.Fixed) SpendingMode.Balance else sub.spendingMode
    return sub.copy(
        accountBalance = nextBalance,
        spendingMode = mode,
        ledger = listOf(entry) + sub.ledger
    )
}

private fun setSubscriptionState(sub: Subscription, state: SubscriptionState, date: LocalDate = LocalDate.now()): Subscription {
    val entry = LedgerEntry(
        type = LedgerType.Adjustment,
        amount = 0.0,
        date = date.toString(),
        note = "State: ${state.name}",
        balanceAfter = sub.accountBalance
    )
    return sub.copy(state = state, ledger = listOf(entry) + sub.ledger)
}

private fun withPriceChange(previous: Subscription, updated: Subscription): Subscription {
    if (roundMoney(previous.price) == roundMoney(updated.price) || previous.currency != updated.currency) return updated
    val entry = LedgerEntry(
        type = LedgerType.PriceChange,
        amount = updated.price,
        date = LocalDate.now().toString(),
        note = "${currencySymbol(previous.currency)}${"%.2f".format(previous.price)} -> ${currencySymbol(updated.currency)}${"%.2f".format(updated.price)}",
        balanceAfter = updated.accountBalance
    )
    return updated.copy(ledger = listOf(entry) + updated.ledger)
}

private fun latestPriceChange(sub: Subscription): LedgerEntry? {
    return sub.ledger.firstOrNull { it.type == LedgerType.PriceChange }
}

private fun subscriptionTemplates(isZh: Boolean): List<SubscriptionTemplate> {
    return if (isZh) {
        listOf(
            SubscriptionTemplate("ChatGPT", "AI", 20.0, Currency.USD, BillingCycle.Monthly, SpendingMode.Fixed),
            SubscriptionTemplate("iCloud", "云服务", 6.0, Currency.CNY, BillingCycle.Monthly, SpendingMode.Fixed),
            SubscriptionTemplate("云服务器", "云服务", 30.0, Currency.CNY, BillingCycle.Monthly, SpendingMode.Hybrid),
            SubscriptionTemplate("域名", "云服务", 80.0, Currency.CNY, BillingCycle.Yearly, SpendingMode.Fixed),
            SubscriptionTemplate("影音会员", "影音", 25.0, Currency.CNY, BillingCycle.Monthly, SpendingMode.Fixed),
            SubscriptionTemplate("游戏点卡", "游戏", 100.0, Currency.CNY, BillingCycle.Custom, SpendingMode.Balance)
        )
    } else {
        listOf(
            SubscriptionTemplate("ChatGPT", "AI", 20.0, Currency.USD, BillingCycle.Monthly, SpendingMode.Fixed),
            SubscriptionTemplate("iCloud", "Cloud", 0.99, Currency.USD, BillingCycle.Monthly, SpendingMode.Fixed),
            SubscriptionTemplate("Cloud server", "Cloud", 5.0, Currency.USD, BillingCycle.Monthly, SpendingMode.Hybrid),
            SubscriptionTemplate("Domain", "Cloud", 12.0, Currency.USD, BillingCycle.Yearly, SpendingMode.Fixed),
            SubscriptionTemplate("Streaming", "Media", 9.99, Currency.USD, BillingCycle.Monthly, SpendingMode.Fixed),
            SubscriptionTemplate("Game balance", "Games", 20.0, Currency.USD, BillingCycle.Custom, SpendingMode.Balance)
        )
    }
}

private fun scheduleReminders(context: Context, subscriptions: List<Subscription>, copy: Copy, daysBefore: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    subscriptions.forEach { sub ->
        cancelReminder(context, sub)
        if (sub.state != SubscriptionState.Active) return@forEach
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("title", copy.appName)
            .putExtra("body", reminderBody(sub.name, copy, daysBefore))
            .putExtra("id", sub.id.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sub.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val billingDate = parseDate(sub.nextBillingDate) ?: run {
            alarmManager.cancel(pendingIntent)
            return@forEach
        }
        val remindAt = billingDate.minusDays(daysBefore.toLong())
        val millis = remindAt
            .atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (millis > System.currentTimeMillis()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }
}

private fun reminderBody(name: String, copy: Copy, daysBefore: Int): String {
    return if (daysBefore == 0) {
        "$name: ${copy.dueToday}"
    } else {
        "$name: ${copy.inDays.format(daysBefore)}"
    }
}

private fun cancelReminder(context: Context, subscription: Subscription) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        subscription.id.hashCode(),
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

private fun cancelReminders(context: Context, subscriptions: List<Subscription>) {
    subscriptions.forEach { cancelReminder(context, it) }
}

private fun exportBackup(subscriptions: List<Subscription>, settings: AppSettings): String {
    val subs = JSONArray()
    subscriptions.forEach { subs.put(it.toJson()) }
    return JSONObject()
        .put("schemaVersion", 3)
        .put("exportedAt", System.currentTimeMillis())
        .put("subscriptions", subs)
        .put("settings", settings.toJson())
        .toString(2)
}

private fun importBackup(raw: String): Pair<List<Subscription>, AppSettings?> {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("[")) {
        val array = JSONArray(trimmed)
        List(array.length()) { index -> array.getJSONObject(index).toSubscription() } to null
    } else {
        val root = JSONObject(trimmed)
        val array = root.optJSONArray("subscriptions") ?: JSONArray()
        val settings = root.optJSONObject("settings")?.toSettings()
        List(array.length()) { index -> array.getJSONObject(index).toSubscription() } to settings
    }
}

private fun AppSettings.toJson(): JSONObject {
    return JSONObject()
        .put("notificationsEnabled", notificationsEnabled)
        .put("language", language.name)
        .put("theme", theme.name)
        .put("reminderDaysBefore", reminderDaysBefore)
        .put("primaryCurrency", primaryCurrency.name)
        .put("usdToCnyRate", usdToCnyRate)
        .put("defaultCurrency", defaultCurrency.name)
        .put("defaultCycle", defaultCycle.name)
        .put("defaultCategory", defaultCategory)
        .put("defaultSpendingMode", defaultSpendingMode.name)
}

private fun JSONObject.toSettings(): AppSettings {
    return AppSettings(
        notificationsEnabled = optBoolean("notificationsEnabled", false),
        language = optString("language", AppLanguage.En.name).toEnum(AppLanguage.En),
        theme = optString("theme", AppThemeMode.Auto.name).toEnum(AppThemeMode.Auto),
        reminderDaysBefore = optInt("reminderDaysBefore", 0).coerceIn(0, 30),
        primaryCurrency = optString("primaryCurrency", Currency.CNY.name).toEnum(Currency.CNY),
        usdToCnyRate = optDouble("usdToCnyRate", 7.2).takeIf { it > 0.0 } ?: 7.2,
        defaultCurrency = optString("defaultCurrency", Currency.CNY.name).toEnum(Currency.CNY),
        defaultCycle = optString("defaultCycle", BillingCycle.Monthly.name).toEnum(BillingCycle.Monthly),
        defaultCategory = optString("defaultCategory", "Other").ifBlank { "Other" },
        defaultSpendingMode = optString("defaultSpendingMode", SpendingMode.Fixed.name).toEnum(SpendingMode.Fixed)
    )
}

@Composable
fun NativeSubRadar() {
    val context = LocalContext.current
    val store = remember { SubRadarStore(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(store.loadSettings()) }
    val copy = if (settings.language == AppLanguage.Zh) zhCopy else enCopy
    val isDark = when (settings.theme) {
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
        AppThemeMode.Auto -> isSystemInDarkTheme()
    }
    var searchQuery by remember { mutableStateOf("") }
    var editorItem by remember { mutableStateOf<Subscription?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var displayMode by remember { mutableStateOf(DisplayMode.List) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var smartFilter by remember { mutableStateOf(SmartFilter.All) }
    var pendingUndo by remember { mutableStateOf<Pair<Int, Subscription>?>(null) }
    var renewalItem by remember { mutableStateOf<Subscription?>(null) }
    var ledgerItem by remember { mutableStateOf<Subscription?>(null) }
    var ledgerType by remember { mutableStateOf(LedgerType.Expense) }
    val subscriptions = remember { mutableStateListOf<Subscription>() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(exportBackup(subscriptions, settings).toByteArray(Charsets.UTF_8))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val (importedSubs, importedSettings) = importBackup(raw)
            subscriptions.clear()
            subscriptions.addAll(autoRenew(importedSubs))
            store.saveSubscriptions(subscriptions)
            importedSettings?.let {
                settings = it
                store.saveSettings(it)
            }
            if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
        }
    }

    LaunchedEffect(Unit) {
        val renewed = autoRenew(store.loadSubscriptions())
        subscriptions.clear()
        subscriptions.addAll(renewed)
        store.saveSubscriptions(renewed)
        if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
    }

    BackHandler(enabled = showSettings || isCreating || editorItem != null || renewalItem != null || ledgerItem != null) {
        when {
            renewalItem != null -> renewalItem = null
            ledgerItem != null -> ledgerItem = null
            isCreating || editorItem != null -> {
                isCreating = false
                editorItem = null
            }
            showSettings -> showSettings = false
        }
    }

    MiuixTheme {
        val palette = palette(isDark)
        Surface(color = palette.bg, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                MainScreen(
                    items = subscriptions
                        .filter { it.name.contains(searchQuery, ignoreCase = true) }
                        .sortedBy { it.nextBillingDate },
                    allItems = subscriptions,
                    settings = settings,
                    displayMode = displayMode,
                    categoryFilter = categoryFilter,
                    smartFilter = smartFilter,
                    copy = copy,
                    palette = palette,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onDisplayModeChange = { displayMode = it },
                    onCategoryFilterChange = { categoryFilter = it },
                    onSmartFilterChange = { smartFilter = it },
                    onOpenSettings = { showSettings = true },
                    onOpenEditor = { editorItem = it; isCreating = false },
                    onRenew = { sub -> renewalItem = sub },
                    onLedger = { sub, type ->
                        ledgerItem = sub
                        ledgerType = type
                    },
                    onStateChange = { sub, state ->
                        val index = subscriptions.indexOfFirst { it.id == sub.id }
                        if (index >= 0) subscriptions[index] = setSubscriptionState(subscriptions[index], state)
                        store.saveSubscriptions(subscriptions)
                        if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
                    },
                    onCreate = { isCreating = true; editorItem = null }
                )

                AnimatedVisibility(
                    visible = showSettings,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                ) {
                    NewSettingsScreen(
                        settings = settings,
                        copy = copy,
                        palette = palette,
                        onBack = { showSettings = false },
                        onExport = { exportLauncher.launch("subradar-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        onSettings = {
                            settings = it
                            store.saveSettings(it)
                            if (it.notificationsEnabled) {
                                scheduleReminders(context, subscriptions, copy, it.reminderDaysBefore)
                            } else {
                                cancelReminders(context, subscriptions)
                            }
                        }
                    )
                }

                SubscriptionEditorOverlay(
                    visible = isCreating || editorItem != null,
                    initial = editorItem,
                    settings = settings,
                    copy = copy,
                    palette = palette,
                    onDismiss = { isCreating = false; editorItem = null },
                    onDelete = { item ->
                        val index = subscriptions.indexOfFirst { it.id == item.id }
                        subscriptions.removeAll { it.id == item.id }
                        store.saveSubscriptions(subscriptions)
                        cancelReminder(context, item)
                        isCreating = false
                        editorItem = null
                        pendingUndo = index.coerceAtLeast(0) to item
                        scope.launch {
                            delay(4200)
                            if (pendingUndo?.second?.id == item.id) pendingUndo = null
                        }
                    },
                    onSave = { item ->
                        if (subscriptions.any { it.id == item.id }) {
                            val index = subscriptions.indexOfFirst { it.id == item.id }
                            if (index >= 0) {
                                val previous = subscriptions[index]
                                subscriptions[index] = withPriceChange(previous, item)
                            }
                        } else {
                            subscriptions.add(item)
                        }
                            settings = settings.copy(
                                defaultCurrency = item.currency,
                                defaultCycle = item.cycle,
                                defaultCategory = item.category,
                                defaultSpendingMode = item.spendingMode
                            )
                            store.saveSettings(settings)
                            val renewed = autoRenew(subscriptions)
                        subscriptions.clear()
                        subscriptions.addAll(renewed)
                        store.saveSubscriptions(subscriptions)
                        if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
                        isCreating = false
                        editorItem = null
                    }
                )
                renewalItem?.let { item ->
                    RenewalDialog(
                        sub = item,
                        copy = copy,
                        palette = palette,
                        onDismiss = { renewalItem = null },
                        onConfirm = { date, amount, note ->
                            val index = subscriptions.indexOfFirst { it.id == item.id }
                            if (index >= 0) subscriptions[index] = applyRenewal(subscriptions[index], date, amount, note)
                            store.saveSubscriptions(subscriptions)
                            if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
                            renewalItem = null
                        }
                    )
                }
                ledgerItem?.let { item ->
                    LedgerDialog(
                        sub = item,
                        type = ledgerType,
                        copy = copy,
                        palette = palette,
                        onDismiss = { ledgerItem = null },
                        onConfirm = { type, date, amount, note ->
                            val index = subscriptions.indexOfFirst { it.id == item.id }
                            if (index >= 0) subscriptions[index] = applyLedger(subscriptions[index], type, amount, date, note)
                            store.saveSubscriptions(subscriptions)
                            ledgerItem = null
                        }
                    )
                }
                pendingUndo?.let { (index, item) ->
                    MiuixSnackbar(
                        message = if (settings.language == AppLanguage.Zh) "\u5DF2\u5220\u9664" else "Deleted",
                        action = if (settings.language == AppLanguage.Zh) "\u64A4\u9500" else "Undo",
                        palette = palette,
                        onAction = {
                            subscriptions.add(index.coerceIn(0, subscriptions.size), item)
                            store.saveSubscriptions(subscriptions)
                            if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
                            pendingUndo = null
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

data class Palette(
    val bg: Color,
    val card: Color,
    val field: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val warning: Color,
    val danger: Color
)

private fun palette(isDark: Boolean) = if (isDark) {
    Palette(Color(0xFF0B0F14), Color(0xFF161B22), Color(0xFF202630), Color(0xFFF2F4F8), Color(0xFF9AA4B2), Color(0xFF2B3340), Color(0xFF4ADE80), Color(0xFF163B27), Color(0xFFF59E0B), Color(0xFFEF4444))
} else {
    Palette(Color(0xFFF7F8FA), Color.White, Color(0xFFEEF1F5), Color(0xFF111827), Color(0xFF687385), Color(0xFFE4E8EF), Color(0xFF16A34A), Color(0xFFE8F7EE), Color(0xFFD97706), Color(0xFFDC2626))
}

data class MiuixButtonColors(val containerColor: Color, val contentColor: Color)
data class MiuixTextFieldColors(
    val focusedContainerColor: Color,
    val unfocusedContainerColor: Color,
    val contentColor: Color,
    val placeholderColor: Color
)

val LocalContentColor = staticCompositionLocalOf { Color(0xFF111827) }

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val resolvedColor = if (color == Color.Unspecified) LocalContentColor.current else color
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            color = resolvedColor,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = if (tint == Color.Unspecified) LocalContentColor.current else tint
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(resolvedTint),
        modifier = modifier
    )
}

object ButtonDefaults {
    fun buttonColors(containerColor: Color, contentColor: Color): MiuixButtonColors {
        return MiuixButtonColors(containerColor, contentColor)
    }
}

object TextFieldDefaults {
    fun colors(
        focusedContainerColor: Color,
        unfocusedContainerColor: Color,
        focusedIndicatorColor: Color = Color.Transparent,
        unfocusedIndicatorColor: Color = Color.Transparent,
        contentColor: Color = Color(0xFF111827),
        placeholderColor: Color = Color(0xFF687385)
    ): MiuixTextFieldColors {
        return MiuixTextFieldColors(focusedContainerColor, unfocusedContainerColor, contentColor, placeholderColor)
    }
}

@Composable
fun Surface(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(color)) { content() }
}

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MiuixButtonColors = MiuixButtonColors(Color(0xFF16A34A), Color.White),
    content: @Composable () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (enabled) colors.containerColor else colors.containerColor.copy(alpha = 0.38f),
        label = "button background"
    )
    val contentColor = if (enabled) colors.contentColor else colors.contentColor.copy(alpha = 0.58f)
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                content()
            }
        }
    }
}

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val background by animateColorAsState(
        targetValue = if (checked) Color(0xFF16A34A) else Color(0xFFCBD5E1),
        label = "switch background"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = tween(180),
        label = "switch knob"
    )
    Box(
        Modifier
            .width(52.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    colors: MiuixTextFieldColors = MiuixTextFieldColors(Color(0xFFEEF1F5), Color(0xFFEEF1F5), Color(0xFF111827), Color(0xFF687385)),
    shape: RoundedCornerShape = RoundedCornerShape(18.dp)
) {
    Column(modifier) {
        label?.let {
            Box(Modifier.padding(start = 4.dp, bottom = 6.dp)) { it() }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.unfocusedContainerColor)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                keyboardOptions = keyboardOptions,
                textStyle = TextStyle(color = colors.contentColor, fontSize = 16.sp),
                cursorBrush = SolidColor(Color(0xFF16A34A)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        CompositionLocalProvider(LocalContentColor provides colors.placeholderColor) {
                            Box { placeholder() }
                        }
                    }
                    inner()
                }
            )
            trailingIcon?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }
    }
}

@Composable
fun MiuixConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    cancel: String,
    palette: Palette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(22.dp)
        ) {
            Text(title, color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(message, color = palette.muted)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(cancel, color = palette.muted, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onConfirm) {
                    Text(confirm, color = palette.danger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MiuixSnackbar(
    message: String,
    action: String,
    palette: Palette,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.text)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = palette.bg, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(18.dp))
        Text(
            action,
            color = palette.accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    items: List<Subscription>,
    allItems: List<Subscription>,
    settings: AppSettings,
    displayMode: DisplayMode,
    categoryFilter: String?,
    smartFilter: SmartFilter,
    copy: Copy,
    palette: Palette,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onSmartFilterChange: (SmartFilter) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditor: (Subscription) -> Unit,
    onRenew: (Subscription) -> Unit,
    onLedger: (Subscription, LedgerType) -> Unit,
    onStateChange: (Subscription, SubscriptionState) -> Unit,
    onCreate: () -> Unit
) {
    var searchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }
    val duplicateIds = duplicateIds(allItems)
    val visibleItems = items
        .filter { categoryFilter == null || it.category == categoryFilter }
        .filter { matchesSmartFilter(it, smartFilter) || (smartFilter == SmartFilter.Attention && duplicateIds.contains(it.id)) }
    val categories = allItems.map { it.category }.distinct().sorted()
    val attentionItems = allItems.filter { needsAttention(it) || duplicateIds.contains(it.id) }
    val lowBalanceCount = allItems.count { isLowBalance(it) }
    val activeTab = when {
        smartFilter == SmartFilter.Attention -> HomeTab.Attention
        displayMode == DisplayMode.Stats -> HomeTab.Stats
        else -> HomeTab.List
    }
    Box(Modifier.fillMaxSize().background(palette.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CompactHeader(
                items = allItems,
                settings = settings,
                copy = copy,
                palette = palette,
                searchQuery = searchQuery,
                searchExpanded = searchExpanded,
                onToggleSearch = {
                    searchExpanded = !searchExpanded
                    if (!searchExpanded) onSearchChange("")
                },
                onOpenSettings = onOpenSettings
            )
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically(animationSpec = tween(220), expandFrom = Alignment.Top) +
                    fadeIn(animationSpec = tween(160)) +
                    slideInVertically(animationSpec = tween(220), initialOffsetY = { -it / 3 }),
                exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) +
                    fadeOut(animationSpec = tween(120)) +
                    slideOutVertically(animationSpec = tween(180), targetOffsetY = { -it / 3 })
            ) {
                HomeSearchField(
                    value = searchQuery,
                    copy = copy,
                    palette = palette,
                    onValueChange = onSearchChange,
                    onClose = {
                        searchExpanded = false
                        onSearchChange("")
                    }
                )
            }
            CategoryFilterRow(
                categories = categories,
                categoryFilter = categoryFilter,
                settings = settings,
                palette = palette,
                onCategoryFilterChange = onCategoryFilterChange
            )
            val contentState = "${activeTab.name}:${when {
                allItems.isEmpty() -> "empty"
                visibleItems.isEmpty() -> "noResults"
                else -> "content"
            }}"
            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    val forward = tabOrder(targetState) > tabOrder(initialState)
                    val enterOffset: (Int) -> Int = { width -> if (forward) width else -width }
                    val exitOffset: (Int) -> Int = { width -> if (forward) -width else width }
                    (slideInHorizontally(animationSpec = tween(220), initialOffsetX = enterOffset) + fadeIn(animationSpec = tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(200), targetOffsetX = exitOffset) + fadeOut(animationSpec = tween(140)))
                },
                label = "main content"
            ) { state ->
                val tab = state.substringBefore(":")
                val result = state.substringAfter(":")
                when {
                    result == "empty" -> EmptyState(copy, palette)
                    result == "noResults" -> EmptyState(
                        copy = copy,
                        palette = palette,
                        title = if (settings.language == AppLanguage.Zh) "未找到订阅" else "No matching subscriptions",
                        description = if (settings.language == AppLanguage.Zh) "换个关键词试试。" else "Try another search term."
                    )
                    tab == HomeTab.Stats.name -> StatsView(visibleItems, settings, palette)
                    else -> SubscriptionList(
                        items = visibleItems,
                        settings = settings,
                        copy = copy,
                        palette = palette,
                        onOpenEditor = onOpenEditor,
                        onRenew = onRenew,
                        onLedger = onLedger,
                        onStateChange = onStateChange,
                        duplicateIds = duplicateIds
                    )
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingNavigationBar(
                activeTab = activeTab,
                attentionCount = attentionItems.size + lowBalanceCount,
                settings = settings,
                palette = palette,
                onSelectTab = { tab ->
                    when (tab) {
                        HomeTab.List -> {
                            onDisplayModeChange(DisplayMode.List)
                            if (smartFilter != SmartFilter.All) onSmartFilterChange(SmartFilter.All)
                        }
                        HomeTab.Stats -> {
                            onDisplayModeChange(DisplayMode.Stats)
                            if (smartFilter != SmartFilter.All) onSmartFilterChange(SmartFilter.All)
                        }
                        HomeTab.Attention -> {
                            onDisplayModeChange(DisplayMode.List)
                            onSmartFilterChange(SmartFilter.Attention)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            FloatingCreateButton(onCreate, palette)
        }
    }
}

private fun tabOrder(state: String): Int {
    return when (state.substringBefore(":")) {
        HomeTab.List.name -> 0
        HomeTab.Stats.name -> 1
        HomeTab.Attention.name -> 2
        else -> 0
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    categoryFilter: String?,
    settings: AppSettings,
    palette: Palette,
    onCategoryFilterChange: (String?) -> Unit
) {
    if (categories.size <= 1) return
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CycleChip(if (settings.language == AppLanguage.Zh) "\u5168\u90E8" else "All", categoryFilter == null, palette) {
            onCategoryFilterChange(null)
        }
        categories.forEach { category ->
            CycleChip(category, categoryFilter == category, palette) {
                onCategoryFilterChange(category)
            }
        }
    }
}

@Composable
fun FloatingNavigationBar(
    activeTab: HomeTab,
    attentionCount: Int,
    settings: AppSettings,
    palette: Palette,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .shadow(16.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(palette.card.copy(alpha = 0.96f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FloatingNavItem(
            icon = Icons.Rounded.CreditCard,
            label = displayModeLabel(HomeTab.List, settings.language),
            selected = activeTab == HomeTab.List,
            palette = palette,
            modifier = Modifier.weight(1f)
        ) { onSelectTab(HomeTab.List) }
        FloatingNavItem(
            icon = Icons.Rounded.Wallet,
            label = displayModeLabel(HomeTab.Stats, settings.language),
            selected = activeTab == HomeTab.Stats,
            palette = palette,
            modifier = Modifier.weight(1f)
        ) { onSelectTab(HomeTab.Stats) }
        FloatingNavItem(
            icon = Icons.Rounded.Notifications,
            label = if (settings.language == AppLanguage.Zh) "待办" else "Due",
            selected = activeTab == HomeTab.Attention,
            badge = attentionCount,
            palette = palette,
            modifier = Modifier.weight(1f),
            onClick = { onSelectTab(HomeTab.Attention) }
        )
    }
}

@Composable
fun FloatingCreateButton(onCreate: () -> Unit, palette: Palette, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(48.dp)
            .shadow(12.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(palette.accent)
            .clickable(onClick = onCreate),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, null, tint = Color.White)
    }
}

@Composable
fun FloatingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    palette: Palette,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) palette.accent else palette.muted,
        animationSpec = tween(180),
        label = "floating nav content"
    )
    val iconOffset by animateDpAsState(
        targetValue = if (selected) (-1).dp else 2.dp,
        animationSpec = tween(180),
        label = "floating nav icon offset"
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 22.dp else 20.dp,
        animationSpec = tween(180),
        label = "floating nav icon size"
    )
    val selectedBubbleWidth by animateDpAsState(
        targetValue = if (selected) 118.dp else 0.dp,
        animationSpec = tween(220),
        label = "floating nav selected bubble width"
    )
    val selectedBubbleHeight by animateDpAsState(
        targetValue = if (selected) 40.dp else 0.dp,
        animationSpec = tween(220),
        label = "floating nav selected bubble height"
    )
    val selectedBubbleColor by animateColorAsState(
        targetValue = if (selected) palette.accentSoft.copy(alpha = 0.72f) else palette.accentSoft.copy(alpha = 0f),
        animationSpec = tween(220),
        label = "floating nav selected bubble color"
    )
    Column(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.width(selectedBubbleWidth).height(selectedBubbleHeight).clip(CircleShape).background(selectedBubbleColor))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.offset(y = iconOffset), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = contentColor, modifier = Modifier.size(iconSize))
                    if (badge > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 7.dp, y = (-7).dp)
                                .height(15.dp)
                                .widthIn(min = 15.dp, max = 26.dp)
                                .clip(RoundedCornerShape(7.5.dp))
                                .background(palette.warning),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (badge > 99) "99+" else badge.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    color = contentColor,
                    fontSize = 9.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CompactHeader(
    items: List<Subscription>,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    searchQuery: String,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(palette.bg).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(copy.appName, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("${totalMonthly(items, settings.language)} ${copy.totalMonthly}", color = palette.muted, fontSize = 12.sp)
            }
            IconButton(onClick = onToggleSearch) {
                Icon(
                    if (searchExpanded || searchQuery.isNotBlank()) Icons.Rounded.Close else Icons.Rounded.Search,
                    null,
                    tint = if (searchExpanded || searchQuery.isNotBlank()) palette.accent else palette.text
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, null, tint = palette.text)
            }
        }
    }
}

@Composable
fun HomeSearchField(
    value: String,
    copy: Copy,
    palette: Palette,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(copy.search) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = palette.muted) },
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, null, tint = palette.muted)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = palette.field,
                unfocusedContainerColor = palette.field,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                contentColor = palette.text,
                placeholderColor = palette.muted
            ),
            modifier = Modifier.fillMaxWidth().heightIn(max = 50.dp)
        )
    }
}

@Composable
fun AttentionCenter(
    items: List<Subscription>,
    lowBalanceCount: Int,
    duplicateCount: Int,
    settings: AppSettings,
    palette: Palette,
    onShowAttention: () -> Unit,
    onShowLowBalance: () -> Unit
) {
    val language = settings.language
    val dueCount = items.count { isDueOrOverdue(it) }
    if (items.isEmpty() && lowBalanceCount == 0) return

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickSignalCard(
            title = if (language == AppLanguage.Zh) "待处理" else "Attention",
            value = items.size.toString(),
            subtitle = if (language == AppLanguage.Zh) "到期 $dueCount · 重复 $duplicateCount" else "Due $dueCount · dupes $duplicateCount",
            palette = palette,
            modifier = Modifier.weight(1f),
            onClick = onShowAttention
        )
        QuickSignalCard(
            title = if (language == AppLanguage.Zh) "余额不足" else "Low balance",
            value = lowBalanceCount.toString(),
            subtitle = if (language == AppLanguage.Zh) "需要充值或调整" else "Needs top-up",
            palette = palette,
            modifier = Modifier.weight(1f),
            onClick = onShowLowBalance
        )
    }
}

@Composable
fun QuickSignalCard(title: String, value: String, subtitle: String, palette: Palette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.card)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title, color = palette.muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SubscriptionList(
    items: List<Subscription>,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onOpenEditor: (Subscription) -> Unit,
    onRenew: (Subscription) -> Unit,
    onLedger: (Subscription, LedgerType) -> Unit,
    onStateChange: (Subscription, SubscriptionState) -> Unit,
    duplicateIds: Set<String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) {
            SubscriptionRow(
                sub = it,
                copy = copy,
                language = settings.language,
                palette = palette,
                onOpenEditor = onOpenEditor,
                onRenew = onRenew,
                onLedger = onLedger,
                onStateChange = onStateChange,
                isDuplicate = duplicateIds.contains(it.id)
            )
        }
    }
}

@Composable
fun StatsView(items: List<Subscription>, settings: AppSettings, palette: Palette) {
    val language = settings.language
    val activeItems = items.filter { it.state != SubscriptionState.Archived }
    val nextSeven = activeItems.count {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), parseDate(it.nextBillingDate) ?: LocalDate.now())
        days in 0..7
    }
    val annual = activeItems.sumOf { monthlyInCurrency(it, settings.primaryCurrency, settings.usdToCnyRate) * 12 }
    val byCategory = activeItems.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { monthlyInCurrency(it, settings.primaryCurrency, settings.usdToCnyRate) } }
        .entries.sortedByDescending { it.value }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = if (language == AppLanguage.Zh) "\u6708\u5747\u652F\u51FA" else "Monthly average",
                value = formatMoney(activeItems.sumOf { monthlyInCurrency(it, settings.primaryCurrency, settings.usdToCnyRate) }, settings.primaryCurrency),
                palette = palette
            )
        }
        item {
            StatCard(
                title = if (language == AppLanguage.Zh) "\u5E74\u5EA6\u9884\u4F30" else "Annual estimate",
                value = formatMoney(annual, settings.primaryCurrency),
                palette = palette
            )
        }
        item {
            StatCard(
                title = if (language == AppLanguage.Zh) "\u672A\u6765 7 \u5929\u5230\u671F" else "Due in 7 days",
                value = nextSeven.toString(),
                palette = palette
            )
        }
        items(byCategory) { entry ->
            StatCard(entry.key, formatMoney(entry.value, settings.primaryCurrency), palette)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, palette: Palette) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card)
            .padding(18.dp)
    ) {
        Text(title, color = palette.muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    }
}

@Composable
fun Header(
    items: List<Subscription>,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(palette.bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(copy.appName, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("${totalMonthly(items, settings.language)} ${copy.totalMonthly}", color = palette.muted, fontSize = 13.sp)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, null, tint = palette.text)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(copy.search) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Rounded.Close, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = palette.field,
                unfocusedContainerColor = palette.field,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                contentColor = palette.text,
                placeholderColor = palette.muted
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EmptyState(
    copy: Copy,
    palette: Palette,
    title: String = copy.noSubsTitle,
    description: String = copy.noSubsDesc
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(palette.field), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CreditCard, null, tint = palette.muted, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = palette.muted, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun SubscriptionRow(
    sub: Subscription,
    copy: Copy,
    language: AppLanguage,
    palette: Palette,
    onOpenEditor: (Subscription) -> Unit,
    onRenew: (Subscription) -> Unit,
    onLedger: (Subscription, LedgerType) -> Unit,
    onStateChange: (Subscription, SubscriptionState) -> Unit,
    isDuplicate: Boolean = false,
    modifier: Modifier = Modifier,
    showRenewAction: Boolean = true
) {
    val today = LocalDate.now()
    val next = parseDate(sub.nextBillingDate) ?: today
    val days = ChronoUnit.DAYS.between(today, next).toInt()
    val isToday = days == 0
    val isPast = days < 0
    val issues = issueLabels(sub, language).let { labels ->
        if (isDuplicate) {
            labels + if (language == AppLanguage.Zh) "疑似重复" else "Possible duplicate"
        } else {
            labels
        }
    }
    val surface = when {
        sub.state == SubscriptionState.Paused -> palette.field
        sub.state == SubscriptionState.Archived -> palette.field
        isToday -> if (palette.bg == Color(0xFF0B0F14)) Color(0xFF33230A) else Color(0xFFFFF4D6)
        isPast -> if (palette.bg == Color(0xFF0B0F14)) Color(0xFF341515) else Color(0xFFFFEBEB)
        else -> palette.card
    }
    val animatedSurface by animateColorAsState(targetValue = surface, label = "subscription surface")
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(animatedSurface)
            .clickable { onOpenEditor(sub) }
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(sub.name, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${currencySymbol(sub.currency)}${"%.2f".format(sub.price)} ${cycleText(sub, copy, language)}", color = palette.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(sub.category, color = palette.muted, fontSize = 12.sp)
                if (sub.state != SubscriptionState.Active) {
                    Text(stateLabel(sub.state, language), color = palette.warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                sub.accountBalance?.let {
                    Text("${copy.accountBalance}: ${currencySymbol(sub.currency)}${"%.2f".format(it)}", color = palette.muted, fontSize = 12.sp)
                    val coverage = if (sub.price > 0) (it / sub.price).toInt() else 0
                    val balanceHint = when {
                        it < sub.price -> if (language == AppLanguage.Zh) {
                            "余额不足 ${currencySymbol(sub.currency)}${"%.2f".format(sub.price - it)}"
                        } else {
                            "Short ${currencySymbol(sub.currency)}${"%.2f".format(sub.price - it)}"
                        }
                        else -> if (language == AppLanguage.Zh) "可覆盖 $coverage 次续费" else "Covers $coverage renewals"
                    }
                    Text(balanceHint, color = if (it < sub.price) palette.danger else palette.muted, fontSize = 12.sp)
                }
                if (sub.ledger.isNotEmpty()) {
                    Text(
                        if (language == AppLanguage.Zh) "最近：${ledgerTypeLabel(sub.ledger.first().type, language)} ${formatMoney(sub.ledger.first().amount, sub.currency)}" else "Latest: ${ledgerTypeLabel(sub.ledger.first().type, language)} ${formatMoney(sub.ledger.first().amount, sub.currency)}",
                        color = palette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                latestPriceChange(sub)?.let {
                    Text(
                        if (language == AppLanguage.Zh) "价格变更：${formatMoney(it.amount, sub.currency)}" else "Price changed: ${formatMoney(it.amount, sub.currency)}",
                        color = palette.warning,
                        fontSize = 12.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isToday) {
                    Text(copy.payToday, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(palette.warning).padding(horizontal = 8.dp, vertical = 3.dp))
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    next.format(DateTimeFormatter.ofPattern(if (language == AppLanguage.Zh) "yyyy年M月d日" else "MMM d, yyyy")),
                    color = if (isPast) palette.danger else if (isToday) palette.warning else palette.text,
                    fontWeight = FontWeight.SemiBold
                )
                val status = when {
                    sub.state == SubscriptionState.Paused -> if (language == AppLanguage.Zh) "已暂停提醒" else "Paused"
                    sub.state == SubscriptionState.Archived -> if (language == AppLanguage.Zh) "已归档" else "Archived"
                    isToday -> copy.dueToday
                    isPast -> if (language == AppLanguage.Zh) "待确认续费 · ${copy.overdue.format(abs(days))}" else "Renewal pending · ${copy.overdue.format(abs(days))}"
                    else -> copy.inDays.format(days)
                }
                Text(status, color = palette.muted, fontSize = 12.sp)
            }
        }
        if (issues.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                issues.forEach { issue -> CycleChip(issue, true, palette) {} }
            }
        }
        if (sub.imageUri != null || showRenewAction) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sub.imageUri != null) {
                    Icon(Icons.Rounded.Image, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (language == AppLanguage.Zh) "图片" else "Image", color = palette.accent, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                if (showRenewAction && sub.state == SubscriptionState.Active) {
                    TextButton(onClick = { onLedger(sub, LedgerType.Expense) }) {
                        Text(if (language == AppLanguage.Zh) "记一笔" else "Log")
                    }
                }
                if (sub.state != SubscriptionState.Active && showRenewAction) {
                    TextButton(onClick = { onStateChange(sub, SubscriptionState.Active) }) {
                        Text(if (language == AppLanguage.Zh) "恢复" else "Resume")
                    }
                }
                if ((isToday || isPast) && showRenewAction && sub.state == SubscriptionState.Active) {
                    TextButton(onClick = { onStateChange(sub, SubscriptionState.Paused) }) {
                        Text(if (language == AppLanguage.Zh) "暂停" else "Pause")
                    }
                    TextButton(onClick = { onStateChange(sub, SubscriptionState.Archived) }) {
                        Text(if (language == AppLanguage.Zh) "归档" else "Archive")
                    }
                    Button(
                        onClick = { onRenew(sub) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentSoft, contentColor = palette.accent)
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(copy.renew)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSettings: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    var rateText by remember(settings.usdToCnyRate) { mutableStateOf(settings.usdToCnyRate.toString()) }
    var defaultCategoryText by remember(settings.defaultCategory) { mutableStateOf(settings.defaultCategory) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onSettings(settings.copy(notificationsEnabled = granted))
        if (!granted && Build.VERSION.SDK_INT >= 33) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = palette.text) }
            Text(copy.settings, color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsSection(title = if (settings.language == AppLanguage.Zh) "偏好" else "Preferences", palette = palette) {
                    SettingsRow(Icons.Rounded.Palette, copy.appearance, palette) {
                        Segmented(AppThemeMode.entries, settings.theme, { themeLabel(it, settings.language) }, palette) {
                            onSettings(settings.copy(theme = it))
                        }
                    }
                    SettingsRow(Icons.Rounded.Translate, copy.language, palette) {
                        Segmented(AppLanguage.entries, settings.language, { if (it == AppLanguage.Zh) "中文" else "EN" }, palette) {
                            onSettings(settings.copy(language = it))
                        }
                    }
                }
            }
            item {
                SettingsSection(title = if (settings.language == AppLanguage.Zh) "提醒" else "Reminders", palette = palette) {
                    SettingsRow(Icons.Rounded.Notifications, copy.notifications, palette, copy.notificationsDesc) {
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSettings(settings.copy(notificationsEnabled = enabled))
                                }
                            }
                        )
                    }
                    SettingsRow(
                        Icons.Rounded.CalendarMonth,
                        if (settings.language == AppLanguage.Zh) "\u63D0\u524D\u63D0\u9192" else "Lead time",
                        palette,
                        if (settings.language == AppLanguage.Zh) "\u5230\u671F\u524D\u591A\u5C11\u5929" else "Days before renewal"
                    ) {
                        Segmented(listOf(0, 1, 3, 7), settings.reminderDaysBefore, { it.toString() }, palette) {
                            onSettings(settings.copy(reminderDaysBefore = it))
                        }
                    }
                }
            }
            item {
                SettingsSection(title = if (settings.language == AppLanguage.Zh) "货币" else "Currency", palette = palette) {
                    SettingsRow(
                        Icons.Rounded.Wallet,
                        if (settings.language == AppLanguage.Zh) "\u4E3B\u8981\u8D27\u5E01" else "Primary currency",
                        palette
                    ) {
                        Segmented(Currency.entries, settings.primaryCurrency, { currencySymbol(it) }, palette) {
                            onSettings(settings.copy(primaryCurrency = it))
                        }
                    }
                    SettingsRow(
                        Icons.Rounded.Wallet,
                        if (settings.language == AppLanguage.Zh) "\u7F8E\u5143\u6C47\u7387" else "USD rate",
                        palette,
                        "1 USD = ${settings.usdToCnyRate} CNY"
                    ) {
                        CompactNumberField(
                            value = rateText,
                            palette = palette,
                            onValueChange = {
                                rateText = it
                                it.toDoubleOrNull()?.takeIf { rate -> rate > 0.0 }?.let { rate ->
                                    onSettings(settings.copy(usdToCnyRate = rate))
                                }
                            }
                        )
                    }
                }
            }
            item {
                SettingsSection(title = if (settings.language == AppLanguage.Zh) "新建默认值" else "New defaults", palette = palette) {
                    SettingsRow(Icons.Rounded.CreditCard, if (settings.language == AppLanguage.Zh) "货币" else "Currency", palette) {
                        Segmented(Currency.entries, settings.defaultCurrency, { currencySymbol(it) }, palette) {
                            onSettings(settings.copy(defaultCurrency = it))
                        }
                    }
                    SettingsRow(Icons.Rounded.CalendarMonth, if (settings.language == AppLanguage.Zh) "周期" else "Cycle", palette) {
                        Segmented(BillingCycle.entries.filter { it != BillingCycle.Custom }, settings.defaultCycle.takeIf { it != BillingCycle.Custom } ?: BillingCycle.Monthly, { cycleShortLabel(it, settings.language) }, palette) {
                            onSettings(settings.copy(defaultCycle = it))
                        }
                    }
                    SettingsColumnRow(Icons.Rounded.Wallet, if (settings.language == AppLanguage.Zh) "消费模式" else "Mode", palette) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpendingMode.entries.forEach { mode ->
                                CycleChip(modeLabel(mode, settings.language), settings.defaultSpendingMode == mode, palette) {
                                    onSettings(settings.copy(defaultSpendingMode = mode))
                                }
                            }
                        }
                    }
                    SettingsColumnRow(Icons.Rounded.CreditCard, if (settings.language == AppLanguage.Zh) "分类" else "Category", palette) {
                        CompactTextField(
                            value = defaultCategoryText,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                            onValueChange = {
                                defaultCategoryText = it
                                onSettings(settings.copy(defaultCategory = it.ifBlank { "Other" }))
                            }
                        )
                    }
                }
            }
            item {
                SettingsSection(title = if (settings.language == AppLanguage.Zh) "数据" else "Data", palette = palette) {
                    SettingsRow(Icons.Rounded.CreditCard, if (settings.language == AppLanguage.Zh) "\u5BFC\u5165\u5907\u4EFD" else "Import backup", palette) {
                        TextButton(onClick = onImport) { Text(if (settings.language == AppLanguage.Zh) "\u5BFC\u5165" else "Import") }
                    }
                    SettingsRow(Icons.Rounded.CreditCard, if (settings.language == AppLanguage.Zh) "\u5BFC\u51FA\u5907\u4EFD" else "Export backup", palette) {
                        TextButton(onClick = onExport) { Text(if (settings.language == AppLanguage.Zh) "\u5BFC\u51FA" else "Export") }
                    }
                    SettingsRow(Icons.Rounded.CreditCard, "SubRadar v2.0.0.4", palette, copy.about) {}
                }
            }
        }
    }
}

@Composable
fun NewSettingsScreen(
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSettings: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    var rateText by remember(settings.usdToCnyRate) { mutableStateOf(settings.usdToCnyRate.toString()) }
    var defaultCategoryText by remember(settings.defaultCategory) { mutableStateOf(settings.defaultCategory) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onSettings(settings.copy(notificationsEnabled = granted))
        if (!granted && Build.VERSION.SDK_INT >= 33) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = palette.text) }
            Column(Modifier.weight(1f)) {
                Text(copy.settings, color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("SubRadar v2.0.0.4", color = palette.muted, fontSize = 12.sp)
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SettingsHeroCard(settings, palette) }
            item {
                SettingsPanelCard(
                    icon = Icons.Rounded.Palette,
                    title = if (settings.language == AppLanguage.Zh) "\u504F\u597D" else "Preferences",
                    subtitle = if (settings.language == AppLanguage.Zh) "\u8BBE\u7F6E\u754C\u9762\u548C\u8BED\u8A00" else "Appearance and language",
                    palette = palette
                ) {
                    SettingsOptionRow(
                        title = copy.appearance,
                        subtitle = if (settings.language == AppLanguage.Zh) "\u6D45\u8272\u3001\u6DF1\u8272\u6216\u81EA\u52A8" else "Light, dark, or auto",
                        palette = palette
                    ) {
                        Segmented(AppThemeMode.entries, settings.theme, { themeLabel(it, settings.language) }, palette) { onSettings(settings.copy(theme = it)) }
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(
                        title = copy.language,
                        subtitle = if (settings.language == AppLanguage.Zh) "\u5207\u6362\u7CFB\u7EDF\u663E\u793A\u8BED\u8A00" else "Switch UI language",
                        palette = palette
                    ) {
                        Segmented(AppLanguage.entries, settings.language, { if (it == AppLanguage.Zh) "\u4E2D\u6587" else "EN" }, palette) { onSettings(settings.copy(language = it)) }
                    }
                }
            }
            item {
                SettingsPanelCard(
                    icon = Icons.Rounded.Notifications,
                    title = if (settings.language == AppLanguage.Zh) "\u63D0\u9192" else "Reminders",
                    subtitle = if (settings.language == AppLanguage.Zh) "\u5230\u671F\u524D\u53CA\u65F6\u63D0\u9192" else "Notify before renewal dates",
                    palette = palette
                ) {
                    SettingsSwitchRow(title = copy.notifications, subtitle = copy.notificationsDesc, palette = palette) {
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSettings(settings.copy(notificationsEnabled = enabled))
                                }
                            }
                        )
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u63D0\u524D\u5929\u6570" else "Lead time",
                        subtitle = if (settings.language == AppLanguage.Zh) "\u8DDD\u79BB\u5230\u671F\u591A\u5C11\u5929\u901A\u77E5" else "Days before renewal",
                        palette = palette
                    ) {
                        Segmented(listOf(0, 1, 3, 7), settings.reminderDaysBefore, { it.toString() }, palette) { onSettings(settings.copy(reminderDaysBefore = it)) }
                    }
                }
            }
            item {
                SettingsPanelCard(
                    icon = Icons.Rounded.Wallet,
                    title = if (settings.language == AppLanguage.Zh) "\u8D27\u5E01" else "Currency",
                    subtitle = if (settings.language == AppLanguage.Zh) "\u7EDF\u8BA1\u53E3\u5F84\u548C\u6C47\u7387" else "Money used by stats and reminders",
                    palette = palette
                ) {
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u4E3B\u8981\u8D27\u5E01" else "Primary currency",
                        subtitle = if (settings.language == AppLanguage.Zh) "\u9996\u9875\u548C\u7EDF\u8BA1\u4F18\u5148\u4F7F\u7528" else "Used by home and stats first",
                        palette = palette
                    ) {
                        Segmented(Currency.entries, settings.primaryCurrency, { currencySymbol(it) }, palette) { onSettings(settings.copy(primaryCurrency = it)) }
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u7F8E\u5143\u6C47\u7387" else "USD rate",
                        subtitle = "1 USD = ${settings.usdToCnyRate} CNY",
                        palette = palette
                    ) {
                        CompactNumberField(
                            value = rateText,
                            palette = palette,
                            onValueChange = {
                                rateText = it
                                it.toDoubleOrNull()?.takeIf { rate -> rate > 0.0 }?.let { rate ->
                                    onSettings(settings.copy(usdToCnyRate = rate))
                                }
                            }
                        )
                    }
                }
            }
            item {
                SettingsPanelCard(
                    icon = Icons.Rounded.CreditCard,
                    title = if (settings.language == AppLanguage.Zh) "\u65B0\u5EFA\u9ED8\u8BA4\u503C" else "New defaults",
                    subtitle = if (settings.language == AppLanguage.Zh) "\u65B0\u5EFA\u8BA2\u9605\u65F6\u81EA\u52A8\u5E26\u5165" else "Applied when creating a subscription",
                    palette = palette
                ) {
                    SettingsOptionRow(title = if (settings.language == AppLanguage.Zh) "\u8D27\u5E01" else "Currency", subtitle = null, palette = palette) {
                        Segmented(Currency.entries, settings.defaultCurrency, { currencySymbol(it) }, palette) { onSettings(settings.copy(defaultCurrency = it)) }
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(title = if (settings.language == AppLanguage.Zh) "\u5468\u671F" else "Cycle", subtitle = null, palette = palette) {
                        Segmented(BillingCycle.entries.filter { it != BillingCycle.Custom }, settings.defaultCycle.takeIf { it != BillingCycle.Custom } ?: BillingCycle.Monthly, { cycleShortLabel(it, settings.language) }, palette) {
                            onSettings(settings.copy(defaultCycle = it))
                        }
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u6D88\u8D39\u6A21\u5F0F" else "Spending mode",
                        subtitle = if (settings.language == AppLanguage.Zh) "\u63A7\u5236\u9ED8\u8BA4\u8BB0\u8D26\u65B9\u5F0F" else "Controls default ledger behavior",
                        palette = palette
                    ) {
                        Segmented(SpendingMode.entries, settings.defaultSpendingMode, { modeLabel(it, settings.language) }, palette) {
                            onSettings(settings.copy(defaultSpendingMode = it))
                        }
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u5206\u7C7B" else "Category",
                        subtitle = if (settings.language == AppLanguage.Zh) "\u65B0\u8BA2\u9605\u7684\u9ED8\u8BA4\u5206\u7C7B" else "Default category for new items",
                        palette = palette,
                        alignTop = true
                    ) {
                        CompactTextField(
                            value = defaultCategoryText,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                            onValueChange = {
                                defaultCategoryText = it
                                onSettings(settings.copy(defaultCategory = it.ifBlank { "Other" }))
                            }
                        )
                    }
                }
            }
            item {
                SettingsPanelCard(
                    icon = Icons.Rounded.CreditCard,
                    title = if (settings.language == AppLanguage.Zh) "\u6570\u636E" else "Data",
                    subtitle = if (settings.language == AppLanguage.Zh) "\u5907\u4EFD\u3001\u5BFC\u5165\u548C\u7248\u672C" else "Backup and app version",
                    palette = palette
                ) {
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u5BFC\u5165\u5907\u4EFD" else "Import backup",
                        subtitle = if (settings.language == AppLanguage.Zh) "\u4ECE JSON \u6587\u4EF6\u6062\u590D\u6570\u636E" else "Restore from a JSON file",
                        palette = palette
                    ) {
                        TextButton(onClick = onImport) { Text(if (settings.language == AppLanguage.Zh) "\u5BFC\u5165" else "Import") }
                    }
                    SettingsThinDivider(palette)
                    SettingsOptionRow(
                        title = if (settings.language == AppLanguage.Zh) "\u5BFC\u51FA\u5907\u4EFD" else "Export backup",
                        subtitle = if (settings.language == AppLanguage.Zh) "\u4FDD\u5B58\u5F53\u524D\u8BBE\u7F6E\u548C\u8BA2\u9605" else "Save current settings and subscriptions",
                        palette = palette
                    ) {
                        TextButton(onClick = onExport) { Text(if (settings.language == AppLanguage.Zh) "\u5BFC\u51FA" else "Export") }
                    }
                    SettingsThinDivider(palette)
                    Text(copy.about, color = palette.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsHeroCard(settings: AppSettings, palette: Palette) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(palette.accentSoft)
            .padding(18.dp)
    ) {
        Text("SubRadar", color = palette.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (settings.language == AppLanguage.Zh) "\u4E13\u6CE8\u8BA2\u9605\u3001\u5230\u671F\u548C\u4F59\u989D\u7BA1\u7406" else "Focused subscription, renewal, and balance management",
            color = palette.muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CycleChip(if (settings.language == AppLanguage.Zh) "\u4E3B\u5E01 ${currencySymbol(settings.primaryCurrency)}" else "Primary ${currencySymbol(settings.primaryCurrency)}", true, palette) {}
            CycleChip(if (settings.notificationsEnabled) if (settings.language == AppLanguage.Zh) "\u63D0\u9192\u5DF2\u5F00" else "Alerts on" else if (settings.language == AppLanguage.Zh) "\u63D0\u9192\u5173\u95ED" else "Alerts off", settings.notificationsEnabled, palette) {}
        }
    }
}

@Composable
fun SettingsPanelCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    palette: Palette,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.card)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(palette.accentSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = palette.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun SettingsOptionRow(
    title: String,
    subtitle: String?,
    palette: Palette,
    alignTop: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                subtitle?.let { Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    palette: Palette,
    content: @Composable () -> Unit
) {
    Row(Modifier.fillMaxWidth().animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        Spacer(Modifier.width(12.dp))
        content()
    }
}

@Composable
fun LegacySettingsOptionRow(
    title: String,
    subtitle: String?,
    palette: Palette,
    alignTop: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(Modifier.fillMaxWidth().animateContentSize(), verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = palette.muted, fontSize = 12.sp) }
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1.05f), contentAlignment = Alignment.CenterEnd) {
            content()
        }
    }
}

@Composable
fun SettingsThinDivider(palette: Palette) {
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.field))
    Spacer(Modifier.height(14.dp))
}

@Composable
fun SettingsColumnRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    palette: Palette,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(palette.accentSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = palette.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                subtitle?.let { Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    palette: Palette,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card)
            .animateContentSize()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(palette.accentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = palette.accent)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            subtitle?.let { Text(it, color = palette.muted, fontSize = 13.sp) }
        }
        content()
    }
}

@Composable
fun SettingsSection(title: String, palette: Palette, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = palette.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.card)
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    palette: Palette,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(palette.accentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            subtitle?.let { Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Spacer(Modifier.width(12.dp))
        Box(contentAlignment = Alignment.CenterEnd) {
            content()
        }
    }
}

@Composable
fun CompactNumberField(value: String, palette: Palette, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = palette.field,
            unfocusedContainerColor = palette.field,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            contentColor = palette.text,
            placeholderColor = palette.muted
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.width(96.dp)
    )
}

@Composable
fun CompactTextField(value: String, palette: Palette, modifier: Modifier = Modifier.width(130.dp), onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = palette.field,
            unfocusedContainerColor = palette.field,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            contentColor = palette.text,
            placeholderColor = palette.muted
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}

@Composable
fun <T> Segmented(values: List<T>, current: T, label: (T) -> String, palette: Palette, onSelect: (T) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().clip(CircleShape).background(palette.field).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        values.forEach { value ->
            val selected = value == current
            val contentColor by animateColorAsState(
                targetValue = if (selected) palette.accent else palette.muted,
                animationSpec = tween(180),
                label = "segment content"
            )
            val bubbleWidth by animateDpAsState(
                targetValue = if (selected) 240.dp else 0.dp,
                animationSpec = tween(220),
                label = "segment bubble width"
            )
            val bubbleHeight by animateDpAsState(
                targetValue = if (selected) 34.dp else 0.dp,
                animationSpec = tween(220),
                label = "segment bubble height"
            )
            val bubbleColor by animateColorAsState(
                targetValue = if (selected) palette.accentSoft.copy(alpha = 0.78f) else palette.accentSoft.copy(alpha = 0f),
                animationSpec = tween(220),
                label = "segment bubble color"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(value) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.width(bubbleWidth).height(bubbleHeight).clip(CircleShape).background(bubbleColor))
                Text(
                    label(value),
                    color = contentColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionEditorOverlay(
    visible: Boolean,
    initial: Subscription?,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onDismiss: () -> Unit,
    onDelete: (Subscription) -> Unit,
    onSave: (Subscription) -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(120))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(240),
                initialOffsetY = { it / 2 }
            ) + fadeIn(animationSpec = tween(120)),
            exit = slideOutVertically(
                animationSpec = tween(180),
                targetOffsetY = { it / 2 }
            ) + fadeOut(animationSpec = tween(100))
        ) {
            SubscriptionEditorSheet(
                initial = initial,
                settings = settings,
                copy = copy,
                palette = palette,
                onDismiss = onDismiss,
                onDelete = onDelete,
                onSave = onSave
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionEditorSheet(
    initial: Subscription?,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onDismiss: () -> Unit,
    onDelete: (Subscription) -> Unit,
    onSave: (Subscription) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var price by remember { mutableStateOf(initial?.price?.toString().orEmpty()) }
    var currency by remember { mutableStateOf(initial?.currency ?: settings.defaultCurrency) }
    var cycle by remember { mutableStateOf(initial?.cycle ?: settings.defaultCycle) }
    var customDuration by remember { mutableStateOf((initial?.customCycleDuration ?: 1).toString()) }
    var customUnit by remember { mutableStateOf(initial?.customCycleUnit ?: CycleUnit.Month) }
    var nextBillingDate by remember { mutableStateOf(initial?.nextBillingDate ?: LocalDate.now().toString()) }
    var startDate by remember { mutableStateOf(initial?.startDate.orEmpty()) }
    var balance by remember { mutableStateOf(initial?.accountBalance?.toString().orEmpty()) }
    var spendingMode by remember { mutableStateOf(initial?.spendingMode ?: settings.defaultSpendingMode) }
    var subscriptionState by remember { mutableStateOf(initial?.state ?: SubscriptionState.Active) }
    var category by remember { mutableStateOf(initial?.category ?: settings.defaultCategory) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var imageUri by remember { mutableStateOf(initial?.imageUri) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingDateForStart by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    val formMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
    val parsedPrice = price.toDoubleOrNull()
    val parsedDuration = customDuration.toIntOrNull()
    val canSave = name.trim().isNotEmpty() &&
        parsedPrice != null &&
        parsedPrice > 0.0 &&
        (cycle != BillingCycle.Custom || (parsedDuration != null && parsedDuration > 0))
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri?.let { persistImage(context, it) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = sheetMaxHeight)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(palette.card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .navigationBarsPadding()
    ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(palette.muted.copy(alpha = 0.28f))
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = palette.text) }
                Text(
                    if (initial == null) copy.newSub else copy.editSub,
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                if (initial != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, null, tint = palette.danger) }
            }
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 0.dp, 18.dp, 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = formMaxHeight)
            ) {
                if (initial == null) {
                    item {
                        Column {
                            Text(if (copy === zhCopy) "快速模板" else "Quick templates", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                subscriptionTemplates(copy === zhCopy).forEach { template ->
                                    CycleChip(template.name, false, palette) {
                                        name = template.name
                                        price = template.price.toString()
                                        currency = template.currency
                                        cycle = template.cycle
                                        category = template.category
                                        spendingMode = template.spendingMode
                                    }
                                }
                            }
                        }
                    }
                }
                item { Field(name, { name = it }, copy.serviceName, palette) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Segmented(Currency.entries, currency, { currencySymbol(it) }, palette, { currency = it })
                        Field(price, { price = it }, copy.price, palette, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CycleChip(copy.monthly, cycle == BillingCycle.Monthly, palette) { cycle = BillingCycle.Monthly }
                        CycleChip(copy.quarterly, cycle == BillingCycle.Quarterly, palette) { cycle = BillingCycle.Quarterly }
                        CycleChip(copy.yearly, cycle == BillingCycle.Yearly, palette) { cycle = BillingCycle.Yearly }
                        CycleChip(copy.custom, cycle == BillingCycle.Custom, palette) { cycle = BillingCycle.Custom }
                    }
                }
                if (cycle == BillingCycle.Custom) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Field(
                                customDuration,
                                { customDuration = it.filter(Char::isDigit) },
                                if (copy === zhCopy) "每" else "Every",
                                palette,
                                KeyboardType.Number,
                                Modifier.weight(1f)
                            )
                            Segmented(CycleUnit.entries, customUnit, { cycleUnitText(it, if (copy === zhCopy) AppLanguage.Zh else AppLanguage.En) }, palette) { customUnit = it }
                        }
                    }
                }
                item {
                    DateButton(
                        label = copy.nextBillingDate,
                        value = nextBillingDate,
                        palette = palette,
                        onClick = {
                            pickingDateForStart = false
                            showDatePicker = true
                        }
                    )
                }
                item {
                    DateButton(
                        label = copy.startDate,
                        value = startDate.ifBlank { "-" },
                        palette = palette,
                        onClick = {
                            pickingDateForStart = true
                            showDatePicker = true
                        },
                        clearText = if (copy === zhCopy) "\u6E05\u9664" else "Clear",
                        onClear = if (startDate.isBlank()) null else ({ startDate = "" })
                    )
                }
                item { Field(balance, { balance = it }, copy.accountBalance, palette, KeyboardType.Decimal) }
                item {
                    Column {
                        Text(if (copy === zhCopy) "消费模式" else "Spending mode", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpendingMode.entries.forEach { mode ->
                                CycleChip(modeLabel(mode, if (copy === zhCopy) AppLanguage.Zh else AppLanguage.En), spendingMode == mode, palette) {
                                    spendingMode = mode
                                }
                            }
                        }
                    }
                }
                item {
                    Column {
                        Text(if (copy === zhCopy) "订阅状态" else "Subscription state", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubscriptionState.entries.forEach { state ->
                                CycleChip(stateLabel(state, if (copy === zhCopy) AppLanguage.Zh else AppLanguage.En), subscriptionState == state, palette) {
                                    subscriptionState = state
                                }
                            }
                        }
                    }
                }
                item {
                    Field(
                        value = category,
                        onValueChange = { category = it },
                        label = if (copy === zhCopy) "\u5206\u7C7B" else "Category",
                        palette = palette
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryPresets(copy === zhCopy).forEach { preset ->
                            CycleChip(preset, category == preset, palette) { category = preset }
                        }
                    }
                }
                item { Field(notes, { notes = it }, copy.notes, palette, minLines = 3) }
                if (!initial?.ledger.isNullOrEmpty()) {
                    item {
                        LedgerHistory(
                            entries = initial.ledger,
                            currency = currency,
                            language = if (copy === zhCopy) AppLanguage.Zh else AppLanguage.En,
                            palette = palette
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        imageUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(82.dp).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Button(onClick = { imageLauncher.launch("image/*") }) {
                            Icon(Icons.Rounded.Image, null)
                            Spacer(Modifier.width(8.dp))
                            Text(copy.addImage)
                        }
                        if (imageUri != null) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { imageUri = null }) {
                                Text(if (copy === zhCopy) "移除" else "Remove")
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            val validPrice = parsedPrice ?: return@Button
                            onSave(
                                Subscription(
                                    id = initial?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    price = validPrice,
                                    currency = currency,
                                    cycle = cycle,
                                    customCycleDuration = parsedDuration.takeIf { cycle == BillingCycle.Custom },
                                    customCycleUnit = customUnit.takeIf { cycle == BillingCycle.Custom },
                                    nextBillingDate = nextBillingDate,
                                    startDate = startDate.ifBlank { null },
                                    accountBalance = balance.toDoubleOrNull(),
                                    spendingMode = spendingMode,
                                    state = subscriptionState,
                                    ledger = initial?.ledger.orEmpty(),
                                    category = category.trim().ifBlank { "Other" },
                                    notes = notes.ifBlank { null },
                                    imageUri = imageUri,
                                    createdAt = initial?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text(copy.save, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    if (showDatePicker) {
        LaunchedEffect(showDatePicker, pickingDateForStart) {
            val initialDate = parseDate(
                if (pickingDateForStart) startDate.ifBlank { LocalDate.now().toString() } else nextBillingDate
            ) ?: LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val date = LocalDate.of(year, month + 1, day).toString()
                    if (pickingDateForStart) startDate = date else nextBillingDate = date
                    showDatePicker = false
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth
            ).apply {
                setOnCancelListener { showDatePicker = false }
                setOnDismissListener { showDatePicker = false }
            }.show()
        }
    }

    if (confirmDelete && initial != null) {
        MiuixConfirmDialog(
            title = copy.delete,
            message = copy.confirmDelete,
            confirm = copy.confirm,
            cancel = copy.cancel,
            palette = palette,
            onConfirm = { onDelete(initial) },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    palette: Palette,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = palette.field,
            unfocusedContainerColor = palette.field,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            contentColor = palette.text,
            placeholderColor = palette.muted
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    )
}

@Composable
fun CycleChip(text: String, selected: Boolean, palette: Palette, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) palette.accent else palette.text,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.accentSoft else palette.field)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
fun DateButton(
    label: String,
    value: String,
    palette: Palette,
    onClick: () -> Unit,
    clearText: String = "Clear",
    onClear: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.field).clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = palette.muted)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = palette.muted, fontSize = 12.sp)
            Text(value, color = palette.text, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        onClear?.let {
            TextButton(onClick = it) {
                Text(clearText)
            }
        }
    }
}

@Composable
fun RenewalDialog(
    sub: Subscription,
    copy: Copy,
    palette: Palette,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Double, String?) -> Unit
) {
    val language = if (copy === zhCopy) AppLanguage.Zh else AppLanguage.En
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var amountText by remember { mutableStateOf(sub.price.toString()) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull()

    LedgerPanel(
        title = if (language == AppLanguage.Zh) "确认续费" else "Confirm renewal",
        palette = palette,
        onDismiss = onDismiss
    ) {
        Text(sub.name, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))
        DateButton(
            label = if (language == AppLanguage.Zh) "实际续费日期" else "Actual renewal date",
            value = dateText,
            palette = palette,
            onClick = { showDatePicker = true }
        )
        Spacer(Modifier.height(10.dp))
        Field(amountText, { amountText = it }, if (language == AppLanguage.Zh) "实际付款金额" else "Actual amount", palette, KeyboardType.Decimal)
        Spacer(Modifier.height(10.dp))
        Field(note, { note = it }, if (language == AppLanguage.Zh) "备注" else "Note", palette)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(parseDate(dateText) ?: LocalDate.now(), amount ?: sub.price, note) },
            enabled = amount != null && amount > 0.0,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == AppLanguage.Zh) "确认并计算下次日期" else "Confirm and update next date", fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        PlatformDatePicker(
            initial = parseDate(dateText) ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDate = {
                dateText = it.toString()
                showDatePicker = false
            }
        )
    }
}

@Composable
fun LedgerDialog(
    sub: Subscription,
    type: LedgerType,
    copy: Copy,
    palette: Palette,
    onDismiss: () -> Unit,
    onConfirm: (LedgerType, LocalDate, Double, String?) -> Unit
) {
    val language = if (copy === zhCopy) AppLanguage.Zh else AppLanguage.En
    var selectedType by remember { mutableStateOf(type) }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull()

    LedgerPanel(
        title = if (language == AppLanguage.Zh) "记一笔" else "Log transaction",
        palette = palette,
        onDismiss = onDismiss
    ) {
        Text(sub.name, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(LedgerType.Expense, LedgerType.TopUp, LedgerType.Refund, LedgerType.Adjustment).forEach { item ->
                CycleChip(ledgerTypeLabel(item, language), selectedType == item, palette) { selectedType = item }
            }
        }
        Spacer(Modifier.height(10.dp))
        DateButton(
            label = if (language == AppLanguage.Zh) "发生日期" else "Date",
            value = dateText,
            palette = palette,
            onClick = { showDatePicker = true }
        )
        Spacer(Modifier.height(10.dp))
        Field(
            amountText,
            { amountText = it },
            if (selectedType == LedgerType.Adjustment) {
                if (language == AppLanguage.Zh) "调整后的余额" else "Balance after adjustment"
            } else {
                if (language == AppLanguage.Zh) "金额" else "Amount"
            },
            palette,
            KeyboardType.Decimal
        )
        Spacer(Modifier.height(10.dp))
        Field(note, { note = it }, if (language == AppLanguage.Zh) "备注" else "Note", palette)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(selectedType, parseDate(dateText) ?: LocalDate.now(), amount ?: 0.0, note) },
            enabled = amount != null && amount >= 0.0,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == AppLanguage.Zh) "保存记录" else "Save transaction", fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        PlatformDatePicker(
            initial = parseDate(dateText) ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDate = {
                dateText = it.toString()
                showDatePicker = false
            }
        )
    }
}

@Composable
fun LedgerPanel(title: String, palette: Palette, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = palette.text) }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun PlatformDatePicker(initial: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(initial) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            setOnCancelListener { onDismiss() }
            setOnDismissListener { onDismiss() }
        }.show()
    }
}

@Composable
fun LedgerHistory(entries: List<LedgerEntry>, currency: Currency, language: AppLanguage, palette: Palette) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.field).padding(14.dp)) {
        Text(if (language == AppLanguage.Zh) "最近记录" else "Recent history", color = palette.text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        entries.take(5).forEach { entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ledgerTypeLabel(entry.type, language), color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(entry.note ?: entry.date, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(entry.amount, currency), color = palette.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    entry.balanceAfter?.let { Text(if (language == AppLanguage.Zh) "余额 ${formatMoney(it, currency)}" else "Balance ${formatMoney(it, currency)}", color = palette.muted, fontSize = 12.sp) }
                }
            }
        }
    }
}

private fun persistImage(context: Context, uri: Uri): String? {
    return runCatching {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.toURI().toString()
    }.getOrNull()
}

private fun currencySymbol(currency: Currency) = if (currency == Currency.CNY) "¥" else "$"

private fun cycleText(sub: Subscription, copy: Copy, language: AppLanguage): String {
    return when (sub.cycle) {
        BillingCycle.Monthly -> "/ ${if (language == AppLanguage.Zh) "月" else "mo"}"
        BillingCycle.Quarterly -> "/ ${if (language == AppLanguage.Zh) "季" else "qtr"}"
        BillingCycle.Yearly -> "/ ${if (language == AppLanguage.Zh) "年" else "yr"}"
        BillingCycle.Custom -> "/ ${sub.customCycleDuration ?: 1} ${cycleUnitText(sub.customCycleUnit, language)}"
    }
}

private fun cycleShortLabel(cycle: BillingCycle, language: AppLanguage): String {
    return when (cycle) {
        BillingCycle.Monthly -> if (language == AppLanguage.Zh) "月" else "Mo"
        BillingCycle.Quarterly -> if (language == AppLanguage.Zh) "季" else "Qtr"
        BillingCycle.Yearly -> if (language == AppLanguage.Zh) "年" else "Yr"
        BillingCycle.Custom -> if (language == AppLanguage.Zh) "自定" else "Custom"
    }
}

private fun totalMonthly(items: List<Subscription>, language: AppLanguage): String {
    var cny = 0.0
    var usd = 0.0
    items.filter { it.state != SubscriptionState.Archived }.forEach { sub ->
        val monthly = when (sub.cycle) {
            BillingCycle.Monthly -> sub.price
            BillingCycle.Quarterly -> sub.price / 3
            BillingCycle.Yearly -> sub.price / 12
            BillingCycle.Custom -> {
                val duration = sub.customCycleDuration ?: 1
                val days = when (sub.customCycleUnit) {
                    CycleUnit.Day -> duration
                    CycleUnit.Week -> duration * 7
                    CycleUnit.Month -> duration * 30
                    CycleUnit.Year -> duration * 365
                    null -> 30
                }
                sub.price / days * 30
            }
        }
        if (sub.currency == Currency.CNY) cny += monthly else usd += monthly
    }
    val parts = buildList {
        if (cny > 0) add("¥${cny.toInt()}")
        if (usd > 0) add("$${usd.toInt()}")
    }
    return if (parts.isEmpty()) if (language == AppLanguage.Zh) "~ ¥0" else "~ $0" else "~ ${parts.joinToString(" + ")}"
}

private fun cycleUnitText(unit: CycleUnit?, language: AppLanguage): String {
    return when (unit) {
        CycleUnit.Day -> if (language == AppLanguage.Zh) "天" else "day"
        CycleUnit.Week -> if (language == AppLanguage.Zh) "周" else "week"
        CycleUnit.Month -> if (language == AppLanguage.Zh) "月" else "month"
        CycleUnit.Year -> if (language == AppLanguage.Zh) "年" else "year"
        null -> if (language == AppLanguage.Zh) "月" else "month"
    }
}

private fun themeLabel(theme: AppThemeMode, language: AppLanguage): String {
    return when (theme) {
        AppThemeMode.Light -> if (language == AppLanguage.Zh) "明亮" else "Light"
        AppThemeMode.Dark -> if (language == AppLanguage.Zh) "深色" else "Dark"
        AppThemeMode.Auto -> if (language == AppLanguage.Zh) "自动" else "Auto"
    }
}

private fun displayModeLabel(mode: HomeTab, language: AppLanguage): String {
    return when (mode) {
        HomeTab.List -> if (language == AppLanguage.Zh) "\u5217\u8868" else "List"
        HomeTab.Stats -> if (language == AppLanguage.Zh) "\u7EDF\u8BA1" else "Stats"
        HomeTab.Attention -> if (language == AppLanguage.Zh) "\u5F85\u529E" else "Due"
    }
}

private fun smartFilterLabel(filter: SmartFilter, language: AppLanguage): String {
    return when (filter) {
        SmartFilter.All -> if (language == AppLanguage.Zh) "全部" else "All"
        SmartFilter.Attention -> if (language == AppLanguage.Zh) "待处理" else "Attention"
        SmartFilter.Due -> if (language == AppLanguage.Zh) "到期" else "Due"
        SmartFilter.LowBalance -> if (language == AppLanguage.Zh) "余额不足" else "Low balance"
        SmartFilter.Active -> if (language == AppLanguage.Zh) "活跃" else "Active"
        SmartFilter.Archived -> if (language == AppLanguage.Zh) "归档" else "Archived"
    }
}

private fun modeLabel(mode: SpendingMode, language: AppLanguage): String {
    return when (mode) {
        SpendingMode.Fixed -> if (language == AppLanguage.Zh) "固定订阅" else "Fixed"
        SpendingMode.Balance -> if (language == AppLanguage.Zh) "余额账户" else "Balance"
        SpendingMode.Metered -> if (language == AppLanguage.Zh) "按量消费" else "Metered"
        SpendingMode.Hybrid -> if (language == AppLanguage.Zh) "固定 + 按量" else "Hybrid"
    }
}

private fun ledgerTypeLabel(type: LedgerType, language: AppLanguage): String {
    return when (type) {
        LedgerType.Renewal -> if (language == AppLanguage.Zh) "续费" else "Renewal"
        LedgerType.Expense -> if (language == AppLanguage.Zh) "消费" else "Expense"
        LedgerType.TopUp -> if (language == AppLanguage.Zh) "充值" else "Top-up"
        LedgerType.Refund -> if (language == AppLanguage.Zh) "退款" else "Refund"
        LedgerType.Adjustment -> if (language == AppLanguage.Zh) "调整" else "Adjustment"
        LedgerType.PriceChange -> if (language == AppLanguage.Zh) "价格变更" else "Price change"
    }
}

private fun stateLabel(state: SubscriptionState, language: AppLanguage): String {
    return when (state) {
        SubscriptionState.Active -> if (language == AppLanguage.Zh) "正常" else "Active"
        SubscriptionState.Paused -> if (language == AppLanguage.Zh) "暂停" else "Paused"
        SubscriptionState.Archived -> if (language == AppLanguage.Zh) "归档" else "Archived"
    }
}

private fun monthlyInCurrency(sub: Subscription, target: Currency, usdToCnyRate: Double): Double {
    if (sub.state == SubscriptionState.Archived) return 0.0
    val monthly = when (sub.cycle) {
        BillingCycle.Monthly -> sub.price
        BillingCycle.Quarterly -> sub.price / 3
        BillingCycle.Yearly -> sub.price / 12
        BillingCycle.Custom -> {
            val duration = sub.customCycleDuration ?: 1
            val days = when (sub.customCycleUnit) {
                CycleUnit.Day -> duration
                CycleUnit.Week -> duration * 7
                CycleUnit.Month -> duration * 30
                CycleUnit.Year -> duration * 365
                null -> 30
            }
            sub.price / days * 30
        }
    }
    return when {
        sub.currency == target -> monthly
        sub.currency == Currency.USD && target == Currency.CNY -> monthly * usdToCnyRate
        sub.currency == Currency.CNY && target == Currency.USD -> monthly / usdToCnyRate
        else -> monthly
    }
}

private fun formatMoney(value: Double, currency: Currency): String {
    return "${currencySymbol(currency)}${"%.0f".format(value)}"
}

private fun formatDateLabel(value: String, language: AppLanguage): String {
    val date = parseDate(value) ?: return value
    val pattern = if (language == AppLanguage.Zh) "yyyy年M月d日" else "MMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern))
}

private fun categoryPresets(isZh: Boolean): List<String> {
    return if (isZh) {
        listOf("\u5F71\u97F3", "\u4E91\u670D\u52A1", "AI", "\u8F6F\u4EF6", "\u6E38\u620F", "\u751F\u6D3B", "\u5176\u4ED6")
    } else {
        listOf("Media", "Cloud", "AI", "Software", "Games", "Life", "Other")
    }
}

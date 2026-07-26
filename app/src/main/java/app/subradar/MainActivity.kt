package app.subradar

import android.Manifest
import android.app.AlarmManager
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.launch

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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
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
enum class DisplayMode { List, Calendar, Stats }

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
    val usdToCnyRate: Double = 7.2
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
                usdToCnyRate = json.optDouble("usdToCnyRate", 7.2).takeIf { it > 0.0 } ?: 7.2
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
    json.put("category", category)
    notes?.let { json.put("notes", it) }
    imageUri?.let { json.put("imageUri", it) }
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
    val today = LocalDate.now()
    return items.map { sub ->
        var current = sub
        var guard = 0
        while (guard < 60) {
            val balance = current.accountBalance ?: break
            if (balance < current.price) break
            val next = parseDate(current.nextBillingDate) ?: break
            if (next.isAfter(today)) break
            current = current.copy(
                accountBalance = ((balance - current.price) * 100).toLong() / 100.0,
                nextBillingDate = addCycle(next, current.cycle, current.customCycleDuration, current.customCycleUnit).toString()
            )
            guard += 1
        }
        current
    }
}

private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

private fun scheduleReminders(context: Context, subscriptions: List<Subscription>, copy: Copy, daysBefore: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    subscriptions.forEach { sub ->
        cancelReminder(context, sub)
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
        .put("schemaVersion", 2)
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
}

private fun JSONObject.toSettings(): AppSettings {
    return AppSettings(
        notificationsEnabled = optBoolean("notificationsEnabled", false),
        language = optString("language", AppLanguage.En.name).toEnum(AppLanguage.En),
        theme = optString("theme", AppThemeMode.Auto.name).toEnum(AppThemeMode.Auto),
        reminderDaysBefore = optInt("reminderDaysBefore", 0).coerceIn(0, 30),
        primaryCurrency = optString("primaryCurrency", Currency.CNY.name).toEnum(Currency.CNY),
        usdToCnyRate = optDouble("usdToCnyRate", 7.2).takeIf { it > 0.0 } ?: 7.2
    )
}

@Composable
fun NativeSubRadar() {
    val context = LocalContext.current
    val store = remember { SubRadarStore(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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

    BackHandler(enabled = showSettings || isCreating || editorItem != null) {
        when {
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
                    copy = copy,
                    palette = palette,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onDisplayModeChange = { displayMode = it },
                    onCategoryFilterChange = { categoryFilter = it },
                    onOpenSettings = { showSettings = true },
                    onOpenEditor = { editorItem = it; isCreating = false },
                    onRenew = { sub ->
                        val baseDate = parseDate(sub.nextBillingDate) ?: LocalDate.now()
                        val next = addCycle(baseDate, sub.cycle, sub.customCycleDuration, sub.customCycleUnit)
                        val balance = sub.accountBalance?.let { if (it >= sub.price) ((it - sub.price) * 100).toLong() / 100.0 else it }
                        val updated = sub.copy(nextBillingDate = next.toString(), accountBalance = balance)
                        val index = subscriptions.indexOfFirst { it.id == sub.id }
                        if (index >= 0) subscriptions[index] = updated
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
                    SettingsScreen(
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

                AnimatedVisibility(
                    visible = isCreating || editorItem != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    SubscriptionEditor(
                        initial = editorItem,
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
                            scope.launch {
                                val undoText = if (settings.language == AppLanguage.Zh) "\u64A4\u9500" else "Undo"
                                val deletedText = if (settings.language == AppLanguage.Zh) "\u5DF2\u5220\u9664" else "Deleted"
                                val result = snackbarHostState.showSnackbar(
                                    message = deletedText,
                                    actionLabel = undoText,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    subscriptions.add(index.coerceIn(0, subscriptions.size), item)
                                    store.saveSubscriptions(subscriptions)
                                    if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
                                }
                            }
                        },
                        onSave = { item ->
                            if (subscriptions.any { it.id == item.id }) {
                                val index = subscriptions.indexOfFirst { it.id == item.id }
                                if (index >= 0) subscriptions[index] = item
                            } else {
                                subscriptions.add(item)
                            }
                            val renewed = autoRenew(subscriptions)
                            subscriptions.clear()
                            subscriptions.addAll(renewed)
                            store.saveSubscriptions(subscriptions)
                            if (settings.notificationsEnabled) scheduleReminders(context, subscriptions, copy, settings.reminderDaysBefore)
                            isCreating = false
                            editorItem = null
                        }
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                )
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    items: List<Subscription>,
    allItems: List<Subscription>,
    settings: AppSettings,
    displayMode: DisplayMode,
    categoryFilter: String?,
    copy: Copy,
    palette: Palette,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditor: (Subscription) -> Unit,
    onRenew: (Subscription) -> Unit,
    onCreate: () -> Unit
) {
    val visibleItems = items.filter { categoryFilter == null || it.category == categoryFilter }
    val categories = allItems.map { it.category }.distinct().sorted()
    Box(Modifier.fillMaxSize().background(palette.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(allItems, settings, copy, palette, searchQuery, onSearchChange, onOpenSettings)
            DashboardControls(
                displayMode = displayMode,
                categories = categories,
                categoryFilter = categoryFilter,
                settings = settings,
                palette = palette,
                onDisplayModeChange = onDisplayModeChange,
                onCategoryFilterChange = onCategoryFilterChange
            )
            val contentState = when {
                allItems.isEmpty() -> "empty"
                visibleItems.isEmpty() -> "noResults"
                else -> displayMode.name
            }
            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.98f))
                },
                label = "main content"
            ) { state ->
                when (state) {
                    "empty" -> EmptyState(copy, palette)
                    "noResults" -> EmptyState(
                        copy = copy,
                        palette = palette,
                        title = if (settings.language == AppLanguage.Zh) "未找到订阅" else "No matching subscriptions",
                        description = if (settings.language == AppLanguage.Zh) "换个关键词试试。" else "Try another search term."
                    )
                    DisplayMode.Stats.name -> StatsView(visibleItems, settings, palette)
                    DisplayMode.Calendar.name -> CalendarView(visibleItems, settings, copy, palette, onOpenEditor)
                    else -> SubscriptionList(
                        items = visibleItems,
                        settings = settings,
                        copy = copy,
                        palette = palette,
                        onOpenEditor = onOpenEditor,
                        onRenew = onRenew
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = onCreate,
            containerColor = palette.accent,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).navigationBarsPadding()
        ) {
            Icon(Icons.Rounded.Add, null)
        }
    }
}

@Composable
fun DashboardControls(
    displayMode: DisplayMode,
    categories: List<String>,
    categoryFilter: String?,
    settings: AppSettings,
    palette: Palette,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onCategoryFilterChange: (String?) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Segmented(DisplayMode.entries, displayMode, { displayModeLabel(it, settings.language) }, palette, onDisplayModeChange)
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
fun SubscriptionList(
    items: List<Subscription>,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onOpenEditor: (Subscription) -> Unit,
    onRenew: (Subscription) -> Unit
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
                onRenew = onRenew
            )
        }
    }
}

@Composable
fun StatsView(items: List<Subscription>, settings: AppSettings, palette: Palette) {
    val language = settings.language
    val nextSeven = items.count {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), parseDate(it.nextBillingDate) ?: LocalDate.now())
        days in 0..7
    }
    val annual = items.sumOf { monthlyInCurrency(it, settings.primaryCurrency, settings.usdToCnyRate) * 12 }
    val byCategory = items.groupBy { it.category }
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
                value = formatMoney(items.sumOf { monthlyInCurrency(it, settings.primaryCurrency, settings.usdToCnyRate) }, settings.primaryCurrency),
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
fun CalendarView(
    items: List<Subscription>,
    settings: AppSettings,
    copy: Copy,
    palette: Palette,
    onOpenEditor: (Subscription) -> Unit
) {
    val grouped = items.sortedBy { it.nextBillingDate }.groupBy { it.nextBillingDate }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grouped.forEach { (date, entries) ->
            item(date) {
                Text(
                    text = formatDateLabel(date, settings.language),
                    color = palette.muted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(entries, key = { it.id }) {
                SubscriptionRow(
                    sub = it,
                    copy = copy,
                    language = settings.language,
                    palette = palette,
                    onOpenEditor = onOpenEditor,
                    onRenew = {},
                    showRenewAction = false
                )
            }
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
                unfocusedIndicatorColor = Color.Transparent
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
    modifier: Modifier = Modifier,
    showRenewAction: Boolean = true
) {
    val today = LocalDate.now()
    val next = parseDate(sub.nextBillingDate) ?: today
    val days = ChronoUnit.DAYS.between(today, next).toInt()
    val isToday = days == 0
    val isPast = days < 0
    val surface = when {
        isToday -> if (palette.bg == Color(0xFF0B0F14)) Color(0xFF33230A) else Color(0xFFFFF4D6)
        isPast -> if (palette.bg == Color(0xFF0B0F14)) Color(0xFF341515) else Color(0xFFFFEBEB)
        else -> palette.card
    }
    val animatedSurface by animateColorAsState(targetValue = surface, label = "subscription surface")
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(animatedSurface)
            .clickable { onOpenEditor(sub) }
            .animateContentSize()
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(sub.name, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${currencySymbol(sub.currency)}${"%.2f".format(sub.price)} ${cycleText(sub, copy, language)}", color = palette.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(sub.category, color = palette.muted, fontSize = 12.sp)
                sub.accountBalance?.let {
                    Text("${copy.accountBalance}: ${currencySymbol(sub.currency)}${"%.2f".format(it)}", color = palette.muted, fontSize = 12.sp)
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
                    isToday -> copy.dueToday
                    isPast -> copy.overdue.format(abs(days))
                    else -> copy.inDays.format(days)
                }
                Text(status, color = palette.muted, fontSize = 12.sp)
            }
        }
        if (sub.imageUri != null || ((isToday || isPast) && showRenewAction)) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sub.imageUri != null) {
                    Icon(Icons.Rounded.Image, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (language == AppLanguage.Zh) "图片" else "Image", color = palette.accent, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                if ((isToday || isPast) && showRenewAction) {
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onSettings(settings.copy(notificationsEnabled = granted))
        if (!granted && Build.VERSION.SDK_INT >= 33) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = palette.text) }
            Text(copy.settings, color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsCard(Icons.Rounded.Palette, copy.appearance, palette) {
                    Segmented(AppThemeMode.entries, settings.theme, { themeLabel(it, settings.language) }, palette) {
                        onSettings(settings.copy(theme = it))
                    }
                }
            }
            item {
                SettingsCard(Icons.Rounded.Translate, copy.language, palette) {
                    Segmented(AppLanguage.entries, settings.language, { if (it == AppLanguage.Zh) "中文" else "EN" }, palette) {
                        onSettings(settings.copy(language = it))
                    }
                }
            }
            item {
                SettingsCard(Icons.Rounded.Notifications, copy.notifications, palette, copy.notificationsDesc) {
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
            }
            item {
                SettingsCard(
                    Icons.Rounded.CalendarMonth,
                    if (settings.language == AppLanguage.Zh) "\u63D0\u524D\u63D0\u9192" else "Reminder lead time",
                    palette,
                    if (settings.language == AppLanguage.Zh) "\u5230\u671F\u524D\u591A\u5C11\u5929\u63D0\u9192" else "Days before renewal"
                ) {
                    Segmented(listOf(0, 1, 3, 7), settings.reminderDaysBefore, { it.toString() }, palette) {
                        onSettings(settings.copy(reminderDaysBefore = it))
                    }
                }
            }
            item {
                SettingsCard(
                    Icons.Rounded.Wallet,
                    if (settings.language == AppLanguage.Zh) "\u4E3B\u8981\u8D27\u5E01" else "Primary currency",
                    palette
                ) {
                    Segmented(Currency.entries, settings.primaryCurrency, { currencySymbol(it) }, palette) {
                        onSettings(settings.copy(primaryCurrency = it))
                    }
                }
            }
            item {
                SettingsCard(
                    Icons.Rounded.Wallet,
                    if (settings.language == AppLanguage.Zh) "\u7F8E\u5143\u6C47\u7387" else "USD rate",
                    palette,
                    if (settings.language == AppLanguage.Zh) "1 USD = ${settings.usdToCnyRate} CNY" else "1 USD = ${settings.usdToCnyRate} CNY"
                ) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = {
                            rateText = it
                            it.toDoubleOrNull()?.takeIf { rate -> rate > 0.0 }?.let { rate ->
                                onSettings(settings.copy(usdToCnyRate = rate))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = palette.field,
                            unfocusedContainerColor = palette.field,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.width(96.dp)
                    )
                }
            }
            item {
                SettingsCard(
                    Icons.Rounded.CreditCard,
                    if (settings.language == AppLanguage.Zh) "\u5BFC\u5165\u5907\u4EFD" else "Import backup",
                    palette
                ) {
                    TextButton(onClick = onImport) { Text(if (settings.language == AppLanguage.Zh) "\u5BFC\u5165" else "Import") }
                }
            }
            item {
                SettingsCard(
                    Icons.Rounded.CreditCard,
                    if (settings.language == AppLanguage.Zh) "\u5BFC\u51FA\u5907\u4EFD" else "Export backup",
                    palette
                ) {
                    TextButton(onClick = onExport) { Text(if (settings.language == AppLanguage.Zh) "\u5BFC\u51FA" else "Export") }
                }
            }
            item {
                SettingsCard(Icons.Rounded.CreditCard, "SubRadar v2.0.0.0", palette, copy.about) {}
            }
        }
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
fun <T> Segmented(values: List<T>, current: T, label: (T) -> String, palette: Palette, onSelect: (T) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(palette.field).padding(3.dp)) {
        values.forEach { value ->
            val selected = value == current
            val background by animateColorAsState(
                targetValue = if (selected) palette.card else Color.Transparent,
                label = "segment background"
            )
            Text(
                label(value),
                color = if (selected) palette.text else palette.muted,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(background)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionEditor(
    initial: Subscription?,
    copy: Copy,
    palette: Palette,
    onDismiss: () -> Unit,
    onDelete: (Subscription) -> Unit,
    onSave: (Subscription) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var price by remember { mutableStateOf(initial?.price?.toString().orEmpty()) }
    var currency by remember { mutableStateOf(initial?.currency ?: Currency.CNY) }
    var cycle by remember { mutableStateOf(initial?.cycle ?: BillingCycle.Monthly) }
    var customDuration by remember { mutableStateOf((initial?.customCycleDuration ?: 1).toString()) }
    var customUnit by remember { mutableStateOf(initial?.customCycleUnit ?: CycleUnit.Month) }
    var nextBillingDate by remember { mutableStateOf(initial?.nextBillingDate ?: LocalDate.now().toString()) }
    var startDate by remember { mutableStateOf(initial?.startDate.orEmpty()) }
    var balance by remember { mutableStateOf(initial?.accountBalance?.toString().orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "Other") }
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

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
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
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = palette.text) }
                Text(if (initial == null) copy.newSub else copy.editSub, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                if (initial != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, null, tint = palette.danger) }
            }
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 0.dp, 18.dp, 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = formMaxHeight)
            ) {
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
    }

    if (showDatePicker) {
        val current = runCatching {
            LocalDate.parse(if (pickingDateForStart) startDate.ifBlank { LocalDate.now().toString() } else nextBillingDate)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
        val state = rememberDatePickerState(initialSelectedDateMillis = current)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.selectedDateMillis ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(selected).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    if (pickingDateForStart) startDate = date else nextBillingDate = date
                    showDatePicker = false
                }) { Text(copy.confirm) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(copy.cancel) } }
        ) { DatePicker(state = state) }
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(copy.delete) },
            text = { Text(copy.confirmDelete) },
            confirmButton = { TextButton(onClick = { onDelete(initial) }) { Text(copy.confirm) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(copy.cancel) } }
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
            unfocusedIndicatorColor = Color.Transparent
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

private fun totalMonthly(items: List<Subscription>, language: AppLanguage): String {
    var cny = 0.0
    var usd = 0.0
    items.forEach { sub ->
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

private fun displayModeLabel(mode: DisplayMode, language: AppLanguage): String {
    return when (mode) {
        DisplayMode.List -> if (language == AppLanguage.Zh) "\u5217\u8868" else "List"
        DisplayMode.Calendar -> if (language == AppLanguage.Zh) "\u65E5\u5386" else "Calendar"
        DisplayMode.Stats -> if (language == AppLanguage.Zh) "\u7EDF\u8BA1" else "Stats"
    }
}

private fun monthlyInCurrency(sub: Subscription, target: Currency, usdToCnyRate: Double): Double {
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

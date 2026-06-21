package com.sansim.app.esim

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sansim.app.i18n.tr
import com.sansim.app.util.LocalAppLanguage
import com.sansim.app.util.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sansim.app.LocalIsDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsimScreen(modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val lang = LocalAppLanguage.current
    fun c(light: Color, darkColor: Color) = if (dark) darkColor else light
    val pageBg = c(Color(0xFFF4F5F7), Color(0xFF000000))
    val cardBg = c(Color.White, Color(0xFF1C1C1E))
    val cardSoft = c(Color(0xFFF8FAFC), Color(0xFF2C2C2E))
    val textPrimary = c(Color(0xFF111827), Color(0xFFF5F5F7))
    val textSecondary = c(Color(0xFF6B7280), Color(0xFF8E8E93))
    val textMuted = c(Color(0xFF8A94A6), Color(0xFF636366))
    val borderColor = c(Color.White.copy(alpha = 0.85f), Color(0xFF2C2C2E).copy(alpha = .9f))
    val blueSoft = c(Color(0xFFEAF3FF), Color(0xFF0A2748))
    val greenSoft = c(Color(0xFFEAFBF0), Color(0xFF123322))
    val neutralSoft = c(Color(0xFFF1F5F9), Color(0xFF2C2C2E))
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { EuiccManager(context) }

    var readerName by remember { mutableStateOf(tr(lang,"未连接")) }
    var eid by remember { mutableStateOf("") }
    var profiles by remember { mutableStateOf<List<EuiccProfile>>(emptyList()) }
    var addresses by remember { mutableStateOf<ConfiguredAddresses?>(null) }
    var status by remember { mutableStateOf(tr(lang,"请选择读卡器")) }
    var busy by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showDownload by remember { mutableStateOf(false) }
    var usbDevices by remember { mutableStateOf<List<UsbDevice>>(emptyList()) }
    var initialized by remember { mutableStateOf(false) }
    var dbStats by remember { mutableStateOf("数据库加载中...") }
    var pendingUsbDevice by remember { mutableStateOf<UsbDevice?>(null) }
    val usbManager = remember { context.getSystemService(Context.USB_SERVICE) as UsbManager }
    val usbPermissionAction = remember { "${context.packageName}.USB_PERMISSION" }

    fun refreshUsb() {
        usbDevices = UsbCcReader.findDevices(context)
        status = tr(lang,"检测到")+" ${usbDevices.size} "+tr(lang,"个 USB 设备")
    }

    fun refreshData() {
        scope.launch {
            busy = true
            try {
                status = tr(lang,"读取 EID/Profile...")
                val result = withContext(Dispatchers.IO) {
                    Triple(manager.getEid(), manager.getProfiles(), runCatching { manager.getConfiguredAddresses() }.getOrNull())
                }
                eid = result.first
                profiles = result.second
                addresses = result.third
                status = tr(lang,"读取成功")+"：${profiles.size} "+tr(lang,"个 Profile")
                LogCollector.d("EsimScreen", "refresh success eid=$eid profiles=${profiles.size}")
            } catch (e: Throwable) {
                status = tr(lang,"读取失败")+"：${e.message}"
                LogCollector.e("EsimScreen", "refresh failed", e)
            } finally { busy = false }
        }
    }

    fun connect(backend: ReaderBackend) {
        scope.launch {
            busy = true
            initialized = false
            try {
                status = tr(lang,"连接")+" ${backend.getReaderName()}..."
                LogCollector.d("EsimScreen", "connect ${backend.getReaderName()}")
                manager.setBackend(backend)
                withContext(Dispatchers.IO) { manager.init() }
                readerName = backend.getReaderName()
                initialized = true
                status = tr(lang,"连接成功")
                refreshData()
            } catch (e: Throwable) {
                status = tr(lang,"连接失败")+"：${e.message}"
                readerName = tr(lang,"未连接")
                LogCollector.e("EsimScreen", "connect failed", e)
            } finally { busy = false }
        }
    }

    fun requestUsbPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            connect(UsbCcReader(context, device))
            return
        }
        pendingUsbDevice = device
        status = tr(lang,"正在请求 USB 权限")+"：${device.productName ?: device.deviceName}"
        LogCollector.d("EsimScreen", "request USB permission ${device.deviceName}")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, device.deviceId, Intent(usbPermissionAction), flags)
        usbManager.requestPermission(device, pi)
    }


    fun switchProfile(profile: EuiccProfile) {
        scope.launch {
            busy = true
            try {
                val isEnabled = profile.state == "enabled"
                status = if (isEnabled) tr(lang,"正在禁用")+" ${profile.iccid}..." else tr(lang,"正在启用")+" ${profile.iccid}..."
                val rc = withContext(Dispatchers.IO) {
                    if (isEnabled) manager.disableProfile(profile.iccid) else manager.enableProfile(profile.iccid)
                }
                LogCollector.d("EsimScreen", "switch profile rc=$rc iccid=${profile.iccid}")
                status = if (rc == 0) tr(lang,"切卡完成") else tr(lang,"切卡返回码")+"：$rc"
                refreshData()
            } catch (e: Throwable) {
                status = tr(lang,"切卡失败")+"：${e.message}"
                LogCollector.e("EsimScreen", "switch failed", e)
            } finally { busy = false }
        }
    }

    fun renameProfile(profile: EuiccProfile, nickname: String) {
        scope.launch {
            busy = true
            try {
                status = tr(lang,"正在重命名")+" ${profile.iccid}..."
                val rc = withContext(Dispatchers.IO) { manager.setNickname(profile.iccid, nickname) }
                LogCollector.d("EsimScreen", "rename profile rc=$rc iccid=${profile.iccid} nickname=$nickname")
                status = if (rc == 0) tr(lang,"重命名完成") else tr(lang,"重命名返回码")+"：$rc"
                refreshData()
            } catch (e: Throwable) {
                status = tr(lang,"重命名失败")+"：${e.message}"
                LogCollector.e("EsimScreen", "rename failed", e)
            } finally { busy = false }
        }
    }

    fun deleteProfile(profile: EuiccProfile) {
        scope.launch {
            busy = true
            try {
                status = tr(lang,"正在删除")+" ${profile.iccid}..."
                val rc = withContext(Dispatchers.IO) { manager.deleteProfile(profile.iccid) }
                LogCollector.d("EsimScreen", "delete profile rc=$rc iccid=${profile.iccid}")
                status = if (rc == 0) tr(lang,"删除完成") else tr(lang,"删除返回码")+"：$rc"
                refreshData()
            } catch (e: Throwable) {
                status = tr(lang,"删除失败")+"：${e.message}"
                LogCollector.e("EsimScreen", "delete failed", e)
            } finally { busy = false }
        }
    }

    fun downloadProfile(smdp: String, matchingId: String, confirmationCode: String) {
        scope.launch {
            busy = true
            try {
                status = tr(lang,"正在认证 SM-DP+...")
                LogCollector.d("EsimScreen", "download auth smdp=$smdp matchingId=$matchingId")
                val auth = withContext(Dispatchers.IO) { manager.authenticateProfile(smdp, matchingId) }
                LogCollector.d("EsimScreen", "authenticate result=$auth")
                val authObj = org.json.JSONObject(auth)
                if (!authObj.optBoolean("success", false)) {
                    throw RuntimeException(authObj.optString("error", "authenticateClient failed"))
                }
                status = tr(lang,"认证成功，正在下载写入 Profile...")
                val dl = withContext(Dispatchers.IO) { manager.downloadProfile(confirmationCode) }
                LogCollector.d("EsimScreen", "download result=$dl")
                val dlObj = org.json.JSONObject(dl)
                if (!dlObj.optBoolean("success", false)) {
                    throw RuntimeException(dlObj.optString("error", "download failed"))
                }
                status = tr(lang,"下载完成")
                refreshData()
            } catch (e: Throwable) {
                status = tr(lang,"下载失败")+"：${e.message}"
                LogCollector.e("EsimScreen", "download failed", e)
                runCatching { withContext(Dispatchers.IO) { manager.cancelSession() } }
            } finally { busy = false }
        }
    }


    DisposableEffect(usbPermissionAction) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != usbPermissionAction) return
                val device = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                } ?: pendingUsbDevice
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                LogCollector.d("EsimScreen", "USB permission result granted=$granted device=${device?.deviceName}")
                if (granted && device != null) {
                    status = tr(lang,"USB 已授权，正在连接...")
                    connect(UsbCcReader(context, device))
                } else {
                    status = tr(lang,"USB 权限被拒绝")
                }
            }
        }
        val filter = IntentFilter(usbPermissionAction)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { manager.release() } }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { EsimDatabase.ensureLoaded(context) }
        dbStats = EsimDatabase.stats(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(L("eSIM 管理"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(L("读卡 · 下载 · Profile 管理"), fontSize = 11.sp, color = textMuted)
                    }
                },
                actions = { TextButton(onClick = { showLogs = true }) { Text(L("日志")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBg)
            )
        },
        containerColor = pageBg
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EsimHeroCard(
                    cardBg = cardBg,
                    cardSoft = cardSoft,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textMuted = textMuted,
                    borderColor = borderColor,
                    readerName = readerName,
                    status = status,
                    eid = eid,
                    profileCount = profiles.size,
                    busy = busy,
                    initialized = initialized,
                    onRefresh = { refreshData() },
                    onDownload = { showDownload = true }
                )
            }

            item {
                ReaderPanel(
                    cardBg = cardBg,
                    cardSoft = cardSoft,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textMuted = textMuted,
                    borderColor = borderColor,
                    blueSoft = blueSoft,
                    usbDevices = usbDevices,
                    busy = busy,
                    onInternal = { connect(OmapReader(context)) },
                    onDetectUsb = { refreshUsb() },
                    onUsb = { requestUsbPermission(it) }
                )
            }

            item {
                SlimInfoCard(
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textMuted = textMuted,
                    borderColor = borderColor,
                    title = L("卡片信息"),
                    subtitle = if (eid.isBlank()) L("尚未读取 EID") else eid,
                    trailing = if (initialized) L("已连接") else L("未连接")
                ) {
                    addresses?.let {
                        if (it.defaultDpAddress.isNotBlank() || it.rootDsAddress.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            InfoLine("Default DP", it.defaultDpAddress.ifBlank { "—" }, textMuted, textPrimary)
                            InfoLine("Root DS", it.rootDsAddress.ifBlank { "—" }, textMuted, textPrimary)
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                    Text("Profiles", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(Modifier.width(8.dp))
                    CountBadge("${profiles.size}")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { refreshData() }, enabled = initialized && !busy) { Text(L("刷新")) }
                }
            }

            if (profiles.isEmpty()) {
                item {
                    EmptyProfileCard(initialized = initialized, cardBg = cardBg, textPrimary = textPrimary, textMuted = textMuted, borderColor = borderColor)
                }
            } else {
                items(profiles, key = { it.iccid }) { p ->
                    ProfileRow(profile = p, eid = eid, busy = busy, cardBg = cardBg, cardSoft = cardSoft, textPrimary = textPrimary, textSecondary = textSecondary, textMuted = textMuted, borderColor = borderColor, greenSoft = greenSoft, neutralSoft = neutralSoft, onSwitch = { switchProfile(p) }, onRename = { renameProfile(p, it) }, onDelete = { deleteProfile(p) })
                }
            }
        }
    }

    if (showLogs) {
        LogViewerDialog(onDismiss = { showLogs = false })
    }
    if (showDownload) {
        DownloadProfileDialog(
            onDismiss = { showDownload = false },
            onConfirm = { smdp, matchingId, confirmationCode ->
                showDownload = false
                downloadProfile(smdp, matchingId, confirmationCode)
            }
        )
    }
}

@Composable
private fun CardBlock(cardBg: Color = Color.White, borderColor: Color = Color.White.copy(alpha = 0.85f), content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().border(0.7.dp, borderColor, RoundedCornerShape(22.dp))
    ) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) }
}

@Composable
private fun EsimHeroCard(
    cardBg: Color,
    cardSoft: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    borderColor: Color,
    readerName: String,
    status: String,
    eid: String,
    profileCount: Int,
    busy: Boolean,
    initialized: Boolean,
    onRefresh: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(shape = RoundedCornerShape(24.dp), color = cardBg, modifier = Modifier.fillMaxWidth().border(.7.dp, borderColor, RoundedCornerShape(24.dp))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Color(0xFFEAF3FF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("e", color = Color(0xFF007AFF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(readerName, color = textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(status, color = textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                StatusDot(initialized)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF007AFF), trackColor = cardSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMetric("Profiles", profileCount.toString(), cardSoft, textMuted, textPrimary, Modifier.weight(1f))
                HeroMetric("EID", if (eid.isBlank()) "—" else eid.takeLast(6), cardSoft, textMuted, textPrimary, Modifier.weight(1f))
                HeroMetric("状态", if (initialized) "Ready" else "Idle", cardSoft, textMuted, textPrimary, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onDownload, enabled = initialized && !busy, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) { Text(L("下载 Profile")) }
                OutlinedButton(onClick = onRefresh, enabled = initialized && !busy, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(46.dp)) { Text(L("刷新")) }
            }
        }
    }
}

@Composable private fun HeroMetric(label: String, value: String, cardSoft: Color, textMuted: Color, textPrimary: Color, modifier: Modifier) {
    Column(modifier.background(cardSoft, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Text(label, color = textMuted, fontSize = 11.sp)
        Text(value, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun StatusDot(ok: Boolean) {
    Row(Modifier.background(if (ok) Color(0xFFDCFCE7) else Color(0xFFF1F5F9), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(if (ok) Color(0xFF22C55E) else Color(0xFF94A3B8), RoundedCornerShape(99.dp)))
        Spacer(Modifier.width(6.dp))
        Text(if (ok) "在线" else "待机", fontSize = 11.sp, color = if (ok) Color(0xFF15803D) else Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ReaderPanel(cardBg: Color, cardSoft: Color, textPrimary: Color, textSecondary: Color, textMuted: Color, borderColor: Color, blueSoft: Color, usbDevices: List<UsbDevice>, busy: Boolean, onInternal: () -> Unit, onDetectUsb: () -> Unit, onUsb: (UsbDevice) -> Unit) {
    CardBlock(cardBg, borderColor) {
        SectionTitle(L("读卡器"), L("选择内置 eSIM 或 USB 实体卡"), textPrimary, textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ReaderButton(L("内置 eSIM"), "OMAPI", Color(0xFF007AFF), textSecondary, !busy, Modifier.weight(1f), onInternal)
            ReaderButton(L("检测 USB"), "CCID", Color(0xFF34C759), textSecondary, !busy, Modifier.weight(1f), onDetectUsb)
        }
        usbDevices.forEach { dev ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cardSoft).clickable(enabled = !busy) { onUsb(dev) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).background(blueSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("U", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(dev.productName ?: "USB Reader", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(dev.deviceName, fontSize = 11.sp, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(L("连接"), color = Color(0xFF007AFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun ReaderButton(title: String, sub: String, color: Color, textSecondary: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(70.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = enabled) { onClick() }, shape = RoundedCornerShape(18.dp), color = color.copy(alpha = .10f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable private fun SlimInfoCard(cardBg: Color, textPrimary: Color, textSecondary: Color, textMuted: Color, borderColor: Color, title: String, subtitle: String, trailing: String, content: @Composable ColumnScope.() -> Unit = {}) {
    CardBlock(cardBg, borderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(subtitle, fontSize = 12.sp, color = textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, fontFamily = if (subtitle.all { it.isDigit() }) FontFamily.Monospace else FontFamily.Default)
            }
            EsimPill(trailing, Color(0xFF007AFF))
        }
        content()
    }
}

@Composable private fun SectionTitle(title: String, sub: String, textPrimary: Color, textMuted: Color) {
    Column { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary); Text(sub, fontSize = 12.sp, color = textMuted) }
}
@Composable private fun InfoLine(k: String, v: String, textMuted: Color, textPrimary: Color) { Row { Text("$k：", fontSize = 12.sp, color = textMuted); Text(v, fontSize = 12.sp, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun CountBadge(text: String) { Text(text, color = Color(0xFF007AFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFFEAF3FF), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) }
@Composable private fun EsimPill(text: String, color: Color) { Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(color.copy(alpha = .11f), RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 5.dp)) }

@Composable private fun EmptyProfileCard(initialized: Boolean, cardBg: Color, textPrimary: Color, textMuted: Color, borderColor: Color) {
    CardBlock(cardBg, borderColor) { Text(if (initialized) L("暂无 Profile") else L("请先连接读卡器"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary); Text(L("连接后会显示 ICCID、运营商、状态和可用操作。"), fontSize = 12.sp, color = textMuted) }
}

@Composable
private fun ProfileRow(
    profile: EuiccProfile,
    eid: String,
    busy: Boolean,
    cardBg: Color,
    cardSoft: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    borderColor: Color,
    greenSoft: Color,
    neutralSoft: Color,
    onSwitch: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val match = remember(profile, eid) { EsimDatabase.match(context, profile, eid) }
    val enabled = profile.state == "enabled"
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val title = profile.nickname.ifBlank { profile.name.ifBlank { profile.serviceProvider.ifBlank { "Unnamed Profile" } } }

    Surface(shape = RoundedCornerShape(24.dp), color = cardBg, modifier = Modifier.fillMaxWidth().border(.7.dp, if (enabled) Color(0xFF34C759).copy(alpha = .38f) else borderColor, RoundedCornerShape(24.dp))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(if (enabled) greenSoft else neutralSoft, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text(title.take(1).uppercase(), color = if (enabled) Color(0xFF16A34A) else Color(0xFF64748B), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile.iccid, fontSize = 11.sp, color = textMuted, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                EsimPill(if (enabled) "启用" else "停用", if (enabled) Color(0xFF16A34A) else Color(0xFF64748B))
            }
            val meta = listOf(match.operatorName, match.countryIso, match.plmn).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, fontSize = 13.sp, color = Color(0xFF007AFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                match.profileSizeBytes?.let { MiniInfo(L("容量"), it.bytesToKbText(), cardSoft, textMuted, textPrimary, Modifier.weight(1f)) }
                if (match.rsp.isNotBlank()) MiniInfo("RSP", match.rsp, cardSoft, textMuted, textPrimary, Modifier.weight(1f))
                if (match.apns.isNotEmpty()) MiniInfo("APN", match.apns.first().apn, cardSoft, textMuted, textPrimary, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSwitch, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(42.dp), colors = ButtonDefaults.buttonColors(containerColor = if (enabled) Color(0xFF64748B) else Color(0xFF34C759))) { Text(if (enabled) "禁用" else "启用") }
                OutlinedButton(onClick = { showRename = true }, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(42.dp)) { Text(L("重命名")) }
                OutlinedButton(onClick = { showDelete = true }, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(42.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30))) { Text(L("删除")) }
            }
        }
    }

    if (showRename) {
        var nickname by remember { mutableStateOf(profile.nickname.ifBlank { profile.name }) }
        AlertDialog(onDismissRequest = { showRename = false }, title = { Text(L("重命名 Profile")) }, text = { OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text(L("昵称")) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { showRename = false; onRename(nickname.trim()) }, enabled = nickname.isNotBlank()) { Text(L("保存")) } }, dismissButton = { TextButton(onClick = { showRename = false }) { Text(L("取消")) } })
    }
    if (showDelete) {
        AlertDialog(onDismissRequest = { showDelete = false }, title = { Text(L("删除 Profile？")) }, text = { Text("将删除 $title。此操作不可恢复，请确认。") }, confirmButton = { TextButton(onClick = { showDelete = false; onDelete() }) { Text("删除", color = Color(0xFFFF3B30)) } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text(L("取消")) } })
    }
}

@Composable private fun MiniInfo(label: String, value: String, cardSoft: Color, textMuted: Color, textPrimary: Color, modifier: Modifier) {
    Column(modifier.background(cardSoft, RoundedCornerShape(14.dp)).padding(10.dp)) { Text(label, fontSize = 10.sp, color = textMuted); Text(value, fontSize = 12.sp, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun DownloadProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (smdp: String, matchingId: String, confirmationCode: String) -> Unit
) {
    var raw by remember { mutableStateOf("") }
    var smdp by remember { mutableStateOf("") }
    var matchingId by remember { mutableStateOf("") }
    var confirmationCode by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    fun parseLpa(text: String) {
        val t = text.trim()
        if (!t.uppercase().startsWith("LPA:")) return
        val body = t.removePrefix("LPA:").removePrefix("lpa:")
        val parts = body.split("$")
        if (parts.size >= 3) {
            smdp = parts[1]
            matchingId = parts[2]
        }
    }

    fun applyScanned(code: String) {
        raw = code.trim()
        parseLpa(raw)
        showScanner = false
    }

    val dark = LocalIsDark.current
    val lang = LocalAppLanguage.current
    fun c(light: Color, darkColor: Color) = if (dark) darkColor else light
    val dialogBg = c(Color(0xFFF2F3F7), Color(0xFF1C1C1E))
    val fieldBg = c(Color.White, Color(0xFF2C2C2E))
    val textPrimary = c(Color(0xFF111827), Color(0xFFF5F5F7))
    val textSecondary = c(Color(0xFF6B7280), Color(0xFF8E8E93))
    val border = c(Color.White.copy(alpha = .86f), Color(0xFF38383A))
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedLabelColor = Color(0xFF007AFF),
        unfocusedLabelColor = textSecondary,
        focusedContainerColor = fieldBg,
        unfocusedContainerColor = fieldBg,
        focusedBorderColor = Color(0xFF007AFF),
        unfocusedBorderColor = border,
        cursorColor = Color(0xFF007AFF)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().border(.7.dp, border, RoundedCornerShape(28.dp))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L("下载 Profile"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(L("扫码或粘贴 LPA 激活码"), fontSize = 12.sp, color = textSecondary)
                    }
                    TextButton(onClick = onDismiss) { Text(L("取消")) }
                }
                Button(
                    onClick = { showScanner = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) { Text(L("扫码二维码")) }
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it; parseLpa(it) },
                    label = { Text(L("LPA 激活码，可粘贴 LPA:1$...")) },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = smdp,
                    onValueChange = { smdp = it },
                    label = { Text(L("SM-DP+ 地址")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matchingId,
                    onValueChange = { matchingId = it },
                    label = { Text(L("Matching ID / 激活码")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmationCode,
                    onValueChange = { confirmationCode = it },
                    label = { Text(L("确认码，可选")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(shape = RoundedCornerShape(16.dp), color = fieldBg, modifier = Modifier.fillMaxWidth()) {
                    Text("流程：authenticateClient → getBoundProfilePackage → STORE DATA 写卡 → 刷新 Profile。", fontSize = 12.sp, color = textSecondary, lineHeight = 17.sp, modifier = Modifier.padding(12.dp))
                }
                Button(
                    onClick = { onConfirm(smdp.trim(), matchingId.trim(), confirmationCode.trim()) },
                    enabled = smdp.isNotBlank() && matchingId.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), disabledContainerColor = c(Color(0xFFE5E7EB), Color(0xFF2C2C2E)), disabledContentColor = textSecondary)
                ) { Text(L("开始下载")) }
            }
        }
    }

    if (showScanner) {
        QrScannerDialog(
            onDismiss = { showScanner = false },
            onResult = { applyScanned(it) }
        )
    }
}

@Composable
private fun LogViewerDialog(onDismiss: () -> Unit) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf(LogCollector.all()) }
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L("eSIM 调试日志")) },
        text = {
            Column(Modifier.heightIn(max = 520.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { text = LogCollector.all(); copied = false }) { Text(L("刷新")) }
                    OutlinedButton(onClick = { LogCollector.clear(); text = ""; copied = false }) { Text(L("清除")) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(text.ifBlank { tr(lang, "暂无日志") }))
                            copied = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (copied) "已复制" else "复制") }
                    Button(
                        onClick = {
                            val sendText = text.ifBlank { tr(lang, "暂无日志") }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, sendText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(intent, "分享 eSIM 日志"))
                            }.onFailure {
                                LogCollector.e("LogViewer", "share text failed", it)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(L("分享文本")) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = text.ifBlank { tr(lang, "暂无日志") },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(L("关闭")) } }
    )
}

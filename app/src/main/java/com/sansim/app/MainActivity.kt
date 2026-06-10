
package com.sansim.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.SSLSocketFactory
import android.util.Base64
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import kotlin.concurrent.thread
import kotlin.math.roundToInt

const val CHANNEL_ID = "san_sim_reminders"
const val PREF = "san_sim_data"

class SanSimApplication: Application() { override fun onCreate(){ super.onCreate(); NotificationHelper.createChannel(this) } }

data class Country(val flag:String,val name:String,val code:String,val iso:String)
data class OperatorInfo(val id:Int,val country:String,val countryCode:String,val carrierName:String,val website:String,val logoUrl:String,val esimSupport:Boolean)
data class PhoneNumberRecord(val id:String=UUID.randomUUID().toString(), val countryCode:String="+86", val countryName:String="中国", val flag:String="🇨🇳", val number:String="", val operator:String="", val expireDate:String=LocalDate.now().plusMonths(1).toString(), val note:String="", val balance:String="", val eid:String="", val smdp:String="", val activationCode:String="", val startDate:String=LocalDate.now().toString(), val createdAt:String=LocalDate.now().toString(), val activatedAt:String="", val longTerm:Boolean=false, val cycleDays:Int=30, val signalStatus:String="在线")
data class App设置(var dark:Boolean=false,var remind天:Int=7,var trafficUrl:String="https://speed.cloudflare.com/__down?bytes=10485760",var trafficKb:Double=1.0,var tgEnabled:Boolean=false,var botToken:String="",var chatId:String="",var keepCycle:String="月",var backgroundUri:String="",var backgroundAlpha:Float=.72f,var reminderEnabled:Boolean=true,var notificationEnabled:Boolean=true,var remindHour:Int=9,var remindMinute:Int=0,var language:String="简体中文",var emailQuickEnabled:Boolean=true,var smtpEnabled:Boolean=false,var smtpHost:String="",var smtpPort:Int=465,var smtpUser:String="",var smtpPass:String="",var smtpFrom:String="",var smtpTo:String="",var cloudEnabled:Boolean=false,var cloudUrl:String="https://ccs.ziranaa.top:16670",var cloudApiKey:String="",var cloudTelegramEnabled:Boolean=true,var cloudEmailEnabled:Boolean=true,var cloudAutoSync:Boolean=false,var showFlag:Boolean=true)

val LocalAppLanguage = compositionLocalOf { "简体中文" }
fun tr(lang:String, key:String):String {
    val extra = mapOf(
        "繁体中文" to mapOf(
            "云端提醒" to "雲端提醒", "启用云端提醒" to "啟用雲端提醒", "自动同步" to "自動同步", "自动同步说明" to "開啟後，新增/編輯/刪除/保號/匯入號碼和修改提醒設定時，會自動同步到雲端。", "API Key说明" to "每台手機生成一次固定 API Key；可直接貼上整段內容，App 會自動擷取真正的 Key 並去掉換行。", "当前 API Key：" to "目前 API Key：", "未设置" to "未設定", "连接成功" to "連線成功", "测试连接" to "測試連線", "已复制 API Key" to "已複製 API Key", "请先生成或填写 API Key" to "請先生成或填寫 API Key", "复制 Key" to "複製 Key", "已有固定Key说明" to "本機已有固定 Key，不會重複生成。如需換號請先手動清空 API Key。", "已生成本机固定 Key，已保存" to "已生成本機固定 Key，已儲存", "生成我的 Key" to "生成我的 Key", "同步成功" to "同步成功", "同步到云端" to "同步到雲端", "云端 Telegram" to "雲端 Telegram", "启用 TG 配置" to "啟用 TG 設定", "TG配置说明" to "先給機器人傳送 /start，再點測試TG。", "云端邮件" to "雲端郵件", "SMTP 自动发邮件" to "SMTP 自動發郵件", "SMTP 服务器" to "SMTP 伺服器", "端口" to "連接埠", "邮箱账号" to "郵箱帳號", "授权码" to "授權碼", "发件邮箱" to "寄件郵箱", "收件邮箱" to "收件郵箱", "SMTP授权码说明" to "授權碼不是郵箱登入密碼；QQ/網易/Gmail 都需要在郵箱後台生成 SMTP 授權碼。", "本地通知提醒" to "本地通知提醒", "通知一键发邮件" to "通知一鍵發郵件", "本地通知说明" to "本地通知依賴手機背景執行；雲端提醒由伺服器傳送，不怕手機被殺背景。", "TG 测试已发送" to "TG 測試已傳送", "测试TG" to "測試TG", "邮件测试已发送" to "郵件測試已傳送", "测试邮件" to "測試郵件", "已触发云端检查" to "已觸發雲端檢查", "立即检查到期" to "立即檢查到期", "云端服务说明" to "雲端服務會每 30 分鐘檢查一次。手機被殺背景時，伺服器仍可按已同步資料傳送 Telegram / 郵件提醒。每個人用獨立 API Key，資料互相隔離。", "真实下载数据测试" to "真實下載資料測試"),
        "English" to mapOf(
            "云端提醒" to "Cloud reminders", "启用云端提醒" to "Enable cloud reminders", "自动同步" to "Auto sync", "自动同步说明" to "When enabled, adding/editing/deleting/renewing/importing numbers and changing reminder settings will sync to the cloud automatically.", "API Key说明" to "Each phone generates one fixed API Key. You can paste a full message; the app extracts the real Key and removes line breaks.", "当前 API Key：" to "Current API Key: ", "未设置" to "Not set", "连接成功" to "Connected", "测试连接" to "Test connection", "已复制 API Key" to "API Key copied", "请先生成或填写 API Key" to "Generate or enter an API Key first", "复制 Key" to "Copy Key", "已有固定Key说明" to "This phone already has a fixed Key. To replace it, clear the API Key manually first.", "已生成本机固定 Key，已保存" to "Fixed Key generated and saved", "生成我的 Key" to "Generate my Key", "同步成功" to "Synced", "同步到云端" to "Sync to cloud", "云端 Telegram" to "Cloud Telegram", "启用 TG 配置" to "Enable TG config", "TG配置说明" to "Send /start to the bot first, then tap Test TG.", "云端邮件" to "Cloud email", "SMTP 自动发邮件" to "SMTP auto email", "SMTP 服务器" to "SMTP server", "端口" to "Port", "邮箱账号" to "Email account", "授权码" to "App password", "发件邮箱" to "From email", "收件邮箱" to "To email", "SMTP授权码说明" to "The app password is not your email login password. QQ/NetEase/Gmail require SMTP app passwords.", "本地通知提醒" to "Local notification", "通知一键发邮件" to "Email action in notification", "本地通知说明" to "Local notifications depend on phone background permissions. Cloud reminders are sent by the server.", "TG 测试已发送" to "TG test sent", "测试TG" to "Test TG", "邮件测试已发送" to "Email test sent", "测试邮件" to "Test email", "已触发云端检查" to "Cloud check triggered", "立即检查到期" to "Check expiry now", "云端服务说明" to "The cloud service checks every 30 minutes. Even if the phone app is killed, the server can still send Telegram/email reminders based on synced data. Each user has an isolated API Key.", "真实下载数据测试" to "Real download data test"),
        "日本語" to mapOf(
            "云端提醒" to "クラウド通知", "启用云端提醒" to "クラウド通知を有効化", "自动同步" to "自動同期", "自动同步说明" to "有効にすると、番号の追加・編集・削除・維持・インポートや通知設定の変更時に自動でクラウド同期します。", "API Key说明" to "端末ごとに固定 API Key を1回生成します。文章ごと貼り付けても Key を抽出し改行を削除します。", "当前 API Key：" to "現在の API Key：", "未设置" to "未設定", "连接成功" to "接続成功", "测试连接" to "接続テスト", "已复制 API Key" to "API Keyをコピーしました", "请先生成或填写 API Key" to "先に API Key を生成または入力してください", "复制 Key" to "Keyをコピー", "已有固定Key说明" to "この端末には固定 Key があります。変更する場合は先に API Key を手動で空にしてください。", "已生成本机固定 Key，已保存" to "端末固定 Key を生成して保存しました", "生成我的 Key" to "Keyを生成", "同步成功" to "同期成功", "同步到云端" to "クラウド同期", "云端 Telegram" to "クラウド Telegram", "启用 TG 配置" to "TG設定を有効化", "TG配置说明" to "先にBotへ /start を送ってからTGテストを押してください。", "云端邮件" to "クラウドメール", "SMTP 自动发邮件" to "SMTP自動メール", "SMTP 服务器" to "SMTPサーバー", "端口" to "ポート", "邮箱账号" to "メールアカウント", "授权码" to "アプリパスワード", "发件邮箱" to "送信元メール", "收件邮箱" to "宛先メール", "SMTP授权码说明" to "通常のログインパスワードではありません。QQ/NetEase/GmailではSMTP用アプリパスワードが必要です。", "本地通知提醒" to "ローカル通知", "通知一键发邮件" to "通知からメール送信", "本地通知说明" to "ローカル通知は端末のバックグラウンド権限に依存します。クラウド通知はサーバーから送信されます。", "TG 测试已发送" to "TGテスト送信済み", "测试TG" to "TGテスト", "邮件测试已发送" to "メールテスト送信済み", "测试邮件" to "メールテスト", "已触发云端检查" to "クラウドチェックを開始しました", "立即检查到期" to "今すぐ期限確認", "云端服务说明" to "クラウドサービスは30分ごとに確認します。アプリが終了されても、同期済みデータに基づきTelegram/メール通知を送信できます。各ユーザーは独立したAPI Keyで隔離されます。", "真实下载数据测试" to "実ダウンロードデータテスト"),
        "阿拉伯语" to mapOf(
            "云端提醒" to "تذكيرات السحابة", "启用云端提醒" to "تفعيل تذكيرات السحابة", "自动同步" to "مزامنة تلقائية", "自动同步说明" to "عند التفعيل، ستتم مزامنة الإضافة والتعديل والحذف والتجديد والاستيراد وتغييرات التذكير تلقائيًا.", "API Key说明" to "يتم إنشاء API Key ثابت لكل هاتف مرة واحدة. يمكن لصق النص كاملًا وسيستخرج التطبيق المفتاح الحقيقي.", "当前 API Key：" to "API Key الحالي: ", "未设置" to "غير مضبوط", "连接成功" to "تم الاتصال", "测试连接" to "اختبار الاتصال", "已复制 API Key" to "تم نسخ API Key", "请先生成或填写 API Key" to "أنشئ أو أدخل API Key أولاً", "复制 Key" to "نسخ Key", "已有固定Key说明" to "هذا الهاتف لديه Key ثابت بالفعل. لتغييره امسح API Key يدويًا أولاً.", "已生成本机固定 Key，已保存" to "تم إنشاء وحفظ Key ثابت", "生成我的 Key" to "إنشاء Key", "同步成功" to "تمت المزامنة", "同步到云端" to "مزامنة للسحابة", "云端 Telegram" to "Telegram السحابي", "启用 TG 配置" to "تفعيل إعداد TG", "TG配置说明" to "أرسل /start إلى البوت أولاً، ثم اختبر TG.", "云端邮件" to "البريد السحابي", "SMTP 自动发邮件" to "إرسال SMTP تلقائي", "SMTP 服务器" to "خادم SMTP", "端口" to "المنفذ", "邮箱账号" to "حساب البريد", "授权码" to "كلمة مرور التطبيق", "发件邮箱" to "بريد المرسل", "收件邮箱" to "بريد المستلم", "SMTP授权码说明" to "كلمة مرور التطبيق ليست كلمة مرور البريد. يتطلب QQ/NetEase/Gmail كلمة مرور SMTP خاصة.", "本地通知提醒" to "إشعار محلي", "通知一键发邮件" to "إرسال بريد من الإشعار", "本地通知说明" to "الإشعارات المحلية تعتمد على صلاحيات الخلفية. تذكيرات السحابة يرسلها الخادم.", "TG 测试已发送" to "تم إرسال اختبار TG", "测试TG" to "اختبار TG", "邮件测试已发送" to "تم إرسال اختبار البريد", "测试邮件" to "اختبار البريد", "已触发云端检查" to "تم تشغيل فحص السحابة", "立即检查到期" to "فحص الانتهاء الآن", "云端服务说明" to "تفحص خدمة السحابة كل 30 دقيقة. حتى إذا أُغلق التطبيق، يمكن للخادم إرسال تذكيرات Telegram/البريد حسب البيانات المتزامنة. لكل مستخدم API Key مستقل.", "真实下载数据测试" to "اختبار تنزيل بيانات حقيقي")
    )
    extra[lang]?.get(key)?.let { return it }

    val supplement = mapOf(
        "繁体中文" to mapOf(
            "流量接口" to "流量接口", "流量接口 URL" to "流量接口 URL", "默认流量 KB" to "預設流量 KB",
            "下载测试接口" to "下載測試接口", "目标流量" to "目標流量", "例：100KB / 1MB / 50MB" to "例：100KB / 1MB / 50MB",
            "204 / 空响应接口不能真正消耗流量，建议使用 Cloudflare 或 Hetzner。" to "204 / 空回應接口不能真正消耗流量，建議使用 Cloudflare 或 Hetzner。",
            "开始刷流量" to "開始刷流量", "确认刷流量？" to "確認刷流量？", "将实际下载约" to "將實際下載約", "确认后会真实消耗当前网络流量。" to "確認後會真實消耗目前網路流量。",
            "请求中…" to "請求中…", "成功" to "成功", "实际读取" to "實際讀取", "目标" to "目標", "耗时" to "耗時", "秒" to "秒", "约" to "約", "失败" to "失敗",
            "选择刷流量号码" to "選擇刷流量號碼", "选择拨号号码" to "選擇撥號號碼", "暂无号码，请先添加号码。" to "暫無號碼，請先新增號碼。",
            "支持 JSON / CSV，导入前建议先导出备份。" to "支援 JSON / CSV，匯入前建議先匯出備份。", "粘贴 JSON 或 CSV 数据" to "貼上 JSON 或 CSV 資料", "导入" to "匯入",
            "隐藏详情" to "隱藏詳情", "显示二维码" to "顯示 QR 碼", "复制号码" to "複製號碼", "删除号码？" to "刪除號碼？", "删除后不可恢复" to "刪除後不可復原",
            "如 AIS / Vodafone / 中国移动" to "如 AIS / Vodafone / 中國移動", "留空时会按号码和国家自动识别。" to "留空時會依號碼和國家自動識別。", "当前识别" to "目前識別", "当前选择" to "目前選擇", "推荐运营商" to "推薦電信商",
            "输入手机号码" to "輸入手機號碼", "如 1 RMB / 4.50 USD / 2GB" to "如 1 RMB / 4.50 USD / 2GB", "预付费 / 资费 / 套餐备注" to "預付費 / 資費 / 套餐備註", "在线 / 离线 / 漫游 / 无服务" to "線上 / 離線 / 漫遊 / 無服務",
            "输入 EID" to "輸入 EID", "服务器地址" to "伺服器地址", "未填写激活信息" to "未填寫啟用資訊", "可扫描/粘贴二维码内容，或从相册选择二维码图片" to "可掃描/貼上 QR 碼內容，或從相簿選擇 QR 碼圖片", "扫描二维码" to "掃描 QR 碼", "相册读取" to "相簿讀取", "激活信息已填写" to "啟用資訊已填寫",
            "未记录" to "未記錄", "填写二维码内容" to "填寫 QR 碼內容", "可粘贴 LPA、SM-DP+ 或激活码，保存后自动解析。" to "可貼上 LPA、SM-DP+ 或啟用碼，儲存後自動解析。", "LPA:1\$SM-DP+\$激活码" to "LPA:1\$SM-DP+\$啟用碼",
            "年" to "年", "月" to "月", "日" to "日", "今天" to "今天", "7天后" to "7天後", "30天后" to "30天後", "90天后" to "90天後",
            "云端地址未填写" to "雲端地址未填寫", "API Key 未填写" to "API Key 未填寫"
        ),
        "English" to mapOf(
            "流量接口" to "Traffic endpoint", "流量接口 URL" to "Traffic endpoint URL", "默认流量 KB" to "Default traffic KB",
            "下载测试接口" to "Download test endpoint", "目标流量" to "Target traffic", "例：100KB / 1MB / 50MB" to "Example: 100KB / 1MB / 50MB",
            "204 / 空响应接口不能真正消耗流量，建议使用 Cloudflare 或 Hetzner。" to "204 / empty-response endpoints do not consume real traffic. Use Cloudflare or Hetzner.",
            "开始刷流量" to "Start traffic test", "确认刷流量？" to "Confirm traffic test?", "将实际下载约" to "About to download", "确认后会真实消耗当前网络流量。" to "This will really consume current network data after confirmation.",
            "请求中…" to "Requesting…", "成功" to "Success", "实际读取" to "Read", "目标" to "Target", "耗时" to "Time", "秒" to "sec", "约" to "about", "失败" to "Failed",
            "选择刷流量号码" to "Select number for traffic test", "选择拨号号码" to "Select number to dial", "暂无号码，请先添加号码。" to "No numbers yet. Add one first.",
            "支持 JSON / CSV，导入前建议先导出备份。" to "Supports JSON / CSV. Export a backup before importing.", "粘贴 JSON 或 CSV 数据" to "Paste JSON or CSV data", "导入" to "Import",
            "隐藏详情" to "Hide details", "显示二维码" to "Show QR", "复制号码" to "Copy number", "删除号码？" to "Delete number?", "删除后不可恢复" to "This cannot be undone",
            "如 AIS / Vodafone / 中国移动" to "e.g. AIS / Vodafone / China Mobile", "留空时会按号码和国家自动识别。" to "Leave blank to auto-detect by number and country.", "当前识别" to "Detected", "当前选择" to "Selected", "推荐运营商" to "Suggested carriers",
            "输入手机号码" to "Enter phone number", "如 1 RMB / 4.50 USD / 2GB" to "e.g. 1 RMB / 4.50 USD / 2GB", "预付费 / 资费 / 套餐备注" to "Prepaid / tariff / plan note", "在线 / 离线 / 漫游 / 无服务" to "Online / Offline / Roaming / No service",
            "输入 EID" to "Enter EID", "服务器地址" to "Server address", "未填写激活信息" to "No activation info", "可扫描/粘贴二维码内容，或从相册选择二维码图片" to "Scan/paste QR content, or select a QR image from album", "扫描二维码" to "Scan QR", "相册读取" to "Read from album", "激活信息已填写" to "Activation info filled",
            "未记录" to "Not recorded", "填写二维码内容" to "Enter QR content", "可粘贴 LPA、SM-DP+ 或激活码，保存后自动解析。" to "Paste LPA, SM-DP+ or activation code; it will be parsed after saving.", "LPA:1\$SM-DP+\$激活码" to "LPA:1\$SM-DP+\$ActivationCode",
            "年" to "Year", "月" to "Month", "日" to "Day", "今天" to "Today", "7天后" to "In 7 days", "30天后" to "In 30 days", "90天后" to "In 90 days",
            "云端地址未填写" to "Cloud address is empty", "API Key 未填写" to "API Key is empty"
        ),
        "日本語" to mapOf(
            "流量接口" to "通信量テスト先", "流量接口 URL" to "通信量テストURL", "默认流量 KB" to "既定通信量 KB",
            "下载测试接口" to "ダウンロードテスト先", "目标流量" to "目標通信量", "例：100KB / 1MB / 50MB" to "例：100KB / 1MB / 50MB",
            "204 / 空响应接口不能真正消耗流量，建议使用 Cloudflare 或 Hetzner。" to "204/空応答のURLは通信量を消費しません。Cloudflare または Hetzner を推奨します。",
            "开始刷流量" to "通信量テスト開始", "确认刷流量？" to "通信量テストを確認？", "将实际下载约" to "実際に約", "确认后会真实消耗当前网络流量。" to "確認後、現在のネットワーク通信量を実際に消費します。",
            "请求中…" to "リクエスト中…", "成功" to "成功", "实际读取" to "読み取り", "目标" to "目標", "耗时" to "時間", "秒" to "秒", "约" to "約", "失败" to "失敗",
            "选择刷流量号码" to "通信量テスト番号を選択", "选择拨号号码" to "発信番号を選択", "暂无号码，请先添加号码。" to "番号がありません。先に追加してください。",
            "支持 JSON / CSV，导入前建议先导出备份。" to "JSON / CSV対応。インポート前にバックアップを推奨します。", "粘贴 JSON 或 CSV 数据" to "JSON または CSV を貼り付け", "导入" to "インポート",
            "隐藏详情" to "詳細を隠す", "显示二维码" to "QRを表示", "复制号码" to "番号をコピー", "删除号码？" to "番号を削除？", "删除后不可恢复" to "削除後は復元できません",
            "如 AIS / Vodafone / 中国移动" to "例：AIS / Vodafone / China Mobile", "留空时会按号码和国家自动识别。" to "空欄なら番号と国から自動識別します。", "当前识别" to "識別結果", "当前选择" to "選択中", "推荐运营商" to "推奨キャリア",
            "输入手机号码" to "電話番号を入力", "如 1 RMB / 4.50 USD / 2GB" to "例：1 RMB / 4.50 USD / 2GB", "预付费 / 资费 / 套餐备注" to "プリペイド / 料金 / メモ", "在线 / 离线 / 漫游 / 无服务" to "オンライン / オフライン / ローミング / 圏外",
            "输入 EID" to "EIDを入力", "服务器地址" to "サーバーアドレス", "未填写激活信息" to "有効化情報なし", "可扫描/粘贴二维码内容，或从相册选择二维码图片" to "QR内容をスキャン/貼り付け、またはアルバムから選択", "扫描二维码" to "QRスキャン", "相册读取" to "アルバムから読む", "激活信息已填写" to "有効化情報入力済み",
            "未记录" to "未記録", "填写二维码内容" to "QR内容を入力", "可粘贴 LPA、SM-DP+ 或激活码，保存后自动解析。" to "LPA、SM-DP+、有効化コードを貼り付けると保存後に解析します。", "LPA:1\$SM-DP+\$激活码" to "LPA:1\$SM-DP+\$ActivationCode",
            "年" to "年", "月" to "月", "日" to "日", "今天" to "今日", "7天后" to "7日後", "30天后" to "30日後", "90天后" to "90日後",
            "云端地址未填写" to "クラウドアドレス未入力", "API Key 未填写" to "API Key未入力"
        ),
        "阿拉伯语" to mapOf(
            "流量接口" to "واجهة البيانات", "流量接口 URL" to "رابط واجهة البيانات", "默认流量 KB" to "البيانات الافتراضية KB",
            "下载测试接口" to "واجهة اختبار التنزيل", "目标流量" to "حجم البيانات", "例：100KB / 1MB / 50MB" to "مثال: 100KB / 1MB / 50MB",
            "204 / 空响应接口不能真正消耗流量，建议使用 Cloudflare 或 Hetzner。" to "واجهات 204/الفارغة لا تستهلك بيانات فعلية. استخدم Cloudflare أو Hetzner.",
            "开始刷流量" to "بدء اختبار البيانات", "确认刷流量？" to "تأكيد اختبار البيانات؟", "将实际下载约" to "سيتم تنزيل حوالي", "确认后会真实消耗当前网络流量。" to "بعد التأكيد سيستهلك بيانات الشبكة فعليًا.",
            "请求中…" to "جارٍ الطلب…", "成功" to "نجاح", "实际读取" to "تمت القراءة", "目标" to "الهدف", "耗时" to "الوقت", "秒" to "ث", "约" to "حوالي", "失败" to "فشل",
            "选择刷流量号码" to "اختر رقم اختبار البيانات", "选择拨号号码" to "اختر رقم الاتصال", "暂无号码，请先添加号码。" to "لا توجد أرقام، أضف رقمًا أولاً.",
            "支持 JSON / CSV，导入前建议先导出备份。" to "يدعم JSON / CSV. يُفضل تصدير نسخة احتياطية قبل الاستيراد.", "粘贴 JSON 或 CSV 数据" to "الصق بيانات JSON أو CSV", "导入" to "استيراد",
            "隐藏详情" to "إخفاء التفاصيل", "显示二维码" to "إظهار QR", "复制号码" to "نسخ الرقم", "删除号码？" to "حذف الرقم؟", "删除后不可恢复" to "لا يمكن التراجع بعد الحذف",
            "如 AIS / Vodafone / 中国移动" to "مثل AIS / Vodafone / China Mobile", "留空时会按号码和国家自动识别。" to "اتركه فارغًا للتعرّف تلقائيًا حسب الرقم والدولة.", "当前识别" to "المكتشف", "当前选择" to "المحدد", "推荐运营商" to "مشغلون مقترحون",
            "输入手机号码" to "أدخل رقم الهاتف", "如 1 RMB / 4.50 USD / 2GB" to "مثل 1 RMB / 4.50 USD / 2GB", "预付费 / 资费 / 套餐备注" to "مسبق الدفع / التعرفة / ملاحظة", "在线 / 离线 / 漫游 / 无服务" to "متصل / غير متصل / تجوال / لا خدمة",
            "输入 EID" to "أدخل EID", "服务器地址" to "عنوان الخادم", "未填写激活信息" to "لا توجد معلومات تفعيل", "可扫描/粘贴二维码内容，或从相册选择二维码图片" to "امسح/الصق محتوى QR أو اختر صورة من الألبوم", "扫描二维码" to "مسح QR", "相册读取" to "قراءة من الألبوم", "激活信息已填写" to "تم إدخال معلومات التفعيل",
            "未记录" to "غير مسجل", "填写二维码内容" to "أدخل محتوى QR", "可粘贴 LPA、SM-DP+ 或激活码，保存后自动解析。" to "يمكن لصق LPA أو SM-DP+ أو رمز التفعيل وسيتم تحليله بعد الحفظ.", "LPA:1\$SM-DP+\$激活码" to "LPA:1\$SM-DP+\$ActivationCode",
            "年" to "السنة", "月" to "الشهر", "日" to "اليوم", "今天" to "اليوم", "7天后" to "بعد 7 أيام", "30天后" to "بعد 30 يومًا", "90天后" to "بعد 90 يومًا",
            "云端地址未填写" to "عنوان السحابة فارغ", "API Key 未填写" to "API Key فارغ"
        )
    )
    supplement[lang]?.get(key)?.let { return it }
    val zhHant = mapOf(
        "号码" to "號碼", "工具" to "工具", "设置" to "設定", "完成" to "完成", "取消" to "取消", "保存" to "儲存", "确认" to "確認", "确认延长" to "確認延長", "删除" to "刪除", "编辑" to "編輯", "保号" to "保號", "刷流量" to "刷流量", "搜索运营商、国家或号码" to "搜尋電信商、國家或號碼", "到期时间（远到近）" to "到期時間（遠到近）", "到期时间（近到远）" to "到期時間（近到遠）", "暂无号码" to "暫無號碼", "点击右下角添加号码" to "點擊右下角新增號碼", "工具" to "工具", "常用工具" to "常用工具", "拨号测试" to "撥號測試", "导出 JSON" to "匯出 JSON", "导出 CSV" to "匯出 CSV", "导入数据" to "匯入資料", "外观" to "外觀", "深色模式" to "深色模式", "更改背景图片" to "更改背景圖片", "清除" to "清除", "提醒设置" to "提醒設定", "开启到期提醒" to "開啟到期提醒", "语言 / Language" to "語言 / Language", "Telegram 通知" to "Telegram 通知", "关于" to "關於", "新增 eSIM" to "新增 eSIM", "编辑 eSIM" to "編輯 eSIM", "运营商与国家" to "電信商與國家", "国家/地区" to "國家/地區", "运营商名称" to "電信商名稱", "号码与套餐" to "號碼與套餐", "手机号码" to "手機號碼", "套餐余额" to "套餐餘額", "套餐备注" to "套餐備註", "信号状态" to "訊號狀態", "日期与周期" to "日期與週期", "开始日期" to "開始日期", "到期日期" to "到期日期", "套餐周期" to "套餐週期", "长期号码" to "長期號碼", "eSIM 激活信息" to "eSIM 啟用資訊", "编辑 EID" to "編輯 EID", "激活码" to "啟用碼", "记录信息" to "記錄資訊", "创建时间" to "建立時間", "激活时间" to "啟用時間", "选择国家区号" to "選擇國家區號", "国家区号库" to "國家區號庫", "全部国家 / 地区" to "全部國家 / 地區", "复制" to "複製", "导出文件" to "匯出檔案", "关闭" to "關閉", "好" to "好")
    val en = mapOf(
        "号码" to "Numbers", "工具" to "Tools", "设置" to "Settings", "完成" to "Done", "取消" to "Cancel", "保存" to "Save", "确认" to "Confirm", "确认延长" to "Extend", "删除" to "Delete", "编辑" to "Edit", "保号" to "Renew", "刷流量" to "Traffic", "搜索运营商、国家或号码" to "Search carrier, country or number", "到期时间（远到近）" to "Expiry date (far to near)", "到期时间（近到远）" to "Expiry date (near to far)", "暂无号码" to "No numbers", "点击右下角添加号码" to "Tap + to add a number", "常用工具" to "Tools", "选择一个号码执行真实下载流量测试" to "Pick a number and run a real download traffic test", "拨号测试" to "Dial test", "选择号码并打开系统拨号器" to "Pick a number and open system dialer", "导出 JSON" to "Export JSON", "生成完整 JSON 备份文本" to "Generate full JSON backup text", "导出 CSV" to "Export CSV", "生成 CSV 表格文本" to "Generate CSV table text", "导入数据" to "Import data", "粘贴 JSON 或 CSV 恢复号码列表" to "Paste JSON or CSV to restore records", "外观" to "Appearance", "深色模式" to "Dark mode", "更改背景图片" to "Change background", "清除" to "Clear", "提醒设置" to "Reminder settings", "开启到期提醒" to "Enable expiry reminders", "语言 / Language" to "Language", "Telegram 通知" to "Telegram notifications", "关于" to "About", "新增 eSIM" to "Add eSIM", "编辑 eSIM" to "Edit eSIM", "运营商与国家" to "Carrier & country", "国家/地区" to "Country/Region", "运营商名称" to "Carrier name", "号码与套餐" to "Number & plan", "手机号码" to "Phone number", "套餐余额" to "Plan balance", "套餐备注" to "Plan note", "信号状态" to "Signal status", "日期与周期" to "Dates & cycle", "开始日期" to "Start date", "到期日期" to "Expiry date", "套餐周期" to "Plan cycle", "长期号码" to "Long-term number", "eSIM 激活信息" to "eSIM activation info", "编辑 EID" to "Edit EID", "激活码" to "Activation code", "记录信息" to "Record info", "创建时间" to "Created at", "激活时间" to "Activated at", "选择国家区号" to "Select country code", "搜索国家 / 区号 / ISO" to "Search country / code / ISO", "国家区号库" to "Country code library", "全部国家 / 地区" to "All countries / regions", "当前语言：" to "Current language: ", "已启用 RTL 右到左布局" to "RTL right-to-left layout enabled", "支持实时切换，主要页面会立即刷新。" to "Language switches live on main pages.", "复制" to "Copy", "导出文件" to "Export file", "关闭" to "Close", "好" to "OK")
    val ja = mapOf(
        "号码" to "番号", "工具" to "ツール", "设置" to "設定", "完成" to "完了", "取消" to "キャンセル", "保存" to "保存", "确认" to "確認", "确认延长" to "延長", "删除" to "削除", "编辑" to "編集", "保号" to "維持", "刷流量" to "通信量", "搜索运营商、国家或号码" to "キャリア・国・番号を検索", "到期时间（远到近）" to "期限（遠い順）", "到期时间（近到远）" to "期限（近い順）", "暂无号码" to "番号なし", "点击右下角添加号码" to "右下の＋で追加", "常用工具" to "ツール", "拨号测试" to "発信テスト", "导出 JSON" to "JSON出力", "导出 CSV" to "CSV出力", "导入数据" to "データ取込", "外观" to "外観", "深色模式" to "ダークモード", "更改背景图片" to "背景を変更", "清除" to "クリア", "提醒设置" to "通知設定", "开启到期提醒" to "期限通知を有効化", "语言 / Language" to "言語 / Language", "Telegram 通知" to "Telegram通知", "关于" to "情報", "新增 eSIM" to "eSIM追加", "编辑 eSIM" to "eSIM編集", "运营商与国家" to "キャリアと国", "国家/地区" to "国/地域", "运营商名称" to "キャリア名", "号码与套餐" to "番号とプラン", "手机号码" to "電話番号", "套餐余额" to "残高", "套餐备注" to "プランメモ", "信号状态" to "信号状態", "日期与周期" to "日付と周期", "开始日期" to "開始日", "到期日期" to "期限日", "套餐周期" to "周期", "长期号码" to "長期番号", "eSIM 激活信息" to "eSIM有効化情報", "编辑 EID" to "EID編集", "激活码" to "アクティベーションコード", "记录信息" to "記録情報", "创建时间" to "作成日時", "激活时间" to "有効化日時", "选择国家区号" to "国番号を選択", "国家区号库" to "国番号ライブラリ", "全部国家 / 地区" to "すべての国/地域", "复制" to "コピー", "导出文件" to "ファイル出力", "关闭" to "閉じる", "好" to "OK")
    val ar = mapOf(
        "号码" to "الأرقام", "工具" to "الأدوات", "设置" to "الإعدادات", "完成" to "تم", "取消" to "إلغاء", "保存" to "حفظ", "确认" to "تأكيد", "确认延长" to "تمديد", "删除" to "حذف", "编辑" to "تعديل", "保号" to "تجديد", "刷流量" to "بيانات", "搜索运营商、国家或号码" to "ابحث عن المشغل أو الدولة أو الرقم", "到期时间（远到近）" to "انتهاء الصلاحية (الأبعد أولاً)", "到期时间（近到远）" to "انتهاء الصلاحية (الأقرب أولاً)", "暂无号码" to "لا توجد أرقام", "点击右下角添加号码" to "اضغط + لإضافة رقم", "常用工具" to "الأدوات", "拨号测试" to "اختبار الاتصال", "导出 JSON" to "تصدير JSON", "导出 CSV" to "تصدير CSV", "导入数据" to "استيراد البيانات", "外观" to "المظهر", "深色模式" to "الوضع الداكن", "更改背景图片" to "تغيير الخلفية", "清除" to "مسح", "提醒设置" to "إعدادات التذكير", "开启到期提醒" to "تفعيل تذكير الانتهاء", "语言 / Language" to "اللغة / Language", "Telegram 通知" to "إشعارات Telegram", "关于" to "حول", "新增 eSIM" to "إضافة eSIM", "编辑 eSIM" to "تعديل eSIM", "运营商与国家" to "المشغل والدولة", "国家/地区" to "الدولة/المنطقة", "运营商名称" to "اسم المشغل", "号码与套餐" to "الرقم والباقه", "手机号码" to "رقم الهاتف", "套餐余额" to "الرصيد", "套餐备注" to "ملاحظة الباقة", "信号状态" to "حالة الإشارة", "日期与周期" to "التاريخ والدورة", "开始日期" to "تاريخ البدء", "到期日期" to "تاريخ الانتهاء", "套餐周期" to "دورة الباقة", "长期号码" to "رقم طويل الأمد", "eSIM 激活信息" to "معلومات تفعيل eSIM", "编辑 EID" to "تعديل EID", "激活码" to "رمز التفعيل", "记录信息" to "معلومات السجل", "创建时间" to "تاريخ الإنشاء", "激活时间" to "تاريخ التفعيل", "选择国家区号" to "اختر رمز الدولة", "国家区号库" to "مكتبة رموز الدول", "全部国家 / 地区" to "كل الدول / المناطق", "复制" to "نسخ", "导出文件" to "تصدير ملف", "关闭" to "إغلاق", "好" to "حسنًا")
    return when(lang){"繁体中文"->zhHant[key];"English"->en[key];"日本語"->ja[key];"阿拉伯语"->ar[key];else->null} ?: key
}

fun dayText(lang:String, n:Long):String = when(lang){"English"->"$n days";"日本語"->"${n}日";"阿拉伯语"->"$n أيام";else->"${n}天"}
fun laterText(lang:String, n:Int):String = if(n==0) tr(lang,"今天") else when(lang){"English"->"$n days later";"日本語"->"${n}日後";"阿拉伯语"->"بعد $n أيام";else->"${n}天后"}
fun expireText(lang:String, days:Long?):String = when { days==null -> tr(lang,"未知"); days<0 -> tr(lang,"已过期") + when(lang){"English"->" ${-days} days";"日本語"->" ${-days}日";"阿拉伯语"->" ${-days} أيام";else->"${-days}天"}; else -> when(lang){"English"->"${days} days left";"日本語"->"残り${days}日";"阿拉伯语"->"متبقي $days أيام";else->"还有${days}天"} }
fun cycleText(lang:String, d:Int):String = if(d==365) tr(lang,"1年") else when(lang){"English"->"$d days";"日本語"->"${d}日";"阿拉伯语"->"$d أيام";else->"${d}天"}
@Composable fun LT(key:String):String = tr(LocalAppLanguage.current,key)
@Composable fun L(key:String):String = tr(LocalAppLanguage.current,key)


object Countries {
    val list = listOf(
        Country("🇨🇳","中国","+86","CN"),
        Country("🇭🇰","香港","+852","HK"),
        Country("🇲🇴","澳门","+853","MO"),
        Country("🇹🇼","台湾","+886","TW"),
        Country("🇺🇸","美国","+1","US"),
        Country("🇨🇦","加拿大","+1","CA"),
        Country("🇬🇧","英国","+44","GB"),
        Country("🇩🇪","德国","+49","DE"),
        Country("🇫🇷","法国","+33","FR"),
        Country("🇮🇹","意大利","+39","IT"),
        Country("🇪🇸","西班牙","+34","ES"),
        Country("🇳🇱","荷兰","+31","NL"),
        Country("🇧🇪","比利时","+32","BE"),
        Country("🇨🇭","瑞士","+41","CH"),
        Country("🇦🇹","奥地利","+43","AT"),
        Country("🇸🇪","瑞典","+46","SE"),
        Country("🇳🇴","挪威","+47","NO"),
        Country("🇩🇰","丹麦","+45","DK"),
        Country("🇫🇮","芬兰","+358","FI"),
        Country("🇮🇸","冰岛","+354","IS"),
        Country("🇮🇪","爱尔兰","+353","IE"),
        Country("🇵🇹","葡萄牙","+351","PT"),
        Country("🇬🇷","希腊","+30","GR"),
        Country("🇵🇱","波兰","+48","PL"),
        Country("🇨🇿","捷克","+420","CZ"),
        Country("🇸🇰","斯洛伐克","+421","SK"),
        Country("🇭🇺","匈牙利","+36","HU"),
        Country("🇷🇴","罗马尼亚","+40","RO"),
        Country("🇧🇬","保加利亚","+359","BG"),
        Country("🇭🇷","克罗地亚","+385","HR"),
        Country("🇸🇮","斯洛文尼亚","+386","SI"),
        Country("🇷🇸","塞尔维亚","+381","RS"),
        Country("🇧🇦","波黑","+387","BA"),
        Country("🇲🇪","黑山","+382","ME"),
        Country("🇲🇰","北马其顿","+389","MK"),
        Country("🇦🇱","阿尔巴尼亚","+355","AL"),
        Country("🇱🇹","立陶宛","+370","LT"),
        Country("🇱🇻","拉脱维亚","+371","LV"),
        Country("🇪🇪","爱沙尼亚","+372","EE"),
        Country("🇲🇩","摩尔多瓦","+373","MD"),
        Country("🇧🇾","白俄罗斯","+375","BY"),
        Country("🇺🇦","乌克兰","+380","UA"),
        Country("🇷🇺","俄罗斯","+7","RU"),
        Country("🇰🇿","哈萨克斯坦","+7","KZ"),
        Country("🇬🇪","格鲁吉亚","+995","GE"),
        Country("🇦🇲","亚美尼亚","+374","AM"),
        Country("🇦🇿","阿塞拜疆","+994","AZ"),
        Country("🇹🇷","土耳其","+90","TR"),
        Country("🇮🇱","以色列","+972","IL"),
        Country("🇦🇪","阿联酋","+971","AE"),
        Country("🇸🇦","沙特阿拉伯","+966","SA"),
        Country("🇶🇦","卡塔尔","+974","QA"),
        Country("🇰🇼","科威特","+965","KW"),
        Country("🇧🇭","巴林","+973","BH"),
        Country("🇴🇲","阿曼","+968","OM"),
        Country("🇯🇴","约旦","+962","JO"),
        Country("🇱🇧","黎巴嫩","+961","LB"),
        Country("🇪🇬","埃及","+20","EG"),
        Country("🇲🇦","摩洛哥","+212","MA"),
        Country("🇹🇳","突尼斯","+216","TN"),
        Country("🇩🇿","阿尔及利亚","+213","DZ"),
        Country("🇳🇬","尼日利亚","+234","NG"),
        Country("🇰🇪","肯尼亚","+254","KE"),
        Country("🇿🇦","南非","+27","ZA"),
        Country("🇯🇵","日本","+81","JP"),
        Country("🇰🇷","韩国","+82","KR"),
        Country("🇸🇬","新加坡","+65","SG"),
        Country("🇲🇾","马来西亚","+60","MY"),
        Country("🇹🇭","泰国","+66","TH"),
        Country("🇻🇳","越南","+84","VN"),
        Country("🇵🇭","菲律宾","+63","PH"),
        Country("🇮🇩","印尼","+62","ID"),
        Country("🇰🇭","柬埔寨","+855","KH"),
        Country("🇱🇦","老挝","+856","LA"),
        Country("🇲🇲","缅甸","+95","MM"),
        Country("🇧🇳","文莱","+673","BN"),
        Country("🇮🇳","印度","+91","IN"),
        Country("🇵🇰","巴基斯坦","+92","PK"),
        Country("🇧🇩","孟加拉国","+880","BD"),
        Country("🇱🇰","斯里兰卡","+94","LK"),
        Country("🇳🇵","尼泊尔","+977","NP"),
        Country("🇲🇻","马尔代夫","+960","MV"),
        Country("🇦🇺","澳大利亚","+61","AU"),
        Country("🇳🇿","新西兰","+64","NZ"),
        Country("🇫🇯","斐济","+679","FJ"),
        Country("🇧🇷","巴西","+55","BR"),
        Country("🇦🇷","阿根廷","+54","AR"),
        Country("🇨🇱","智利","+56","CL"),
        Country("🇨🇴","哥伦比亚","+57","CO"),
        Country("🇵🇪","秘鲁","+51","PE"),
        Country("🇲🇽","墨西哥","+52","MX"),
        Country("🇺🇾","乌拉圭","+598","UY"),
        Country("🇵🇾","巴拉圭","+595","PY"),
        Country("🇧🇴","玻利维亚","+591","BO"),
        Country("🇪🇨","厄瓜多尔","+593","EC"),
        Country("🇻🇪","委内瑞拉","+58","VE"),
        Country("🇨🇷","哥斯达黎加","+506","CR"),
        Country("🇵🇦","巴拿马","+507","PA"),
        Country("🇬🇹","危地马拉","+502","GT"),
        Country("🇩🇴","多米尼加","+1","DO"),
        Country("🇯🇲","牙买加","+1","JM"),
        Country("🇱🇺","卢森堡","+352","LU"),
        Country("🇲🇹","马耳他","+356","MT"),
        Country("🇨🇾","塞浦路斯","+357","CY"),
        Country("🇲🇨","摩纳哥","+377","MC"),
        Country("🇱🇮","列支敦士登","+423","LI"),
        Country("🇦🇩","安道尔","+376","AD"),
        Country("🇸🇲","圣马力诺","+378","SM"),
        Country("🇽🇰","科索沃","+383","XK"),
        Country("🇬🇮","直布罗陀","+350","GI"),
        Country("🇫🇴","法罗群岛","+298","FO"),
        Country("🇮🇶","伊拉克","+964","IQ"),
        Country("🇮🇷","伊朗","+98","IR"),
        Country("🇸🇾","叙利亚","+963","SY"),
        Country("🇾🇪","也门","+967","YE"),
        Country("🇵🇸","巴勒斯坦","+970","PS"),
        Country("🇦🇫","阿富汗","+93","AF"),
        Country("🇺🇿","乌兹别克斯坦","+998","UZ"),
        Country("🇰🇬","吉尔吉斯斯坦","+996","KG"),
        Country("🇹🇯","塔吉克斯坦","+992","TJ"),
        Country("🇹🇲","土库曼斯坦","+993","TM"),
        Country("🇲🇳","蒙古","+976","MN"),
        Country("🇧🇹","不丹","+975","BT"),
        Country("🇹🇱","东帝汶","+670","TL"),
        Country("🇵🇬","巴布亚新几内亚","+675","PG"),
        Country("🇼🇸","萨摩亚","+685","WS"),
        Country("🇹🇴","汤加","+676","TO"),
        Country("🇻🇺","瓦努阿图","+678","VU"),
        Country("🇸🇧","所罗门群岛","+677","SB"),
        Country("🇳🇨","新喀里多尼亚","+687","NC"),
        Country("🇵🇫","法属波利尼西亚","+689","PF"),
        Country("🇬🇺","关岛","+1","GU"),
        Country("🇬🇭","加纳","+233","GH"),
        Country("🇨🇮","科特迪瓦","+225","CI"),
        Country("🇸🇳","塞内加尔","+221","SN"),
        Country("🇨🇲","喀麦隆","+237","CM"),
        Country("🇪🇹","埃塞俄比亚","+251","ET"),
        Country("🇹🇿","坦桑尼亚","+255","TZ"),
        Country("🇺🇬","乌干达","+256","UG"),
        Country("🇿🇲","赞比亚","+260","ZM"),
        Country("🇿🇼","津巴布韦","+263","ZW"),
        Country("🇦🇴","安哥拉","+244","AO"),
        Country("🇲🇿","莫桑比克","+258","MZ"),
        Country("🇲🇺","毛里求斯","+230","MU"),
        Country("🇲🇬","马达加斯加","+261","MG"),
        Country("🇷🇪","留尼汪","+262","RE"),
        Country("🇱🇾","利比亚","+218","LY"),
        Country("🇸🇩","苏丹","+249","SD"),
        Country("🇨🇩","刚果(金)","+243","CD"),
        Country("🇨🇬","刚果(布)","+242","CG"),
        Country("🇷🇼","卢旺达","+250","RW"),
        Country("🇧🇼","博茨瓦纳","+267","BW"),
        Country("🇳🇦","纳米比亚","+264","NA"),
        Country("🇧🇧","巴巴多斯","+1","BB"),
        Country("🇹🇹","特立尼达和多巴哥","+1","TT"),
        Country("🇧🇸","巴哈马","+1","BS"),
        Country("🇨🇺","古巴","+53","CU"),
        Country("🇭🇳","洪都拉斯","+504","HN"),
        Country("🇳🇮","尼加拉瓜","+505","NI"),
        Country("🇸🇻","萨尔瓦多","+503","SV"),
        Country("🇧🇿","伯利兹","+501","BZ"),
        Country("🇬🇾","圭亚那","+592","GY"),
        Country("🇸🇷","苏里南","+597","SR"),

    )
}


object OperatorDatabase {
    val list = listOf(
        OperatorInfo(1,"中国","CN","中国移动","https://www.10086.cn","https://logo.clearbit.com/10086.cn",true),
        OperatorInfo(2,"中国","CN","中国联通","https://www.10010.com","https://logo.clearbit.com/10010.com",true),
        OperatorInfo(3,"中国","CN","中国电信","https://www.189.cn","https://logo.clearbit.com/189.cn",true),
        OperatorInfo(4,"中国","CN","中国广电","https://www.10099.com.cn","https://logo.clearbit.com/10099.com.cn",false),
        OperatorInfo(5,"香港","HK","CSL","https://www.hkcsl.com","https://logo.clearbit.com/hkcsl.com",true),
        OperatorInfo(6,"香港","HK","3HK","https://www.three.com.hk","https://logo.clearbit.com/three.com.hk",true),
        OperatorInfo(7,"香港","HK","SmarTone","https://www.smartone.com","https://logo.clearbit.com/smartone.com",true),
        OperatorInfo(8,"香港","HK","China Mobile Hong Kong","https://www.hk.chinamobile.com","https://logo.clearbit.com/hk.chinamobile.com",true),
        OperatorInfo(9,"澳门","MO","CTM","https://www.ctm.net","https://logo.clearbit.com/ctm.net",true),
        OperatorInfo(10,"澳门","MO","3 Macau","https://www.three.com.mo","https://logo.clearbit.com/three.com.mo",true),
        OperatorInfo(11,"台湾","TW","中华电信","https://www.cht.com.tw","https://logo.clearbit.com/cht.com.tw",true),
        OperatorInfo(12,"台湾","TW","台湾大哥大","https://www.taiwanmobile.com","https://logo.clearbit.com/taiwanmobile.com",true),
        OperatorInfo(13,"台湾","TW","远传电信","https://www.fetnet.net","https://logo.clearbit.com/fetnet.net",true),
        OperatorInfo(14,"台湾","TW","台湾之星","https://www.tstartel.com","https://logo.clearbit.com/tstartel.com",true),
        OperatorInfo(15,"美国","US","T-Mobile","https://www.t-mobile.com","https://logo.clearbit.com/t-mobile.com",true),
        OperatorInfo(16,"美国","US","AT&T","https://www.att.com","https://logo.clearbit.com/att.com",true),
        OperatorInfo(17,"美国","US","Verizon","https://www.verizon.com","https://logo.clearbit.com/verizon.com",true),
        OperatorInfo(18,"美国","US","US Mobile","https://www.usmobile.com","https://logo.clearbit.com/usmobile.com",true),
        OperatorInfo(19,"美国","US","Mint Mobile","https://www.mintmobile.com","https://logo.clearbit.com/mintmobile.com",true),
        OperatorInfo(20,"美国","US","Visible","https://www.visible.com","https://logo.clearbit.com/visible.com",true),
        OperatorInfo(21,"美国","US","Google Fi","https://fi.google.com","https://logo.clearbit.com/fi.google.com",true),
        OperatorInfo(22,"美国","US","Boost Mobile","https://www.boostmobile.com","https://logo.clearbit.com/boostmobile.com",true),
        OperatorInfo(23,"美国","US","Cricket Wireless","https://www.cricketwireless.com","https://logo.clearbit.com/cricketwireless.com",true),
        OperatorInfo(24,"加拿大","CA","Rogers","https://www.rogers.com","https://logo.clearbit.com/rogers.com",true),
        OperatorInfo(25,"加拿大","CA","Bell","https://www.bell.ca","https://logo.clearbit.com/bell.ca",true),
        OperatorInfo(26,"加拿大","CA","Telus","https://www.telus.com","https://logo.clearbit.com/telus.com",true),
        OperatorInfo(27,"加拿大","CA","Freedom Mobile","https://www.freedommobile.ca","https://logo.clearbit.com/freedommobile.ca",true),
        OperatorInfo(28,"加拿大","CA","Fido","https://www.fido.ca","https://logo.clearbit.com/fido.ca",true),
        OperatorInfo(29,"英国","GB","EE","https://www.ee.co.uk","https://logo.clearbit.com/ee.co.uk",true),
        OperatorInfo(30,"英国","GB","O2 UK","https://www.o2.co.uk","https://logo.clearbit.com/o2.co.uk",true),
        OperatorInfo(31,"英国","GB","Vodafone UK","https://www.vodafone.co.uk","https://logo.clearbit.com/vodafone.co.uk",true),
        OperatorInfo(32,"英国","GB","Three UK","https://www.three.co.uk","https://logo.clearbit.com/three.co.uk",true),
        OperatorInfo(33,"英国","GB","giffgaff","https://www.giffgaff.com","https://logo.clearbit.com/giffgaff.com",true),
        OperatorInfo(34,"德国","DE","Deutsche Telekom","https://www.telekom.de","https://logo.clearbit.com/telekom.de",true),
        OperatorInfo(35,"德国","DE","Vodafone Germany","https://www.vodafone.de","https://logo.clearbit.com/vodafone.de",true),
        OperatorInfo(36,"德国","DE","O2 Germany","https://www.o2online.de","https://logo.clearbit.com/o2online.de",true),
        OperatorInfo(37,"德国","DE","1&1","https://www.1und1.de","https://logo.clearbit.com/1und1.de",true),
        OperatorInfo(38,"法国","FR","Orange","https://www.orange.fr","https://logo.clearbit.com/orange.fr",true),
        OperatorInfo(39,"法国","FR","SFR","https://www.sfr.fr","https://logo.clearbit.com/sfr.fr",true),
        OperatorInfo(40,"法国","FR","Bouygues Telecom","https://www.bouyguestelecom.fr","https://logo.clearbit.com/bouyguestelecom.fr",true),
        OperatorInfo(41,"法国","FR","Free Mobile","https://mobile.free.fr","https://logo.clearbit.com/mobile.free.fr",true),
        OperatorInfo(42,"意大利","IT","TIM","https://www.tim.it","https://logo.clearbit.com/tim.it",true),
        OperatorInfo(43,"意大利","IT","Vodafone Italy","https://www.vodafone.it","https://logo.clearbit.com/vodafone.it",true),
        OperatorInfo(44,"意大利","IT","WindTre","https://www.windtre.it","https://logo.clearbit.com/windtre.it",true),
        OperatorInfo(45,"意大利","IT","Iliad Italy","https://www.iliad.it","https://logo.clearbit.com/iliad.it",true),
        OperatorInfo(46,"西班牙","ES","Movistar","https://www.movistar.es","https://logo.clearbit.com/movistar.es",true),
        OperatorInfo(47,"西班牙","ES","Orange Spain","https://www.orange.es","https://logo.clearbit.com/orange.es",true),
        OperatorInfo(48,"西班牙","ES","Vodafone Spain","https://www.vodafone.es","https://logo.clearbit.com/vodafone.es",true),
        OperatorInfo(49,"西班牙","ES","Yoigo","https://www.yoigo.com","https://logo.clearbit.com/yoigo.com",true),
        OperatorInfo(50,"荷兰","NL","KPN","https://www.kpn.com","https://logo.clearbit.com/kpn.com",true),
        OperatorInfo(51,"荷兰","NL","Vodafone Netherlands","https://www.vodafone.nl","https://logo.clearbit.com/vodafone.nl",true),
        OperatorInfo(52,"荷兰","NL","Odido","https://www.odido.nl","https://logo.clearbit.com/odido.nl",true),
        OperatorInfo(53,"比利时","BE","Proximus","https://www.proximus.be","https://logo.clearbit.com/proximus.be",true),
        OperatorInfo(54,"比利时","BE","Orange Belgium","https://www.orange.be","https://logo.clearbit.com/orange.be",true),
        OperatorInfo(55,"比利时","BE","BASE","https://www.base.be","https://logo.clearbit.com/base.be",false),
        OperatorInfo(56,"瑞士","CH","Swisscom","https://www.swisscom.ch","https://logo.clearbit.com/swisscom.ch",true),
        OperatorInfo(57,"瑞士","CH","Sunrise","https://www.sunrise.ch","https://logo.clearbit.com/sunrise.ch",true),
        OperatorInfo(58,"瑞士","CH","Salt","https://www.salt.ch","https://logo.clearbit.com/salt.ch",true),
        OperatorInfo(59,"奥地利","AT","A1","https://www.a1.net","https://logo.clearbit.com/a1.net",true),
        OperatorInfo(60,"奥地利","AT","Magenta Telekom","https://www.magenta.at","https://logo.clearbit.com/magenta.at",true),
        OperatorInfo(61,"奥地利","AT","Drei Austria","https://www.drei.at","https://logo.clearbit.com/drei.at",true),
        OperatorInfo(62,"瑞典","SE","Telia Sweden","https://www.telia.se","https://logo.clearbit.com/telia.se",true),
        OperatorInfo(63,"瑞典","SE","Tele2 Sweden","https://www.tele2.se","https://logo.clearbit.com/tele2.se",true),
        OperatorInfo(64,"瑞典","SE","Telenor Sweden","https://www.telenor.se","https://logo.clearbit.com/telenor.se",true),
        OperatorInfo(65,"挪威","NO","Telenor Norway","https://www.telenor.no","https://logo.clearbit.com/telenor.no",true),
        OperatorInfo(66,"挪威","NO","Telia Norway","https://www.telia.no","https://logo.clearbit.com/telia.no",true),
        OperatorInfo(67,"挪威","NO","Ice","https://www.ice.no","https://logo.clearbit.com/ice.no",true),
        OperatorInfo(68,"丹麦","DK","TDC NET","https://tdcnet.dk","https://logo.clearbit.com/tdcnet.dk",false),
        OperatorInfo(69,"丹麦","DK","Telenor Denmark","https://www.telenor.dk","https://logo.clearbit.com/telenor.dk",true),
        OperatorInfo(70,"丹麦","DK","3 Denmark","https://www.3.dk","https://logo.clearbit.com/3.dk",true),
        OperatorInfo(71,"芬兰","FI","Elisa","https://elisa.fi","https://logo.clearbit.com/elisa.fi",true),
        OperatorInfo(72,"芬兰","FI","DNA","https://www.dna.fi","https://logo.clearbit.com/dna.fi",true),
        OperatorInfo(73,"芬兰","FI","Telia Finland","https://www.telia.fi","https://logo.clearbit.com/telia.fi",true),
        OperatorInfo(74,"冰岛","IS","冰岛 Mobile","","",false),
        OperatorInfo(75,"冰岛","IS","冰岛 Telecom","","",false),
        OperatorInfo(76,"爱尔兰","IE","Vodafone Ireland","https://n.vodafone.ie","https://logo.clearbit.com/n.vodafone.ie",true),
        OperatorInfo(77,"爱尔兰","IE","Three Ireland","https://www.three.ie","https://logo.clearbit.com/three.ie",true),
        OperatorInfo(78,"爱尔兰","IE","Eir","https://www.eir.ie","https://logo.clearbit.com/eir.ie",true),
        OperatorInfo(79,"葡萄牙","PT","MEO","https://www.meo.pt","https://logo.clearbit.com/meo.pt",true),
        OperatorInfo(80,"葡萄牙","PT","NOS","https://www.nos.pt","https://logo.clearbit.com/nos.pt",true),
        OperatorInfo(81,"葡萄牙","PT","Vodafone Portugal","https://www.vodafone.pt","https://logo.clearbit.com/vodafone.pt",true),
        OperatorInfo(82,"希腊","GR","Cosmote","https://www.cosmote.gr","https://logo.clearbit.com/cosmote.gr",true),
        OperatorInfo(83,"希腊","GR","Vodafone Greece","https://www.vodafone.gr","https://logo.clearbit.com/vodafone.gr",true),
        OperatorInfo(84,"希腊","GR","Nova Greece","https://nova.gr","https://logo.clearbit.com/nova.gr",true),
        OperatorInfo(85,"波兰","PL","Orange Poland","https://www.orange.pl","https://logo.clearbit.com/orange.pl",true),
        OperatorInfo(86,"波兰","PL","Play","https://www.play.pl","https://logo.clearbit.com/play.pl",true),
        OperatorInfo(87,"波兰","PL","Plus","https://www.plus.pl","https://logo.clearbit.com/plus.pl",true),
        OperatorInfo(88,"波兰","PL","T-Mobile Poland","https://www.t-mobile.pl","https://logo.clearbit.com/t-mobile.pl",true),
        OperatorInfo(89,"捷克","CZ","O2 Czech Republic","https://www.o2.cz","https://logo.clearbit.com/o2.cz",true),
        OperatorInfo(90,"捷克","CZ","T-Mobile Czech Republic","https://www.t-mobile.cz","https://logo.clearbit.com/t-mobile.cz",true),
        OperatorInfo(91,"捷克","CZ","Vodafone Czech Republic","https://www.vodafone.cz","https://logo.clearbit.com/vodafone.cz",true),
        OperatorInfo(92,"斯洛伐克","SK","斯洛伐克 Mobile","","",false),
        OperatorInfo(93,"斯洛伐克","SK","斯洛伐克 Telecom","","",false),
        OperatorInfo(94,"匈牙利","HU","匈牙利 Mobile","","",false),
        OperatorInfo(95,"匈牙利","HU","匈牙利 Telecom","","",false),
        OperatorInfo(96,"罗马尼亚","RO","罗马尼亚 Mobile","","",false),
        OperatorInfo(97,"罗马尼亚","RO","罗马尼亚 Telecom","","",false),
        OperatorInfo(98,"保加利亚","BG","保加利亚 Mobile","","",false),
        OperatorInfo(99,"保加利亚","BG","保加利亚 Telecom","","",false),
        OperatorInfo(100,"克罗地亚","HR","克罗地亚 Mobile","","",false),
        OperatorInfo(101,"克罗地亚","HR","克罗地亚 Telecom","","",false),
        OperatorInfo(102,"斯洛文尼亚","SI","斯洛文尼亚 Mobile","","",false),
        OperatorInfo(103,"斯洛文尼亚","SI","斯洛文尼亚 Telecom","","",false),
        OperatorInfo(104,"塞尔维亚","RS","塞尔维亚 Mobile","","",false),
        OperatorInfo(105,"塞尔维亚","RS","塞尔维亚 Telecom","","",false),
        OperatorInfo(106,"波黑","BA","波黑 Mobile","","",false),
        OperatorInfo(107,"波黑","BA","波黑 Telecom","","",false),
        OperatorInfo(108,"黑山","ME","黑山 Mobile","","",false),
        OperatorInfo(109,"黑山","ME","黑山 Telecom","","",false),
        OperatorInfo(110,"北马其顿","MK","北马其顿 Mobile","","",false),
        OperatorInfo(111,"北马其顿","MK","北马其顿 Telecom","","",false),
        OperatorInfo(112,"阿尔巴尼亚","AL","阿尔巴尼亚 Mobile","","",false),
        OperatorInfo(113,"阿尔巴尼亚","AL","阿尔巴尼亚 Telecom","","",false),
        OperatorInfo(114,"立陶宛","LT","立陶宛 Mobile","","",false),
        OperatorInfo(115,"立陶宛","LT","立陶宛 Telecom","","",false),
        OperatorInfo(116,"拉脱维亚","LV","拉脱维亚 Mobile","","",false),
        OperatorInfo(117,"拉脱维亚","LV","拉脱维亚 Telecom","","",false),
        OperatorInfo(118,"爱沙尼亚","EE","爱沙尼亚 Mobile","","",false),
        OperatorInfo(119,"爱沙尼亚","EE","爱沙尼亚 Telecom","","",false),
        OperatorInfo(120,"摩尔多瓦","MD","摩尔多瓦 Mobile","","",false),
        OperatorInfo(121,"摩尔多瓦","MD","摩尔多瓦 Telecom","","",false),
        OperatorInfo(122,"白俄罗斯","BY","白俄罗斯 Mobile","","",false),
        OperatorInfo(123,"白俄罗斯","BY","白俄罗斯 Telecom","","",false),
        OperatorInfo(124,"乌克兰","UA","乌克兰 Mobile","","",false),
        OperatorInfo(125,"乌克兰","UA","乌克兰 Telecom","","",false),
        OperatorInfo(126,"俄罗斯","RU","俄罗斯 Mobile","","",false),
        OperatorInfo(127,"俄罗斯","RU","俄罗斯 Telecom","","",false),
        OperatorInfo(128,"哈萨克斯坦","KZ","哈萨克斯坦 Mobile","","",false),
        OperatorInfo(129,"哈萨克斯坦","KZ","哈萨克斯坦 Telecom","","",false),
        OperatorInfo(130,"格鲁吉亚","GE","格鲁吉亚 Mobile","","",false),
        OperatorInfo(131,"格鲁吉亚","GE","格鲁吉亚 Telecom","","",false),
        OperatorInfo(132,"亚美尼亚","AM","亚美尼亚 Mobile","","",false),
        OperatorInfo(133,"亚美尼亚","AM","亚美尼亚 Telecom","","",false),
        OperatorInfo(134,"阿塞拜疆","AZ","阿塞拜疆 Mobile","","",false),
        OperatorInfo(135,"阿塞拜疆","AZ","阿塞拜疆 Telecom","","",false),
        OperatorInfo(136,"土耳其","TR","Turkcell","https://www.turkcell.com.tr","https://logo.clearbit.com/turkcell.com.tr",true),
        OperatorInfo(137,"土耳其","TR","Vodafone Turkey","https://www.vodafone.com.tr","https://logo.clearbit.com/vodafone.com.tr",true),
        OperatorInfo(138,"土耳其","TR","Turk Telekom","https://www.turktelekom.com.tr","https://logo.clearbit.com/turktelekom.com.tr",true),
        OperatorInfo(139,"以色列","IL","Partner","https://www.partner.co.il","https://logo.clearbit.com/partner.co.il",true),
        OperatorInfo(140,"以色列","IL","Cellcom","https://www.cellcom.co.il","https://logo.clearbit.com/cellcom.co.il",true),
        OperatorInfo(141,"以色列","IL","Pelephone","https://www.pelephone.co.il","https://logo.clearbit.com/pelephone.co.il",true),
        OperatorInfo(142,"阿联酋","AE","Etisalat UAE","https://www.etisalat.ae","https://logo.clearbit.com/etisalat.ae",true),
        OperatorInfo(143,"阿联酋","AE","du","https://www.du.ae","https://logo.clearbit.com/du.ae",true),
        OperatorInfo(144,"阿联酋","AE","Virgin Mobile UAE","https://www.virginmobile.ae","https://logo.clearbit.com/virginmobile.ae",true),
        OperatorInfo(145,"沙特阿拉伯","SA","STC","https://www.stc.com.sa","https://logo.clearbit.com/stc.com.sa",true),
        OperatorInfo(146,"沙特阿拉伯","SA","Mobily","https://www.mobily.com.sa","https://logo.clearbit.com/mobily.com.sa",true),
        OperatorInfo(147,"沙特阿拉伯","SA","Zain KSA","https://www.sa.zain.com","https://logo.clearbit.com/sa.zain.com",true),
        OperatorInfo(148,"卡塔尔","QA","卡塔尔 Mobile","","",false),
        OperatorInfo(149,"卡塔尔","QA","卡塔尔 Telecom","","",false),
        OperatorInfo(150,"科威特","KW","科威特 Mobile","","",false),
        OperatorInfo(151,"科威特","KW","科威特 Telecom","","",false),
        OperatorInfo(152,"巴林","BH","巴林 Mobile","","",false),
        OperatorInfo(153,"巴林","BH","巴林 Telecom","","",false),
        OperatorInfo(154,"阿曼","OM","阿曼 Mobile","","",false),
        OperatorInfo(155,"阿曼","OM","阿曼 Telecom","","",false),
        OperatorInfo(156,"约旦","JO","约旦 Mobile","","",false),
        OperatorInfo(157,"约旦","JO","约旦 Telecom","","",false),
        OperatorInfo(158,"黎巴嫩","LB","黎巴嫩 Mobile","","",false),
        OperatorInfo(159,"黎巴嫩","LB","黎巴嫩 Telecom","","",false),
        OperatorInfo(160,"埃及","EG","埃及 Mobile","","",false),
        OperatorInfo(161,"埃及","EG","埃及 Telecom","","",false),
        OperatorInfo(162,"摩洛哥","MA","摩洛哥 Mobile","","",false),
        OperatorInfo(163,"摩洛哥","MA","摩洛哥 Telecom","","",false),
        OperatorInfo(164,"突尼斯","TN","突尼斯 Mobile","","",false),
        OperatorInfo(165,"突尼斯","TN","突尼斯 Telecom","","",false),
        OperatorInfo(166,"阿尔及利亚","DZ","阿尔及利亚 Mobile","","",false),
        OperatorInfo(167,"阿尔及利亚","DZ","阿尔及利亚 Telecom","","",false),
        OperatorInfo(168,"尼日利亚","NG","尼日利亚 Mobile","","",false),
        OperatorInfo(169,"尼日利亚","NG","尼日利亚 Telecom","","",false),
        OperatorInfo(170,"肯尼亚","KE","肯尼亚 Mobile","","",false),
        OperatorInfo(171,"肯尼亚","KE","肯尼亚 Telecom","","",false),
        OperatorInfo(172,"南非","ZA","Vodacom","https://www.vodacom.co.za","https://logo.clearbit.com/vodacom.co.za",true),
        OperatorInfo(173,"南非","ZA","MTN South Africa","https://www.mtn.co.za","https://logo.clearbit.com/mtn.co.za",true),
        OperatorInfo(174,"南非","ZA","Cell C","https://www.cellc.co.za","https://logo.clearbit.com/cellc.co.za",false),
        OperatorInfo(175,"日本","JP","NTT Docomo","https://www.docomo.ne.jp","https://logo.clearbit.com/docomo.ne.jp",true),
        OperatorInfo(176,"日本","JP","SoftBank","https://www.softbank.jp","https://logo.clearbit.com/softbank.jp",true),
        OperatorInfo(177,"日本","JP","au KDDI","https://www.au.com","https://logo.clearbit.com/au.com",true),
        OperatorInfo(178,"日本","JP","Rakuten Mobile","https://network.mobile.rakuten.co.jp","https://logo.clearbit.com/network.mobile.rakuten.co.jp",true),
        OperatorInfo(179,"韩国","KR","SK Telecom","https://www.sktelecom.com","https://logo.clearbit.com/sktelecom.com",true),
        OperatorInfo(180,"韩国","KR","KT","https://www.kt.com","https://logo.clearbit.com/kt.com",true),
        OperatorInfo(181,"韩国","KR","LG U+","https://www.lguplus.com","https://logo.clearbit.com/lguplus.com",true),
        OperatorInfo(182,"新加坡","SG","Singtel","https://www.singtel.com","https://logo.clearbit.com/singtel.com",true),
        OperatorInfo(183,"新加坡","SG","StarHub","https://www.starhub.com","https://logo.clearbit.com/starhub.com",true),
        OperatorInfo(184,"新加坡","SG","M1","https://www.m1.com.sg","https://logo.clearbit.com/m1.com.sg",true),
        OperatorInfo(185,"新加坡","SG","SIMBA","https://simba.sg","https://logo.clearbit.com/simba.sg",true),
        OperatorInfo(186,"马来西亚","MY","Maxis","https://www.maxis.com.my","https://logo.clearbit.com/maxis.com.my",true),
        OperatorInfo(187,"马来西亚","MY","CelcomDigi","https://www.celcomdigi.com","https://logo.clearbit.com/celcomdigi.com",true),
        OperatorInfo(188,"马来西亚","MY","U Mobile","https://www.u.com.my","https://logo.clearbit.com/u.com.my",true),
        OperatorInfo(189,"马来西亚","MY","Yes","https://www.yes.my","https://logo.clearbit.com/yes.my",true),
        OperatorInfo(190,"马来西亚","MY","Unifi Mobile","https://unifi.com.my","https://logo.clearbit.com/unifi.com.my",true),
        OperatorInfo(191,"泰国","TH","AIS","https://www.ais.th","https://logo.clearbit.com/ais.th",true),
        OperatorInfo(192,"泰国","TH","True","https://www.true.th","https://logo.clearbit.com/true.th",true),
        OperatorInfo(193,"泰国","TH","DTAC","https://www.dtac.co.th","https://logo.clearbit.com/dtac.co.th",true),
        OperatorInfo(194,"越南","VN","越南 Mobile","","",false),
        OperatorInfo(195,"越南","VN","越南 Telecom","","",false),
        OperatorInfo(196,"菲律宾","PH","Globe","https://www.globe.com.ph","https://logo.clearbit.com/globe.com.ph",true),
        OperatorInfo(197,"菲律宾","PH","Smart","https://smart.com.ph","https://logo.clearbit.com/smart.com.ph",true),
        OperatorInfo(198,"菲律宾","PH","DITO","https://dito.ph","https://logo.clearbit.com/dito.ph",true),
        OperatorInfo(199,"印尼","ID","Telkomsel","https://www.telkomsel.com","https://logo.clearbit.com/telkomsel.com",true),
        OperatorInfo(200,"印尼","ID","Indosat","https://www.ioh.co.id","https://logo.clearbit.com/ioh.co.id",true),
        OperatorInfo(201,"印尼","ID","XL Axiata","https://www.xl.co.id","https://logo.clearbit.com/xl.co.id",true),
        OperatorInfo(202,"印尼","ID","Smartfren","https://www.smartfren.com","https://logo.clearbit.com/smartfren.com",true),
        OperatorInfo(203,"柬埔寨","KH","柬埔寨 Mobile","","",false),
        OperatorInfo(204,"柬埔寨","KH","柬埔寨 Telecom","","",false),
        OperatorInfo(205,"老挝","LA","老挝 Mobile","","",false),
        OperatorInfo(206,"老挝","LA","老挝 Telecom","","",false),
        OperatorInfo(207,"缅甸","MM","缅甸 Mobile","","",false),
        OperatorInfo(208,"缅甸","MM","缅甸 Telecom","","",false),
        OperatorInfo(209,"文莱","BN","文莱 Mobile","","",false),
        OperatorInfo(210,"文莱","BN","文莱 Telecom","","",false),
        OperatorInfo(211,"印度","IN","Jio","https://www.jio.com","https://logo.clearbit.com/jio.com",true),
        OperatorInfo(212,"印度","IN","Airtel India","https://www.airtel.in","https://logo.clearbit.com/airtel.in",true),
        OperatorInfo(213,"印度","IN","Vi India","https://www.myvi.in","https://logo.clearbit.com/myvi.in",true),
        OperatorInfo(214,"巴基斯坦","PK","Jazz","https://jazz.com.pk","https://logo.clearbit.com/jazz.com.pk",false),
        OperatorInfo(215,"巴基斯坦","PK","Zong","https://www.zong.com.pk","https://logo.clearbit.com/zong.com.pk",false),
        OperatorInfo(216,"巴基斯坦","PK","Telenor Pakistan","https://www.telenor.com.pk","https://logo.clearbit.com/telenor.com.pk",false),
        OperatorInfo(217,"孟加拉国","BD","孟加拉国 Mobile","","",false),
        OperatorInfo(218,"孟加拉国","BD","孟加拉国 Telecom","","",false),
        OperatorInfo(219,"斯里兰卡","LK","斯里兰卡 Mobile","","",false),
        OperatorInfo(220,"斯里兰卡","LK","斯里兰卡 Telecom","","",false),
        OperatorInfo(221,"尼泊尔","NP","尼泊尔 Mobile","","",false),
        OperatorInfo(222,"尼泊尔","NP","尼泊尔 Telecom","","",false),
        OperatorInfo(223,"马尔代夫","MV","马尔代夫 Mobile","","",false),
        OperatorInfo(224,"马尔代夫","MV","马尔代夫 Telecom","","",false),
        OperatorInfo(225,"澳大利亚","AU","Telstra","https://www.telstra.com.au","https://logo.clearbit.com/telstra.com.au",true),
        OperatorInfo(226,"澳大利亚","AU","Optus","https://www.optus.com.au","https://logo.clearbit.com/optus.com.au",true),
        OperatorInfo(227,"澳大利亚","AU","Vodafone AU","https://www.vodafone.com.au","https://logo.clearbit.com/vodafone.com.au",true),
        OperatorInfo(228,"新西兰","NZ","Spark","https://www.spark.co.nz","https://logo.clearbit.com/spark.co.nz",true),
        OperatorInfo(229,"新西兰","NZ","One NZ","https://one.nz","https://logo.clearbit.com/one.nz",true),
        OperatorInfo(230,"新西兰","NZ","2degrees","https://www.2degrees.nz","https://logo.clearbit.com/2degrees.nz",true),
        OperatorInfo(231,"斐济","FJ","斐济 Mobile","","",false),
        OperatorInfo(232,"斐济","FJ","斐济 Telecom","","",false),
        OperatorInfo(233,"巴西","BR","Vivo Brazil","https://www.vivo.com.br","https://logo.clearbit.com/vivo.com.br",true),
        OperatorInfo(234,"巴西","BR","Claro Brazil","https://www.claro.com.br","https://logo.clearbit.com/claro.com.br",true),
        OperatorInfo(235,"巴西","BR","TIM Brazil","https://www.tim.com.br","https://logo.clearbit.com/tim.com.br",true),
        OperatorInfo(236,"阿根廷","AR","Personal","https://www.personal.com.ar","https://logo.clearbit.com/personal.com.ar",true),
        OperatorInfo(237,"阿根廷","AR","Claro Argentina","https://www.claro.com.ar","https://logo.clearbit.com/claro.com.ar",true),
        OperatorInfo(238,"阿根廷","AR","Movistar Argentina","https://www.movistar.com.ar","https://logo.clearbit.com/movistar.com.ar",true),
        OperatorInfo(239,"智利","CL","智利 Mobile","","",false),
        OperatorInfo(240,"智利","CL","智利 Telecom","","",false),
        OperatorInfo(241,"哥伦比亚","CO","哥伦比亚 Mobile","","",false),
        OperatorInfo(242,"哥伦比亚","CO","哥伦比亚 Telecom","","",false),
        OperatorInfo(243,"秘鲁","PE","秘鲁 Mobile","","",false),
        OperatorInfo(244,"秘鲁","PE","秘鲁 Telecom","","",false),
        OperatorInfo(245,"墨西哥","MX","Telcel","https://www.telcel.com","https://logo.clearbit.com/telcel.com",true),
        OperatorInfo(246,"墨西哥","MX","AT&T Mexico","https://www.att.com.mx","https://logo.clearbit.com/att.com.mx",true),
        OperatorInfo(247,"墨西哥","MX","Movistar Mexico","https://www.movistar.com.mx","https://logo.clearbit.com/movistar.com.mx",true),
        OperatorInfo(248,"乌拉圭","UY","乌拉圭 Mobile","","",false),
        OperatorInfo(249,"乌拉圭","UY","乌拉圭 Telecom","","",false),
        OperatorInfo(250,"巴拉圭","PY","巴拉圭 Mobile","","",false),
        OperatorInfo(251,"巴拉圭","PY","巴拉圭 Telecom","","",false),
        OperatorInfo(252,"玻利维亚","BO","玻利维亚 Mobile","","",false),
        OperatorInfo(253,"玻利维亚","BO","玻利维亚 Telecom","","",false),
        OperatorInfo(254,"厄瓜多尔","EC","厄瓜多尔 Mobile","","",false),
        OperatorInfo(255,"厄瓜多尔","EC","厄瓜多尔 Telecom","","",false),
        OperatorInfo(256,"委内瑞拉","VE","委内瑞拉 Mobile","","",false),
        OperatorInfo(257,"委内瑞拉","VE","委内瑞拉 Telecom","","",false),
        OperatorInfo(258,"哥斯达黎加","CR","哥斯达黎加 Mobile","","",false),
        OperatorInfo(259,"哥斯达黎加","CR","哥斯达黎加 Telecom","","",false),
        OperatorInfo(260,"巴拿马","PA","巴拿马 Mobile","","",false),
        OperatorInfo(261,"巴拿马","PA","巴拿马 Telecom","","",false),
        OperatorInfo(262,"危地马拉","GT","危地马拉 Mobile","","",false),
        OperatorInfo(263,"危地马拉","GT","危地马拉 Telecom","","",false),
        OperatorInfo(264,"多米尼加","DO","多米尼加 Mobile","","",false),
        OperatorInfo(265,"多米尼加","DO","多米尼加 Telecom","","",false),
        OperatorInfo(266,"牙买加","JM","牙买加 Mobile","","",false),
        OperatorInfo(267,"牙买加","JM","牙买加 Telecom","","",false),
    )
    fun byCountry(iso:String)=list.filter{it.countryCode.equals(iso,true)}
    fun find(name:String, iso:String?=null):OperatorInfo? {
        val q=name.trim()
        if(q.isBlank()) return null
        return list.firstOrNull{ (iso==null || it.countryCode.equals(iso,true)) && (it.carrierName.equals(q,true) || q.contains(it.carrierName,true) || it.carrierName.contains(q,true)) }
            ?: list.firstOrNull{ it.carrierName.equals(q,true) || q.contains(it.carrierName,true) || it.carrierName.contains(q,true) }
    }
    fun firstNameFor(iso:String)=byCountry(iso).firstOrNull()?.carrierName ?: "未知运营商"
    fun logoFor(name:String, iso:String?=null)=find(name,iso)?.logoUrl.orEmpty()
}

object DataStore {
    fun load设置(ctx:Context):App设置 {
        val p=ctx.getSharedPreferences(PREF,0); val o=JSONObject(p.getString("settings","{}")!!)
        return App设置(o.optBoolean("dark"),o.optInt("remind天",7),o.optString("trafficUrl","https://speed.cloudflare.com/__down?bytes=10485760"),o.optDouble("trafficKb",1.0),o.optBoolean("tgEnabled"),o.optString("botToken"),o.optString("chatId"),o.optString("keepCycle","月"),o.optString("backgroundUri",""),o.optDouble("backgroundAlpha",0.72).toFloat(),o.optBoolean("reminderEnabled",true),o.optBoolean("notificationEnabled",true),o.optInt("remindHour",9),o.optInt("remindMinute",0),o.optString("language","简体中文"),o.optBoolean("emailQuickEnabled",true),o.optBoolean("smtpEnabled",false),o.optString("smtpHost",""),o.optInt("smtpPort",465),o.optString("smtpUser",""),o.optString("smtpPass",""),o.optString("smtpFrom",""),o.optString("smtpTo",""),o.optBoolean("cloudEnabled",false),o.optString("cloudUrl","https://ccs.ziranaa.top:16670"),o.optString("cloudApiKey",""),o.optBoolean("cloudTelegramEnabled",true),o.optBoolean("cloudEmailEnabled",true),o.optBoolean("cloudAutoSync",false),o.optBoolean("showFlag",true))
    }
    fun save设置(ctx:Context,s:App设置){
        val o=JSONObject().put("dark",s.dark).put("remind天",s.remind天).put("trafficUrl",s.trafficUrl).put("trafficKb",s.trafficKb).put("tgEnabled",s.tgEnabled).put("botToken",s.botToken).put("chatId",s.chatId).put("keepCycle",s.keepCycle).put("backgroundUri",s.backgroundUri).put("backgroundAlpha",s.backgroundAlpha.toDouble()).put("reminderEnabled",s.reminderEnabled).put("notificationEnabled",s.notificationEnabled).put("remindHour",s.remindHour).put("remindMinute",s.remindMinute).put("language",s.language).put("emailQuickEnabled",s.emailQuickEnabled).put("smtpEnabled",s.smtpEnabled).put("smtpHost",s.smtpHost).put("smtpPort",s.smtpPort).put("smtpUser",s.smtpUser).put("smtpPass",s.smtpPass).put("smtpFrom",s.smtpFrom).put("smtpTo",s.smtpTo).put("cloudEnabled",s.cloudEnabled).put("cloudUrl",s.cloudUrl).put("cloudApiKey",s.cloudApiKey).put("cloudTelegramEnabled",s.cloudTelegramEnabled).put("cloudEmailEnabled",s.cloudEmailEnabled).put("cloudAutoSync",s.cloudAutoSync).put("showFlag",s.showFlag)
        ctx.getSharedPreferences(PREF,0).edit().putString("settings",o.toString()).apply(); ReminderScheduler.schedule全部(ctx)
    }
    fun normalizeLongTerm(r:PhoneNumberRecord):PhoneNumberRecord{
        if(!r.longTerm) return r
        val today=LocalDate.now(); var exp=runCatching{LocalDate.parse(r.expireDate)}.getOrNull() ?: return r
        val step=r.cycleDays.coerceIn(1,3650)
        while(exp.isBefore(today)) exp=exp.plusDays(step.toLong())
        return if(exp.toString()!=r.expireDate) r.copy(expireDate=exp.toString()) else r
    }
    fun loadRecords(ctx:Context):List<PhoneNumberRecord>{
        val arr=JSONArray(ctx.getSharedPreferences(PREF,0).getString("records","[]"))
        return (0 until arr.length()).map{ val o=arr.getJSONObject(it)
            normalizeLongTerm(PhoneNumberRecord(
                id=o.optString("id",UUID.randomUUID().toString()), countryCode=o.optString("countryCode","+86"), countryName=o.optString("countryName","中国"), flag=o.optString("flag","🇨🇳"), number=o.optString("number"), operator=o.optString("operator"), expireDate=o.optString("expireDate",LocalDate.now().plusDays(30).toString()), note=o.optString("note"),
                balance=o.optString("balance"), eid=o.optString("eid"), smdp=o.optString("smdp"), activationCode=o.optString("activationCode"), startDate=o.optString("startDate",LocalDate.now().toString()), createdAt=o.optString("createdAt",LocalDate.now().toString()), activatedAt=o.optString("activatedAt"), longTerm=o.optBoolean("longTerm",false), cycleDays=o.optInt("cycleDays",30), signalStatus=o.optString("signalStatus","在线")
            ))
        }
    }
    fun recordJson(r:PhoneNumberRecord)=JSONObject().put("id",r.id).put("countryCode",r.countryCode).put("countryName",r.countryName).put("flag",r.flag).put("number",r.number).put("operator",r.operator).put("expireDate",r.expireDate).put("note",r.note).put("balance",r.balance).put("eid",r.eid).put("smdp",r.smdp).put("activationCode",r.activationCode).put("startDate",r.startDate).put("createdAt",r.createdAt).put("activatedAt",r.activatedAt).put("longTerm",r.longTerm).put("cycleDays",r.cycleDays).put("signalStatus",r.signalStatus)
    fun saveRecords(ctx:Context,list:List<PhoneNumberRecord>){ val arr=JSONArray(); list.forEach{ arr.put(recordJson(it)) }; ctx.getSharedPreferences(PREF,0).edit().putString("records",arr.toString()).apply(); ReminderScheduler.schedule全部(ctx) }
}

object NotificationHelper {
    fun createChannel(ctx:Context){ if(Build.VERSION.SDK_INT>=26){ val nm=ctx.getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Sim Jiang 到期提醒",NotificationManager.IMPORTANCE_HIGH)) } }
    fun notify(ctx:Context,id:Int,title:String,text:String,emailIntent:Intent?=null){
        val b=Notification.Builder(ctx,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setAutoCancel(true)
        if(emailIntent!=null){
            val pi=PendingIntent.getActivity(ctx,id+900000,emailIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            b.addAction(android.R.drawable.ic_dialog_email,"发邮件",pi)
        }
        ctx.getSystemService(NotificationManager::class.java).notify(id,b.build())
    }
}
object ReminderScheduler {
    fun schedule全部(ctx:Context){
        val am=ctx.getSystemService(AlarmManager::class.java) ?: return
        val settings=DataStore.load设置(ctx)
        if(!settings.reminderEnabled && !settings.smtpEnabled) return
        val canExact = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
        DataStore.loadRecords(ctx).forEach{ r->
            val date=runCatching{LocalDate.parse(r.expireDate)}.getOrNull()?:return@forEach
            val time=date.minusDays(settings.remind天.toLong()).atTime(settings.remindHour.coerceIn(0,23),settings.remindMinute.coerceIn(0,59)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if(time>System.currentTimeMillis()){
                val pi=PendingIntent.getBroadcast(ctx,r.id.hashCode(),Intent(ctx,ReminderReceiver::class.java).putExtra("id",r.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                runCatching{
                    if(canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi)
                }
            }
        }
    }
}
class ReminderReceiver: BroadcastReceiver(){ override fun onReceive(ctx:Context,intent:Intent){
    val id=intent.getStringExtra("id"); val r=DataStore.loadRecords(ctx).firstOrNull{it.id==id}?:return
    val s=DataStore.load设置(ctx)
    val subject="Sim Jiang 号码到期提醒：${r.operator.ifBlank{r.countryName}} ${r.countryCode} ${formatNumber(r.number)}"
    val body=buildEmailBody(r,s)
    val msg="${r.flag} ${r.countryCode} ${formatNumber(r.number)} 将于 ${r.expireDate} 到期"
    if(s.notificationEnabled){
        val emailIntent=if(s.emailQuickEnabled) makeEmailIntent(s.smtpTo,subject,body) else null
        NotificationHelper.notify(ctx,r.id.hashCode(),"号码即将到期",msg,emailIntent)
    }
    if(s.tgEnabled) sendTelegram(s.botToken,s.chatId,"⏰ Sim Jiang 到期提醒\n$msg")
    if(s.smtpEnabled) sendSmtpMail(s,subject,body)
} }
class BootReceiver: BroadcastReceiver(){ override fun onReceive(ctx:Context,intent:Intent){ ReminderScheduler.schedule全部(ctx) } }
fun sendTelegram(token:String,chatId:String,text:String){ if(token.isBlank()||chatId.isBlank()) return; thread { runCatching{ val u="https://api.telegram.org/bot$token/sendMessage?chat_id=${URLEncoder.encode(chatId,"UTF-8")}&text=${URLEncoder.encode(text,"UTF-8")}"; (URL(u).openConnection() as HttpURLConnection).apply{connectTimeout=8000;readTimeout=8000}.inputStream.close() } } }
fun buildEmailBody(r:PhoneNumberRecord,s:App设置):String = """
Sim Jiang 到期提醒

号码：${r.countryCode} ${formatNumber(r.number)}
国家/地区：${r.countryName}
运营商：${r.operator.ifBlank{r.countryName}}
到期日期：${r.expireDate}
套餐余额：${r.balance.ifBlank{"未填写"}}
EID：${r.eid.ifBlank{"未填写"}}
备注：${r.note.ifBlank{"无"}}

请及时保号、充值或刷流量。
""".trimIndent()
fun makeEmailIntent(to:String,subject:String,body:String):Intent = Intent(Intent.ACTION_SENDTO).apply{
    data=Uri.parse("mailto:")
    if(to.isNotBlank()) putExtra(Intent.EXTRA_EMAIL,arrayOf(to))
    putExtra(Intent.EXTRA_SUBJECT,subject)
    putExtra(Intent.EXTRA_TEXT,body)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
fun smtpB64(s:String)=Base64.encodeToString(s.toByteArray(Charsets.UTF_8),Base64.NO_WRAP)
fun sendSmtpMail(s:App设置,subject:String,body:String,onResult:((Boolean,String)->Unit)?=null){
    fun done(ok:Boolean,msg:String){ onResult?.let{ Handler(Looper.getMainLooper()).post{ it(ok,msg) } } }
    if(!s.smtpEnabled || s.smtpHost.isBlank() || s.smtpUser.isBlank() || s.smtpPass.isBlank() || s.smtpTo.isBlank()) { done(false,"SMTP 未配置完整"); return }
    thread{
        val res=runCatching{
            val port=s.smtpPort.coerceIn(1,65535)
            val socket=(SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(s.smtpHost,port)
            val reader=socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val writer=socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            fun readResp():String = reader.readLine() ?: ""
            fun cmd(x:String):String{ writer.write(x+"\r\n"); writer.flush(); return readResp() }
            readResp()
            cmd("EHLO simjiang.local")
            cmd("AUTH LOGIN")
            cmd(smtpB64(s.smtpUser))
            cmd(smtpB64(s.smtpPass))
            val from=s.smtpFrom.ifBlank{s.smtpUser}
            cmd("MAIL FROM:<$from>")
            cmd("RCPT TO:<${s.smtpTo}>")
            cmd("DATA")
            val mail="From: $from\r\nTo: ${s.smtpTo}\r\nSubject: =?UTF-8?B?${smtpB64(subject)}?=\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n$body\r\n."
            cmd(mail)
            cmd("QUIT")
            socket.close()
            true
        }
        res.onSuccess{ done(true,"测试邮件已提交 SMTP") }.onFailure{ done(false,"发送失败：${it.javaClass.simpleName}: ${it.message}") }
    }
}

class MainActivity: ComponentActivity(){ private val req=registerForActivityResult(ActivityResultContracts.RequestPermission()){}; override fun onCreate(b:Bundle?){ super.onCreate(b); if(Build.VERSION.SDK_INT>=33) req.launch(Manifest.permission.POST_NOTIFICATIONS); setContent{ App(this) } } }

@Composable fun App(ctx:Context){
    var settings by remember{ mutableStateOf(DataStore.load设置(ctx)) }
    var records by remember{ mutableStateOf(DataStore.loadRecords(ctx)) }
    var screen by remember{ mutableStateOf("home") }
    var edit by remember{ mutableStateOf<PhoneNumberRecord?>(null) }
    var trafficTarget by remember{ mutableStateOf<PhoneNumberRecord?>(null) }
    var toolMessage by remember{ mutableStateOf<String?>(null) }
    var exportDialog by remember{ mutableStateOf<Pair<String,String>?>(null) }
    var filter by remember{ mutableStateOf("全部") }
    var sortMode by remember{ mutableStateOf("到期近") }
    var search by remember{ mutableStateOf("") }
    val colors=if(settings.dark) darkColorScheme(primary=Color(0xFF0A84FF),background=Color(0xFF0B0F17),surface=Color(0xFF151922)) else lightColorScheme(primary=Color(0xFF007AFF),background=Color(0xFFF4F5F7),surface=Color.White)
    val lang = settings.language
    fun tx(key:String)=tr(lang,key)
    fun autoCloudSync(rs:List<PhoneNumberRecord>, st:App设置){
        if(st.cloudEnabled && st.cloudAutoSync && cleanCloudApiKey(st.cloudApiKey).isNotBlank()){
            cloudPost(st,"/api/sync",cloudPayload(rs,st)){_,_->}
        }
    }
    MaterialTheme(colors){
        CompositionLocalProvider(LocalLayoutDirection provides if(settings.language=="阿拉伯语") LayoutDirection.Rtl else LayoutDirection.Ltr, LocalAppLanguage provides settings.language){
        run{ val editing = edit!=null
            if(editing && edit!=null){
                Full编辑Screen(init=edit!!, onDismiss={edit=null}, onSave={r->
                    val c=Countries.list.firstOrNull{it.code==r.countryCode && it.name==r.countryName} ?: Countries.list.firstOrNull{it.code==r.countryCode} ?: Countries.list.first()
                    val nr=r.copy(countryCode=c.code,countryName=c.name,flag=c.flag,operator= if(r.operator.isBlank()) guessOperator(r.number,c.iso) else r.operator, createdAt=if(r.createdAt.isBlank()) LocalDate.now().toString() else r.createdAt, activatedAt=if(r.activatedAt.isBlank() && (r.smdp.isNotBlank() || r.activationCode.isNotBlank())) LocalDate.now().toString() else r.activatedAt)
                    records= if(records.any{it.id==nr.id}) records.map{if(it.id==nr.id)nr else it} else records+nr
                    DataStore.saveRecords(ctx,records); autoCloudSync(records,settings)
                    edit=null
                }, onDelete={r->
                    records=records.filter{it.id!=r.id}
                    DataStore.saveRecords(ctx,records); autoCloudSync(records,settings)
                    edit=null
                })
            } else {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)){
                    SimHubTopBar(screen,settings.dark,{ settings=settings.copy(dark=!settings.dark); DataStore.save设置(ctx,settings) },search,{ search=it }){ target->
                        when(target){
                            "add" -> edit=PhoneNumberRecord()
                            "export" -> toolMessage=tx("导出数据已准备")+"："+tx("当前共有")+" ${records.size} "+tx("个号码")+"。\n"+tx("数据已保存在本机应用存储中，后续可接入文件导出。")
                            "grid" -> screen="countries"
                            else -> screen=target
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()){
                        when(screen){
                            "home"->Home(ctx,records,settings,search,filter,sortMode,{filter=it},{sortMode=if(sortMode=="到期近") "到期远" else "到期近"},{edit=PhoneNumberRecord()},{edit=it},{r->records=records.filter{it.id!=r.id};DataStore.saveRecords(ctx,records); autoCloudSync(records,settings)},{dial(ctx,it)},{trafficTarget=it},{r,months->val nr=r.copy(expireDate=LocalDate.parse(r.expireDate).plusDays(months.toLong()).toString());records=records.map{if(it.id==r.id)nr else it};DataStore.saveRecords(ctx,records); autoCloudSync(records,settings)})
                            "keep"->KeepPage(records,{r,m-> val nr=r.copy(expireDate=LocalDate.parse(r.expireDate).plusDays(m.toLong()).toString()); records=records.map{if(it.id==r.id)nr else it}; DataStore.saveRecords(ctx,records); autoCloudSync(records,settings)})
                            "tools"->ToolsPage(settings,records,{trafficTarget=it},{dial(ctx,it)},{ exportDialog="json" to exportRecordsJson(records,settings) },{ exportDialog="csv" to exportRecordsCsv(records) },{ text-> val imported=parseRecordsAny(text); if(imported.isNotEmpty()){ records=imported; DataStore.saveRecords(ctx,records); autoCloudSync(records,settings); toolMessage=tx("导入完成")+"：${records.size} "+tx("个号码") } else toolMessage=tx("导入失败：未识别 JSON/CSV 数据") })
                            "settings"->设置Page(ctx,settings,records,{settings=it;DataStore.save设置(ctx,it); autoCloudSync(records,it)})
                            "countries"->CountryPage()
                        }
                    }
                    SimHubBottomNav(screen){ screen=it }
                }
            }
        }
        }
    }
    if(trafficTarget!=null) TrafficDialog(ctx,trafficTarget!!,settings,{trafficTarget=null})
    toolMessage?.let { msg ->
        IOSInfoDialog(L("操作结果"),msg){toolMessage=null}
    }
    exportDialog?.let { item -> ExportDataDialog(ctx,item.first,item.second){exportDialog=null} }
}

@Composable fun Header(screen:String,on:(String)->Unit){ SimHubTopBar(screen,false,{},"",{},on) }

@Composable fun IOSInfoDialog(title:String,message:String,onDismiss:()->Unit){
    Dialog(onDismissRequest=onDismiss){ Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFFF2F3F7)){ Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalAlignment=Alignment.CenterHorizontally){ Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827)); Text(message,fontSize=13.sp,color=Color(0xFF374151)); Button(onDismiss,modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF))){Text(L("好"))} } } }
}

@Composable fun ExportDataDialog(ctx:Context,type:String,content:String,onDismiss:()->Unit){
    val clipboard=LocalClipboardManager.current
    val ext=if(type=="csv") "csv" else "json"
    val title=if(type=="csv") L("导出 CSV") else L("导出 JSON")
    val exportFileTitle=L("导出文件")
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFFF2F3F7),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text(title,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827))
                Text(L("可以复制到剪贴板，也可以生成文件并调用系统分享。"),fontSize=13.sp,color=Color(0xFF8A94A6))
                Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha=.86f)).border(.7.dp,Color.White,RoundedCornerShape(16.dp)).verticalScroll(rememberScrollState()).padding(12.dp)){
                    Text(content,fontSize=11.sp,color=Color(0xFF374151),lineHeight=16.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button({ clipboard.setText(AnnotatedString(content)) },modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF007AFF))){Text(L("复制"))}
                    Button({ shareExportFile(ctx,"SimJiang-export-${System.currentTimeMillis()}.$ext",if(ext=="csv") "text/csv" else "application/json",content,exportFileTitle) },modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF))){Text(L("导出文件"))}
                }
                TextButton(onDismiss,modifier=Modifier.align(Alignment.CenterHorizontally)){Text(L("关闭"))}
            }
        }
    }
}

fun shareExportFile(ctx:Context,fileName:String,mime:String,content:String,title:String="导出文件"){
    runCatching{
        val dir=File(ctx.cacheDir,"exports"); dir.mkdirs()
        val f=File(dir,fileName); f.writeText(content,Charsets.UTF_8)
        val uri=FileProvider.getUriForFile(ctx,ctx.packageName+".fileprovider",f)
        val intent=Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(intent,title))
    }
}




@Composable fun SimHubTopBar(screen:String,dark:Boolean,onToggleDark:()->Unit,search:String,onSearch:(String)->Unit,on:(String)->Unit){
    val bg=if(dark) Color(0xFF0B0F17) else Color(0xFFF4F6FA)
    val surface=if(dark) Color(0xFF151922) else Color.White
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(Modifier.fillMaxWidth().background(bg).padding(start=18.dp,end=18.dp,top=statusBarTop+8.dp,bottom=6.dp)){
        if(screen=="home"){
            // search bar + dark mode toggle on same row
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                TextField(value=search,onValueChange=onSearch,modifier=Modifier.weight(1f).heightIn(min=36.dp).clip(RoundedCornerShape(12.dp)),singleLine=true,
                    placeholder={Text(L("搜索运营商、国家或号码"),fontSize=13.sp,color=Color(0xFF8E8E93),maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={Canvas(Modifier.size(16.dp)){drawCircle(Color(0xFF8E8E93),radius=size.width/2-1.dp.toPx(),style=Stroke(1.5.dp.toPx()));drawLine(Color(0xFF8E8E93),Offset(size.width*.65f,size.height*.65f),Offset(size.width*.85f,size.height*.85f),strokeWidth=1.5.dp.toPx())}},
                    colors=TextFieldDefaults.colors(focusedContainerColor=surface,unfocusedContainerColor=surface,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
                IconCircle(if(dark) "M" else "S",onToggleDark)
            }
        }else{
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(when(screen){"tools"->L("工具");"settings"->L("设置");else->L("号码")},fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),color=if(dark) Color.White else Color(0xFF111827))
            }
        }
    }
}

@Composable fun IconCircle(text:String,onClick:()->Unit){
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha=.92f)).border(.6.dp,Color(0xFFE5E7EB),RoundedCornerShape(17.dp)).clickable{onClick()},contentAlignment=Alignment.Center){Text(text,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF374151))}
}

@Composable fun FilterToolRow(filter:String,sortMode:String,onFilter:(String)->Unit,onSort:()->Unit,count:Int){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
        FilterTool("≡",filter,Modifier.weight(1f)){onFilter(when(filter){"全部"->"正常";"正常"->"即将到期";"即将到期"->"已过期";else->"全部"})}
        FilterTool("↕",if(sortMode=="到期近") L("近到远") else L("远到近"),Modifier.weight(1f)){onSort()}
        Text("$count",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color(0xFF007AFF))
    }
}

@Composable fun FilterTool(icon:String,text:String,m:Modifier,onClick:()->Unit){
    Row(m.height(36.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF007AFF)).clickable{onClick()}.padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){Text(icon,fontSize=12.sp,color=Color.White);Spacer(Modifier.width(5.dp));Text(text,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1)}
}


@Composable fun CompactSimCard(r:PhoneNumberRecord,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit,days:Long?,remindDays:Int,showFlag:Boolean=true){
    val progress=when{days==null->.35f; days<0->.04f; else->(days.coerceIn(0,120).toFloat()/120f).coerceIn(.08f,.98f)}
    var hidden by remember{ mutableStateOf(true) }
    var del by remember{ mutableStateOf(false) }
    var keep by remember{ mutableStateOf(false) }
    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=.35f)),elevation=CardDefaults.cardElevation(0.dp),modifier=Modifier.fillMaxWidth().height(150.dp).border(1.dp,Color.White.copy(alpha=.50f),RoundedCornerShape(24.dp))){
        Box(Modifier.fillMaxSize()){
            // frosted glass shimmer
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.45f),Color.White.copy(alpha=.22f),Color.White.copy(alpha=.38f)))).clip(RoundedCornerShape(24.dp)))
            if(showFlag){
                FlagArtPanel(r,Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.43f))
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.56f).background(Brush.horizontalGradient(listOf(Color.White.copy(alpha=.60f),Color.White.copy(alpha=.40f),Color.White.copy(alpha=.15f),Color.Transparent))))
            }
            Column(Modifier.fillMaxSize().padding(start=10.dp,end=10.dp,top=7.dp,bottom=6.dp),verticalArrangement=Arrangement.SpaceBetween){
                Row(verticalAlignment=Alignment.CenterVertically){
                    OperatorLogo44(r.operator.ifBlank{r.countryName}, Countries.list.firstOrNull{it.code==r.countryCode && it.name==r.countryName}?.iso ?: Countries.list.firstOrNull{it.code==r.countryCode}?.iso)
                    Spacer(Modifier.width(8.dp))
                    Text(r.operator.ifBlank{r.countryName},fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                    if(r.longTerm) Text("Long-term",fontSize=9.sp,fontWeight=FontWeight.Bold,color=Color.White,modifier=Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF34C759)).padding(horizontal=6.dp,vertical=2.dp))
                    else Text("∞",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.White,modifier=Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF007AFF)).padding(horizontal=6.dp,vertical=2.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(r.countryName,fontSize=12.sp,color=Color(0xFF6B7280),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.widthIn(max=72.dp))
                }
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(L("预付费")+" · ",fontSize=12.sp,color=Color(0xFF374151),maxLines=1)
                    Box(Modifier.size(13.dp).clip(RoundedCornerShape(99.dp)).background(Color(0xFF22C55E)),contentAlignment=Alignment.Center){Text("✓",fontSize=8.sp,color=Color.White)}
                    Spacer(Modifier.width(4.dp))
                    Text("${formatDateByLang(r.expireDate, LocalAppLanguage.current)} · ${expireText(LocalAppLanguage.current,days)}",fontSize=12.sp,color=Color(0xFF16A34A),fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("☎ ${r.countryCode} ${if(hidden) "•••• ${r.number.takeLast(4)}" else formatNumber(r.number)}",fontSize=15.sp,fontWeight=FontWeight.Medium,color=Color(0xFF111827),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                    Text(r.balance.ifBlank{estimateBalance(r)},fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF007AFF),maxLines=1)
                    Spacer(Modifier.width(8.dp))
                    Text(if(hidden)"◉" else "◎",fontSize=16.sp,color=Color(0xFF374151),modifier=Modifier.clickable{hidden=!hidden})
                }
                Row(verticalAlignment=Alignment.CenterVertically){Text("EID ${r.eid.ifBlank{fakeEidForCard(r)}}",fontSize=10.sp,color=Color(0xFF6B7280),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f)); Text(signalIcon(r.signalStatus)+" "+r.signalStatus,fontSize=10.sp,color=Color(0xFF16A34A),maxLines=1)}
                Box(Modifier.fillMaxWidth(.80f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE5E7EB))){Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color(0xFF22C55E)))}
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){
                    CardIconAction("keep",Color(0xFF8B5CF6)){keep=true}
                    CardIconAction("traffic",Color(0xFF007AFF)){onTraffic(r)}
                    CardIconAction("edit",Color(0xFFFF9500)){on编辑(r)}
                }
            }
        }
    }
    if(keep) KeepCycleDialog(r,onKeep){keep=false}
    if(del) IOSConfirmDialog(L("删除号码？"),L("删除")+" ${r.countryCode} ${formatNumber(r.number)} "+L("删除后不可恢复"),true,{del=false},{del=false;onDel(r)})
}

@Composable fun MiniAction(text:String,color:Color,onClick:()->Unit){
    Box(Modifier.widthIn(min=48.dp,max=62.dp).height(28.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha=.10f)).clickable{onClick()},contentAlignment=Alignment.Center){Text(text,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=color,maxLines=1)}
}

@Composable fun CardIconAction(type:String,color:Color,onClick:()->Unit){
    Box(Modifier.width(30.dp).height(40.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha=.14f)).clickable{onClick()},contentAlignment=Alignment.Center){
        Canvas(Modifier.width(16.dp).height(26.dp)){
            val w=size.width; val h=size.height; val st=Stroke(width=1.9f)
            when(type){
                "keep"->{ // shield / 保号
                    drawLine(color,Offset(w*.5f,h*.16f),Offset(w*.82f,h*.30f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.82f,h*.30f),Offset(w*.82f,h*.54f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.82f,h*.54f),Offset(w*.5f,h*.86f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.5f,h*.86f),Offset(w*.18f,h*.54f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.18f,h*.54f),Offset(w*.18f,h*.30f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.18f,h*.30f),Offset(w*.5f,h*.16f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.36f,h*.50f),Offset(w*.46f,h*.62f),strokeWidth=1.9f)
                    drawLine(color,Offset(w*.46f,h*.62f),Offset(w*.66f,h*.38f),strokeWidth=1.9f)
                }
                "traffic"->{ // bars / 刷流量
                    drawLine(color,Offset(w*.24f,h*.80f),Offset(w*.24f,h*.56f),strokeWidth=2.4f)
                    drawLine(color,Offset(w*.5f,h*.80f),Offset(w*.5f,h*.36f),strokeWidth=2.4f)
                    drawLine(color,Offset(w*.76f,h*.80f),Offset(w*.76f,h*.18f),strokeWidth=2.4f)
                }
                "edit"->{ // pencil / 编辑
                    drawLine(color,Offset(w*.26f,h*.74f),Offset(w*.70f,h*.30f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.70f,h*.30f),Offset(w*.82f,h*.42f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.82f,h*.42f),Offset(w*.38f,h*.86f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.38f,h*.86f),Offset(w*.20f,h*.86f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.20f,h*.86f),Offset(w*.26f,h*.74f),strokeWidth=2.0f)
                }
                else->{ // trash / 删除
                    drawLine(color,Offset(w*.22f,h*.30f),Offset(w*.78f,h*.30f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.40f,h*.30f),Offset(w*.42f,h*.18f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.42f,h*.18f),Offset(w*.58f,h*.18f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.58f,h*.18f),Offset(w*.60f,h*.30f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.28f,h*.30f),Offset(w*.32f,h*.84f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.32f,h*.84f),Offset(w*.68f,h*.84f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.68f,h*.84f),Offset(w*.72f,h*.30f),strokeWidth=2.0f)
                    drawLine(color,Offset(w*.5f,h*.40f),Offset(w*.5f,h*.74f),strokeWidth=1.6f)
                }
            }
        }
    }
}

@Composable fun OperatorLogo44(name:String, iso:String?=null){
    val info=remember(name, iso){ OperatorDatabase.find(name, iso) }
    val display=info?.carrierName ?: name
    val localLogo = remember(display, name, iso){ OperatorLogoAssets.assetFor(display, iso).ifBlank { OperatorLogoAssets.assetFor(name, iso) } }
    val onlineLogo = if(localLogo.isBlank()) (info?.logoUrl ?: "") else ""
    val assetPath = localLogo.removePrefix("file:///android_asset/")
    val assetBitmap = rememberAssetBitmap(assetPath)
    val op=display.uppercase()
    val label=when{
        "移动" in display || "CHINA MOBILE" in op -> "CM"
        "联通" in display || "UNICOM" in op -> "CU"
        "电信" in display || "TELECOM" in op -> "CT"
        "广电" in display -> "CB"
        "GIFFGAFF" in op -> "giff"
        "3HK" in op || "THREE" in op -> "3"
        "HKT" in op || "CSL" in op -> "HKT"
        "SMARTONE" in op -> "ST"
        "CMHK" in op || "CHINA MOBILE HONG KONG" in op -> "CMHK"
        "RAKUTEN" in op -> "R"
        "SOFTBANK" in op -> "SB"
        "DOCOMO" in op -> "doc"
        "AIS" in op -> "AIS"
        "TRUE" in op -> "TRUE"
        "DTAC" in op -> "dtac"
        "VODAFONE" in op -> "V"
        "T-MOBILE" in op -> "T"
        "AT&T" in op -> "AT&T"
        "VERIZON" in op -> "VZ"
        info!=null -> display.split(" ").filter{it.isNotBlank()}.take(2).joinToString(""){it.first().uppercase()}.ifBlank{"SIM"}
        else -> "SIM"
    }
    val bg=when(label){"CM"->Color(0xFF0085D0);"CU"->Color(0xFFE60012);"CT"->Color(0xFF005BAC);"AIS"->Color(0xFF78BE20);"R"->Color(0xFFBF0000);"V"->Color(0xFFE60000);"T"->Color(0xFFE20074);else->Color(0xFF111827)}
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(if(assetBitmap!=null || onlineLogo.isNotBlank()) Color.White else bg).border(.75.dp,Color(0xFFE5E7EB),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){
        when{
            assetBitmap!=null -> Image(bitmap=assetBitmap,contentDescription=display,contentScale=ContentScale.Fit,modifier=Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
            onlineLogo.isNotBlank() -> AsyncImage(model=onlineLogo,contentDescription=display,contentScale=ContentScale.Fit,modifier=Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
            else -> Text(label,fontSize=if(label.length>3) 8.sp else 12.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable fun SubscriptionCard(r:PhoneNumberRecord,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit,days:Long?,remindDays:Int){
    val status=if(days==null)L("未知") else if(days<0)L("已过期") else if(days<=remindDays)L("即将到期") else L("预付费")
    val theme=countryTheme(r.countryCode,r.countryName)
    val progress=when{days==null->.25f; days<0->1f; else->(1f-(days.coerceIn(0,365).toFloat()/365f)).coerceIn(.08f,.92f)}
    var del by remember{ mutableStateOf(false) }; var keep by remember{ mutableStateOf(false) }; var hidden by remember{ mutableStateOf(false) }
    Card(shape=RoundedCornerShape(24.dp),elevation=CardDefaults.cardElevation(3.dp),modifier=Modifier.fillMaxWidth().clickable{on编辑(r)}){
        Box(Modifier.background(Brush.linearGradient(theme)).padding(15.dp)){
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    OperatorLogo(r.operator.ifBlank{guessOperator(r.number, Countries.list.firstOrNull{it.code==r.countryCode}?.iso ?: r.countryName)})
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)){
                        Row(verticalAlignment=Alignment.CenterVertically){ Text(r.operator.ifBlank{r.countryName},fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis); Spacer(Modifier.width(6.dp)); Text("CO",fontSize=9.sp,color=Color.White,modifier=Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF007AFF).copy(alpha=.75f)).padding(horizontal=4.dp,vertical=1.dp)) }
                        Text(r.countryName,fontSize=12.sp,color=Color.White.copy(alpha=.85f),maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Text(if(hidden)"◉" else "◎",fontSize=19.sp,color=Color.White.copy(alpha=.9f),modifier=Modifier.clickable{hidden=!hidden})
                }
                Row(verticalAlignment=Alignment.CenterVertically){ Text("✓",fontSize=12.sp,color=Color.White); Spacer(Modifier.width(5.dp)); Text(status,fontSize=12.sp,color=Color.White.copy(alpha=.92f)); Spacer(Modifier.width(7.dp)); Text("${r.expireDate} · ${if(days==null)"未知" else if(days<0)"已过期 ${-days} 天" else "还有 ${days} 天"}",fontSize=12.sp,color=Color.White.copy(alpha=.92f),maxLines=1,overflow=TextOverflow.Ellipsis) }
                Text(if(hidden) maskNumber(formatNumber(r.number)) else "${r.countryCode} ${maskNumber(formatNumber(r.number))}",fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(r.note.ifBlank{L("预付费 / 保号套餐")},fontSize=12.sp,color=Color.White.copy(alpha=.82f),maxLines=1,overflow=TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha=.28f))){ Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color(0xFF34C759))) }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    WhitePill(L("保号")){keep=true}
                    WhitePill(L("刷流量")){onTraffic(r)}
                    WhitePill(L("删除"),danger=true){del=true}
                }
            }
        }
    }
    if(keep) KeepCycleDialog(r,onKeep){keep=false}
    if(del) IOSConfirmDialog(L("删除号码？"),L("删除")+" ${r.countryCode} ${formatNumber(r.number)} "+L("删除后不可恢复"),true,{del=false},{del=false;onDel(r)})
}

@Composable fun WhitePill(text:String,danger:Boolean=false,onClick:()->Unit){
    val c=if(danger) Color(0xFFFF3B30) else Color(0xFF007AFF)
    Text(text,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=c,modifier=Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha=.92f)).clickable{onClick()}.padding(horizontal=10.dp,vertical=5.dp))
}

fun countryTheme(code:String,name:String):List<Color>{
    return when{
        code=="+66" || name.contains("泰") -> listOf(Color(0xFF6A5CFF),Color(0xFF21C784))
        code=="+1" -> listOf(Color(0xFF1E3A8A),Color(0xFFDC2626))
        code=="+81" -> listOf(Color(0xFFEF4444),Color(0xFFFFD1D1))
        code=="+49" -> listOf(Color(0xFF111827),Color(0xFFF59E0B))
        code=="+852" || name.contains("香港") -> listOf(Color(0xFFB91C1C),Color(0xFFFF6B6B))
        code=="+853" || name.contains("澳门") -> listOf(Color(0xFF047857),Color(0xFFF59E0B))
        code=="+86" || name.contains("中国") -> listOf(Color(0xFFE60012),Color(0xFFD40000))
        else -> listOf(Color(0xFF2563EB),Color(0xFF0EA5E9))
    }
}

@Composable fun FlagArtPanel(r:PhoneNumberRecord,m:Modifier){
    val ctx = LocalContext.current
    val colors=countryTheme(r.countryCode,r.countryName)
    val iso = Countries.list.firstOrNull{it.code==r.countryCode && it.name==r.countryName}?.iso
        ?: Countries.list.firstOrNull{it.code==r.countryCode}?.iso
        ?: when{
            r.countryName.contains("中国") -> "CN"
            r.countryName.contains("香港") -> "HK"
            r.countryName.contains("澳门") -> "MO"
            else -> ""
        }
    val assetPath = if(iso.isBlank()) "" else "flag_backgrounds/${iso.lowercase()}.png"
    val flagBitmap = rememberAssetBitmap(assetPath)
    Box(m.background(Brush.linearGradient(colors)),contentAlignment=Alignment.Center){
        if(flagBitmap != null){
            Image(bitmap=flagBitmap,contentDescription=r.countryName,contentScale=ContentScale.FillBounds,modifier=Modifier.fillMaxSize().graphicsLayer(alpha=.96f))
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.White.copy(alpha=.16f),Color.Transparent,Color.Black.copy(alpha=.20f)))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.10f),Color.Transparent,Color.Black.copy(alpha=.10f)))))
        }else{
            when{
                r.countryCode=="+86" || r.countryName.contains("中国") -> Box(Modifier.fillMaxSize()){
                    Text("★",fontSize=46.sp,color=Color(0xFFFFD21F).copy(alpha=.92f),modifier=Modifier.align(Alignment.TopStart).padding(start=20.dp,top=16.dp).graphicsLayer(rotationZ=-8f))
                    Text("★",fontSize=15.sp,color=Color(0xFFFFD21F).copy(alpha=.88f),modifier=Modifier.align(Alignment.TopStart).padding(start=70.dp,top=13.dp).graphicsLayer(rotationZ=18f))
                    Text("★",fontSize=15.sp,color=Color(0xFFFFD21F).copy(alpha=.88f),modifier=Modifier.align(Alignment.TopStart).padding(start=84.dp,top=31.dp).graphicsLayer(rotationZ=36f))
                    Text("★",fontSize=15.sp,color=Color(0xFFFFD21F).copy(alpha=.88f),modifier=Modifier.align(Alignment.TopStart).padding(start=83.dp,top=53.dp).graphicsLayer(rotationZ=10f))
                    Text("★",fontSize=15.sp,color=Color(0xFFFFD21F).copy(alpha=.88f),modifier=Modifier.align(Alignment.TopStart).padding(start=68.dp,top=70.dp).graphicsLayer(rotationZ=-18f))
                }
                else -> Text(r.flag,fontSize=68.sp,color=Color.White.copy(alpha=.82f),modifier=Modifier.graphicsLayer(scaleX=1.08f,scaleY=1.08f))
            }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.White.copy(alpha=.12f),Color.Transparent,Color.Black.copy(alpha=.18f)))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.10f),Color.Transparent,Color.Black.copy(alpha=.10f)))))
        }
    }
}

object AssetBitmapCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
    fun cached(path:String): ImageBitmap? = cache[path]
    fun decode(ctx:Context, path:String): ImageBitmap? {
        if(path.isBlank()) return null
        cache[path]?.let { return it }
        val bmp = runCatching { ctx.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        if(bmp != null) cache[path] = bmp
        return bmp
    }
}

@Composable
fun rememberAssetBitmap(path:String): ImageBitmap? {
    val ctx = LocalContext.current
    // 命中缓存直接同步返回，避免闪烁；未命中则后台线程解码，主线程不阻塞
    return produceState<ImageBitmap?>(initialValue = AssetBitmapCache.cached(path), key1 = path) {
        if(path.isBlank()) { value = null; return@produceState }
        AssetBitmapCache.cached(path)?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) { AssetBitmapCache.decode(ctx, path) }
    }.value
}

fun parseLpa(text:String):Pair<String,String>{
    val raw=text.trim()
    if(raw.isBlank()) return "" to ""
    val lpa=raw.removePrefix("LPA:1$").removePrefix("lpa:1$")
    val parts=lpa.split("$")
    if(parts.size>=2 && parts[0].contains('.')) return parts[0].trim() to parts[1].trim()
    val smdp=Regex("""(?i)(SM-DP\+?|SMDP|服务器|地址)[:：\s]+([^\s,;，；]+)""").find(raw)?.groupValues?.getOrNull(2)?.trim().orEmpty()
    val code=Regex("""(?i)(激活码|Activation\s*Code|code|AC)[:：\s]+([^\s,;，；]+)""").find(raw)?.groupValues?.getOrNull(2)?.trim().orEmpty()
    if(smdp.isNotBlank() || code.isNotBlank()) return smdp to code
    return "" to raw
}
fun formatChineseDate(s:String):String = runCatching{ val d=LocalDate.parse(s); "${d.year}年${d.monthValue}月${d.dayOfMonth}日" }.getOrElse{s}
fun formatDateByLang(s:String,lang:String):String = runCatching{ val d=LocalDate.parse(s); when(lang){"English"->"${d.year}-${d.monthValue.toString().padStart(2,'0')}-${d.dayOfMonth.toString().padStart(2,'0')}";"日本語"->"${d.year}年${d.monthValue}月${d.dayOfMonth}日";"阿拉伯语"->"${d.dayOfMonth}/${d.monthValue}/${d.year}";else->"${d.year}年${d.monthValue}月${d.dayOfMonth}日"} }.getOrElse{s}
fun estimateBalance(r:PhoneNumberRecord):String = when{
    r.countryCode=="+81" -> "250.00 CNY"
    r.countryCode=="+1" -> "4.50 USD"
    r.countryCode=="+49" -> "0.01 USD"
    r.countryCode=="+66" -> "2.40 CNY"
    else -> "--"
}
fun signalIcon(s:String)=when{ s.contains("离线")||s.contains("无") -> "○"; s.contains("弱") -> "▂"; s.contains("强") -> "▂▄▆"; else -> "▂▄" }

@Composable fun SimHubBottomNav(screen:String,on:(String)->Unit){
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.98f),shadowElevation=7.dp){
        Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal=20.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically){
            listOf("home" to L("号码"),"tools" to L("工具"),"settings" to L("设置")).forEach{ item->
                val sel=screen==item.first
                val scale by animateFloatAsState(targetValue=if(sel)1.02f else 1f,animationSpec=tween(120),label="navScale")
                val tint=if(sel) Color(0xFF007AFF) else Color(0xFF8E8E93)
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp),modifier=Modifier.clip(RoundedCornerShape(16.dp)).background(if(sel)Color(0xFF007AFF).copy(alpha=.08f) else Color.Transparent).clickable{on(item.first)}.padding(horizontal=18.dp,vertical=7.dp).graphicsLayer(scaleX=scale,scaleY=scale)){
                    BottomLineIcon(item.first,tint)
                    Text(item.second,fontSize=11.sp,fontWeight=if(sel)FontWeight.SemiBold else FontWeight.Normal,color=tint)
                }
            }
        }
    }
}

@Composable fun BottomLineIcon(type:String,color:Color){
    Canvas(Modifier.size(22.dp)){
        val w=size.width; val h=size.height
        val stroke=Stroke(width=2.2f)
        when(type){
            "home"->{
                drawRoundRect(color,topLeft=Offset(w*.22f,h*.16f),size=Size(w*.56f,h*.68f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.09f,w*.09f),style=stroke)
                drawLine(color,Offset(w*.34f,h*.30f),Offset(w*.66f,h*.30f),strokeWidth=2.2f)
                drawCircle(color,radius=w*.045f,center=Offset(w*.50f,h*.70f))
            }
            "tools"->{
                drawRoundRect(color,topLeft=Offset(w*.18f,h*.34f),size=Size(w*.64f,h*.42f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.08f,w*.08f),style=stroke)
                drawLine(color,Offset(w*.38f,h*.34f),Offset(w*.38f,h*.24f),strokeWidth=2.2f)
                drawLine(color,Offset(w*.62f,h*.34f),Offset(w*.62f,h*.24f),strokeWidth=2.2f)
                drawLine(color,Offset(w*.38f,h*.24f),Offset(w*.62f,h*.24f),strokeWidth=2.2f)
            }
            else->{
                drawCircle(color,radius=w*.26f,center=Offset(w*.5f,h*.5f),style=stroke)
                drawCircle(color,radius=w*.075f,center=Offset(w*.5f,h*.5f))
                for(a in listOf(0f,90f,180f,270f)){
                    val rad=Math.toRadians(a.toDouble()).toFloat()
                    val x1=w*.5f+kotlin.math.cos(rad)*w*.34f; val y1=h*.5f+kotlin.math.sin(rad)*h*.34f
                    val x2=w*.5f+kotlin.math.cos(rad)*w*.43f; val y2=h*.5f+kotlin.math.sin(rad)*h*.43f
                    drawLine(color,Offset(x1,y1),Offset(x2,y2),strokeWidth=2.0f)
                }
            }
        }
    }
}

object OperatorLogoAssets {
    val map: Map<String,String> = mapOf(
        "CH|swisscom" to "file:///android_asset/operator_logos/ch_swisscom.png",
        "FR|orange france" to "file:///android_asset/operator_logos/fr_orange.png",
        "NL|kpn" to "file:///android_asset/operator_logos/nl_kpn.png",
        "US|at&t" to "file:///android_asset/operator_logos/us_att.png",
        "US|at-t" to "file:///android_asset/operator_logos/us_att.png",
        "US|att" to "file:///android_asset/operator_logos/us_att.png",
        "US|fi" to "file:///android_asset/operator_logos/us_google_fi.png",
        "US|google fi" to "file:///android_asset/operator_logos/us_google_fi.png",
        "US|google-fi" to "file:///android_asset/operator_logos/us_google_fi.png",
        "AU|optus" to "file:///android_asset/operator_logos/au_optus.png",
        "AU|telstra" to "file:///android_asset/operator_logos/au_telstra.png",
        "AU|vodafone" to "file:///android_asset/operator_logos/au_vodafone.png",
        "AU|vodafone au" to "file:///android_asset/operator_logos/au_vodafone.png",
        "AU|vodafone australia" to "file:///android_asset/operator_logos/au_vodafone.png",
        "CN|china broadcast" to "file:///android_asset/operator_logos/cn_china_broadcast.png",
        "CN|china broadnet" to "file:///android_asset/operator_logos/cn_china_broadcast.png",
        "CN|china mobile" to "file:///android_asset/operator_logos/cn_china_mobile.png",
        "CN|china telecom" to "file:///android_asset/operator_logos/cn_china_telecom.png",
        "CN|china unicom" to "file:///android_asset/operator_logos/cn_china_unicom.png",
        "CN|china_broadcast" to "file:///android_asset/operator_logos/cn_china_broadcast.png",
        "CN|china_mobile" to "file:///android_asset/operator_logos/cn_china_mobile.png",
        "CN|china_telecom" to "file:///android_asset/operator_logos/cn_china_telecom.png",
        "CN|china_unicom" to "file:///android_asset/operator_logos/cn_china_unicom.png",
        "CN|chinabroadcast" to "file:///android_asset/operator_logos/cn_china_broadcast.png",
        "CN|chinamobile" to "file:///android_asset/operator_logos/cn_china_mobile.png",
        "CN|chinatelecom" to "file:///android_asset/operator_logos/cn_china_telecom.png",
        "CN|chinaunicom" to "file:///android_asset/operator_logos/cn_china_unicom.png",
        "CN|中国广电" to "file:///android_asset/operator_logos/cn_china_broadcast.png",
        "CN|中国电信" to "file:///android_asset/operator_logos/cn_china_telecom.png",
        "CN|中国移动" to "file:///android_asset/operator_logos/cn_china_mobile.png",
        "CN|中国联通" to "file:///android_asset/operator_logos/cn_china_unicom.png",
        "DE|deutsche telekom" to "file:///android_asset/operator_logos/de_telekom.png",
        "DE|o2" to "file:///android_asset/operator_logos/de_o2.png",
        "DE|o2 de" to "file:///android_asset/operator_logos/de_o2.png",
        "DE|o2 germany" to "file:///android_asset/operator_logos/de_o2.png",
        "DE|telekom" to "file:///android_asset/operator_logos/de_telekom.png",
        "DE|vodafone" to "file:///android_asset/operator_logos/de_vodafone.png",
        "DE|vodafone de" to "file:///android_asset/operator_logos/de_vodafone.png",
        "DE|vodafone germany" to "file:///android_asset/operator_logos/de_vodafone.png",
        "FR|bouygues" to "file:///android_asset/operator_logos/fr_bouygues.png",
        "FR|bouygues telecom" to "file:///android_asset/operator_logos/fr_bouygues.png",
        "FR|orange" to "file:///android_asset/operator_logos/fr_orange.png",
        "FR|sfr" to "file:///android_asset/operator_logos/fr_sfr.png",
        "GB|3 uk" to "file:///android_asset/operator_logos/uk_three.png",
        "GB|ee" to "file:///android_asset/operator_logos/uk_ee.png",
        "GB|o2" to "file:///android_asset/operator_logos/uk_o2.png",
        "GB|o2 uk" to "file:///android_asset/operator_logos/uk_o2.png",
        "GB|telefonica uk" to "file:///android_asset/operator_logos/uk_o2.png",
        "GB|three" to "file:///android_asset/operator_logos/uk_three.png",
        "GB|three uk" to "file:///android_asset/operator_logos/uk_three.png",
        "GB|vodafone" to "file:///android_asset/operator_logos/uk_vodafone.png",
        "GB|vodafone uk" to "file:///android_asset/operator_logos/uk_vodafone.png",
        "HK|3 hong kong" to "file:///android_asset/operator_logos/hk_three.png",
        "HK|3hk" to "file:///android_asset/operator_logos/hk_three.png",
        "HK|china mobile hong kong" to "file:///android_asset/operator_logos/hk_cmhk.png",
        "HK|cmhk" to "file:///android_asset/operator_logos/hk_cmhk.png",
        "HK|csl" to "file:///android_asset/operator_logos/hk_csl.png",
        "HK|hkt" to "file:///android_asset/operator_logos/hk_csl.png",
        "HK|hong kong csl" to "file:///android_asset/operator_logos/hk_csl.png",
        "HK|smartone" to "file:///android_asset/operator_logos/hk_smartone.png",
        "HK|three" to "file:///android_asset/operator_logos/hk_three.png",
        "HK|three hk" to "file:///android_asset/operator_logos/hk_three.png",
        "HK|中国移动香港" to "file:///android_asset/operator_logos/hk_cmhk.png",
        "ID|indosat" to "file:///android_asset/operator_logos/id_indosat.png",
        "ID|indosat ooredoo" to "file:///android_asset/operator_logos/id_indosat.png",
        "ID|telkomsel" to "file:///android_asset/operator_logos/id_telkomsel.png",
        "ID|xl" to "file:///android_asset/operator_logos/id_xl.png",
        "ID|xl axiata" to "file:///android_asset/operator_logos/id_xl.png",
        "IN|airtel" to "file:///android_asset/operator_logos/in_airtel.png",
        "IN|bharti airtel" to "file:///android_asset/operator_logos/in_airtel.png",
        "IN|jio" to "file:///android_asset/operator_logos/in_jio.png",
        "IN|reliance jio" to "file:///android_asset/operator_logos/in_jio.png",
        "IN|vi" to "file:///android_asset/operator_logos/in_vi.png",
        "IN|vodafone idea" to "file:///android_asset/operator_logos/in_vi.png",
        "JP|au" to "file:///android_asset/operator_logos/jp_au.png",
        "JP|au(kddi)" to "file:///android_asset/operator_logos/jp_au.png",
        "JP|docomo" to "file:///android_asset/operator_logos/jp_docomo.png",
        "JP|kddi" to "file:///android_asset/operator_logos/jp_au.png",
        "JP|ntt docomo" to "file:///android_asset/operator_logos/jp_docomo.png",
        "JP|rakuten" to "file:///android_asset/operator_logos/jp_rakuten.png",
        "JP|rakuten mobile" to "file:///android_asset/operator_logos/jp_rakuten.png",
        "JP|softbank" to "file:///android_asset/operator_logos/jp_softbank.png",
        "KR|kt" to "file:///android_asset/operator_logos/kr_kt.png",
        "KR|lg u plus" to "file:///android_asset/operator_logos/kr_lg_uplus.png",
        "KR|lg u+" to "file:///android_asset/operator_logos/kr_lg_uplus.png",
        "KR|lg uplus" to "file:///android_asset/operator_logos/kr_lg_uplus.png",
        "KR|lg_uplus" to "file:///android_asset/operator_logos/kr_lg_uplus.png",
        "KR|lguplus" to "file:///android_asset/operator_logos/kr_lg_uplus.png",
        "KR|sk telecom" to "file:///android_asset/operator_logos/kr_sk_telecom.png",
        "KR|sk_telecom" to "file:///android_asset/operator_logos/kr_sk_telecom.png",
        "KR|skt" to "file:///android_asset/operator_logos/kr_sk_telecom.png",
        "KR|sktelecom" to "file:///android_asset/operator_logos/kr_sk_telecom.png",
        "MY|celcom" to "file:///android_asset/operator_logos/my_celcom.png",
        "MY|celcomdigi" to "file:///android_asset/operator_logos/my_digi.png",
        "MY|digi" to "file:///android_asset/operator_logos/my_digi.png",
        "MY|maxis" to "file:///android_asset/operator_logos/my_maxis.png",
        "SG|m1" to "file:///android_asset/operator_logos/sg_m1.png",
        "SG|singtel" to "file:///android_asset/operator_logos/sg_singtel.png",
        "SG|starhub" to "file:///android_asset/operator_logos/sg_starhub.png",
        "TH|ais" to "file:///android_asset/operator_logos/th_ais.png",
        "TH|dtac" to "file:///android_asset/operator_logos/th_dtac.png",
        "TH|true" to "file:///android_asset/operator_logos/th_true.png",
        "TH|truemove h" to "file:///android_asset/operator_logos/th_true.png",
        "TW|chunghwa" to "file:///android_asset/operator_logos/tw_chunghwa.png",
        "TW|chunghwa telecom" to "file:///android_asset/operator_logos/tw_chunghwa.png",
        "TW|fareastone" to "file:///android_asset/operator_logos/tw_fareastone.png",
        "TW|fet" to "file:///android_asset/operator_logos/tw_fareastone.png",
        "TW|taiwan mobile" to "file:///android_asset/operator_logos/tw_taiwan_mobile.png",
        "TW|taiwan_mobile" to "file:///android_asset/operator_logos/tw_taiwan_mobile.png",
        "TW|taiwanmobile" to "file:///android_asset/operator_logos/tw_taiwan_mobile.png",
        "TW|中华电信" to "file:///android_asset/operator_logos/tw_chunghwa.png",
        "TW|台湾大哥大" to "file:///android_asset/operator_logos/tw_taiwan_mobile.png",
        "TW|远传电信" to "file:///android_asset/operator_logos/tw_fareastone.png",
        "US|t mobile" to "file:///android_asset/operator_logos/us_tmobile.png",
        "US|t-mobile" to "file:///android_asset/operator_logos/us_tmobile.png",
        "US|tmobile" to "file:///android_asset/operator_logos/us_tmobile.png",
        "US|us mobile" to "file:///android_asset/operator_logos/us_us_mobile.png",
        "US|us_mobile" to "file:///android_asset/operator_logos/us_us_mobile.png",
        "US|usmobile" to "file:///android_asset/operator_logos/us_us_mobile.png",
        "US|verizon" to "file:///android_asset/operator_logos/us_verizon.png",
        "US|verizon wireless" to "file:///android_asset/operator_logos/us_verizon.png"
    )
    fun assetFor(name:String, iso:String?=null):String {
        val q=name.trim().lowercase()
        if(q.isBlank()) return ""
        fun norm(x:String)=x.lowercase().replace("&","and").replace("+","plus").replace(Regex("[^a-z0-9一-龥]+"),"")
        val nq=norm(q)
        if(iso!=null) {
            val prefix="${iso.uppercase()}|"
            map[prefix+q]?.let{return it}
            map.entries.firstOrNull{ it.key.startsWith(prefix) && norm(it.key.substringAfter("|"))==nq }?.let{return it.value}
        }
        map.entries.firstOrNull{
            val k=it.key.substringAfter("|")
            it.key.endsWith("|$q") || norm(k)==nq || nq.contains(norm(k)) || norm(k).contains(nq)
        }?.let{return it.value}
        return ""
    }
}

@Composable fun AppBackground(settings:App设置){
    if(settings.backgroundUri.isNotBlank()){
        AsyncImage(model=settings.backgroundUri,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize().background(Color(0xFFF4F5F7)))
        Box(Modifier.fillMaxSize().background((if(settings.dark) Color.Black else Color.White).copy(alpha=(1f-settings.backgroundAlpha).coerceIn(.18f,.82f))))
    }else Box(Modifier.fillMaxSize().background(if(settings.dark) Color(0xFF0B0F17) else Color(0xFFF4F5F7)))
}

@Composable fun Home(ctx:Context,records:List<PhoneNumberRecord>,settings:App设置,search:String,filter:String,sortMode:String,on筛选:(String)->Unit,on排序:()->Unit,onAdd:()->Unit,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onDial:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit){
    val today=LocalDate.now()
    fun daysOf(r:PhoneNumberRecord)=runCatching{LocalDate.parse(r.expireDate).toEpochDay()-today.toEpochDay()}.getOrNull()
    val q=search.trim().lowercase()
    val filtered=records.filter{ r->
        val d=daysOf(r)
        val ok=when(filter){"正常"->d!=null && d>settings.remind天;"即将到期"->d!=null && d in 0..settings.remind天;"已过期"->d!=null && d<0;else->true}
        ok && (q.isEmpty() || (r.number+r.operator+r.countryName+r.countryCode+r.note).lowercase().contains(q))
    }
    val shown=if(sortMode=="到期远") filtered.sortedByDescending{ daysOf(it) ?: Long.MIN_VALUE } else filtered.sortedBy{ daysOf(it) ?: Long.MAX_VALUE }
    Box(Modifier.fillMaxSize()){
        AppBackground(settings)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{ FilterToolRow(filter,sortMode,on筛选,on排序,shown.size) }
            if(shown.isEmpty()) item{ Box(Modifier.fillMaxWidth().height(260.dp),contentAlignment=Alignment.Center){ Column(horizontalAlignment=Alignment.CenterHorizontally){Text(L("暂无号码"),fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text(L("点击右下角添加号码"),fontSize=13.sp,color=Color(0xFF8E8E93))} } }
            else items(shown,key={it.id}){ r-> CompactSimCard(r,on编辑,onDel,onTraffic,onKeep,daysOf(r),settings.remind天,settings.showFlag) }
            item{ Spacer(Modifier.height(90.dp)) }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=86.dp).size(56.dp)){
            FloatingActionButton(onClick=onAdd,containerColor=Color(0xFF007AFF),contentColor=Color.White,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxSize()){Text("＋",fontSize=27.sp,fontWeight=FontWeight.Medium)}
        }
    }
}

@Composable fun SmallActionPill(text:String,color:Color=Color(0xFF007AFF),onClick:()->Unit){
    val source=remember{ MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).96f else 1f,animationSpec=tween(120),label="pillPress")
    Text(text,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=color,modifier=Modifier.graphicsLayer(scaleX=scale,scaleY=scale).clip(RoundedCornerShape(999.dp)).background(color.copy(alpha=.08f)).clickable(interactionSource=source,indication=null){onClick()}.padding(horizontal=9.dp,vertical=5.dp))
}

@Composable fun TinyActionButton(text:String,color:Color=Color(0xFF007AFF),onClick:()->Unit){ SmallActionPill(text,color,onClick) }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun DefaultNumberCard(r:PhoneNumberRecord,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onDial:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit){
    val today=LocalDate.now(); val days=runCatching{LocalDate.parse(r.expireDate).toEpochDay()-today.toEpochDay()}.getOrNull()
    val color=when{days==null->Color(0xFF8A94A6);days<0->Color(0xFFFF3B30);days<=7->Color(0xFFFF9500);else->Color(0xFF34C759)}
    var confirmDelete by remember{ mutableStateOf(false) }
    var keepDlg by remember{ mutableStateOf(false) }
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xF7FFFFFF)),elevation=CardDefaults.cardElevation(defaultElevation=6.dp),modifier=Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=2.dp).border(1.dp,Color.White.copy(alpha=.75f),RoundedCornerShape(24.dp))){
        Column(Modifier.padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.White),contentAlignment=Alignment.Center){Text(r.flag,fontSize=27.sp)}
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                    Text(L("默认保号号码"),fontSize=11.sp,color=Color(0xFF007AFF),fontWeight=FontWeight.Bold)
                    Text(r.operator.ifBlank{r.countryName},fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${r.countryCode} ${maskNumber(formatNumber(r.number))}",fontSize=13.sp,color=Color(0xFF4B5563))
                    Text(L("到期")+"：${r.expireDate} · "+expireText(LocalAppLanguage.current,days),fontSize=12.sp,color=color)
                    Text(L("备注")+"：${r.note.ifBlank{L("预付费 / 保号套餐")}}",fontSize=10.sp,color=Color(0xFF6B7280),maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF007AFF)).clickable{on编辑(r)},contentAlignment=Alignment.Center){Text("›",color=Color.White,fontSize=25.sp)}
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
                SmallActionPill(L("保号"),Color(0xFF007AFF)){keepDlg=true}
                SmallActionPill(L("刷流量"),Color(0xFF007AFF)){onTraffic(r)}
                SmallActionPill(L("删除"),Color(0xFFFF3B30)){confirmDelete=true}
            }
        }
    }
    if(keepDlg) KeepCycleDialog(r,onKeep){keepDlg=false}
    if(confirmDelete) IOSConfirmDialog(L("删除号码？"),L("删除")+" ${r.countryCode} ${formatNumber(r.number)} "+L("删除后不可恢复"),true,{confirmDelete=false},{confirmDelete=false;onDel(r)})
}


@Composable fun OperatorLogo(name:String){
    val op=name.uppercase()
    val label=when{
        "移动" in name || "CHINA MOBILE" in op -> "CM"
        "联通" in name || "UNICOM" in op -> "CU"
        "电信" in name || "TELECOM" in op -> "CT"
        "广电" in name -> "CB"
        "US MOBILE" in op -> "USM"
        "3HK" in op || "THREE" in op -> "3"
        "HKT" in op || "CSL" in op -> "HKT"
        "SMARTONE" in op -> "ST"
        "CMHK" in op -> "CMHK"
        "CTM" in op -> "CTM"
        "RAKUTEN" in op -> "R"
        "SOFTBANK" in op -> "SB"
        "DOCOMO" in op -> "doc"
        "AIS" in op -> "AIS"
        "TRUE" in op -> "TRUE"
        "DTAC" in op -> "dtac"
        "VODAFONE" in op -> "V"
        "T-MOBILE" in op -> "T"
        "AT&T" in op -> "AT&T"
        "VERIZON" in op -> "VZ"
        else -> name.take(3).ifBlank{"SIM"}
    }
    val bg=when(label){"CM"->Color(0xFF22C55E);"CU"->Color(0xFFE11D48);"CT"->Color(0xFF2563EB);"AIS"->Color(0xFF16A34A);"R"->Color(0xFFE91E63);"V"->Color(0xFFE60000);"USM"->Color(0xFF2563EB);"3"->Color(0xFF7C3AED);else->Color(0xFF111827)}
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(bg),contentAlignment=Alignment.Center){Text(label,fontSize=if(label.length>3) 9.sp else 14.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1)}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun NumberListRow(r:PhoneNumberRecord,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit,days:Long?){
    val status=if(days==null)L("未知") else if(days<0)L("已过期") else if(days<=7)L("即将到期") else L("正常")
    val statusColor=when{days!=null && days<0->Color(0xFFFF3B30);days!=null && days<=7->Color(0xFFFF9500);else->Color(0xFF007AFF)}
    var confirmDelete by remember{ mutableStateOf(false) }
    var keepDlg by remember{ mutableStateOf(false) }
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(5.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.clickable{on编辑(r)}){
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF1F5FA)),contentAlignment=Alignment.Center){Text(r.flag,fontSize=25.sp)}
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Text(r.operator.ifBlank{r.countryName},fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${r.countryCode} ${maskNumber(formatNumber(r.number))}",fontSize=12.sp,color=Color(0xFF6B7280),maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(L("到期")+"：${r.expireDate} · "+expireText(LocalAppLanguage.current,days),fontSize=12.sp,color=statusColor,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Text(status,fontSize=10.sp,color=statusColor,modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha=.10f)).padding(horizontal=7.dp,vertical=4.dp))
                Spacer(Modifier.width(6.dp)); Text("›",fontSize=22.sp,color=Color(0xFF9CA3AF))
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                TinyActionButton(L("保号")){keepDlg=true}
                TinyActionButton(L("刷流量")){onTraffic(r)}
                TinyActionButton(L("删除"),Color(0xFFFF3B30)){confirmDelete=true}
            }
        }
    }
    if(keepDlg) KeepCycleDialog(r,onKeep){keepDlg=false}
    if(confirmDelete) IOSConfirmDialog(L("删除号码？"),L("删除")+" ${r.countryCode} ${formatNumber(r.number)} "+L("删除后不可恢复"),true,{confirmDelete=false},{confirmDelete=false;onDel(r)})
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun KeepCycleDialog(r:PhoneNumberRecord,onKeep:(PhoneNumberRecord,Int)->Unit,onDismiss:()->Unit){
    var days by remember{ mutableStateOf(30) }
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(30.dp),color=Color(0xFFF2F3F7),modifier=Modifier.fillMaxWidth(.92f).widthIn(max=360.dp)){
            Column(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp)){
                Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(L("延长保号"),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827))
                    Text("${r.countryCode} ${formatNumber(r.number)}",fontSize=15.sp,color=Color(0xFF8A94A6),maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text(L("选择周期"),fontSize=13.sp,color=Color(0xFF8A94A6),modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
                    FlowRow(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        listOf(7 to "7",15 to "15",30 to "30",90 to "90",180 to "180",365 to "365").forEach{(d,label)-> IOSChip(label,days==d,Modifier.width(68.dp).height(44.dp)){days=d} }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    Button(onClick=onDismiss,modifier=Modifier.weight(1f).height(54.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF374151))){Text(L("取消"),fontSize=16.sp)}
                    Button(onClick={onKeep(r,days);onDismiss()},modifier=Modifier.weight(1f).height(54.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF),contentColor=Color.White)){Text(L("确认延长"),fontSize=16.sp,fontWeight=FontWeight.SemiBold)}
                }
            }
        }
    }
}

@Composable fun KeepPage(records:List<PhoneNumberRecord>,onKeep:(PhoneNumberRecord,Int)->Unit){
    var selectedId by remember{ mutableStateOf(records.firstOrNull()?.id ?: "") }; var months by remember{ mutableStateOf(30) }
    val r=records.firstOrNull{it.id==selectedId} ?: records.firstOrNull()
    Column(Modifier.fillMaxSize().background(Color(0xFFF2F3F7)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        if(r==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(L("暂无号码"))}} else {
            IOSSection(L("选择号码")){ records.forEach{ item-> KeepChoice(item.operator.ifBlank{item.countryName}+"  "+item.countryCode+" "+maskNumber(formatNumber(item.number)), selectedId==item.id){selectedId=item.id} } }
            IOSSection(L("选择保号周期")){ listOf(7 to "7",15 to "15",30 to "30",90 to "90",180 to "180",365 to "365").forEach{(m,label)-> KeepChoice(label, months==m){months=m} } }
            Button(onClick={onKeep(r,months)},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF))){Text(L("确认延长"))}
        }
    }
}
@Composable fun KeepChoice(text:String,selected:Boolean,onClick:()->Unit){ Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if(selected) Color(0xFFEAF3FF) else Color.White).clickable{onClick()}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(if(selected)"●" else "○",color=Color(0xFF007AFF));Spacer(Modifier.width(10.dp));Text(text,fontSize=16.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)} }


@Composable fun ToolsPage(settings:App设置,records:List<PhoneNumberRecord>,onTraffic:(PhoneNumberRecord)->Unit,onDial:(PhoneNumberRecord)->Unit,onExportJson:()->Unit,onExportCsv:()->Unit,onImportText:(String)->Unit){
    var pickTraffic by remember{ mutableStateOf(false) }
    var pickDial by remember{ mutableStateOf(false) }
    var importDlg by remember{ mutableStateOf(false) }
    var importText by remember{ mutableStateOf("") }
    Box(Modifier.fillMaxSize()){
        AppBackground(settings)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item{
                IOSSection(L("常用工具")){
                    ToolRow("traffic",L("刷流量"),L("选择一个号码执行真实下载流量测试")){ pickTraffic=true }
                    ToolRow("dial",L("拨号测试"),L("选择号码并打开系统拨号器")){ pickDial=true }
                    ToolRow("export_json",L("导出 JSON"),L("生成完整 JSON 备份文本")){ onExportJson() }
                    ToolRow("export_csv",L("导出 CSV"),L("生成 CSV 表格文本")){ onExportCsv() }
                    ToolRow("import",L("导入数据"),L("粘贴 JSON 或 CSV 恢复号码列表")){ importDlg=true }
                }
            }
        }
    }
    if(pickTraffic) NumberPickerDialog(L("选择刷流量号码"),records,{pickTraffic=false}){ pickTraffic=false; onTraffic(it) }
    if(pickDial) NumberPickerDialog(L("选择拨号号码"),records,{pickDial=false}){ pickDial=false; onDial(it) }
    if(importDlg) IOSImportDialog(importText,{importText=it},{importDlg=false},{onImportText(importText);importDlg=false})
}

@Composable fun ToolRow(iconType:String,title:String,sub:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable{onClick()}.padding(horizontal=6.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F6FF)),contentAlignment=Alignment.Center){ ToolLineIcon(iconType,Color(0xFF007AFF)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)){Text(title,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF111827));Text(sub,fontSize=12.sp,color=Color(0xFF8A94A6),maxLines=1,overflow=TextOverflow.Ellipsis)}
        Text("›",fontSize=24.sp,color=Color(0xFFC7C7CC))
    }
}

@Composable fun ToolLineIcon(type:String,color:Color){
    Canvas(Modifier.size(22.dp)){
        val w=size.width; val h=size.height
        val stroke=Stroke(width=2.1f)
        when(type){
            "traffic"->{
                drawRoundRect(color,topLeft=Offset(w*.18f,h*.22f),size=Size(w*.64f,h*.56f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.08f,w*.08f),style=stroke)
                drawLine(color,Offset(w*.30f,h*.62f),Offset(w*.30f,h*.44f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.50f,h*.62f),Offset(w*.50f,h*.34f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.70f,h*.62f),Offset(w*.70f,h*.50f),strokeWidth=2.1f)
            }
            "dial"->{
                drawLine(color,Offset(w*.34f,h*.26f),Offset(w*.66f,h*.26f),strokeWidth=2.1f)
                drawArc(color,180f,180f,false,topLeft=Offset(w*.20f,h*.18f),size=Size(w*.60f,h*.42f),style=stroke)
                drawLine(color,Offset(w*.28f,h*.66f),Offset(w*.40f,h*.54f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.72f,h*.66f),Offset(w*.60f,h*.54f),strokeWidth=2.1f)
            }
            "export_json"->{
                drawRoundRect(color,topLeft=Offset(w*.24f,h*.18f),size=Size(w*.52f,h*.60f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.07f,w*.07f),style=stroke)
                drawLine(color,Offset(w*.50f,h*.28f),Offset(w*.50f,h*.58f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.40f,h*.48f),Offset(w*.50f,h*.58f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.60f,h*.48f),Offset(w*.50f,h*.58f),strokeWidth=2.1f)
            }
            "export_csv"->{
                drawRoundRect(color,topLeft=Offset(w*.24f,h*.18f),size=Size(w*.52f,h*.60f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.07f,w*.07f),style=stroke)
                drawLine(color,Offset(w*.34f,h*.34f),Offset(w*.66f,h*.34f),strokeWidth=2.0f)
                drawLine(color,Offset(w*.34f,h*.46f),Offset(w*.66f,h*.46f),strokeWidth=2.0f)
                drawLine(color,Offset(w*.34f,h*.58f),Offset(w*.58f,h*.58f),strokeWidth=2.0f)
            }
            else->{
                drawRoundRect(color,topLeft=Offset(w*.24f,h*.18f),size=Size(w*.52f,h*.60f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.07f,w*.07f),style=stroke)
                drawLine(color,Offset(w*.50f,h*.58f),Offset(w*.50f,h*.28f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.40f,h*.38f),Offset(w*.50f,h*.28f),strokeWidth=2.1f)
                drawLine(color,Offset(w*.60f,h*.38f),Offset(w*.50f,h*.28f),strokeWidth=2.1f)
            }
        }
    }
}

@Composable fun NumberPickerDialog(title:String,records:List<PhoneNumberRecord>,onDismiss:()->Unit,onPick:(PhoneNumberRecord)->Unit){
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFFF2F3F7),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),modifier=Modifier.weight(1f))
                    TextButton(onDismiss){Text(L("取消"),color=Color(0xFF007AFF))}
                }
                if(records.isEmpty()) Box(Modifier.fillMaxWidth().height(120.dp),contentAlignment=Alignment.Center){Text(L("暂无号码，请先添加号码。"),color=Color(0xFF8A94A6))}
                else LazyColumn(Modifier.heightIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    items(records){ r ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable{onPick(r)}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){
                            Text(r.flag,fontSize=25.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(r.operator.ifBlank{r.countryName},fontWeight=FontWeight.SemiBold,color=Color(0xFF111827)); Text("${r.countryCode} ${maskNumber(formatNumber(r.number))}",fontSize=12.sp,color=Color(0xFF6B7280))}; Text("›",fontSize=24.sp,color=Color(0xFFC7C7CC))
                        }
                    }
                }
            }
        }
    }
}

@Composable fun IOSImportDialog(value:String,onValue:(String)->Unit,onDismiss:()->Unit,onImport:()->Unit){
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFFF2F3F7),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()){
                    Text(L("导入数据"),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827))
                    Text(L("支持 JSON / CSV，导入前建议先导出备份。"),fontSize=13.sp,color=Color(0xFF8A94A6),lineHeight=18.sp)
                }
                TextField(value=value,onValueChange=onValue,modifier=Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(18.dp)),placeholder={Text(L("粘贴 JSON 或 CSV 数据"))},minLines=7,colors=TextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Button(onClick=onDismiss,modifier=Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF374151))){Text(L("取消"))}
                    Button(onClick=onImport,modifier=Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF),contentColor=Color.White)){Text(L("导入"))}
                }
            }
        }
    }
}

@Composable fun SimHubStat(t:String,v:String,c:Color,m:Modifier){ Card(m,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(1.5.dp)){Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(v,fontSize=22.sp,fontWeight=FontWeight.Bold,color=c);Text(t,fontSize=12.sp,color=Color(0xFF8A94A6))}} }

@Composable fun SoftStat(t:String,v:String,c:Color,m:Modifier){ SimHubStat(t,v,c,m) }

@Composable fun QQStat(t:String,v:String,c:Color,m:Modifier){ SimHubStat(t,v,c,m) }

@Composable fun Stat(t:String,v:String,m:Modifier){ SimHubStat(t,v,Color(0xFF007AFF),m) }
@Composable fun NumberCard(r:PhoneNumberRecord,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onDial:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit){ SimHubCard(r,on编辑,onDel,onDial,onTraffic,onKeep) }

@Composable fun SimHubCard(r:PhoneNumberRecord,on编辑:(PhoneNumberRecord)->Unit,onDel:(PhoneNumberRecord)->Unit,onDial:(PhoneNumberRecord)->Unit,onTraffic:(PhoneNumberRecord)->Unit,onKeep:(PhoneNumberRecord,Int)->Unit){
    val exp=runCatching{LocalDate.parse(r.expireDate)}.getOrNull()
    val today=LocalDate.now()
    val days=exp?.toEpochDay()?.minus(today.toEpochDay())
    val progress=if(days==null) 0f else (days.coerceIn(0,90)/90f).coerceIn(0f,1f)
    val longTerm = days!=null && days>60
    var menu by remember{ mutableStateOf(false) }
    var confirm删除 by remember{ mutableStateOf(false) }
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xF8FFFFFF)),elevation=CardDefaults.cardElevation(defaultElevation=6.dp),modifier=Modifier.fillMaxWidth().padding(vertical=2.dp).border(1.dp,Color.White.copy(alpha=.70f),RoundedCornerShape(24.dp))){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F5FA)),contentAlignment=Alignment.Center){Text(r.flag,fontSize=25.sp)}
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(r.operator.ifBlank{r.countryName},fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f,false))
                        if(longTerm){ Spacer(Modifier.width(6.dp)); Text(L("长期号码"),fontSize=11.sp,color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF007AFF)).padding(horizontal=6.dp,vertical=2.dp)) }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){Text(if(r.note.isBlank()) L("预付费") else r.note,fontSize=12.sp,color=Color(0xFF6B7280),maxLines=1,overflow=TextOverflow.Ellipsis); Spacer(Modifier.width(7.dp)); Text(if(days==null) "无到期日" else if(days<0) "❌ "+expireText(LocalAppLanguage.current,days) else "✅ ${r.expireDate} · "+expireText(LocalAppLanguage.current,days),fontSize=13.sp,color=if(days!=null&&days<0) Color(0xFFFF3B30) else Color(0xFF34C759),maxLines=1,overflow=TextOverflow.Ellipsis)}
                }
            }
            LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)),color=Color(0xFF34C759),trackColor=Color(0xFFE9EDF3))
            Row(verticalAlignment=Alignment.CenterVertically){Text("☎",fontSize=16.sp); Spacer(Modifier.width(7.dp)); Text("${r.countryCode} ${maskNumber(formatNumber(r.number))}",fontSize=13.sp,color=Color(0xFF4B5563),modifier=Modifier); Text("👁",fontSize=15.sp)}
            Row(verticalAlignment=Alignment.CenterVertically){Text("#",fontSize=14.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.width(7.dp)); Text("EID: ${fakeEidForCard(r)}",fontSize=12.sp,color=Color(0xFF6B7280),maxLines=1,overflow=TextOverflow.Ellipsis)}
            TextButton(onClick={menu=!menu},contentPadding=PaddingValues(0.dp)){Text(if(menu) L("隐藏详情") else "⌄ "+L("显示二维码"),color=Color(0xFF007AFF),fontSize=14.sp)}
        }
    }
    if(menu){
        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(0.dp),modifier=Modifier.fillMaxWidth().padding(top=8.dp)){
            Column{
                MenuRow("✎",L("编辑"),Color(0xFF374151)){on编辑(r)}
                Divider(color=Color(0xFFE5E7EB),modifier=Modifier.padding(start=48.dp))
                MenuRow("⧉",L("复制号码"),Color(0xFF374151)){onDial(r)}
                Divider(color=Color(0xFFE5E7EB),modifier=Modifier.padding(start=48.dp))
                MenuRow("🗑",L("删除"),Color(0xFFFF3B30)){confirm删除=true}
            }
        }
    }
    if(confirm删除) IOSConfirmDialog(L("删除号码？"),L("删除")+" ${r.countryCode} ${formatNumber(r.number)} "+L("删除后不可恢复"),true,{confirm删除=false},{confirm删除=false;onDel(r)})
}

@Composable fun MenuRow(icon:String,title:String,color:Color,onClick:()->Unit){ Row(Modifier.fillMaxWidth().clickable{onClick()}.padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=18.sp); Spacer(Modifier.width(10.dp)); Text(title,fontSize=16.sp,color=color,fontWeight=if(color==Color(0xFFFF3B30)) FontWeight.Bold else FontWeight.Normal)} }
fun maskNumber(n:String):String{ val ds=n.filter{it.isDigit()}; return if(ds.length<=4) n else ds.take(4)+" •••• "+ds.takeLast(4) }
fun fakeEidForCard(r:PhoneNumberRecord):String{ val seed=(r.id+r.number).hashCode().toString().filter{it.isDigit()}.padEnd(24,'0').take(24); return "89044000 ${seed.chunked(8).joinToString(" ")}" }


@OptIn(ExperimentalLayoutApi::class)
@Composable fun Full编辑Screen(init:PhoneNumberRecord,onDismiss:()->Unit,onSave:(PhoneNumberRecord)->Unit,onDelete:(PhoneNumberRecord)->Unit={}){
    BackHandler { onDismiss() }
    var r by remember { mutableStateOf(init) }
    var countryDlg by remember { mutableStateOf(false) }
    var qrText by remember { mutableStateOf("") }
    var qrDlg by remember { mutableStateOf(false) }
    var qrInput by remember { mutableStateOf("") }
    val editLang = LocalAppLanguage.current
    val albumLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if(uri!=null) {
            qrText = tr(editLang,"已选择相册图片：")+"${uri}"
            r = r.copy(note = (r.note.ifBlank { tr(editLang,"预付费 / 保号套餐") }) + "\n"+tr(editLang,"二维码图片：")+"${uri}")
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFF2F3F7))){
        Column(Modifier.fillMaxSize()){
            val editStatusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Row(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,top=editStatusBarTop+8.dp,bottom=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick=onDismiss,modifier=Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha=.90f)).border(.7.dp,Color.White.copy(alpha=.75f),RoundedCornerShape(12.dp)),contentPadding=PaddingValues(horizontal=12.dp,vertical=0.dp)){Text(L("取消"),color=Color(0xFF007AFF),fontWeight=FontWeight.SemiBold)}
                Text(if(init.number.isBlank()) L("新增 eSIM") else L("编辑 eSIM"),fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827))
                TextButton(onClick={onSave(r)},modifier=Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF007AFF)),contentPadding=PaddingValues(horizontal=14.dp,vertical=0.dp)){Text(L("完成"),fontWeight=FontWeight.Bold,color=Color.White)}
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                item{
                    SettingsSection(L("运营商与国家")){
                        IOSValueRow(L("国家/地区"),"${r.flag} ${r.countryName} ${r.countryCode}"){ countryDlg=true }
                        IOSDividerLine()
                        IOSField(L("运营商名称"),r.operator,{r=r.copy(operator=it)},L("如 AIS / Vodafone / 中国移动"))
                        val currentIso = remember(r.countryCode, r.countryName){ Countries.list.firstOrNull{it.code==r.countryCode && it.name==r.countryName}?.iso ?: Countries.list.firstOrNull{it.code==r.countryCode}?.iso ?: r.countryName }
                        val detectedOperator = remember(r.number, currentIso){ guessOperator(r.number,currentIso) }
                        val selectedOperator = r.operator.ifBlank { detectedOperator }
                        Text(L("留空时会按号码和国家自动识别。"),fontSize=11.sp,color=Color(0xFF8A94A6))
                        Text(L("当前识别")+"：${detectedOperator}",fontSize=11.sp,color=Color(0xFF8A94A6))
                        Text(L("当前选择")+"：${selectedOperator}",fontSize=11.sp,color=Color(0xFF007AFF),fontWeight=FontWeight.SemiBold)
                        val suggestions = remember(currentIso){ OperatorDatabase.byCountry(currentIso).take(8) }
                        if(suggestions.isNotEmpty()){
                            Text(L("推荐运营商"),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF6B7280),modifier=Modifier.padding(top=6.dp))
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                                suggestions.forEach{ op ->
                                    val active = selectedOperator.equals(op.carrierName,true)
                                    IOSChip(op.carrierName,active){ r=r.copy(operator=op.carrierName) }
                                }
                            }
                        }
                    }
                }
                item{
                    SettingsSection(L("号码与套餐")){
                        IOSField(L("手机号码"),r.number,{r=r.copy(number=it.filter{c->c.isDigit()})},L("输入手机号码"))
                        IOSDividerLine()
                        IOSField(L("套餐余额"),r.balance,{r=r.copy(balance=it)},L("如 1 RMB / 4.50 USD / 2GB"))
                        IOSDividerLine()
                        IOSField(L("套餐备注"),r.note,{r=r.copy(note=it)},L("预付费 / 资费 / 套餐备注"),singleLine=false,minLines=2)
                        IOSDividerLine()
                        IOSField(L("信号状态"),r.signalStatus,{r=r.copy(signalStatus=it)},L("在线 / 离线 / 漫游 / 无服务"))
                    }
                }
                item{
                    SettingsSection(L("日期与周期")){
                        Text(L("开始日期"),fontSize=12.sp,color=Color(0xFF8A94A6)); DateOnlyEditor(r.startDate){r=r.copy(startDate=it)}
                        IOSDividerLine()
                        Text(L("到期日期"),fontSize=12.sp,color=Color(0xFF8A94A6)); DateOnlyEditor(r.expireDate){r=r.copy(expireDate=it)}
                        IOSDividerLine()
                        Text(L("套餐周期"),fontSize=12.sp,color=Color(0xFF8A94A6))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){ listOf(7,15,30).forEach{ d-> IOSChip(cycleText(LocalAppLanguage.current,d),r.cycleDays==d,Modifier.weight(1f)){ r=r.copy(cycleDays=d,expireDate=runCatching{LocalDate.parse(r.startDate).plusDays(d.toLong()).toString()}.getOrElse{LocalDate.now().plusDays(d.toLong()).toString()}) } } }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){ listOf(90,180,365).forEach{ d-> IOSChip(cycleText(LocalAppLanguage.current,d),r.cycleDays==d,Modifier.weight(1f)){ r=r.copy(cycleDays=d,expireDate=runCatching{LocalDate.parse(r.startDate).plusDays(d.toLong()).toString()}.getOrElse{LocalDate.now().plusDays(d.toLong()).toString()}) } } }
                        IOSDividerLine()
                        IOSSwitchRow(L("长期号码"),r.longTerm){r=r.copy(longTerm=it)}
                    }
                }
                item{
                    SettingsSection(L("eSIM 激活信息")){
                        IOSField(L("编辑 EID"),r.eid,{r=r.copy(eid=it)},L("输入 EID"))
                        IOSDividerLine()
                        IOSField("SM-DP+",r.smdp,{r=r.copy(smdp=it)},L("服务器地址"))
                        IOSDividerLine()
                        IOSField(L("激活码"),r.activationCode,{r=r.copy(activationCode=it)},"Activation Code")
                        IOSDividerLine()
                        Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF4F6FA)).border(.7.dp,Color(0xFFE5E7EB),RoundedCornerShape(16.dp)).padding(12.dp)){
                            Text(qrText.ifBlank { L("未填写激活信息")+"\n"+L("可扫描/粘贴二维码内容，或从相册选择二维码图片") },color=Color(0xFF6B7280),fontSize=13.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            IOSChip(L("扫描二维码"),false,Modifier.weight(1f)){ qrDlg=true }
                            IOSChip(L("相册读取"),false,Modifier.weight(1f)){ albumLauncher.launch("image/*") }
                        }
                        Text(if(qrText.isBlank()) L("未填写激活信息") else "✅ "+L("激活信息已填写"),color=if(qrText.isBlank()) Color(0xFF8A94A6) else Color(0xFF34C759),fontSize=13.sp)
                    }
                }
                item{
                    SettingsSection(L("记录信息")){
                        IOSInfoRow(L("创建时间"),r.createdAt.ifBlank{LocalDate.now().toString()})
                        IOSDividerLine()
                        IOSInfoRow(L("激活时间"),r.activatedAt.ifBlank{L("未记录")})
                    }
                }
                item{ Spacer(Modifier.height(28.dp)) }
                item{
                    var showDel by remember{mutableStateOf(false)}
                    Button(onClick={showDel=true},modifier=Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFF3B30)),contentPadding=PaddingValues(horizontal=16.dp)){
                        Text(L("删除"),fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
                    }
                    if(showDel){
                        Dialog(onDismissRequest={showDel=false}){
                            Surface(shape=RoundedCornerShape(20.dp),color=Color.White){
                                Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                    Text(L("确认删除"),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827))
                                    Text(L("删除后无法恢复，确定要删除这个号码吗？"),fontSize=14.sp,color=Color(0xFF6B7280))
                                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                        Button(onClick={showDel=false},modifier=Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFF2F3F7))){Text(L("取消"),color=Color(0xFF007AFF),fontSize=16.sp)}
                                        Button(onClick={onDelete(r)},modifier=Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFF3B30))){Text(L("删除"),color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold)}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if(qrDlg) IOSQrInputDialog(qrInput,{qrInput=it},{qrDlg=false}){
        qrText=qrInput.ifBlank{tr(editLang,"已手动触发扫码入口")}
        if(qrInput.isNotBlank()){
            val parts=parseLpa(qrInput)
            r=r.copy(smdp=parts.first.ifBlank{r.smdp},activationCode=parts.second.ifBlank{r.activationCode},note=(r.note.ifBlank{tr(editLang,"预付费 / 保号套餐")})+"\n"+tr(editLang,"二维码：")+qrInput)
        }
        qrDlg=false
    }
    if(countryDlg) CountryDialog({countryDlg=false}){c->r=r.copy(countryCode=c.code,countryName=c.name,flag=c.flag);countryDlg=false}
}

@Composable fun IOSDividerLine(){ Box(Modifier.fillMaxWidth().height(.7.dp).background(Color(0xFFE5E7EB))) }

@Composable fun IOSInfoRow(title:String,value:String){
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Text(title,fontSize=15.sp,color=Color(0xFF111827)); Text(value,fontSize=14.sp,color=Color(0xFF8A94A6),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable fun IOSValueRow(title:String,value:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable{onClick()}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
        Text(title,fontSize=15.sp,color=Color(0xFF111827),modifier=Modifier.width(92.dp)); Text(value,fontSize=15.sp,color=Color(0xFF374151),modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis); Text("›",fontSize=24.sp,color=Color(0xFFC7C7CC))
    }
}

@Composable fun IOSField(label:String,value:String,onValue:(String)->Unit,placeholder:String,singleLine:Boolean=true,minLines:Int=1){
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
        Text(label,fontSize=13.sp,color=Color(0xFF8A94A6),modifier=Modifier.padding(start=2.dp))
        TextField(value=value,onValueChange=onValue,modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),singleLine=singleLine,minLines=minLines,placeholder={Text(placeholder,fontSize=13.sp,color=Color(0xFFB0B7C3),maxLines=1,overflow=TextOverflow.Ellipsis)},colors=TextFieldDefaults.colors(focusedContainerColor=Color(0xFFF7F8FA),unfocusedContainerColor=Color(0xFFF7F8FA),focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent),textStyle=androidx.compose.ui.text.TextStyle(fontSize=15.sp,color=Color(0xFF111827)))
    }
}

@Composable fun IOSQrInputDialog(value:String,onValue:(String)->Unit,onDismiss:()->Unit,onSave:()->Unit){
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFFF2F3F7),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text(L("填写二维码内容"),fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827))
                Text(L("可粘贴 LPA、SM-DP+ 或激活码，保存后自动解析。"),fontSize=13.sp,color=Color(0xFF8A94A6),lineHeight=18.sp)
                TextField(value=value,onValueChange=onValue,modifier=Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)),placeholder={Text(L("LPA:1\$SM-DP+\$激活码"))},minLines=5,colors=TextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Button(onClick=onDismiss,modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF374151))){Text(L("取消"))}
                    Button(onClick=onSave,modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF),contentColor=Color.White)){Text(L("保存"))}
                }
            }
        }
    }
}

@Composable fun CompactDateEditor(value:String,onChange:(String)->Unit){
    val parsed=runCatching{LocalDate.parse(value)}.getOrElse{LocalDate.now().plusDays(30)}
    var y by remember(value){ mutableStateOf(parsed.year.toString()) }
    var m by remember(value){ mutableStateOf(parsed.monthValue.toString().padStart(2,'0')) }
    var d by remember(value){ mutableStateOf(parsed.dayOfMonth.toString().padStart(2,'0')) }
    fun commit(){
        if(y.isBlank() || m.isBlank() || d.isBlank()) return
        val yy=(y.toIntOrNull() ?: parsed.year).coerceIn(1970,9999)
        val mm=(m.toIntOrNull() ?: parsed.monthValue).coerceIn(1,12)
        val maxDay=java.time.YearMonth.of(yy,mm).lengthOfMonth()
        val dd=(d.toIntOrNull() ?: parsed.dayOfMonth).coerceIn(1,maxDay)
        onChange(runCatching{LocalDate.of(yy,mm,dd)}.getOrElse{LocalDate.now().plusDays(30)}.toString())
    }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            DateBox(L("年"),y,{ v-> y=v.filter{c->c.isDigit()}.takeLast(4); commit() },Modifier.weight(1.25f))
            DateBox(L("月"),m,{ v-> m=v.filter{c->c.isDigit()}.takeLast(2); commit() },Modifier.weight(.85f))
            DateBox(L("日"),d,{ v-> d=v.filter{c->c.isDigit()}.takeLast(2); commit() },Modifier.weight(.85f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf(0,7,30,90).forEach{ n-> DateQuick(laterText(LocalAppLanguage.current,n),Modifier.weight(1f)){onChange(LocalDate.now().plusDays(n.toLong()).toString())} }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf(7,15,30,90,180,365).forEach{ n-> DateQuick(cycleText(LocalAppLanguage.current,n),Modifier.weight(1f)){onChange(LocalDate.now().plusDays(n.toLong()).toString())} }
        }
    }
}

@Composable fun DateOnlyEditor(value:String,onChange:(String)->Unit){
    val parsed=runCatching{LocalDate.parse(value)}.getOrElse{LocalDate.now()}
    var y by remember{ mutableStateOf(parsed.year.toString()) }
    var m by remember{ mutableStateOf(parsed.monthValue.toString()) }
    var d by remember{ mutableStateOf(parsed.dayOfMonth.toString()) }
    // 仅当外部（如周期按钮）改变日期且与当前输入不一致时才同步，输入过程中不回填
    LaunchedEffect(value){
        val cur=runCatching{ LocalDate.of(y.toIntOrNull()?:0, m.toIntOrNull()?:0, d.toIntOrNull()?:0).toString() }.getOrNull()
        if(cur!=value){
            y=parsed.year.toString(); m=parsed.monthValue.toString(); d=parsed.dayOfMonth.toString()
        }
    }
    fun commit(){
        val yy=y.toIntOrNull() ?: return
        val mm=m.toIntOrNull() ?: return
        val dd=d.toIntOrNull() ?: return
        if(mm !in 1..12) return
        val maxDay=runCatching{ java.time.YearMonth.of(yy,mm).lengthOfMonth() }.getOrElse{31}
        if(dd !in 1..maxDay) return
        onChange(runCatching{LocalDate.of(yy,mm,dd)}.getOrNull()?.toString() ?: return)
    }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            DateBox(L("年"),y,{ v-> y=v.filter{c->c.isDigit()}.take(4); commit() },Modifier.weight(1.25f))
            DateBox(L("月"),m,{ v-> m=v.filter{c->c.isDigit()}.take(2); commit() },Modifier.weight(.85f))
            DateBox(L("日"),d,{ v-> d=v.filter{c->c.isDigit()}.take(2); commit() },Modifier.weight(.85f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf(0,7,30,90).forEach{ n-> DateQuick(laterText(LocalAppLanguage.current,n),Modifier.weight(1f)){ val nd=LocalDate.now().plusDays(n.toLong()); y=nd.year.toString(); m=nd.monthValue.toString(); d=nd.dayOfMonth.toString(); onChange(nd.toString()) } }
        }
    }
}

@Composable fun DateBox(label:String,value:String,onValue:(String)->Unit,m:Modifier){
    Column(m,verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text(label,fontSize=11.sp,color=Color(0xFF8A94A6))
        OutlinedTextField(value=value,onValueChange=onValue,singleLine=true,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(12.dp),textStyle=androidx.compose.ui.text.TextStyle(fontSize=14.sp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Color(0xFFD1D5DB),unfocusedBorderColor=Color(0xFFD1D5DB),focusedContainerColor=Color.White,unfocusedContainerColor=Color.White))
    }
}

@Composable fun DateQuick(text:String,m:Modifier,onClick:()->Unit){
    Box(m.height(34.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF4F5F8)).clickable{onClick()},contentAlignment=Alignment.Center){Text(text,fontSize=12.sp,color=Color(0xFF007AFF),maxLines=1)}
}

@Composable fun FormSection(title:String, content:@Composable ColumnScope.()->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(title,fontSize=13.sp,color=Color(0xFF8A94A6),modifier=Modifier.padding(start=4.dp))
        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(0.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){ content() }
        }
    }
}

@Composable fun 编辑Dialog(init:PhoneNumberRecord,onDismiss:()->Unit,onSave:(PhoneNumberRecord)->Unit){ Full编辑Screen(init,onDismiss,onSave) }
@Composable fun DateFields(value:String,on:(String)->Unit){ var y by remember(value){mutableStateOf(value.split("-").getOrNull(0)?:LocalDate.now().year.toString())}; var m by remember(value){mutableStateOf(value.split("-").getOrNull(1)?:"01")}; var d by remember(value){mutableStateOf(value.split("-").getOrNull(2)?:"01")}; fun emit(){ val mm=m.padStart(2,'0'); val dd=d.padStart(2,'0'); on("$y-$mm-$dd")}; Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){OutlinedTextField(y,{y=it.take(4).filter(Char::isDigit);emit()},Modifier,label={Text("年")});OutlinedTextField(m,{m=it.take(2).filter(Char::isDigit);emit()},Modifier,label={Text("月")});OutlinedTextField(d,{d=it.take(2).filter(Char::isDigit);emit()},Modifier,label={Text("日")})}; Row{ listOf("今天" to 0,"7天后" to 7,"30天后" to 30,"90天后" to 90).forEach{TextButton({on(LocalDate.now().plusDays(it.second.toLong()).toString())}){Text(it.first)}} } }
@Composable fun CountryDialog(onDismiss:()->Unit,onPick:(Country)->Unit){
    var q by remember{mutableStateOf("")}
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFFF2F3F7),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(L("选择国家区号"),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),modifier=Modifier.weight(1f))
                    TextButton(onDismiss){Text(L("取消"),color=Color(0xFF007AFF))}
                }
                TextField(value=q,onValueChange={q=it},modifier=Modifier.fillMaxWidth().heightIn(min=36.dp).clip(RoundedCornerShape(12.dp)),singleLine=true,placeholder={Text(L("搜索国家 / 区号 / ISO"))},leadingIcon={Canvas(Modifier.size(16.dp)){drawCircle(Color(0xFF8E8E93),radius=size.width/2-1.dp.toPx(),style=Stroke(1.5.dp.toPx()));drawLine(Color(0xFF8E8E93),Offset(size.width*.65f,size.height*.65f),Offset(size.width*.85f,size.height*.85f),strokeWidth=1.5.dp.toPx())}},colors=TextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
                LazyColumn(Modifier.heightIn(max=460.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    items(Countries.list.filter{it.name.contains(q,true)||it.code.contains(q)||it.iso.contains(q,true)}){ c->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).clickable{onPick(c)}.padding(horizontal=13.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Text(c.flag,fontSize=24.sp);Spacer(Modifier.width(10.dp));Text(c.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Text("${c.code}  ${c.iso}",fontSize=13.sp,color=Color(0xFF8A94A6));Spacer(Modifier.width(4.dp));Text("›",fontSize=22.sp,color=Color(0xFFC7C7CC))}
                    }
                }
            }
        }
    }
}

@Composable fun CountryPage(){
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF2F3F7)).padding(horizontal=18.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{ Text(L("国家区号库"),fontSize=28.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),modifier=Modifier.padding(horizontal=4.dp,vertical=4.dp)) }
        item{
            IOSSection(L("全部国家 / 地区")){
                Countries.list.forEach{ c->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).padding(horizontal=4.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text(c.flag,fontSize=24.sp);Spacer(Modifier.width(10.dp));Text(c.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Text("${c.code}  ${c.iso}",fontSize=13.sp,color=Color(0xFF8A94A6))}
                }
            }
        }
        item{Spacer(Modifier.height(80.dp))}
    }
}

@OptIn(ExperimentalLayoutApi::class)

fun recordToJson(r:PhoneNumberRecord)=JSONObject()
    .put("id",r.id).put("countryCode",r.countryCode).put("countryName",r.countryName).put("flag",r.flag)
    .put("number",r.number).put("operator",r.operator).put("expireDate",r.expireDate).put("note",r.note)
    .put("balance",r.balance).put("eid",r.eid).put("smdp",r.smdp).put("activationCode",r.activationCode)
    .put("startDate",r.startDate).put("createdAt",r.createdAt).put("activatedAt",r.activatedAt)
    .put("longTerm",r.longTerm).put("cycleDays",r.cycleDays).put("signalStatus",r.signalStatus)

fun cleanCloudApiKey(raw:String):String {
    val t=raw.trim()
    if(t.isBlank()) return ""
    val exact=Regex("[A-Za-z0-9_-]{24,80}").findAll(t).map{it.value}.filterNot{ it.equals("API",true) || it.equals("Key",true) }.toList()
    return (exact.lastOrNull() ?: t.lineSequence().map{it.trim()}.firstOrNull{it.isNotBlank()} ?: "").replace(Regex("[\r\n\t ]+"),"").trim()
}
fun cleanCloudUrl(raw:String):String = raw.trim().trimEnd('/')

fun cloudPayload(records:List<PhoneNumberRecord>,s:App设置):String{
    val settings=JSONObject()
        .put("remind天",s.remind天).put("remindDays",s.remind天)
        .put("tgEnabled",s.tgEnabled).put("botToken",s.botToken).put("chatId",s.chatId)
        .put("smtpEnabled",s.smtpEnabled).put("smtpHost",s.smtpHost).put("smtpPort",s.smtpPort)
        .put("smtpUser",s.smtpUser).put("smtpPass",s.smtpPass).put("smtpFrom",s.smtpFrom).put("smtpTo",s.smtpTo)
        .put("cloudTelegramEnabled",s.cloudTelegramEnabled).put("cloudEmailEnabled",s.cloudEmailEnabled)
    val arr=JSONArray(); records.forEach{arr.put(recordToJson(it))}
    return JSONObject().put("settings",settings).put("records",arr).toString()
}
fun cloudPost(s:App设置,path:String,body:String,lang:String="简体中文",onResult:(Boolean,String)->Unit){
    val apiKey=cleanCloudApiKey(s.cloudApiKey)
    val cloudUrl=cleanCloudUrl(if(s.cloudUrl.isBlank()) "https://ccs.ziranaa.top:16670" else s.cloudUrl)
    if(cloudUrl.isBlank()){onResult(false,tr(lang,"云端地址未填写"));return}
    val needsAuth=!(path.startsWith("/api/register")||path.startsWith("/api/status"))
    if(needsAuth&&apiKey.isBlank()){onResult(false,tr(lang,"API Key 未填写"));return}
    thread{
        val res=runCatching{
            val base=cloudUrl
            val c=(URL(base+path).openConnection() as HttpURLConnection)
            if(path.startsWith("/api/status")){
                c.requestMethod="GET"; c.connectTimeout=12000; c.readTimeout=10000
            }else{
                c.requestMethod="POST"; c.connectTimeout=12000; c.readTimeout=20000; c.doOutput=true
                c.setRequestProperty("Content-Type","application/json; charset=utf-8")
                if(needsAuth) c.setRequestProperty("X-API-Key",apiKey)
                c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
            }
            c.inputStream.bufferedReader(Charsets.UTF_8).readText()
        }.fold({it},{tr(lang,"失败")+": ${it.javaClass.simpleName}: ${it.message}"})
        Handler(Looper.getMainLooper()).post{onResult(!res.startsWith(tr(lang,"失败")),res)}
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun 设置Page(ctx:Context,s:App设置,records:List<PhoneNumberRecord>,on:(App设置)->Unit){
    var st by remember{s.mutableState()}
    var cloudMsg by remember{ mutableStateOf("") }
    val pageLang = LocalAppLanguage.current
    fun S(key:String)=tr(pageLang,key)
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> if(uri!=null){ st=st.copyMut{backgroundUri=uri.toString()}; on(st) } }
    Column(Modifier.fillMaxSize().background(if(st.dark) Color(0xFF0B0F17) else Color(0xFFF2F3F7)).verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        SettingsSection(L("外观")){
            IOSSwitchRow(L("深色模式"),st.dark){ st=st.copyMut{dark=it}; on(st) }
            IOSSwitchRow(L("显示首页卡片国旗"),st.showFlag){ st=st.copyMut{showFlag=it}; on(st) }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({bgPicker.launch("image/*")},shape=RoundedCornerShape(14.dp)){Text(L("更改背景图片"))}
                TextButton({st=st.copyMut{backgroundUri=""};on(st)}){Text(L("清除"))}
            }
            if(st.backgroundUri.isNotBlank()){
                Text(L("已设置自定义背景"),fontSize=11.sp,color=Color(0xFF007AFF))
                Text(L("背景遮罩透明度")+"：${(st.backgroundAlpha*100).roundToInt()}%",fontSize=12.sp,color=Color(0xFF8A94A6))
                Slider(value=st.backgroundAlpha,onValueChange={v->st=st.copyMut{backgroundAlpha=v};on(st)},valueRange=0f..1f)
            }
        }
        SettingsSection(L("提醒设置")){
            IOSSwitchRow(L("开启到期提醒"),st.reminderEnabled){ st=st.copyMut{reminderEnabled=it}; on(st) }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){ listOf(1,3,7).forEach{ d-> IOSChip(L("提前")+cycleText(LocalAppLanguage.current,d),st.remind天==d,Modifier.weight(1f)){ st=st.copyMut{remind天=d}; on(st) } } }
            OutlinedTextField(st.remind天.toString(),{st=st.copyMut{remind天=it.toIntOrNull()?:7};on(st)},modifier=Modifier.fillMaxWidth(),label={Text(L("自定义提前天数"))},singleLine=true)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(st.remindHour.toString(),{st=st.copyMut{remindHour=(it.toIntOrNull()?:9).coerceIn(0,23)};on(st)},modifier=Modifier.weight(1f),label={Text(L("小时"))},singleLine=true)
                OutlinedTextField(st.remindMinute.toString(),{st=st.copyMut{remindMinute=(it.toIntOrNull()?:0).coerceIn(0,59)};on(st)},modifier=Modifier.weight(1f),label={Text(L("分钟"))},singleLine=true)
            }
        }
        TrafficInterfaceSettings(st,{ ns-> st=ns; on(st) })

        SettingsSection(S("云端提醒")){
            IOSSwitchRow(S("启用云端提醒"),st.cloudEnabled){ st=st.copyMut{cloudEnabled=it}; on(st) }
            IOSSwitchRow(S("自动同步"),st.cloudAutoSync){ st=st.copyMut{cloudAutoSync=it}; on(st) }
            Text(S("自动同步说明"),fontSize=11.sp,color=Color(0xFF8A94A6),lineHeight=16.sp)
            PlainInput("API Key",st.cloudApiKey){ st=st.copyMut{cloudApiKey=cleanCloudApiKey(it)}; on(st) }
            Text(S("API Key说明"),fontSize=11.sp,color=Color(0xFF8A94A6),lineHeight=16.sp)
            Text(S("当前 API Key：")+if(st.cloudApiKey.isNotBlank()) cleanCloudApiKey(st.cloudApiKey) else S("未设置"),fontSize=12.sp,color=Color(0xFF8A94A6),lineHeight=17.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({ cloudPost(st.copyMut{cloudUrl="https://ccs.ziranaa.top:16670"},"/api/status",cloudPayload(records,st)){ok,msg-> cloudMsg=if(ok) S("连接成功") else msg } },shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Text("测试连接")}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({
                    if(st.cloudApiKey.isNotBlank()) {
                        val clipboard=ctx.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("api key",cleanCloudApiKey(st.cloudApiKey)))
                        cloudMsg=S("已复制 API Key")
                    }else cloudMsg=S("请先生成或填写 API Key")
                },shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Text(S("复制 Key"))}
                Button({
                    val existing=cleanCloudApiKey(st.cloudApiKey)
                    if(existing.isNotBlank()){
                        cloudMsg=S("已有固定Key说明")
                    }else{
                        cloudPost(st,"/api/register","{}"){ok,msg-> if(ok) { try{ val r=JSONObject(msg); val k=r.optString("apiKey",""); if(k.isNotBlank()){ st=st.copyMut{cloudApiKey=k}; on(st); cloudMsg=S("已生成本机固定 Key，已保存") }else cloudMsg=msg } catch(_:Exception){ cloudMsg=msg } }else cloudMsg=msg }
                    }
                },shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Text(S("生成我的 Key"))}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({ cloudPost(st,"/api/sync",cloudPayload(records,st)){ok,msg-> cloudMsg=if(ok) S("同步成功") else msg } },shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Text(S("同步到云端"))}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                IOSSwitchRow(S("云端 Telegram"),st.cloudTelegramEnabled){ st=st.copyMut{cloudTelegramEnabled=it}; on(st) }
            }
            if(st.cloudTelegramEnabled){
                IOSSwitchRow(S("启用 TG 配置"),st.tgEnabled){ st=st.copyMut{tgEnabled=it}; on(st) }
                PlainInput("Bot Token",st.botToken){ st=st.copyMut{botToken=it}; on(st) }
                PlainInput("Chat ID",st.chatId){ st=st.copyMut{chatId=it}; on(st) }
                Text(S("TG配置说明"),fontSize=11.sp,color=Color(0xFF8A94A6),lineHeight=16.sp)
            }
            IOSDividerLine()
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                IOSSwitchRow(S("云端邮件"),st.cloudEmailEnabled){ st=st.copyMut{cloudEmailEnabled=it}; on(st) }
            }
            if(st.cloudEmailEnabled){
                IOSSwitchRow(S("SMTP 自动发邮件"),st.smtpEnabled){ st=st.copyMut{smtpEnabled=it}; on(st) }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Box(Modifier.weight(1f)){ PlainInput(S("SMTP 服务器"),st.smtpHost){ st=st.copyMut{smtpHost=it}; on(st) } }
                    Box(Modifier.weight(.45f)){ PlainInput(S("端口"),st.smtpPort.toString()){ st=st.copyMut{smtpPort=it.toIntOrNull()?:465}; on(st) } }
                }
                PlainInput(S("邮箱账号"),st.smtpUser){ st=st.copyMut{smtpUser=it}; on(st) }
                PlainInput(S("授权码"),st.smtpPass){ st=st.copyMut{smtpPass=it}; on(st) }
                PlainInput(S("发件邮箱"),st.smtpFrom){ st=st.copyMut{smtpFrom=it}; on(st) }
                PlainInput(S("收件邮箱"),st.smtpTo){ st=st.copyMut{smtpTo=it}; on(st) }
                Text(S("SMTP授权码说明"),fontSize=11.sp,color=Color(0xFF8A94A6),lineHeight=16.sp)
            }
            IOSDividerLine()
            IOSSwitchRow(S("本地通知提醒"),st.notificationEnabled){ st=st.copyMut{notificationEnabled=it}; on(st) }
            IOSSwitchRow(S("通知一键发邮件"),st.emailQuickEnabled){ st=st.copyMut{emailQuickEnabled=it}; on(st) }
            Text(S("本地通知说明"),fontSize=11.sp,color=Color(0xFF8A94A6),lineHeight=16.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({ cloudPost(st,"/api/test-telegram",cloudPayload(records,st)){ok,msg-> cloudMsg=if(ok) S("TG 测试已发送") else msg } },shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Text(S("测试TG"))}
                Button({ cloudPost(st,"/api/test-email",cloudPayload(records,st)){ok,msg-> cloudMsg=if(ok) S("邮件测试已发送") else msg } },shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)){Text(S("测试邮件"))}
            }
            Button({ cloudPost(st,"/api/check-now",cloudPayload(records,st)){ok,msg-> cloudMsg=if(ok) S("已触发云端检查") else msg } },shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Text(S("立即检查到期"))}
            Text(S("云端服务说明"),fontSize=12.sp,color=Color(0xFF8A94A6),lineHeight=17.sp)
            if(cloudMsg.isNotBlank()) Text(cloudMsg,fontSize=12.sp,color=Color(0xFF007AFF),lineHeight=17.sp)
        }


        SettingsSection(L("语言 / Language")){
            Text(L("当前语言：")+st.language,fontSize=13.sp,color=Color(0xFF8A94A6))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                listOf("简体中文","繁体中文","English","日本語","阿拉伯语").forEach{ lang -> IOSChip(lang,st.language==lang){ st=st.copyMut{language=lang}; on(st) } }
            }
            Text(if(st.language=="阿拉伯语") L("已启用 RTL 右到左布局") else L("支持实时切换，主要页面会立即刷新。"),fontSize=12.sp,color=Color(0xFF8A94A6))
        }
        SettingsSection(L("关于")){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFF007AFF)),contentAlignment=Alignment.Center){Text("i",color=Color.White,fontWeight=FontWeight.Bold)}
                Spacer(Modifier.width(10.dp))
                Text("Sim Jiang v2.8.60\n"+L("开发者")+"：伍六柒\n"+L("本地数据存储"),fontSize=13.sp,color=Color(0xFF4B5563),lineHeight=20.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable fun IOSSection(title:String,content:@Composable ColumnScope.()->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(title,fontSize=13.sp,color=Color(0xFF8A94A6),modifier=Modifier.padding(start=4.dp))
        Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=.82f)),elevation=CardDefaults.cardElevation(0.dp),modifier=Modifier.fillMaxWidth().border(.7.dp,Color.White.copy(alpha=.9f),RoundedCornerShape(20.dp))){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){content()}
        }
    }
}

@Composable fun SettingsSection(title:String,content:@Composable ColumnScope.()->Unit){
    var expanded by remember(title){ mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(0.dp)){
        Surface(shape=RoundedCornerShape(if(expanded) 20.dp else 18.dp),color=Color.White.copy(alpha=.88f),tonalElevation=0.dp,modifier=Modifier.fillMaxWidth().border(.7.dp,Color.White.copy(alpha=.95f),RoundedCornerShape(if(expanded) 20.dp else 18.dp))){
            Column{
                Row(Modifier.fillMaxWidth().height(52.dp).clickable{expanded=!expanded}.padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(title,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF111827),modifier=Modifier.weight(1f))
                    Text(if(expanded) "⌃" else "›",fontSize=22.sp,color=Color(0xFF8A94A6),fontWeight=FontWeight.SemiBold)
                }
                if(expanded){
                    IOSDividerLine()
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){content()}
                }
            }
        }
    }
}
@Composable fun IOSSwitchRow(title:String,checked:Boolean,onChecked:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
        Text(title,fontSize=16.sp,color=Color(0xFF111827)); Switch(checked,onChecked)
    }
}

fun App设置.mutableState()= mutableStateOf(this)
@Composable fun TrafficInterfaceSettings(st:App设置,onChange:(App设置)->Unit){
    SettingsSection(L("流量接口")){
        PlainInput(label=L("流量接口 URL"),value=st.trafficUrl,onValue={ onChange(st.copyMut{trafficUrl=it}) })
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
            listOf(
                "Cloudflare" to "https://speed.cloudflare.com/__down?bytes=10485760",
                "Hetzner" to "https://speed.hetzner.de/10MB.bin",
                "ThinkBroadband" to "https://ipv4.download.thinkbroadband.com/10MB.zip"
            ).forEach{ item-> Text(item.first,fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF00A7D9),modifier=Modifier.clip(RoundedCornerShape(8.dp)).clickable{onChange(st.copyMut{trafficUrl=item.second})}.padding(horizontal=6.dp,vertical=4.dp)) }
        }
        PlainInput(label=L("默认流量 KB"),value=st.trafficKb.toString(),onValue={ onChange(st.copyMut{trafficKb=it.toDoubleOrNull()?:st.trafficKb}) })
    }
}

@Composable fun PlainInput(label:String,value:String,onValue:(String)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text(label,fontSize=13.sp,color=Color(0xFF374151))
        OutlinedTextField(value=value,onValueChange=onValue,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),singleLine=true,shape=RoundedCornerShape(13.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Color(0xFFD1D5DB),unfocusedBorderColor=Color(0xFFD1D5DB),focusedContainerColor=Color.White,unfocusedContainerColor=Color.White))
    }
}

fun App设置.copyMut(block:App设置.()->Unit):App设置{ val n=this.copy(); n.block(); return n }
fun App设置.copy()=App设置(dark,remind天,trafficUrl,trafficKb,tgEnabled,botToken,chatId,keepCycle,backgroundUri,backgroundAlpha,reminderEnabled,notificationEnabled,remindHour,remindMinute,language,emailQuickEnabled,smtpEnabled,smtpHost,smtpPort,smtpUser,smtpPass,smtpFrom,smtpTo,cloudEnabled,cloudUrl,cloudApiKey,cloudTelegramEnabled,cloudEmailEnabled,cloudAutoSync)
@Composable fun Presets(on:(String)->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
        mapOf(
            "Cloudflare" to "https://speed.cloudflare.com/__down?bytes=10485760",
            "Hetzner" to "https://speed.hetzner.de/10MB.bin",
            "ThinkBroadband" to "https://ipv4.download.thinkbroadband.com/10MB.zip",
            "Google204" to "https://www.google.com/generate_204"
        ).forEach{TextButton({on(it.value)}){Text(it.key)}}
    }
}


@Composable fun IOSConfirmDialog(title:String,message:String,danger:Boolean=false,onCancel:()->Unit,onConfirm:()->Unit){
    Dialog(onDismissRequest=onCancel){
        Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFFF2F3F7),tonalElevation=0.dp,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                Text(message,fontSize=14.sp,color=Color(0xFF6B7280),lineHeight=20.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable{onCancel()},contentAlignment=Alignment.Center){Text(L("取消"),fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF007AFF))}
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(if(danger) Color(0xFFFF3B30) else Color(0xFF007AFF)).clickable{onConfirm()},contentAlignment=Alignment.Center){Text(L("确认"),fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Color.White)}
                }
            }
        }
    }
}

@Composable fun TrafficDialog(ctx:Context,r:PhoneNumberRecord,s:App设置,onDismiss:()->Unit){
    var url by remember{mutableStateOf(if(s.trafficUrl.contains("generate_204")) "https://speed.cloudflare.com/__down?bytes=10485760" else s.trafficUrl)}
    var amount by remember{mutableStateOf(if(s.trafficKb>1.0) "${s.trafficKb.roundToInt()}KB" else "1MB")}
    var confirm by remember{mutableStateOf(false)}
    var result by remember{mutableStateOf<String?>(null)}
    val lang = LocalAppLanguage.current
    Dialog(onDismissRequest=onDismiss){
        Surface(shape=RoundedCornerShape(28.dp),color=Color(0xFFF2F3F7),tonalElevation=0.dp,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF007AFF)),contentAlignment=Alignment.Center){Text("▥",fontSize=22.sp,color=Color.White,fontWeight=FontWeight.Bold)}
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){Text(L("刷流量"),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color(0xFF111827));Text(L("真实下载数据测试"),fontSize=12.sp,color=Color(0xFF8A94A6))}
                    TextButton(onDismiss){Text(L("关闭"),color=Color(0xFF007AFF))}
                }
                IOSSection(L("号码")){
                    Row(verticalAlignment=Alignment.CenterVertically){Text(r.flag,fontSize=24.sp);Spacer(Modifier.width(8.dp));Column{Text(r.operator.ifBlank{r.countryName},fontWeight=FontWeight.SemiBold);Text("${r.countryCode} ${formatNumber(r.number)}",fontSize=13.sp,color=Color(0xFF6B7280))}}
                }
                IOSSection(L("下载测试接口")){
                    PlainInput(label="URL",value=url,onValue={url=it})
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        listOf(
                            "Cloudflare" to "https://speed.cloudflare.com/__down?bytes=10485760",
                            "Hetzner" to "https://speed.hetzner.de/10MB.bin",
                            "Think" to "https://ipv4.download.thinkbroadband.com/10MB.zip"
                        ).forEach{ item-> IOSChip(item.first, url==item.second, Modifier.weight(1f)){url=item.second} }
                    }
                }
                IOSSection(L("目标流量")){
                    PlainInput(label=L("例：100KB / 1MB / 50MB"),value=amount,onValue={amount=it})
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        listOf("100KB","1MB","5MB","10MB").forEach{ IOSChip(it, amount==it, Modifier.weight(1f)){amount=it} }
                    }
                    Text(L("204 / 空响应接口不能真正消耗流量，建议使用 Cloudflare 或 Hetzner。"),fontSize=12.sp,color=Color(0xFF8A94A6),lineHeight=17.sp)
                }
                result?.let{
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp)){Text(it,fontSize=13.sp,color=Color(0xFF374151))}
                }
                Button(onClick={confirm=true},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF007AFF))){Text(L("开始刷流量"),fontSize=16.sp,fontWeight=FontWeight.SemiBold)}
            }
        }
    }
    if(confirm) IOSConfirmDialog(L("确认刷流量？"),L("将实际下载约")+" ${amount} "+L("目标流量")+"。\n"+L("确认后会真实消耗当前网络流量。"),false,{confirm=false},{
        confirm=false
        val targetKb=parseTrafficKb(amount).coerceIn(1.0,1024.0*500.0)
        result=tr(lang,"请求中…")
        consumeTraffic(url,targetKb,lang){msg->result=msg; if(s.tgEnabled) sendTelegram(s.botToken,s.chatId,"📶 Sim Jiang ${tr(lang,"刷流量")}\n${r.countryCode} ${formatNumber(r.number)}\n$msg")}
    })
}

@Composable fun IOSChip(text:String,selected:Boolean,m:Modifier=Modifier,onClick:()->Unit){
    Box(m.height(34.dp).clip(RoundedCornerShape(12.dp)).background(if(selected) Color(0xFF007AFF) else Color(0xFFF4F5F8)).border(.7.dp,if(selected) Color(0xFF007AFF) else Color(0xFFE5E7EB),RoundedCornerShape(12.dp)).clickable{onClick()},contentAlignment=Alignment.Center){Text(text,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(selected) Color.White else Color(0xFF007AFF),maxLines=1,overflow=TextOverflow.Ellipsis)}
}



fun csvEscape(v:String)= if(v.any{ it==',' || it=='"' || it=='\n' || it=='\r' }) "\""+v.replace("\"","\"\"")+"\"" else v
fun csvLine(values:List<String>)=values.joinToString(","){csvEscape(it)}
fun recordFields(r:PhoneNumberRecord)=listOf(r.id,r.countryCode,r.countryName,r.flag,r.number,r.operator,r.expireDate,r.note,r.balance,r.eid,r.smdp,r.activationCode,r.startDate,r.createdAt,r.activatedAt,r.longTerm.toString(),r.cycleDays.toString(),r.signalStatus)
val recordHeader=listOf("id","countryCode","countryName","flag","number","operator","expireDate","note","balance","eid","smdp","activationCode","startDate","createdAt","activatedAt","longTerm","cycleDays","signalStatus")

fun exportRecordsJson(records:List<PhoneNumberRecord>,settings:App设置):String{
    val root=JSONObject()
    val arr=JSONArray()
    records.forEach{ r-> arr.put(DataStore.recordJson(r)) }
    root.put("type","san-sim-export").put("version",2).put("count",records.size).put("records",arr)
    return root.toString(2)
}

fun exportRecordsCsv(records:List<PhoneNumberRecord>):String{
    return buildString{
        appendLine(csvLine(recordHeader))
        records.forEach{ appendLine(csvLine(recordFields(it))) }
    }
}

fun splitCsvLine(line:String):List<String>{
    val out=mutableListOf<String>(); val sb=StringBuilder(); var q=false; var i=0
    while(i<line.length){ val ch=line[i]; when{
        q && ch=='"' && i+1<line.length && line[i+1]=='"' -> { sb.append('"'); i++ }
        ch=='"' -> q=!q
        ch==',' && !q -> { out.add(sb.toString()); sb.clear() }
        else -> sb.append(ch)
    }; i++ }
    out.add(sb.toString()); return out
}

fun parseRecordObject(o:JSONObject)=PhoneNumberRecord(
    id=o.optString("id",UUID.randomUUID().toString()), countryCode=o.optString("countryCode","+86"), countryName=o.optString("countryName","中国"), flag=o.optString("flag","🇨🇳"), number=o.optString("number"), operator=o.optString("operator"), expireDate=o.optString("expireDate",LocalDate.now().plusDays(30).toString()), note=o.optString("note"),
    balance=o.optString("balance"), eid=o.optString("eid"), smdp=o.optString("smdp"), activationCode=o.optString("activationCode"), startDate=o.optString("startDate",LocalDate.now().toString()), createdAt=o.optString("createdAt",LocalDate.now().toString()), activatedAt=o.optString("activatedAt"), longTerm=o.optBoolean("longTerm",false), cycleDays=o.optInt("cycleDays",30), signalStatus=o.optString("signalStatus","在线")
)

fun parseRecordsJson(text:String):List<PhoneNumberRecord>{
    return runCatching{
        val trimmed=text.trim()
        val arr=if(trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).getJSONArray("records")
        (0 until arr.length()).map{ parseRecordObject(arr.getJSONObject(it)) }.filter{it.number.isNotBlank()}
    }.getOrElse{ emptyList() }
}

fun parseRecordsCsv(text:String):List<PhoneNumberRecord>{
    return runCatching{
        val lines=text.lines().filter{it.isNotBlank()}
        if(lines.size<2) return@runCatching emptyList<PhoneNumberRecord>()
        val header=splitCsvLine(lines.first()).map{it.trim()}
        lines.drop(1).mapNotNull{ line->
            val vals=splitCsvLine(line); val map=header.mapIndexedNotNull{ i,k-> vals.getOrNull(i)?.let{k to it} }.toMap()
            val o=JSONObject(); map.forEach{(k,v)-> when(k){"longTerm"->o.put(k,v.toBoolean());"cycleDays"->o.put(k,v.toIntOrNull()?:30);else->o.put(k,v)} }
            parseRecordObject(o).takeIf{it.number.isNotBlank()}
        }
    }.getOrElse{ emptyList() }
}

fun parseRecordsAny(text:String):List<PhoneNumberRecord> = parseRecordsJson(text).ifEmpty{ parseRecordsCsv(text) }

fun parseTrafficKb(text:String):Double{
    val t=text.trim().uppercase().replace(" ","")
    val num=Regex("""[0-9]+(\.[0-9]+)?""").find(t)?.value?.toDoubleOrNull() ?: 1.0
    return when{
        t.contains("GB") || t.endsWith("G") -> num*1024.0*1024.0
        t.contains("MB") || t.endsWith("M") -> num*1024.0
        else -> num
    }
}

fun consumeTraffic(url:String,kb:Double,lang:String,cb:(String)->Unit){
    thread{
        val want=(kb*1024).roundToInt().coerceAtLeast(1)
        val res=runCatching{
            var total=0
            val started=System.currentTimeMillis()
            var round=0
            while(total<want && round<30){
                round++
                val sep=if(url.contains("?")) "&" else "?"
                val u=if(url.contains("speed.cloudflare.com/__down")) url.replace(Regex("""bytes=\d+"""),"bytes=${want-total}") else url+sep+"_san="+System.nanoTime()
                val c=(URL(u).openConnection() as HttpURLConnection)
                c.connectTimeout=15000
                c.readTimeout=30000
                c.instanceFollowRedirects=true
                c.useCaches=false
                c.setRequestProperty("Cache-Control","no-cache")
                c.setRequestProperty("Pragma","no-cache")
                c.setRequestProperty("User-Agent","SanSIM/1.5.6")
                val buf=ByteArray(8192)
                c.inputStream.use{ input->
                    while(total<want){
                        val n=input.read(buf,0,minOf(buf.size,want-total))
                        if(n<=0) break
                        total+=n
                    }
                }
                c.disconnect()
            }
            val sec=maxOf(0.001,(System.currentTimeMillis()-started)/1000.0)
            val speed=total/1024.0/sec
            tr(lang,"成功")+"："+tr(lang,"实际读取")+" ${"%.2f".format(total/1024.0)}KB / "+tr(lang,"目标")+" ${"%.2f".format(want/1024.0)}KB，"+tr(lang,"耗时")+" ${"%.1f".format(sec)} "+tr(lang,"秒")+"，"+tr(lang,"约")+" ${"%.1f".format(speed)}KB/s"
        }.getOrElse{tr(lang,"失败")+"：${it.javaClass.simpleName}: ${it.message}"}
        cb(res)
    }
}
fun dial(ctx:Context,r:PhoneNumberRecord){ ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${r.countryCode}${r.number}"))) }
fun formatNumber(n:String)=n.chunked(4).joinToString(" ")
fun guessOperator(n:String,iso:String):String{
    val x=n.filter{it.isDigit()}
    val p3=x.take(3); val p4=x.take(4)
    return when{
        iso=="CN" && (p4 in listOf("1340","1341","1342","1343","1344","1345","1346","1347","1348") || p3 in listOf("135","136","137","138","139","147","148","150","151","152","157","158","159","172","178","182","183","184","187","188","195","197","198"))->"中国移动"
        iso=="CN" && (p3 in listOf("130","131","132","145","146","155","156","166","175","176","185","186","196"))->"中国联通"
        iso=="CN" && (p3 in listOf("133","149","153","173","174","177","180","181","189","190","191","193","199"))->"中国电信"
        iso=="CN" && p3=="192"->"中国广电"
        iso=="CN" && (p3 in listOf("162","165","167","170","171"))->"虚拟运营商"
        iso=="HK"->"3HK"
        iso=="US"||iso=="CA"->OperatorDatabase.firstNameFor(iso)
        iso=="TH"->"AIS"
        iso=="JP"->"NTT Docomo"
        else->OperatorDatabase.firstNameFor(iso)
    }
}

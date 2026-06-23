package com.sansim.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sansim.app.LocalIsDark

@Composable
private fun dk(dark: Color, light: Color): Color =
    if (LocalIsDark.current) dark else light

/**
 * Tag selector with preset chips + custom tag input.
 * Tags are stored as comma-separated string.
 */
@Composable
fun TagSelector(
    selectedTags: String,
    onTagsChanged: (String) -> Unit
) {
    val presetTags = listOf("保号卡", "旅行备用", "测试卡", "中国卡")
    val currentList = selectedTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    var customTagInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Preset tag chips - row 1
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            presetTags.take(2).forEach { tag ->
                val isSelected = tag in currentList
                TagChip(tag, isSelected, Modifier.weight(1f)) {
                    onTagsChanged(toggleTag(selectedTags, tag))
                }
            }
        }
        // Preset tag chips - row 2
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            presetTags.drop(2).forEach { tag ->
                val isSelected = tag in currentList
                TagChip(tag, isSelected, Modifier.weight(1f)) {
                    onTagsChanged(toggleTag(selectedTags, tag))
                }
            }
            repeat(2 - presetTags.drop(2).size) {
                Spacer(Modifier.weight(1f))
            }
        }

        // Custom tags displayed as chips
        val customTags = currentList.filter { it !in presetTags }
        if (customTags.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                customTags.forEach { tag ->
                    TagChip(tag, true) {
                        onTagsChanged(toggleTag(selectedTags, tag))
                    }
                }
            }
        }

        // Add custom tag input
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = customTagInput,
                onValueChange = { customTagInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(dk(Color(0xFF1C1C1E), Color(0xFFF4F5F8)))
                    .border(0.7.dp, dk(Color(0xFF38383A), Color(0xFFE5E7EB)), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = dk(Color(0xFFE5E5E7), Color(0xFF111827))
                ),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                decorationBox = { inner ->
                    Box {
                        if (customTagInput.isEmpty()) {
                            Text(
                                "添加自定义标签",
                                fontSize = 13.sp,
                                color = Color(0xFF8A94A6)
                            )
                        }
                        inner()
                    }
                }
            )
            Box(
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF007AFF))
                    .clickable {
                        val trimmed = customTagInput.trim()
                        if (trimmed.isNotBlank() && trimmed !in currentList) {
                            val newTags = if (selectedTags.isBlank()) trimmed
                            else "$selectedTags,$trimmed"
                            onTagsChanged(newTags)
                            customTagInput = ""
                        }
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Show current tags summary
        if (currentList.isNotEmpty()) {
            Text(
                "当前标签：${currentList.joinToString(", ")}",
                fontSize = 11.sp,
                color = Color(0xFF8A94A6)
            )
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    selected: Boolean,
    m: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        m
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF007AFF) else Color(0xFFF4F5F8))
            .border(
                0.7.dp,
                if (selected) Color(0xFF007AFF) else Color(0xFFE5E7EB),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF007AFF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private fun toggleTag(current: String, tag: String): String {
    val list = current.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
    if (tag in list) list.remove(tag) else list.add(tag)
    return list.joinToString(",")
}

/**
 * Card background picker - shows preview + country grid from assets/flag_backgrounds/
 */
@Composable
fun CardBackgroundPicker(
    currentAssetName: String,
    onSelect: (String) -> Unit
) {
    val countryNames = mapOf(
        "cn" to "中国", "hk" to "香港", "tw" to "台湾", "mo" to "澳门",
        "us" to "美国", "gb" to "英国", "jp" to "日本", "kr" to "韩国",
        "de" to "德国", "fr" to "法国", "it" to "意大利", "es" to "西班牙",
        "pt" to "葡萄牙", "nl" to "荷兰", "be" to "比利时", "ch" to "瑞士",
        "at" to "奥地利", "se" to "瑞典", "no" to "挪威", "dk" to "丹麦",
        "fi" to "芬兰", "pl" to "波兰", "cz" to "捷克", "hu" to "匈牙利",
        "ro" to "罗马尼亚", "gr" to "希腊", "tr" to "土耳其",
        "ru" to "俄罗斯", "ua" to "乌克兰", "in" to "印度", "th" to "泰国",
        "vn" to "越南", "ph" to "菲律宾", "my" to "马来西亚", "sg" to "新加坡",
        "id" to "印尼", "au" to "澳大利亚", "nz" to "新西兰", "ca" to "加拿大",
        "mx" to "墨西哥", "br" to "巴西", "ar" to "阿根廷", "cl" to "智利",
        "co" to "哥伦比亚", "za" to "南非", "eg" to "埃及", "ng" to "尼日利亚",
        "ke" to "肯尼亚", "ae" to "阿联酋", "sa" to "沙特", "il" to "以色列",
        "pk" to "巴基斯坦", "bd" to "孟加拉", "il" to "以色列",
        "ad" to "安道尔", "al" to "阿尔巴尼亚", "am" to "亚美尼亚",
        "ao" to "安哥拉", "az" to "阿塞拜疆", "ba" to "波黑",
        "bb" to "巴巴多斯", "bf" to "布基纳法索", "bg" to "保加利亚",
        "bi" to "布隆迪", "bj" to "贝宁", "bn" to "文莱", "bo" to "玻利维亚",
        "bs" to "巴哈马", "bt" to "不丹", "bw" to "博茨瓦纳", "by" to "白俄罗斯",
        "bz" to "伯利兹", "cm" to "喀麦隆", "cy" to "塞浦路斯",
        "dj" to "吉布提", "dm" to "多米尼克", "do" to "多米尼加",
        "dz" to "阿尔及利亚", "ec" to "厄瓜多尔", "ee" to "爱沙尼亚",
        "er" to "厄立特里亚", "et" to "埃塞俄比亚", "fj" to "斐济",
        "ga" to "加蓬", "gd" to "格林纳达", "ge" to "格鲁吉亚",
        "gh" to "加纳", "gm" to "冈比亚", "gn" to "几内亚",
        "gt" to "危地马拉", "gy" to "圭亚那", "hn" to "洪都拉斯",
        "hr" to "克罗地亚", "ht" to "海地", "ie" to "爱尔兰", "is" to "冰岛",
        "jm" to "牙买加", "jo" to "约旦", "kg" to "吉尔吉斯",
        "km" to "科摩罗", "kw" to "科威特", "kz" to "哈萨克斯坦",
        "la" to "老挝", "lb" to "黎巴嫩", "lk" to "斯里兰卡",
        "lr" to "利比里亚", "lt" to "立陶宛", "lu" to "卢森堡",
        "lv" to "拉脱维亚", "ly" to "利比亚", "ma" to "摩洛哥",
        "mc" to "摩纳哥", "md" to "摩尔多瓦", "me" to "黑山",
        "mg" to "马达加斯加", "ml" to "马里", "mm" to "缅甸",
        "mn" to "蒙古", "mr" to "毛里塔尼亚", "mt" to "马耳他",
        "mu" to "毛里求斯", "mv" to "马尔代夫", "mw" to "马拉维",
        "mz" to "莫桑比克", "na" to "纳米比亚", "ne" to "尼日尔",
        "ni" to "尼加拉瓜", "np" to "尼泊尔", "pa" to "巴拿马",
        "pe" to "秘鲁", "pg" to "巴布亚新几内亚", "pr" to "波多黎各",
        "py" to "巴拉圭", "qa" to "卡塔尔", "rs" to "塞尔维亚",
        "rw" to "卢旺达", "sd" to "苏丹", "si" to "斯洛文尼亚",
        "sk" to "斯洛伐克", "sl" to "塞拉利昂", "sn" to "塞内加尔",
        "so" to "索马里", "ss" to "南苏丹", "sv" to "萨尔瓦多",
        "sy" to "叙利亚", "td" to "乍得", "tg" to "多哥",
        "tj" to "塔吉克斯坦", "tm" to "土库曼斯坦", "tn" to "突尼斯",
        "tz" to "坦桑尼亚", "ug" to "乌干达", "uy" to "乌拉圭",
        "uz" to "乌兹别克斯坦", "ve" to "委内瑞拉", "ye" to "也门",
        "zm" to "赞比亚", "zw" to "津巴布韦", "xk" to "科索沃",
        "pf" to "法属波利尼西亚", "sz" to "斯威士兰",
        "aq" to "南极洲", "bv" to "布韦岛"
    )

    // Commonly-used backgrounds for quick selection
    val availableBackgrounds = listOf(
        "cn", "hk", "tw", "us", "gb", "jp", "kr", "de", "fr", "th",
        "au", "sg", "my", "in", "vn", "ph", "id", "ca", "br", "tr",
        "ae", "nz", "ru", "it", "es", "nl", "se", "ch", "mo", "il"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Current selection preview
        if (currentAssetName.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(width = 80.dp, height = 50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF007AFF), RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = "file:///android_asset/flag_backgrounds/$currentAssetName",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column {
                    val code = currentAssetName.replace(".jpg", "").replace(".png", "").replace(".jpeg", "")
                    Text(
                        countryNames[code] ?: code,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = dk(Color(0xFFE5E5E7), Color(0xFF111827))
                    )
                    Text(
                        "当前卡片背景",
                        fontSize = 11.sp,
                        color = Color(0xFF8A94A6)
                    )
                }
            }
        }

        // Background grid - 4 per row
        val rows = availableBackgrounds.chunked(4)
        rows.forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                rowItems.forEach { code ->
                    val fileName = "$code.jpg"
                    val isSelected = currentAssetName == fileName || currentAssetName == "$code.png"
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                if (isSelected) 2.dp else 0.7.dp,
                                if (isSelected) Color(0xFF007AFF) else dk(
                                    Color(0xFF38383A),
                                    Color(0xFFE5E7EB)
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelect(fileName) }
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/flag_backgrounds/$fileName",
                            contentDescription = countryNames[code] ?: code,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelected) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF007AFF).copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "\u2713",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // Clear selection
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(dk(Color(0xFF1C1C1E), Color(0xFFF4F5F8)))
                .border(0.7.dp, dk(Color(0xFF38383A), Color(0xFFE5E7EB)), RoundedCornerShape(10.dp))
                .clickable { onSelect("") },
            contentAlignment = Alignment.Center
        ) {
            Text("清除背景", fontSize = 13.sp, color = Color(0xFF8A94A6))
        }
    }
}

/**
 * Color picker row - 8 preset color circles with checkmark on selected.
 * Returns hex color string like "#3A3A3C"
 */
@Composable
fun ColorPickerRow(
    currentColor: String,
    onColorChanged: (String) -> Unit
) {
    val colors = listOf(
        "#3A3A3C" to "深灰",
        "#007AFF" to "蓝色",
        "#64D2FF" to "青色",
        "#30B0C7" to "蓝绿",
        "#A8DB10" to "青柠",
        "#8CC63F" to "黄绿",
        "#FF9500" to "橙色",
        "#FF3B30" to "红橙"
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { (hex, label) ->
            val isSelected = hex.equals(currentColor, ignoreCase = true) ||
                    (currentColor.isBlank() && hex == "#3A3A3C")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            if (isSelected) 2.5.dp else 1.dp,
                            if (isSelected) Color.White else Color(0xFF38383A).copy(alpha = 0.3f),
                            CircleShape
                        )
                        .clickable { onColorChanged(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Text(
                            "\u2713",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    label,
                    fontSize = 9.sp,
                    color = Color(0xFF8A94A6)
                )
            }
        }
    }
}

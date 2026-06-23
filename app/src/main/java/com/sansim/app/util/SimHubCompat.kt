package com.sansim.app.util

import android.content.Context
import android.net.Uri
import com.sansim.app.data.model.PhoneNumberRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * SimHub JSON 兼容层 —— 导入 / 导出
 * 格式文档: https://github.com/yanglh1/simj-pp (SimhubCompatibility)
 */
object SimHubCompat {

    // ── 国家码 → 区号 映射（从 country_map.json 构建） ──
    private var countryDialMap: Map<String, String> = emptyMap()
    private var countryNameMap: Map<String, String> = emptyMap()

    fun init(context: Context) {
        try {
            val text = context.assets.open("country_map.json").bufferedReader().readText()
            val root = JSONObject(text)
            val countries = root.getJSONArray("countries")
            val dialMap = mutableMapOf<String, String>()
            val nameMap = mutableMapOf<String, String>()
            for (i in 0 until countries.length()) {
                val c = countries.getJSONObject(i)
                val cc = c.getString("countryCode")
                if (c.has("callingCodes")) {
                    val codes = c.getJSONArray("callingCodes")
                    if (codes.length() > 0) {
                        dialMap[cc] = codes.getString(0)
                    }
                }
                if (c.has("englishName")) {
                    nameMap[cc] = c.getString("englishName")
                }
            }
            countryDialMap = dialMap
            countryNameMap = nameMap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── 导入：SimHub JSON → PhoneNumberRecord 列表 ──
    fun importFromJson(context: Context, uri: Uri): List<PhoneNumberRecord> {
        if (countryDialMap.isEmpty()) init(context)
        val text = context.contentResolver.openInputStream(uri)?.use {
            BufferedReader(InputStreamReader(it)).readText()
        } ?: return emptyList()
        return parseJsonArray(text)
    }

    fun parseJsonArray(text: String): List<PhoneNumberRecord> {
        val arr = try { JSONArray(text) } catch (e: Exception) { return emptyList() }
        val result = mutableListOf<PhoneNumberRecord>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result.add(jsonToRecord(obj))
        }
        return result
    }

    private fun jsonToRecord(obj: JSONObject): PhoneNumberRecord {
        val countryCode = obj.optString("countryCode", "")
        val dialCode = countryDialMap[countryCode] ?: ""
        val countryName = obj.optString("countryName", countryNameMap[countryCode] ?: countryCode)

        val expiryRaw = obj.optString("expiryDate", "")
        val expireDate = parseIsoDate(expiryRaw)

        val createdRaw = obj.optString("createdAt", "")
        val createdAt = parseIsoDate(createdRaw)

        val price = obj.optString("price", "")
        val currency = obj.optString("currencyCode", "")
        val balance = if (price.isNotBlank() && currency.isNotBlank()) "$price $currency"
                      else obj.optString("currentBalance", "")

        // tags: SimHub uses JSON array, simJ uses comma-separated string
        val tags = if (obj.has("tags")) {
            val arr = obj.getJSONArray("tags")
            (0 until arr.length()).joinToString(",") { arr.getString(it) }
        } else ""

        val transactionNotes = obj.optString("transactionNotes", "")
        val customPrompt = obj.optString("customPrompt", "")
        val websiteURL = obj.optString("websiteURL", "")
        val cyclePaymentMinorUnits = obj.optInt("cyclePaymentMinorUnits", 0)
        val currencyCode = obj.optString("currencyCode", "")
        val cardBackgroundAssetName = obj.optString("cardBackgroundAssetName", "")
        val cardColorHex = obj.optString("cardColorHex", "")

        // Build note from plan only (tags/transactionNotes/customPrompt now have dedicated fields)
        val noteParts = mutableListOf<String>()
        val plan = obj.optString("plan", "")
        if (plan.isNotBlank()) noteParts.add(plan)
        val note = noteParts.joinToString("\n")

        return PhoneNumberRecord(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            countryCode = dialCode,
            countryName = countryName,
            flag = obj.optString("flag", flagFromCode(countryCode)),
            number = obj.optString("phoneNumber", "").replace(" ", ""),
            operator = obj.optString("carrier", ""),
            expireDate = expireDate,
            note = note,
            balance = balance,
            eid = obj.optString("eid", ""),
            smdp = obj.optString("smdpAddress", ""),
            activationCode = obj.optString("activationCode", ""),
            createdAt = createdAt.ifBlank { LocalDate.now().toString() },
            longTerm = obj.optBoolean("isLongTerm", false),
            cycleDays = obj.optInt("renewDays", 30),
            tags = tags,
            transactionNotes = transactionNotes,
            customPrompt = customPrompt,
            websiteURL = websiteURL,
            cyclePaymentMinorUnits = cyclePaymentMinorUnits,
            currencyCode = currencyCode,
            cardBackgroundAssetName = cardBackgroundAssetName,
            cardColorHex = cardColorHex,
        )
    }

    // ── 导出：PhoneNumberRecord 列表 → SimHub JSON ──
    fun exportToJson(records: List<PhoneNumberRecord>): String {
        val arr = JSONArray()
        for (r in records) {
            arr.put(recordToJson(r))
        }
        return arr.toString(2)
    }

    private fun recordToJson(r: PhoneNumberRecord): JSONObject {
        val obj = JSONObject()
        obj.put("id", r.id)

        // 反查 ISO 国家码
        val iso = dialToIso(r.countryCode)
        obj.put("countryCode", iso)
        obj.put("countryName", r.countryName)
        obj.put("flag", r.flag)
        obj.put("carrier", r.operator)
        obj.put("phoneNumber", r.number)
        obj.put("eid", r.eid)
        obj.put("smdpAddress", r.smdp)
        obj.put("activationCode", r.activationCode)
        obj.put("isLongTerm", r.longTerm)
        obj.put("renewDays", r.cycleDays)
        obj.put("renewalUnit", "days")
        obj.put("renewalIntervalValue", r.cycleDays)

        // 到期日期 → ISO-8601
        if (r.expireDate.isNotBlank()) {
            obj.put("expiryDate", "${r.expireDate}T00:00:00Z")
        }
        if (r.createdAt.isNotBlank()) {
            obj.put("createdAt", "${r.createdAt}T00:00:00Z")
        }

        // 余额
        if (r.balance.isNotBlank()) {
            obj.put("currentBalance", r.balance)
        }

        // tags: simJ uses comma-separated string, SimHub uses JSON array
        if (r.tags.isNotBlank()) {
            val tagsArr = JSONArray()
            r.tags.split(",").forEach { tag ->
                val t = tag.trim()
                if (t.isNotBlank()) tagsArr.put(t)
            }
            obj.put("tags", tagsArr)
        }

        // Export new SimHub fields
        if (r.transactionNotes.isNotBlank()) obj.put("transactionNotes", r.transactionNotes)
        if (r.customPrompt.isNotBlank()) obj.put("customPrompt", r.customPrompt)
        if (r.websiteURL.isNotBlank()) obj.put("websiteURL", r.websiteURL)
        if (r.cyclePaymentMinorUnits > 0) obj.put("cyclePaymentMinorUnits", r.cyclePaymentMinorUnits)
        if (r.currencyCode.isNotBlank()) obj.put("currencyCode", r.currencyCode)
        if (r.cardColorHex.isNotBlank()) obj.put("cardColorHex", r.cardColorHex)

        // cardBackgroundAssetName: use stored value or generate from ISO
        val bgName = r.cardBackgroundAssetName.ifBlank {
            if (iso.isNotBlank()) isoToBackgroundName(iso) else ""
        }
        if (bgName.isNotBlank()) obj.put("cardBackgroundAssetName", bgName)

        // note → plan (if note contains content not covered by dedicated fields)
        if (r.note.isNotBlank()) {
            obj.put("plan", r.note)
        }

        obj.put("updatedAt", java.time.Instant.now().toString())
        return obj
    }

    // ── 工具函数 ──
    private fun parseIsoDate(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            raw.substring(0, 10)
        } catch (e: Exception) {
            raw
        }
    }

    private fun flagFromCode(iso: String): String {
        if (iso.length != 2) return ""
        val first = 0x1F1E6 + (iso[0].uppercaseChar() - 'A')
        val second = 0x1F1E6 + (iso[1].uppercaseChar() - 'A')
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    private fun dialToIso(dialCode: String): String {
        for ((iso, dial) in countryDialMap) {
            if (dial == dialCode) return iso
        }
        return when (dialCode) {
            "+86" -> "CN"
            "+852" -> "HK"
            "+853" -> "MO"
            "+886" -> "TW"
            "+44" -> "GB"
            "+1" -> "US"
            "+81" -> "JP"
            "+82" -> "KR"
            "+65" -> "SG"
            "+60" -> "MY"
            "+66" -> "TH"
            "+84" -> "VN"
            "+63" -> "PH"
            "+62" -> "ID"
            "+91" -> "IN"
            "+7" -> "RU"
            "+33" -> "FR"
            "+49" -> "DE"
            "+39" -> "IT"
            "+34" -> "ES"
            "+31" -> "NL"
            "+46" -> "SE"
            "+47" -> "NO"
            "+45" -> "DK"
            "+358" -> "FI"
            "+41" -> "CH"
            "+43" -> "AT"
            "+32" -> "BE"
            "+351" -> "PT"
            "+30" -> "GR"
            "+90" -> "TR"
            "+55" -> "BR"
            "+52" -> "MX"
            "+54" -> "AR"
            "+56" -> "CL"
            "+57" -> "CO"
            "+51" -> "PE"
            "+61" -> "AU"
            "+64" -> "NZ"
            "+27" -> "ZA"
            "+20" -> "EG"
            "+971" -> "AE"
            "+966" -> "SA"
            "+965" -> "KW"
            "+974" -> "QA"
            "+973" -> "BH"
            "+968" -> "OM"
            "+962" -> "JO"
            "+961" -> "LB"
            "+972" -> "IL"
            "+380" -> "UA"
            "+48" -> "PL"
            "+420" -> "CZ"
            "+36" -> "HU"
            "+40" -> "RO"
            "+359" -> "BG"
            "+385" -> "HR"
            "+381" -> "RS"
            "+386" -> "SI"
            "+421" -> "SK"
            "+370" -> "LT"
            "+371" -> "LV"
            "+372" -> "EE"
            "+354" -> "IS"
            "+353" -> "IE"
            "+356" -> "MT"
            "+352" -> "LU"
            "+355" -> "AL"
            "+389" -> "MK"
            "+382" -> "ME"
            "+387" -> "BA"
            "+998" -> "UZ"
            "+996" -> "KG"
            "+992" -> "TJ"
            "+993" -> "TM"
            "+976" -> "MN"
            "+855" -> "KH"
            "+856" -> "LA"
            "+95" -> "MM"
            "+880" -> "BD"
            "+94" -> "LK"
            "+977" -> "NP"
            "+93" -> "AF"
            "+98" -> "IR"
            "+964" -> "IQ"
            "+963" -> "SY"
            "+254" -> "KE"
            "+256" -> "UG"
            "+255" -> "TZ"
            "+234" -> "NG"
            "+233" -> "GH"
            "+212" -> "MA"
            "+216" -> "TN"
            "+213" -> "DZ"
            "+218" -> "LY"
            "+249" -> "SD"
            "+251" -> "ET"
            "+252" -> "SO"
            "+260" -> "ZM"
            "+263" -> "ZW"
            "+265" -> "MW"
            "+267" -> "BW"
            "+264" -> "NA"
            "+258" -> "MZ"
            "+261" -> "MG"
            "+230" -> "MU"
            "+248" -> "SC"
            "+960" -> "MV"
            "+975" -> "BT"
            "+673" -> "BN"
            "+670" -> "TL"
            "+675" -> "PG"
            "+679" -> "FJ"
            "+685" -> "WS"
            "+676" -> "TO"
            "+678" -> "VU"
            "+686" -> "KI"
            "+688" -> "TV"
            "+691" -> "FM"
            "+692" -> "MH"
            "+375" -> "BY"
            "+373" -> "MD"
            "+374" -> "AM"
            "+995" -> "GE"
            "+994" -> "AZ"
            "+502" -> "GT"
            "+503" -> "SV"
            "+504" -> "HN"
            "+505" -> "NI"
            "+506" -> "CR"
            "+507" -> "PA"
            "+591" -> "BO"
            "+593" -> "EC"
            "+595" -> "PY"
            "+598" -> "UY"
            "+592" -> "GY"
            "+597" -> "SR"
            "+297" -> "AW"
            "+246" -> "IO"
            "+357" -> "CY"
            else -> ""
        }
    }

    private fun isoToBackgroundName(iso: String): String {
        val name = when (iso.uppercase()) {
            "CN" -> "China"
            "HK" -> "HongKong"
            "MO" -> "Macau"
            "TW" -> "Taiwan"
            "JP" -> "Japan"
            "KR" -> "SouthKorea"
            "US" -> "UnitedStates"
            "GB" -> "UnitedKingdom"
            "DE" -> "Germany"
            "FR" -> "France"
            "IT" -> "Italy"
            "ES" -> "Spain"
            "PT" -> "Portugal"
            "NL" -> "Netherlands"
            "BE" -> "Belgium"
            "CH" -> "Switzerland"
            "AT" -> "Austria"
            "SE" -> "Sweden"
            "NO" -> "Norway"
            "DK" -> "Denmark"
            "FI" -> "Finland"
            "IS" -> "Iceland"
            "IE" -> "Ireland"
            "PL" -> "Poland"
            "CZ" -> "Czechia"
            "SK" -> "Slovakia"
            "HU" -> "Hungary"
            "RO" -> "Romania"
            "BG" -> "Bulgaria"
            "HR" -> "Croatia"
            "RS" -> "Serbia"
            "SI" -> "Slovenia"
            "BA" -> "BosniaHerzegovina"
            "MK" -> "NorthMacedonia"
            "ME" -> "Montenegro"
            "AL" -> "Albania"
            "GR" -> "Greece"
            "TR" -> "Turkey"
            "RU" -> "Russia"
            "UA" -> "Ukraine"
            "BY" -> "Belarus"
            "MD" -> "Moldova"
            "LT" -> "Lithuania"
            "LV" -> "Latvia"
            "EE" -> "Estonia"
            "MT" -> "Malta"
            "LU" -> "Luxembourg"
            "LI" -> "Liechtenstein"
            "AD" -> "Andorra"
            "MC" -> "Monaco"
            "SM" -> "SanMarino"
            "VA" -> "VaticanCity"
            "SG" -> "Singapore"
            "MY" -> "Malaysia"
            "TH" -> "Thailand"
            "VN" -> "Vietnam"
            "PH" -> "Philippines"
            "ID" -> "Indonesia"
            "IN" -> "India"
            "BD" -> "Bangladesh"
            "PK" -> "Pakistan"
            "LK" -> "SriLanka"
            "NP" -> "Nepal"
            "BT" -> "Bhutan"
            "MV" -> "Maldives"
            "MM" -> "Myanmar"
            "KH" -> "Cambodia"
            "LA" -> "Laos"
            "BN" -> "Brunei"
            "TL" -> "TimorLeste"
            "AU" -> "Australia"
            "NZ" -> "NewZealand"
            "PG" -> "PapuaNewGuinea"
            "FJ" -> "Fiji"
            "WS" -> "Samoa"
            "TO" -> "Tonga"
            "VU" -> "Vanuatu"
            "KI" -> "Kiribati"
            "TV" -> "Tuvalu"
            "FM" -> "Micronesia"
            "MH" -> "MarshallIslands"
            "BR" -> "Brazil"
            "MX" -> "Mexico"
            "AR" -> "Argentina"
            "CL" -> "Chile"
            "CO" -> "Colombia"
            "PE" -> "Peru"
            "VE" -> "Venezuela"
            "EC" -> "Ecuador"
            "BO" -> "Bolivia"
            "PY" -> "Paraguay"
            "UY" -> "Uruguay"
            "GY" -> "Guyana"
            "SR" -> "Suriname"
            "GT" -> "Guatemala"
            "BZ" -> "Belize"
            "SV" -> "ElSalvador"
            "HN" -> "Honduras"
            "NI" -> "Nicaragua"
            "CR" -> "CostaRica"
            "PA" -> "Panama"
            "CU" -> "Cuba"
            "JM" -> "Jamaica"
            "HT" -> "Haiti"
            "DO" -> "DominicanRepublic"
            "TT" -> "TrinidadAndTobago"
            "BB" -> "Barbados"
            "BS" -> "Bahamas"
            "AG" -> "AntiguaAndBarbuda"
            "DM" -> "Dominica"
            "GD" -> "Grenada"
            "KN" -> "SaintKittsAndNevis"
            "LC" -> "SaintLucia"
            "VC" -> "SaintVincentAndTheGrenadines"
            "AI" -> "Anguilla"
            "AW" -> "Aruba"
            "KY" -> "CaymanIslands"
            "CW" -> "Curacao"
            "GP" -> "Guadeloupe"
            "MQ" -> "Martinique"
            "PR" -> "PuertoRico"
            "TC" -> "TurksAndCaicos"
            "VG" -> "BritishVirginIslands"
            "VI" -> "USVirginIslands"
            "GL" -> "Greenland"
            "BM" -> "Bermuda"
            "FK" -> "FalklandIslands"
            "GF" -> "FrenchGuiana"
            "SA" -> "SaudiArabia"
            "AE" -> "UnitedArabEmirates"
            "QA" -> "Qatar"
            "KW" -> "Kuwait"
            "BH" -> "Bahrain"
            "OM" -> "Oman"
            "JO" -> "Jordan"
            "LB" -> "Lebanon"
            "IL" -> "Israel"
            "PS" -> "Palestine"
            "IQ" -> "Iraq"
            "IR" -> "Iran"
            "SY" -> "Syria"
            "YE" -> "Yemen"
            "AF" -> "Afghanistan"
            "KZ" -> "Kazakhstan"
            "UZ" -> "Uzbekistan"
            "TM" -> "Turkmenistan"
            "KG" -> "Kyrgyzstan"
            "TJ" -> "Tajikistan"
            "MN" -> "Mongolia"
            "GE" -> "Georgia"
            "AM" -> "Armenia"
            "AZ" -> "Azerbaijan"
            "CY" -> "Cyprus"
            "EG" -> "Egypt"
            "ZA" -> "SouthAfrica"
            "NG" -> "Nigeria"
            "KE" -> "Kenya"
            "GH" -> "Ghana"
            "TZ" -> "Tanzania"
            "UG" -> "Uganda"
            "ET" -> "Ethiopia"
            "MA" -> "Morocco"
            "TN" -> "Tunisia"
            "DZ" -> "Algeria"
            "LY" -> "Libya"
            "SD" -> "Sudan"
            "SO" -> "Somalia"
            "ZM" -> "Zambia"
            "ZW" -> "Zimbabwe"
            "MW" -> "Malawi"
            "MZ" -> "Mozambique"
            "MG" -> "Madagascar"
            "MU" -> "Mauritius"
            "SC" -> "Seychelles"
            "BW" -> "Botswana"
            "NA" -> "Namibia"
            "SZ" -> "Eswatini"
            "LS" -> "Lesotho"
            "AO" -> "Angola"
            "CM" -> "Cameroon"
            "CI" -> "CoteDIvoire"
            "SN" -> "Senegal"
            "ML" -> "Mali"
            "BF" -> "BurkinaFaso"
            "NE" -> "Niger"
            "TD" -> "Chad"
            "CF" -> "CentralAfricanRepublic"
            "CD" -> "CongoKinshasa"
            "CG" -> "CongoBrazzaville"
            "GA" -> "Gabon"
            "GQ" -> "EquatorialGuinea"
            "BI" -> "Burundi"
            "RW" -> "Rwanda"
            "DJ" -> "Djibouti"
            "ER" -> "Eritrea"
            "GM" -> "Gambia"
            "GN" -> "Guinea"
            "GW" -> "GuineaBissau"
            "LR" -> "Liberia"
            "SL" -> "SierraLeone"
            "TG" -> "Togo"
            "BJ" -> "Benin"
            "MR" -> "Mauritania"
            "KM" -> "Comoros"
            "CV" -> "CapeVerde"
            "ST" -> "SaoTomeAndPrincipe"
            "SH" -> "SaintHelena"
            "AC" -> "AscensionIsland"
            "TA" -> "TristanDaCunha"
            "YT" -> "Mayotte"
            "RE" -> "Reunion"
            "IO" -> "BritishIndianOceanTerritory"
            "AQ" -> "Antarctica"
            else -> ""
        }
        if (name.isBlank()) return ""
        return "CardBackground${name}Lighttrail"
    }
}

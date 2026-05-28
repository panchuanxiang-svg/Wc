import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.w3c.dom.Document // 请根据实际项目使用的 DOM 解析库确保导入正确

object VersionFetch {

    // 共享的 OkHttpClient 实例，避免重复创建引发内存泄漏与套接字溢出
    private val pClient = OkHttpClient()

    // ... (工程中原有的 hybridGetLatestVersion, getLatestVersion 等方法保持不变) ...

    /**
     * ① 递增 Beta 版本字符串 (完美处理 A-Z 满 26 进位逻辑)
     * 当输入如 "ZZZZ" 时，经过循环会全部变为 "AAAA"
     */
    private fun incrementBetaVersion(version: String): String {
        val chars = version.toCharArray()
        for (i in chars.indices.reversed()) {
            val c = chars[i]
            when (c) {
                'Z' -> {
                    chars[i] = 'A' // 逢 Z 变为 A，继续向前一位循环进位
                }
                in 'A'..'Y' -> {
                    chars[i] = c + 1 // A-Y 直接加 1，完成递增并返回
                    return String(chars)
                }
            }
        }
        return String(chars)
    }

    /**
     * ② 探测后续可能的 Beta 版本 (异常不中断，稳健探测 5 次)
     */
    suspend fun probeNextBetas(
        currentVersion: String,
        model: String,
        region: String,
    ): List<String> {
        val found = mutableListOf<String>()
        var next = currentVersion

        repeat(5) {
            next = incrementBetaVersion(next)
            try {
                val result = FusClient.getFirmwareInformation(
                    model = model,
                    region = region,
                    version = next,
                )
                if (result != null) {
                    found.add(next)
                }
            } catch (_: Exception) {
                // 捕获异常，交由 repeat 机制继续执行后续探测
            }
        }
        return found
    }

    /**
     * ③ 解析 FUS 历史固件版本信息 (全面移除不安全断言 `!!`，引入全安全空值兜底)
     */
    fun parseHistoryInfos(
        historyDoc: Document,
    ): List<SmartBinaryInfo> {
        val allInfos = historyDoc.getElementsByTag("BINARY_INFO")

        if (allInfos.isEmpty()) {
            return listOf()
        }

        return allInfos.map { info ->
            SmartBinaryInfo(
                index = info.firstDataElementDataByTagName("BINARY_INDEX")?.toIntOrNull(),
                sequence = info.firstDataElementDataByTagName("BINARY_SEQUENCE")?.toIntOrNull() ?: 0,
                modelName = info.firstDataElementDataByTagName("BINARY_MODEL_NAME") ?: "",
                displayName = info.firstDataElementDataByTagName("BINARY_MODEL_DISPLAYNAME"),
                swVersion = info.firstDataElementDataByTagName("BINARY_SW_VERSION") ?: "",
                displayVersion = info.firstDataElementDataByTagName("BINARY_SW_DISPLAYVERSION"),
                directVersion = info.firstDataElementDataByTagName("BINARY_DIRECT_VERSION"),
                localCode = info.firstDataElementDataByTagName("BINARY_LOCAL_CODE") ?: "",
                buyerCode = info.firstDataElementDataByTagName("BINARY_BUYER_CODE"),
                nature = info.firstDataElementDataByTagName("BINARY_NATURE")?.toIntOrNull(),
                status = info.firstDataElementDataByTagName("BINARY_STATUS")?.toIntOrNull(),
                exists = info.firstDataElementDataByTagName("BINARY_EXIST")?.toIntOrNull(),
                osName = info.firstDataElementDataByTagName("BINARY_OS_NAME"),
                platform = info.firstDataElementDataByTagName("DEVICE_PLATFORM"),
                openDate = info.firstDataElementDataByTagName("BINARY_OPEN_DATE"),
                sharing = info.firstDataElementDataByTagName("SHARING_BINARY")?.toIntOrNull(),
                category = info.firstDataElementDataByTagName("BINARY_CATEGORY"),
                open = info.firstDataElementDataByTagName("AID_OPEN")?.toIntOrNull(),
            )
        }.sortedBy { it.sequence }
    }

    /**
     * ④ 修复 fetchGithubFirmware 
     * 解决点 1：使用 .use { ... } 自动关闭 Response 规避 Socket 连接泄露崩溃
     * 解决点 2：将 sequence 修改为 10000 + i，彻底根除与 FUS 数据源的排序相撞灾难
     */
    suspend fun fetchGithubFirmware(): List<SmartBinaryInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://raw.githubusercontent.com/Mai119920513/SamsungTestFirmwareVersionDecrypt/main/firmware.json"
            
            val request = Request.Builder()
                .url(url)
                .build()
                
            // 使用 Kotlin 的 use 关键字，无论执行是否成功，离开作用域时都会自动 close() 释放流
            pClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                
                val json = JSONArray(body)
                val list = mutableListOf<SmartBinaryInfo>()
                
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    list.add(
                        SmartBinaryInfo(
                            index = null,
                            // 关键修复：GitHub 源版本使用高位基数区隔，防止污染 FUS 数据排序
                            sequence = 10000 + i,
                            modelName = obj.optString("model"),
                            displayName = obj.optString("name"),
                            swVersion = obj.optString("version"),
                            displayVersion = obj.optString("version"),
                            directVersion = null,
                            localCode = obj.optString("region"),
                            buyerCode = null,
                            nature = null,
                            status = 1,
                            exists = 1,
                            osName = null,
                            platform = null,
                            openDate = null,
                            sharing = null,
                            category = "GITHUB",
                            open = null
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * ⑤ 统一入口：合并官方 FUS 历史流与第三方 GitHub 固件流
     * 精确多条件复合排序：官方 FUS 永远置顶，GitHub 紧随其后，且各自内部完美有序排列
     */
    suspend fun getUnifiedFirmwareList(
        historyDoc: Document
    ): List<SmartBinaryInfo> {
        val fus = parseHistoryInfos(historyDoc)
        val github = fetchGithubFirmware()
        
        return (fus + github).sortedWith(
            compareBy<SmartBinaryInfo> { it.category == "GITHUB" }
                .thenBy { it.sequence }
        )
    }
}

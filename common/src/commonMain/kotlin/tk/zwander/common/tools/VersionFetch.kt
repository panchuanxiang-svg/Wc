import org.w3c.dom.Document // 请根据实际项目使用的 DOM 解析库（如 org.jsoup.nodes 或 org.w3c.dom）确保导入正确

object VersionFetch {

    // ... (工程中原有的 hybridGetLatestVersion, getLatestVersion 等方法保持不变) ...

    /**
     * ① 修复 incrementBetaVersion (完美处理 A-Z 满 26 进位逻辑，规避字符串首位溢出)
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
        // 如果循环走完都未能 return，说明类似 "ZZZZ" 全溢出了，兜底将其变为以 'A' 开头
        return "A" + String(chars).substring(1)
    }

    /**
     * ② 修复 probeNextBetas (避免异常直接终止，完整探测 5 次)
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
                // 调用 FusClient.getFirmwareInformation
                val result = FusClient.getFirmwareInformation(
                    model = model,
                    region = region,
                    version = next,
                )
                if (result != null) {
                    found.add(next)
                }
            } catch (_: Exception) {
                // 捕获异常但不要 return，让 repeat 机制继续执行，探测后续的可能版本
            }
        }
        return found
    }

    /**
     * ③ 修复 parseHistoryInfos (全面移除不安全断言 `!!`，引入全安全兜底机制)
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
                // 核心整型与字符串均通过 `?:` 赋安全缺省值，规避 NPE 导致的直接闪退
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
}

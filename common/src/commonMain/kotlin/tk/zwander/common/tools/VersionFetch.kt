object VersionFetch {
    
    // ... (之前的 hybridGetLatestVersion, getLatestVersion 等方法保持不变) ...

    /**
     * 递增 Beta 版本字符串 (例如 ZYEA -> ZYEB)
     */
    private fun incrementBetaVersion(version: String): String {
        val chars = version.toCharArray()
        for (i in chars.indices.reversed()) {
            val c = chars[i]
            if (c in 'A' until 'Z') {
                chars[i] = c + 1
                return String(chars)
            }
        }
        return version
    }

    /**
     * 探测后续可能的 Beta 版本
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
                // 注意：这里调用了 FusClient.getFirmwareInformation
                // 如果你的项目中此方法路径不同，请确保导入正确
                val result = FusClient.getFirmwareInformation(
                    model = model,
                    region = region,
                    version = next,
                )
                if (result != null) {
                    found.add(next)
                }
            } catch (_: Exception) {
                return found
            }
        }
        return found
    }

    // ... (之前的 parseHistoryInfos 和其他方法保持不变) ...
    
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
                sequence = info.firstDataElementDataByTagName("BINARY_SEQUENCE")?.toInt()!!,
                modelName = info.firstDataElementDataByTagName("BINARY_MODEL_NAME")!!,
                displayName = info.firstDataElementDataByTagName("BINARY_MODEL_DISPLAYNAME"),
                swVersion = info.firstDataElementDataByTagName("BINARY_SW_VERSION")!!,
                displayVersion = info.firstDataElementDataByTagName("BINARY_SW_DISPLAYVERSION"),
                directVersion = info.firstDataElementDataByTagName("BINARY_DIRECT_VERSION"),
                localCode = info.firstDataElementDataByTagName("BINARY_LOCAL_CODE")!!,
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

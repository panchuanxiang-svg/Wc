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
        }
            // ✅ 修改：删除 filterNot，保留所有版本 (包括 Beta)
            .sortedBy { it.sequence }
    }

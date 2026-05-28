package tk.zwander.common.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import tk.zwander.common.data.SmartBinaryInfo
import tk.zwander.common.util.globalHttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

object VersionFetch {

    private fun incrementBetaVersion(version: String): String {
        val chars = version.toCharArray()
        for (i in chars.indices.reversed()) {
            when (chars[i]) {
                'Z' -> chars[i] = 'A'
                in 'A'..'Y' -> {
                    chars[i] = chars[i] + 1
                    return String(chars)
                }
            }
        }
        return String(chars)
    }

    suspend fun probeNextBetas(
        currentVersion: String,
        model: String,
        region: String
    ): List<String> {
        val found = mutableListOf<String>()
        var next = currentVersion

        repeat(5) {
            next = incrementBetaVersion(next)
            try {
                val result = FusClient.getFirmwareInformation(
                    model = model,
                    region = region,
                    version = next
                )
                if (result != null) found.add(next)
            } catch (_: Exception) {}
        }
        return found
    }

    /**
     * 🔥 最安全解析方式：直接使用 DOM Document（强类型）
     */
    fun parseHistoryInfos(historyDoc: org.w3c.dom.Document?): List<SmartBinaryInfo> {
        if (historyDoc == null) return emptyList()

        val nodes = historyDoc.getElementsByTagName("BINARY_INFO")
        if (nodes.length == 0) return emptyList()

        val list = mutableListOf<SmartBinaryInfo>()

        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? org.w3c.dom.Element ?: continue

            list.add(
                SmartBinaryInfo(
                    index = el.getElementsByTagName("BINARY_INDEX").item(0)?.textContent?.toIntOrNull(),
                    sequence = el.getElementsByTagName("BINARY_SEQUENCE").item(0)?.textContent?.toIntOrNull() ?: 0,
                    modelName = el.getElementsByTagName("BINARY_MODEL_NAME").item(0)?.textContent ?: "",
                    displayName = el.getElementsByTagName("BINARY_MODEL_DISPLAYNAME").item(0)?.textContent,
                    swVersion = el.getElementsByTagName("BINARY_SW_VERSION").item(0)?.textContent ?: "",
                    displayVersion = el.getElementsByTagName("BINARY_SW_DISPLAYVERSION").item(0)?.textContent,
                    directVersion = el.getElementsByTagName("BINARY_DIRECT_VERSION").item(0)?.textContent,
                    localCode = el.getElementsByTagName("BINARY_LOCAL_CODE").item(0)?.textContent ?: "",
                    buyerCode = el.getElementsByTagName("BINARY_BUYER_CODE").item(0)?.textContent,
                    nature = el.getElementsByTagName("BINARY_NATURE").item(0)?.textContent?.toIntOrNull(),
                    status = el.getElementsByTagName("BINARY_STATUS").item(0)?.textContent?.toIntOrNull(),
                    exists = el.getElementsByTagName("BINARY_EXIST").item(0)?.textContent?.toIntOrNull(),
                    osName = el.getElementsByTagName("BINARY_OS_NAME").item(0)?.textContent,
                    platform = el.getElementsByTagName("DEVICE_PLATFORM").item(0)?.textContent,
                    openDate = el.getElementsByTagName("BINARY_OPEN_DATE").item(0)?.textContent,
                    sharing = el.getElementsByTagName("SHARING_BINARY").item(0)?.textContent?.toIntOrNull(),
                    category = "FUS",
                    open = el.getElementsByTagName("AID_OPEN").item(0)?.textContent?.toIntOrNull()
                )
            )
        }

        return list.sortedByDescending { it.sequence }
    }

    suspend fun fetchGithubFirmware(): List<SmartBinaryInfo> = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://raw.githubusercontent.com/Mai119920513/SamsungTestFirmwareVersionDecrypt/main/firmware.json"

            val response = globalHttpClient.get(url).bodyAsText()
            val json = JSONArray(response)

            val list = mutableListOf<SmartBinaryInfo>()

            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)

                list.add(
                    SmartBinaryInfo(
                        index = null,
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun hybridGetLatestVersion(
        historyDoc: org.w3c.dom.Document?
    ): List<SmartBinaryInfo> {

        val fus = parseHistoryInfos(historyDoc)
        val github = fetchGithubFirmware()

        return (fus + github)
            .filter { it.swVersion.isNotBlank() }
            .sortedWith(
                compareBy<SmartBinaryInfo> { it.category == "GITHUB" }
                    .thenByDescending { it.sequence }
            )
    }
}

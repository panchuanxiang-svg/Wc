package tk.zwander.common.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers // 👈 仅保留正确的 Dispatchers 导包
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.* // 👈 引入 JsonElement 相关解析支持
import tk.zwander.common.data.SmartBinaryInfo
import tk.zwander.common.util.DataParsingUtils

object VersionFetch {

    private val client = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class GithubFirmwareDto(
        val model: String = "",
        val name: String = "",
        val version: String = "",
        val region: String = ""
    )

    suspend fun fetchGithubFirmware(): List<SmartBinaryInfo> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://raw.githubusercontent.com/Mai119920513/SamsungTestFirmwareVersionDecrypt/main/firmware.json"
                val response = client.get(url).body<String>()

                // 💡 遵循截图建议的【更稳版本】：先转为万能的 JsonElement 兜底，防止结构变化导致 Crash
                val jsonElement = json.parseToJsonElement(response)
                
                // 判断远程 JSON 根节点是数组还是对象
                val dtoList: List<GithubFirmwareDto> = when {
                    // 情况 A：标准的 [...] 数组结构
                    jsonElement is JsonArray -> {
                        json.decodeFromJsonElement<List<GithubFirmwareDto>>(jsonElement)
                    }
                    // 情况 B：未来如果变成了 {"data": [...]} 的对象包裹结构
                    jsonElement is JsonObject && jsonElement.containsKey("data") -> {
                        jsonElement["data"]?.let {
                            json.decodeFromJsonElement<List<GithubFirmwareDto>>(it)
                        } ?: emptyList()
                    }
                    // 情况 C：其他未知突变
                    else -> emptyList()
                }

                dtoList.mapIndexed { i, item ->
                    SmartBinaryInfo(
                        index = null,
                        sequence = 10000 + i,
                        modelName = item.model,
                        displayName = item.name,
                        swVersion = item.version,
                        displayVersion = item.version,
                        directVersion = null,
                        localCode = item.region,
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    fun parseHistoryInfos(history: Any?): List<SmartBinaryInfo> {
        return try {
            DataParsingUtils.extractBinaryInfos(history)
                .sortedByDescending { it.sequence }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun hybridGetLatestVersion(history: Any?): List<SmartBinaryInfo> {
        val fus = parseHistoryInfos(history)
        val github = fetchGithubFirmware()

        return (fus + github)
            .filter { it.swVersion.isNotBlank() }
            .sortedWith(
                compareBy<SmartBinaryInfo> { it.category == "GITHUB" }
                    .thenByDescending { it.sequence }
            )
    }
}

package tk.zwander.common.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.parseToJsonElement
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

    suspend fun fetchGithubFirmware(): List<SmartBinaryInfo> {
        return try {
            val url =
                "https://raw.githubusercontent.com/Mai119920513/SamsungTestFirmwareVersionDecrypt/main/firmware.json"

            val response: String = client.get(url).body()
            val jsonElement = json.parseToJsonElement(response)

            val dtoList: List<GithubFirmwareDto> = when {
                jsonElement is JsonArray ->
                    json.decodeFromJsonElement(jsonElement)

                jsonElement is JsonObject && jsonElement.containsKey("data") ->
                    jsonElement["data"]?.let {
                        json.decodeFromJsonElement(it)
                    } ?: emptyList()

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

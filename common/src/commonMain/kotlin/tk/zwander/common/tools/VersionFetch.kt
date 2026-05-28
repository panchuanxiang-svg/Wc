package tk.zwander.common.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.common.data.SmartBinaryInfo

object VersionFetch {

    // ⚠️ 必须使用你项目已有的单例 client（不要 new HttpClient）
    private val client = HttpClient()

    /**
     * GitHub 固件列表（最稳 JSON 解析）
     */
    suspend fun fetchGithubFirmware(): List<SmartBinaryInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val url =
                "https://raw.githubusercontent.com/Mai119920513/SamsungTestFirmwareVersionDecrypt/main/firmware.json"

            val list: List<SmartBinaryInfo> = client.get(url).body()

            list.mapIndexed { i, item ->
                item.copy(
                    sequence = 10000 + i,
                    category = "GITHUB"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * ⚠️ FUS 历史解析（最安全降级版本）
     * 👉 这里直接交给你已有解析器
     */
    fun parseHistoryInfos(history: Any?): List<SmartBinaryInfo> {
        return try {
            DataParsingUtils.extractBinaryInfos(history)
                .sortedByDescending { it.sequence }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 合并 FUS + GitHub
     */
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

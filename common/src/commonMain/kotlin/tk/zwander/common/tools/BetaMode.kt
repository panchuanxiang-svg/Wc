package tk.zwander.common.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class BetaInfo(
val official: String?,
val beta: String?,
val android: String?,
val updateTime: String?,
val description: String?,
)

object BetaMode {
private const val JSON_URL =
"https://cdn.jsdelivr.net/gh/Mai19930513/SamsungTestFirmwareVersionDecrypt/firmware_mini.json"

suspend fun getBetaInfo(
    model: String,
    region: String,
): BetaInfo? = withContext(Dispatchers.IO) {
    try {
        val response = FusClient.request(
            Request(
                url = JSON_URL,
                method = "GET",
            )
        )

        if (response.isEmpty()) {
            return@withContext null
        }

        val json = Json.parseToJsonElement(
            response.decodeToString()
        ).jsonObject

        val modelObj =
            json[model]?.jsonObject
                ?: return@withContext null

        val regionObj =
            modelObj[region]?.jsonObject
                ?: return@withContext null

        BetaInfo(
            official =
                regionObj["最新正式版"]
                    ?.jsonPrimitive
                    ?.content,

            beta =
                regionObj["常规更新测试版"]
                    ?.jsonPrimitive
                    ?.content,

            android =
                regionObj["测试版安卓版本"]
                    ?.jsonPrimitive
                    ?.content,

            updateTime =
                regionObj["最新测试版上传时间"]
                    ?.jsonPrimitive
                    ?.content,

            description =
                regionObj["最新版本号说明"]
                    ?.jsonPrimitive
                    ?.content,
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

}

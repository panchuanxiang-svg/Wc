package tk.zwander.common.tools

import com.fleeksoft.io.exception.ArrayIndexOutOfBoundsException
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.utils.io.core.toByteArray
import tk.zwander.common.util.firstElementByTagName
import tk.zwander.common.util.globalHttpClient

object FusClient {

    enum class Request(val value: String, val cloud: Boolean) {
        GENERATE_NONCE("NF_SmartDownloadGenerateNonce.do", false),
        BINARY_INFORM("NF_SmartDownloadBinaryInform.do", false),
        BINARY_INIT("NF_SmartDownloadBinaryInitForMass.do", false),
        HISTORY("SmartHistory.do", false),
    }

    private var nonce = ""
    private var auth: String = ""
    private var sessionId: String = ""

    suspend fun getNonce(): String {
        if (nonce.isBlank()) {
            generateNonce()
        }

        return nonce
    }

    private suspend fun generateNonce() {
        println("Generating nonce.")

        makeReq(Request.GENERATE_NONCE)

        println("Nonce: $nonce")
        println("Auth: $auth")
    }

    private suspend fun makeSignatureHash(signature: String?): String? {
        if (signature == null) return null

        val hasher = CryptUtils.md5Provider.hasher()

        val a = hasher.hash(
            "auth:$nonce:00000001".toByteArray()
        ).toHexString()

        val b = hasher.hash(
            "interface:$signature".toByteArray()
        ).toHexString()

        return hasher.hash(
            "$a:FUS:$b".toByteArray()
        ).toHexString()
    }

    private suspend fun getAuthV(
        includeNonce: Boolean = true,
        signature: String? = null,
        cloud: Boolean = false,
    ): String {

        val hasSignature = !signature.isNullOrBlank()

        val nonceValue = when {
            includeNonce && hasSignature -> {
                val chars = "abcdefghijklmnopqrstuvwxyz0123456789"

                CharArray(16) {
                    chars.random()
                }.joinToString("")
            }

            includeNonce -> nonce

            else -> ""
        }

        return "FUS nonce=\"${if (cloud) nonceValue else nonce}\", " +
                "signature=\"${makeSignatureHash(signature?.takeIf { !it.isBlank() }) ?: auth}\", " +
                "nc=\"${if (hasSignature) "00000001" else ""}\", " +
                "type=\"${if (hasSignature) "auth" else ""}\", " +
                "realm=\"${if (hasSignature) "interface" else ""}\""
    }

    /**
     * 查询固件信息
     */
    suspend fun getFirmwareInformation(
        model: String,
        region: String,
        version: String,
    ): String? {

        val xml = """
            <FUSMsg>
                <FUSHdr>
                    <ProtoVer>1</ProtoVer>
                    <SessionID>0</SessionID>
                    <MsgID>1</MsgID>
                </FUSHdr>

                <FUSBody>
                    <Put>
                        <ACCESS_MODE Data="1"/>
                        <BINARY_NATURE Data="1"/>
                        <CLIENT_PRODUCT Data="Smart Switch"/>
                        <DEVICE_FW_VERSION Data="$version"/>
                        <DEVICE_LOCAL_CODE Data="$region"/>
                        <DEVICE_MODEL_NAME Data="$model"/>
                    </Put>
                </FUSBody>
            </FUSMsg>
        """.trimIndent()

        return try {

            val response = makeReq(
                request = Request.BINARY_INFORM,
                data = xml,
                signature = model,
            )

            println("========== FUS RESPONSE ==========")
            println(response)
            println("==================================")

            if (
                response.contains("BINARY_NAME") ||
                response.contains("BINARY_BYTE_SIZE") ||
                response.contains("MODEL_PATH")
            ) {
                response
            } else {
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun makeReq(
        request: Request,
        data: String = "",
        signature: String? = null,
    ): String {

        if (
            nonce.isBlank() &&
            request != Request.GENERATE_NONCE
        ) {
            generateNonce()
        }

        val authV = getAuthV(
            cloud = request.cloud,
            signature = signature,
        )

        val response =
            globalHttpClient.request(
                "https://neofussvr.sslcs.cdngc.net/${request.value}"
            ) {

                method = HttpMethod.Post

                headers {
                    append("Authorization", authV)
                    append("User-Agent", "SMART 2.0")

                    append(
                        "Cookie",
                        "JSESSIONID=$sessionId;SESSION=$sessionId"
                    )

                    append(
                        HttpHeaders.ContentLength,
                        "${data.toByteArray().size}"
                    )
                }

                setBody(data)
            }

        val body = response.bodyAsText()

        /**
         * 401 自动重试
         */
        if (
            request != Request.GENERATE_NONCE &&
            response.is401(body)
        ) {

            println("401 detected, regenerating nonce...")

            generateNonce()

            return makeReq(
                request,
                data,
                signature,
            )
        }

        /**
         * 更新 NONCE
         */
        val newNonce =
            response.headers["NONCE"]
                ?: response.headers["nonce"]

        if (newNonce != null) {

            try {
                nonce = newNonce

                try {
                    auth = CryptUtils.decryptNonce(
                        nonce.take(16)
                            .padEnd(16, '0')
                    )
                } catch (_: Exception) {
                }

            } catch (e: ArrayIndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }

        /**
         * 更新 Session
         */
        val setCookie =
            response.headers["Set-Cookie"]
                ?: response.headers["set-cookie"]

        if (setCookie != null) {

            sessionId =
                setCookie.substringAfter("JSESSIONID=")
                    .substringBefore(";")
        }

        return body
    }

    /**
     * 检测 401
     */
    private fun HttpResponse.is401(body: String): Boolean {

        if (status.value == 401) {
            return true
        }

        return try {

            val xml = Ksoup.parse(body)

            val status =
                xml.firstElementByTagName("FUSBody")
                    ?.firstElementByTagName("Results")
                    ?.firstElementByTagName("Status")
                    ?.text()

            status == "401"

        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Beta 固件探测
 */
object BetaProbe {

    /**
     * 生成下一个 Beta 版本号
     *
     * 示例:
     * ZYF1 -> ZYF2
     * ZYF9 -> ZYFA
     * ZYFA -> ZYFB
     * ZYFZ -> ZYG1
     */
    private fun incrementBetaVersion(
        version: String,
    ): String {

        val chars = version.toCharArray()

        for (i in chars.indices.reversed()) {

            val c = chars[i]

            when {

                c in '0' until '9' -> {
                    chars[i] = c + 1
                    return String(chars)
                }

                c == '9' -> {
                    chars[i] = 'A'
                    return String(chars)
                }

                c in 'A' until 'Y' -> {
                    chars[i] = c + 1
                    return String(chars)
                }

                c == 'Z' -> {

                    chars[i] = '1'

                    for (j in i - 1 downTo 0) {

                        val prev = chars[j]

                        if (prev in 'A' until 'Z') {

                            chars[j] = prev + 1

                            return String(chars)
                        }
                    }
                }
            }
        }

        return version
    }

    /**
     * 扫描 Beta 固件
     */
    suspend fun probeBetas(
        baseVersion: String,
        model: String,
        region: String,
    ): List<String> {

        val found = mutableListOf<String>()

        var current = baseVersion

        println("========== START BETA PROBE ==========")
        println("BASE VERSION: $baseVersion")

        repeat(30) {

            current = incrementBetaVersion(current)

            println("CHECKING: $current")

            try {

                val result =
                    FusClient.getFirmwareInformation(
                        model = model,
                        region = region,
                        version = current,
                    )

                if (result != null) {

                    println("FOUND BETA: $current")

                    if (!found.contains(current)) {
                        found.add(current)
                    }

                } else {

                    println("NOT FOUND: $current")
                }

            } catch (e: Exception) {

                println("ERROR CHECKING $current")

                e.printStackTrace()
            }
        }

        println("========== BETA RESULT ==========")

        found.forEach {
            println("FOUND: $it")
        }

        println("=================================")

        return found
    }
}

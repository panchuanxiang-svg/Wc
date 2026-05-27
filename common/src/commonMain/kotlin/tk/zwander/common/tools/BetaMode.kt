package tk.zwander.common.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

object BetaMode {
    private const val TEST_URL =
        "https://fota-cloud-dn.ospserver.net/firmware/%s/%s/version.test.xml"

    suspend fun getBetaVersions(
        model: String,
        region: String,
    ): List<String> = withContext(Dispatchers.Default) {
        try {
            val url = TEST_URL.format(region, model)

            val request = Request(
                url = url,
                method = "GET",
            )

            val response = FusClient.request(request)

            if (response.isEmpty()) {
                return@withContext emptyList()
            }

            val xml = response.decodeToString()

            val md5List = Regex("<value>(.*?)</value>")
                .findAll(xml)
                .map {
                    it.groupValues[1]
                        .trim()
                        .lowercase()
                }
                .toList()

            if (md5List.isEmpty()) {
                return@withContext emptyList()
            }

            val stableInfo = VersionFetch.getLatestVersion(model, region)

            val stableVersion = stableInfo?.version ?: return@withContext emptyList()

            val split = stableVersion.split("/")

            if (split.size < 3) {
                return@withContext emptyList()
            }

            val apPrefix = split[0].dropLast(6)
            val cscPrefix = split[1].dropLast(5)
            val cpPrefix = split[2].dropLast(6)

            val results = mutableListOf<String>()

            val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

            for (bootloader in chars) {
                for (update in chars) {
                    for (year in chars) {
                        for (month in "ABCDEFGHIJKL") {
                            for (serial in chars) {
                                val suffix =
                                    "$bootloader$update$year$month$serial"

                                val cp =
                                    cpPrefix + "U" + suffix

                                val version =
                                    buildString {
                                        append(apPrefix)
                                        append("U")
                                        append(suffix)

                                        append("/")

                                        append(cscPrefix)
                                        append(suffix)

                                        append("/")

                                        append(cp)
                                    }

                                val md5 = version.md5()

                                if (md5List.contains(md5)) {
                                    results.add(version)
                                }

                                val betaSuffix =
                                    "${bootloader}Z$year$month$serial"

                                val betaVersion =
                                    buildString {
                                        append(apPrefix)
                                        append("U")
                                        append(betaSuffix)

                                        append("/")

                                        append(cscPrefix)
                                        append(betaSuffix)

                                        append("/")

                                        append(cp)
                                    }

                                val betaMd5 = betaVersion.md5()

                                if (md5List.contains(betaMd5)) {
                                    results.add(betaVersion)
                                }
                            }
                        }
                    }
                }
            }

            return@withContext results
                .distinct()
                .sorted()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}

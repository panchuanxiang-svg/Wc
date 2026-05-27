package tk.zwander.common.tools.delegates

import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.common.data.BinaryFileInfo
import tk.zwander.common.tools.BetaMode // 确保添加此导入
import tk.zwander.common.tools.CryptUtils
import tk.zwander.common.tools.FusClient
import tk.zwander.common.tools.Request
import tk.zwander.common.tools.VersionFetch
import tk.zwander.common.util.BifrostSettings
import tk.zwander.common.util.ChangelogHandler
import tk.zwander.common.util.Event
import tk.zwander.common.util.FileManager
import tk.zwander.common.util.eventManager
import tk.zwander.common.util.invoke
import tk.zwander.common.util.streamOperationWithProgress
import tk.zwander.commonCompose.model.DownloadModel
import tk.zwander.samloaderkotlin.resources.MR
import kotlin.time.ExperimentalTime

object Downloader {
    interface DownloadErrorCallback {
        fun onError(info: DownloadErrorInfo)
    }

    data class DownloadErrorInfo(
        val message: String,
        val callback: DownloadErrorConfirmCallback,
    )

    data class DownloadErrorConfirmCallback(
        val onAccept: suspend () -> Unit,
        val onCancel: suspend () -> Unit,
    )

    // ... (onDownload 和 performDownload 代码保持不变) ...

    suspend fun onFetch(
        model: DownloadModel,
        betaMode: Boolean = false,
        incrementalMode: Boolean = false
    ) {
        model.statusText.value = ""
        model.changelog.value = null
        model.osCode.value = ""

        try {
            if (betaMode || incrementalMode) {
                val betaInfo = BetaMode.getBetaInfo(
                    model.model.value,
                    model.region.value
                )

                if (betaInfo?.beta != null) {
                    model.fw.value = betaInfo.beta
                    model.osCode.value = betaInfo.android ?: ""
                    // 修改：更新 changelog 并正常结束任务
                    model.changelog.value = betaInfo.description
                    model.endJob("")
                    return
                }
            }

            val (fw, os, error, output) = VersionFetch.hybridGetLatestVersion(
                model.model.value,
                model.region.value,
            )

            if (error != null) {
                model.endJob(
                    MR.strings.firmwareCheckError(
                        error.message.toString(),
                        output.replace("\t", "  ")
                    )
                )
                return
            }

            model.changelog.value = ChangelogHandler.getChangelog(
                model.model.value,
                model.region.value,
                fw.split("/")[0],
            )

            model.fw.value = fw
            model.osCode.value = os

            model.endJob("")
        } catch (e: Throwable) {
            model.endJob(e.message ?: "Unknown error")
        }
    }
}


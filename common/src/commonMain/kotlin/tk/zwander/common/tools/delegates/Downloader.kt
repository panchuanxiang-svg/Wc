    suspend fun onFetch(
        model: DownloadModel,
        betaMode: Boolean = false,
        incrementalMode: Boolean = false,
        selectedBeta: String? = null // 新增参数：接收 UI 选中的版本
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

                // ✅ 核心修改：优先使用用户选中的版本，否则 fallback 到列表首项
                val selectedVersion = selectedBeta ?: betaInfo?.betaList?.firstOrNull()

                if (selectedVersion != null) {
                    model.fw.value = selectedVersion
                    model.osCode.value = betaInfo?.android ?: ""
                    
                    // 更新 changelog
                    model.changelog.value = betaInfo?.description
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

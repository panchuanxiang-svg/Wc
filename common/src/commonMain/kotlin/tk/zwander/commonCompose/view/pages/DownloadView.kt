@ExperimentalTime
@Composable
internal fun DownloadView() {
    val model = LocalDownloadModel.current
    val hasRunningJobs by model.hasRunningJobs.collectAsState(false)
    var manual by model.manual.collectAsMutableState()

    var betaMode by remember { mutableStateOf(false) }
    var incrementalMode by remember { mutableStateOf(false) }

    val modelModel by model.model.collectAsState()
    val region by model.region.collectAsState()
    val fw by model.fw.collectAsState()

    // ✅ 先拿 betaInfo（必须在 selectedBeta 前）
    val betaInfo by produceState<BetaInfo?>(initialValue = null, modelModel, region) {
        value = null
        if (modelModel.isNotBlank() && region.isNotBlank()) {
            value = BetaMode.getBetaInfo(modelModel, region)
        }
    }

    // ✅ 选择状态（只跟机型/地区绑定）
    var selectedBeta by remember(modelModel, region) {
        mutableStateOf<String?>(null)
    }

    val canCheckVersion =
        !manual && modelModel.isNotBlank() && region.isNotBlank() && !hasRunningJobs

    val canDownload =
        modelModel.isNotBlank() && region.isNotBlank() && fw.isNotBlank() && !hasRunningJobs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {

        HybridButton(
            onClick = {
                model.launchJob {
                    Downloader.onFetch(
                        model = model,
                        betaMode = betaMode,
                        incrementalMode = incrementalMode,
                        selectedBeta = selectedBeta
                    )
                }
            },
            enabled = canCheckVersion,
            text = "检查更新"
        )

        Spacer(Modifier.height(8.dp))

        // ✅ Beta 列表 UI
        AnimatedVisibility(visible = betaMode && betaInfo?.betaList?.isNotEmpty() == true) {
            Column {

                Text(text = "Beta 版本列表")

                Spacer(Modifier.height(6.dp))

                betaInfo?.betaList?.forEach { version ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBeta = version }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = selectedBeta == version,
                            onCheckedChange = { checked ->
                                selectedBeta = if (checked) version else null
                            }
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(text = version)
                    }
                }
            }
        }
    }
}

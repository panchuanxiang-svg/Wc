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

    // ✅ 关键：选中版本（随机型/地区变化重置）
    var selectedBeta by remember(modelModel, region) {
        mutableStateOf<String?>(null)
    }

    // ✅ 拉取 Beta 数据（随机型/地区刷新）
    val betaInfo by produceState<BetaInfo?>(initialValue = null, modelModel, region) {
        value =
            if (modelModel.isNotBlank() && region.isNotBlank()) {
                BetaMode.getBetaInfo(modelModel, region)
            } else null
    }

    val canCheckVersion =
        !manual && modelModel.isNotBlank() && region.isNotBlank() && !hasRunningJobs

    val canDownload =
        modelModel.isNotBlank() && region.isNotBlank() && fw.isNotBlank() && !hasRunningJobs

    val canChangeOption = !hasRunningJobs

    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {

        // =========================
        // 按钮区
        // =========================

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
            text = "检查更新",
            description = "获取最新版本",
            vectorIcon = painterResource(MR.images.refresh),
            parentSize = 1000
        )

        Spacer(Modifier.height(8.dp))

        HybridButton(
            onClick = {
                model.launchJob {
                    Downloader.onDownload(model)
                }
            },
            enabled = canDownload,
            text = "下载",
            description = "下载固件",
            vectorIcon = painterResource(MR.images.download),
            parentSize = 1000
        )

        Spacer(Modifier.height(12.dp))

        // =========================
        // Beta / 增量选择
        // =========================

        Row(verticalAlignment = Alignment.CenterVertically) {

            Checkbox(
                checked = betaMode,
                onCheckedChange = {
                    betaMode = it
                    if (it) incrementalMode = false
                }
            )
            Text("Beta 测试版本")

            Spacer(Modifier.width(12.dp))

            Checkbox(
                checked = incrementalMode,
                onCheckedChange = {
                    incrementalMode = it
                    if (it) betaMode = false
                }
            )
            Text("增量更新")
        }

        Spacer(Modifier.height(10.dp))

        // =========================
        // Beta 列表 UI（核心）
        // =========================

        AnimatedVisibility(
            visible = betaMode && betaInfo?.betaList.isNullOrEmpty().not()
        ) {

            Column {

                Text("可选 Beta 版本")

                Spacer(Modifier.height(6.dp))

                betaInfo?.betaList?.forEach { version ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedBeta = version
                            }
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

        // =========================
        // 提示信息
        // =========================

        if (betaMode && betaInfo?.betaList.isNullOrEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("暂无 Beta 版本", color = MaterialTheme.colorScheme.error)
        }
    }
}

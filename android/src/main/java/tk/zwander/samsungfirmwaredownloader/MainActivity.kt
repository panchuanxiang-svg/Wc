package tk.zwander.samsungfirmwaredownloader

import android.content.*
import android.os.*
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import kotlinx.atomicfu.atomic
import tk.zwander.common.IDownloaderService
import tk.zwander.common.data.SmartBinaryInfo
import tk.zwander.common.tools.VersionFetch

class MainActivity : FragmentActivity(),
    CoroutineScope,
    ServiceConnection {

    private val job = Job()
    override val coroutineContext = Dispatchers.Main + job

    private val service = atomic<IDownloaderService?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            var list by remember { mutableStateOf<List<SmartBinaryInfo>>(emptyList()) }
            var downloading by remember { mutableStateOf<String?>(null) }

            // 🔥 初始化加载固件（只走 GitHub + FUS 合并入口）
            LaunchedEffect(Unit) {
                try {
                    list = VersionFetch.fetchGithubFirmware()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {

                Text(
                    text = "固件列表",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list) { item ->

                        val isDownloading = item.swVersion == downloading

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    downloading = item.swVersion
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Column(modifier = Modifier.weight(1f)) {

                                Text(text = item.swVersion)

                                if (isDownloading) {
                                    Text(
                                        text = "Downloading...",
                                        color = Color.Red
                                    )
                                }

                                if (item.category == "GITHUB") {
                                    Text(
                                        text = "测试版",
                                        color = Color.Gray
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    downloading = item.swVersion

                                    val s = service.value
                                    if (s != null) {
                                        launch {
                                            try {
                                                s.startDownload(item.swVersion)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(if (isDownloading) "下载中" else "下载")
                            }
                        }

                        Divider()
                    }
                }
            }
        }
    }

    // 🔥 Service绑定（必须，否则下载不会触发）
    override fun onResume() {
        super.onResume()
        DownloaderService.bind(this, this)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service.value = IDownloaderService.Stub.asInterface(binder)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service.value = null
    }
    }

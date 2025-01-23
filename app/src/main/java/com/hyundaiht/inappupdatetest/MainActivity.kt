package com.hyundaiht.inappupdatetest

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.hyundaiht.inappupdatetest.ui.theme.InAppUpdateTestTheme

class MainActivity : ComponentActivity() {
    private var handler: Handler? = null
    private val tag = javaClass.simpleName
    private val byteState = mutableStateOf<String>("")

    private val callback =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // handle callback
            handler?.post {
                Toast.makeText(
                    this@MainActivity,
                    "inAppUpdate resultCode = ${result.resultCode}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val retryCallback =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // handle callback
            handler?.post {
                Toast.makeText(
                    this@MainActivity,
                    "retryInAppUpdate resultCode = ${result.resultCode}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(mainLooper)

        enableEdgeToEdge()
        setContent {
            InAppUpdateTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        val rememberByte by remember { byteState }

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .background(Color.Gray), text = getVersionInfo()
                        )

                        Text(modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(Color.Yellow), text = rememberByte)

                        Button(onClick = {
                            registerAppUpdateInfoListener()
                        }) {
                            Text("registerAppUpdateInfoListener 실행")
                        }

                        Button(onClick = {
                            checkAppUpdateInfo()
                        }) {
                            Text("checkAppUpdateInfo 실행")
                        }

                        Button(onClick = {
                            checkAppUpdateInfoOption()
                        }) {
                            Text("checkAppUpdateInfoOption 실행")
                        }

                        Button(onClick = {
                            executeInAppUpdate()
                        }) {
                            Text("executeInAppUpdate 실행")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        retryInAppUpdate()
    }

    private fun retryInAppUpdate() {
        val context: Context = this@MainActivity
        val appUpdateManager = AppUpdateManagerFactory.create(context)

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        retryCallback,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
    }

    private fun getVersionInfo(): String {
        val info: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        val version = info.versionName
        return "$version Version"
    }

    private fun registerAppUpdateInfoListener() {
        val context: Context = this@MainActivity
        val appUpdateManager = AppUpdateManagerFactory.create(context)

        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                byteState.value = "$bytesDownloaded/$totalBytesToDownload"
            } else {
                byteState.value = "InstallStateUpdatedListener Not DOWNLOADING"
            }
        }

        appUpdateManager.registerListener(listener)
    }

    private fun checkAppUpdateInfo() {
        val context: Context = this@MainActivity
        val appUpdateManager = AppUpdateManagerFactory.create(context)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val updateAvailability = appUpdateInfo.updateAvailability()
            val isUpdateTypeAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            Toast.makeText(
                context,
                "appUpdateInfo updateAvailability = $updateAvailability, isUpdateTypeAllowed = $isUpdateTypeAllowed",
                Toast.LENGTH_SHORT
            ).show()
            /* if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                 // This example applies an immediate update. To apply a flexible update
                 // instead, pass in AppUpdateType.FLEXIBLE
                 && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
             ) {
                 // Request the update.
                 Toast.makeText(context, "checkAppUpdateInfo UPDATE_AVAILABLE && IMMEDIATE", Toast.LENGTH_SHORT).show()
             } else {
                 Toast.makeText(context, "checkAppUpdateInfo else", Toast.LENGTH_SHORT).show()
             }*/
        }
    }

    private fun checkAppUpdateInfoOption() {
        val context: Context = this@MainActivity
        val appUpdateManager = AppUpdateManagerFactory.create(context)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks whether the platform allows the specified type of update,
        // and current version staleness.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val clientVersionStalenessDays = appUpdateInfo.clientVersionStalenessDays()
            val updatePriority = appUpdateInfo.updatePriority()
            Toast.makeText(
                context,
                "$clientVersionStalenessDays,$updatePriority",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun executeInAppUpdate() {
        val context: Context = this@MainActivity
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    callback,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            } else {
                Toast.makeText(context, "executeInAppUpdate failure", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
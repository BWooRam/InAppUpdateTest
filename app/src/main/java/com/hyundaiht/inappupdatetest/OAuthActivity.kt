package com.hyundaiht.inappupdatetest

import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.hyundaiht.inappupdatetest.ui.theme.InAppUpdateTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class OAuthActivity : ComponentActivity() {
    private var handler: Handler? = null
    private val tag = javaClass.simpleName

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        Log.d(tag, "onNewIntent intent data = ${intent.data}")
        Log.d(tag, "onNewIntent intent code = ${intent.getStringExtra("code")}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate intent data = ${intent.data}")
        Log.d(tag, "onCreate intent code = ${intent.getStringExtra("code")}")
        handler = Handler(mainLooper)
        enableEdgeToEdge()
        setContent {
            InAppUpdateTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(onClick = {
                            startOAuthLogin(this@OAuthActivity)
                        }) {
                            Text("get OAuth2.0 getInitToken 실행")
                        }
                        OAuthScreen(AUTH_URL)
                    }
                }
            }
        }
    }

    private val CLIENT_ID =
        "1060536707451-ngcq9j1lmlog8ber0kvnm2n48396ueji.apps.googleusercontent.com"
    private val REDIRECT_URI = "com.hyundaiht.inappupdatetest:/oauthredirect"
    private val AUTH_URL = "https://accounts.google.com/o/oauth2/auth" +
            "?client_id=$CLIENT_ID" +
            "&redirect_uri=$REDIRECT_URI" +
            "&response_type=code" +
            "&scope=https://www.googleapis.com/auth/androidpublisher"

    private fun startOAuthLogin(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private var code: String? = null

    @Composable
    fun OAuthScreen(url: String) {
        val currentUrl by remember { mutableStateOf(url) }
        var isOAuthComplete by remember { mutableStateOf(false) }

        if (isOAuthComplete) {
            OAuthWebView(url = "https://accounts.google.com/o/oauth2/token", isPost = true, onUrlChange = { newUrl ->
                if (newUrl.startsWith("com.hyundaiht.inappupdatetest:/oauthredirect")) {
                    Log.d(tag, "onUrlChange start")
                }
            })
        } else {
            OAuthWebView(url = currentUrl, isPost = false, onUrlChange = { newUrl ->
                if (newUrl.startsWith("com.hyundaiht.inappupdatetest:/oauthredirect")) {
                    val authCode = Uri.parse(newUrl).getQueryParameter("code")
                    if (!authCode.isNullOrEmpty()) {
                        Log.d(tag, "Auth Code: $authCode")
                        code = authCode
                        isOAuthComplete = true
                    }
                }
            })
        }
    }

    @Composable
    fun OAuthWebView(url: String, isPost: Boolean, onUrlChange: (String) -> Unit) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            request?.url?.toString()?.let { newUrl ->
                                onUrlChange(newUrl) // OAuth 리디렉션 감지
                            }
                            return false
                        }
                    }


                    if (isPost) {
                        val postData = "grant_type=authorization_code&" +
                                "code=$code&" +
                                "client_id=$CLIENT_ID&"+
                                "redirect_uri=$REDIRECT_URI"
                        postUrl(url, postData.toByteArray())
                    } else
                        loadUrl(url)
                }


            }
        )
    }

}
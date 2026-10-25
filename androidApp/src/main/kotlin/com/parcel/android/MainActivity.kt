package com.parcel.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.parcel.presentation.ParcelApp
import com.parcel.presentation.LocalParcelAds
import androidx.compose.runtime.CompositionLocalProvider
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    private lateinit var ads: AndroidParcelAds
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        get<PayUSandboxGateway>().attach(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(10, 15, 28)),
        )
        ads = AndroidParcelAds(this)
        ads.start()
        setContent { CompositionLocalProvider(LocalParcelAds provides ads) { ParcelApp() } }
    }
    override fun onDestroy() {
        ads.dispose()
        get<PayUSandboxGateway>().detach(this)
        super.onDestroy()
    }
}

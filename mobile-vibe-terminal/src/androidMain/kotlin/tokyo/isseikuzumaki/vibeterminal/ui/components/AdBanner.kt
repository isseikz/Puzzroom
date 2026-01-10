package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import tokyo.isseikuzumaki.vibeterminal.BuildConfig

/**
 * バナー広告を表示するComposable
 *
 * ConnectionListScreenの下部に配置して使用する。
 * 広告の読み込みに失敗した場合は何も表示しない。
 *
 * @param modifier Modifier
 * @param adUnitId 広告ユニットID（BuildConfigから取得、環境変数で上書き可能）
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
) {
    var isAdLoaded by remember { mutableStateOf(false) }
    var adView by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            adView?.destroy()
        }
    }

    if (isAdLoaded || adView == null) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                    adView = this
                }
            },
            modifier = modifier.fillMaxWidth()
        )
    }
}

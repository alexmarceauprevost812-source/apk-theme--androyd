package ca.lexos.demon

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        web = WebView(this)
        web.setBackgroundColor(Color.BLACK)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Pont(), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    // Bouton retour : ferme l'écran SMS au lieu de quitter l'accueil
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("window.retourAndroid && window.retourAndroid()", null)
    }

    inner class Pont {
        /** Ouvre Messages avec le numéro et le texte, après l'animation du démon. */
        @JavascriptInterface
        fun envoyerSms(numero: String, texte: String) {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(numero)))
            i.putExtra("sms_body", texte)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runOnUiThread { try { startActivity(i) } catch (_: Exception) {} }
        }

        @JavascriptInterface
        fun vibrer(ms: Int) {
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms.toLong())
        }

        @JavascriptInterface
        fun ouvrirAppli(nom: String) {
            val intent = when (nom) {
                "appel" -> Intent(Intent.ACTION_DIAL)
                "photo" -> Intent("android.media.action.STILL_IMAGE_CAMERA")
                else -> null
            } ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runOnUiThread { try { startActivity(intent) } catch (_: Exception) {} }
        }
    }
}

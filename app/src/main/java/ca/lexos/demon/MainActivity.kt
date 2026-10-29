package ca.lexos.demon

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var pret = false

    // Recharge la liste quand une appli est installée ou désinstallée
    private val surveilleApplis = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { js("window.lexos && lexos.rechargerApplis()") }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        web = WebView(this)
        web.setBackgroundColor(Color.BLACK)
        web.overScrollMode = View.OVER_SCROLL_NEVER
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { pret = true }
        }
        web.addJavascriptInterface(Pont(), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(surveilleApplis, f)
    }

    override fun onDestroy() {
        try { unregisterReceiver(surveilleApplis) } catch (_: Exception) {}
        super.onDestroy()
    }

    // Bouton Accueil pendant qu'on est déjà dans le lanceur : retour à l'accueil
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        js("window.lexos && lexos.accueil()")
    }

    override fun onResume() {
        super.onResume()
        if (pret) js("window.lexos && lexos.reveil()")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        js("window.lexos && lexos.retour()")
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }

    private fun lancer(i: Intent) = runOnUiThread {
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) } catch (_: Exception) {}
    }

    private fun iconeBase64(pkg: String): String = try {
        val d = packageManager.getApplicationIcon(pkg)
        val taille = 96
        val b = Bitmap.createBitmap(taille, taille, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, taille, taille)
        d.draw(c)
        val o = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.PNG, 100, o)
        "data:image/png;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) { "" }

    inner class Pont {

        /** Liste toutes les applis du tiroir. Répond via lexos.recevoirApplis(json). */
        @JavascriptInterface
        fun listerApplis() {
            thread {
                val pm = packageManager
                val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val liste = if (Build.VERSION.SDK_INT >= 33)
                    pm.queryIntentActivities(i, PackageManager.ResolveInfoFlags.of(0))
                else @Suppress("DEPRECATION") pm.queryIntentActivities(i, 0)

                val arr = JSONArray()
                liste.filter { it.activityInfo.packageName != packageName }
                    .sortedBy { it.loadLabel(pm).toString().lowercase() }
                    .forEach {
                        val pkg = it.activityInfo.packageName
                        arr.put(JSONObject().apply {
                            put("nom", it.loadLabel(pm).toString())
                            put("pkg", pkg)
                            put("icone", iconeBase64(pkg))
                        })
                    }
                val json = JSONObject.quote(arr.toString())
                js("lexos.recevoirApplis(JSON.parse($json))")
            }
        }

        @JavascriptInterface
        fun ouvrir(pkg: String) {
            packageManager.getLaunchIntentForPackage(pkg)?.let { lancer(it) }
        }

        @JavascriptInterface
        fun infosAppli(pkg: String) {
            lancer(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
        }

        @JavascriptInterface
        fun desinstaller(pkg: String) {
            lancer(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
        }

        @JavascriptInterface
        fun envoyerSms(numero: String, texte: String) {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(numero)))
            i.putExtra("sms_body", texte)
            lancer(i)
        }

        @JavascriptInterface
        fun ouvrirSysteme(nom: String) {
            val i = when (nom) {
                "appel" -> Intent(Intent.ACTION_DIAL)
                "sms" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
                "photo" -> Intent("android.media.action.STILL_IMAGE_CAMERA")
                "web" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
                "parametres" -> Intent(Settings.ACTION_SETTINGS)
                "accueil" -> Intent(Settings.ACTION_HOME_SETTINGS)
                "fond" -> Intent(Intent.ACTION_SET_WALLPAPER)
                "fondanime" -> Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DemonWallpaper::class.java))
                "goodlock" -> Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/com.samsung.android.goodlock"))
                "clavier" -> Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                "affichage" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                else -> null
            } ?: return
            lancer(i)
        }

        @JavascriptInterface
        fun batterie(): String {
            val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val niv = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val max = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val etat = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charge = etat == BatteryManager.BATTERY_STATUS_CHARGING || etat == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (niv >= 0) niv * 100 / max else -1
            return "{\"pct\":$pct,\"charge\":$charge}"
        }

        @JavascriptInterface
        fun vibrer(ms: Int) {
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms.toLong())
        }

        @JavascriptInterface
        fun estAccueilParDefaut(): Boolean {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val r = packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
            return r?.activityInfo?.packageName == packageName
        }
    }
}

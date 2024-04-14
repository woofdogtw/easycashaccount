package tw.idv.woofdog.easycashaccount.utils

import java.io.File
import java.time.Instant

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract

class Utils {
    companion object {
        // SharedPreferences keys.
        const val SP_SYNC_TYPE = "syncType"
        const val SP_SYNC_FTP_HOSTNAME = "syncFtpHostName"
        const val SP_SYNC_FTP_PORT = "syncFtpPort"
        const val SP_SYNC_FTP_USERNAME = "syncFtpUserName"
        const val SP_SYNC_FTP_PASSWORD = "syncFtpPassword"
        const val SP_SYNC_FTP_FILE_DIR = "syncFtpFileDir"
        const val SP_SYNC_GOOGLE_REM = "syncGoogleRem"
        const val SP_SYNC_GOOGLE_FILE_DIR = "syncGoogleFileDir"
        const val SP_SYNC_GOOGLE_REFRESH_TOKEN = "syncGoogleRefresh"
        const val SP_SYNC_MS_REM = "syncMsRem"
        const val SP_SYNC_MS_FILE_DIR = "syncMsFileDir"
        const val SP_SYNC_MS_REFRESH_TOKEN = "syncMsRefresh"
        const val SP_CUR_VERSION = "curVersion"

        const val LEGACY_APP_DIR = "tw.idv.woofdog.easycashaccount/databases"

        fun getCurrentTimeEpoch(): Long {
            return Instant.now().epochSecond
        }

        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("easycashaccount", 0)
        }

        fun getPackageVersion(context: Context): String {
            return context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }

        fun checkOldDbExist(): String? {
            val externStoragePath = Environment.getExternalStorageDirectory().absolutePath
            val oldDbPath = "$externStoragePath/$LEGACY_APP_DIR"
            return if (File(oldDbPath).exists()) oldDbPath else null
        }

        fun getUriForOpenDocumentTree(context: Context, filePath: String): Uri? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return null
            }

            val f = File(filePath)
            return (context.getSystemService(Context.STORAGE_SERVICE) as StorageManager).let {
                it.getStorageVolume(f)?.createOpenDocumentTreeIntent()?.run {
                    val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI)
                    }
                    val path = filePath.replace("/", "%2F")
                    Uri.parse("${uri.toString().replace("/root/", "/document/")}%3A$path")
                }
            }
        }

        fun formatMoney(v: Double): String {
            return "%.2f".format(v).removeSuffix(".00").removeSuffix(".10").removeSuffix(".20")
                .removeSuffix(".30").removeSuffix(".40").removeSuffix(".50").removeSuffix(".60")
                .removeSuffix(".70").removeSuffix(".80").removeSuffix(".90")
        }
    }
}

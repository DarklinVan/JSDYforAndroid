
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class FileDownloader(
    private val onProgress: ((Int) -> Unit)? = null,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Exception) -> Unit)? = null,
    private val context: Context
) {

    private var destinationPath: String? = null
    private var destinationFileName:String? =null
    private var downloadDir:File? =null
    private var url: String? = null

    fun download(url: String, destinationFileName: String) {
        this.destinationFileName = destinationFileName
        this.url = url
        downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DY")
        if (!downloadDir?.exists()!!) downloadDir?.mkdirs()
        this.destinationPath = "${downloadDir?.absolutePath}/$destinationFileName"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = downloadFile()
                withContext(Dispatchers.Main) {
                    if (result) {
                        onSuccess?.invoke()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFailure?.invoke(e)
                }
            }
        }
    }

    private suspend fun downloadFile(): Boolean {
        var connection: HttpURLConnection? = null
        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            val url = URL(this.url)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP response code: ${connection.responseCode}")
            }

            val fileLength = connection.contentLength
            input = connection.inputStream

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, destinationFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "file/*")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/DY")
                }

                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let{
                    output = context.contentResolver.openOutputStream(it) as FileOutputStream?
                    val data = ByteArray(1024)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count.toLong()
                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress?.invoke(progress)
                            }
                        }
                        output?.write(data, 0, count)
                    }
                }
            } else {
                output = FileOutputStream(destinationPath)
                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress?.invoke(progress)
                        }
                    }
                    output?.write(data, 0, count)
                }
            }
            return true
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                output?.close()
                input?.close()
                connection?.disconnect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

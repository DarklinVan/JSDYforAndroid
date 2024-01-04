import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class FileDownloader(
    private val context: Context,

    private val onProgress: ((Int) -> Unit)? = null,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Exception) -> Unit)? = null
) : AsyncTask<Void, Int, Boolean>() {

    private var destinationPath: String? = null
    private var url: String? = null
    init {

    }

    override fun doInBackground(vararg params: Void?): Boolean {
        var connection: HttpURLConnection? = null
        var input: InputStream? = null
        var output: FileOutputStream? = null

        return try {
            val url = URL(url)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP response code: ${connection.responseCode}")
            }

            val fileLength = connection.contentLength

            input = connection.inputStream
            output = FileOutputStream(destinationPath)

            val data = ByteArray(1024)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                if (isCancelled) {
                    return false
                }

                total += count.toLong()
                if (fileLength > 0) {
                    publishProgress((total * 100 / fileLength).toInt())
                }

                output.write(data, 0, count)
            }

            true
        } catch (e: Exception) {
            onFailure?.invoke(e)
            false
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

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        values[0]?.let { onProgress?.invoke(it) }
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        if (result) {
            onSuccess?.invoke()
        }
    }
    fun download(url: String,destinationFileName: String){
        this.url = url
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Douyin")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        this.destinationPath = "$downloadDir/$destinationFileName"

        execute()
    }
}

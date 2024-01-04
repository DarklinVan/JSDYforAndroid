package com.darklinvan.jsdy

import FileDownloader
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.widget.Toast
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.FormBody
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private var buttonFlag: Boolean = false
    private val apiURL = "http://60.204.242.29:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.editTextTextMultiLine)
        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)
        progressBar = findViewById(R.id.progressBar)
        button.isEnabled = buttonFlag
        button.setOnClickListener {
            // 当按钮被点击时执行的操作
            val inputText = editText.text.toString()
            if(inputText.isNullOrBlank()){
                showToast("链接不能为空")
            }else{
                editText.text.clear()
                val pattern = "https://v.douyin.com/[a-zA-Z0-9]+/?".toRegex()
                val urls = pattern.findAll(inputText).map { it.value }.toList()
                val requestBody = FormBody.Builder()
                    .add("urls", urls.joinToString(separator = ","))
                    .build()
                HttpManager.post("$apiURL/parse", requestBody, object : HttpManager.HttpCallback {
                    override fun onSuccess(response: HttpManager.HttpResponse) {
                        println("Status Code: ${response.statusCode}")
                        println("Headers: ${response.headers}")
                        println("Body: ${response.body}")
                        if (response != null) {

                            //appendTextView("响应体：${response.body}")
                            val r = GsonBuilder().setLenient().create().fromJson(response.body,APIResponse::class.java)
                            println(r)
                            if(r.success){
                                for (item in r.itemList){
                                    if(!(item.error?:"").isNullOrBlank()){
                                        item.error?.let { it1 -> appendTextView(it1) }
                                        continue
                                    }else{
                                        appendTextView("检测到${if(item.type==0) "视频,链接为${item.url}" else "图片,数量为${item.imgs!!.size}"}")
                                        appendTextView("正在准备下载")
                                        if(item.type==0){
                                            val onProgressUpdate: (Int) -> Unit = { progress ->
                                                progressBar.post { progressBar.progress = progress }
                                            }
                                            // 下载成功的回调
                                            val onSuccessCallback: () -> Unit = {
                                                println("Download completed successfully.")
                                                appendTextView("下载完成,保存位置为:/storage/emulated/0/Download/dy")
                                            }
                                            // 下载失败的回调
                                            val onFailureCallback: (Exception) -> Unit = { exception ->
                                                println("Download failed. Exception: $exception")
                                                appendTextView("下载失败，错误代码: $exception")
                                            }
                                            appendTextView("正在下载视频")
                                            val fileDownloader = FileDownloader(
                                                context = applicationContext,
                                                onProgress = onProgressUpdate,
                                                onSuccess = onSuccessCallback ,
                                                onFailure = onFailureCallback
                                            )
                                            val fileUrl = item.url
                                            val destinationPath = "${item.itemId}.mp4"


                                            fileDownloader.download(fileUrl!!,destinationPath)
                                        }else if(item.type==1){
                                            var total = item.imgs!!.size
                                            // 下载成功的回调
                                            val onSuccessCallback: () -> Unit = {
                                                println("Download completed successfully.")
                                            }
                                            // 下载失败的回调
                                            val onFailureCallback: (Exception) -> Unit = { exception ->
                                                println("Download failed. Exception: $exception")
                                                appendTextView("下载失败，错误代码: $exception")
                                            }


                                            appendTextView("正在下载图片")
                                            for ((i, img) in item.imgs!!.withIndex()){
                                                println("Downloading Pic ${i+1}/$total")
                                                val destinationPath = "${item.itemId}_$i.jpg"
                                                val progress = ((i+1)/total)*100
                                                progressBar.post{progressBar.progress= progress}
                                                val fileDownloader = FileDownloader(
                                                    context = applicationContext,
                                                    onSuccess = onSuccessCallback ,
                                                    onFailure = onFailureCallback
                                                )
                                                fileDownloader.download(img.url, destinationPath)
                                            }
                                            appendTextView("图片下载结束")
                                        }


                                    }
                                }
                            }
                        }
                    }

                    override fun onError(error: String) {
                        println("Error: $error")
                        appendTextView("错误：${error}")
                    }

                    override fun onCompleted() {
                        button.post{button.isEnabled=true}
                    }
                })

            }

        }
        init()
    }
    private fun init(){
        appendTextView("正在进行初始化,请稍后......")
        appendTextView("初始化完毕，正在检测服务器连接状况")
        HttpManager.get(apiURL, object : HttpManager.HttpCallback {
            override fun onSuccess(response: HttpManager.HttpResponse) {
                println("Status Code: ${response.statusCode}")
                println("Headers: ${response.headers}")
                println("Body: ${response.body}")
                if (response != null) {
                    if(response.statusCode==200){
                        appendTextView("连接服务器成功")
                    }else{
                        appendTextView("连接服务器失败\n状态码:${response.statusCode}\n响应内容:${response.body}")
                    }
                }else{
                    appendTextView("收到空响应。")
                }
            }

            override fun onError(error: String) {
                println("Error: $error")
                appendTextView("错误：${error}")
            }
            override fun onCompleted() {
                button.post{button.isEnabled=true}
            }
        })
    }
    private fun appendTextView(text: String) {
        // 更新 TextView 中的文本
        textView.post{
            textView.append("$text\n")
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

package com.darklinvan.jsdy

import FileDownloader
import HttpManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.gson.GsonBuilder
import okhttp3.FormBody

class MainActivity : ComponentActivity() {

    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private var buttonFlag: Boolean = false
    private var apiURL:String = "https://jsdy.xduinfo.top"
    private lateinit var sharedPreferences:SharedPreferences

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
            commit()
        }
        editText.setOnKeyListener { v, keyCode, event ->
            // 检查按下的键是否是“Enter”键并且是按下动作
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                commit()
                true // 返回 true 表示事件已被处理
            } else {
                false // 返回 false 表示事件未被处理，继续传递事件
            }
        }
        init()
    }
    private fun init(){
        sharedPreferences = getSharedPreferences("JSDY", Context.MODE_PRIVATE)
        apiURL = sharedPreferences.getString("API", "https://jsdy.xduinfo.top")!!
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
    private fun commit(){
        val inputText = editText.text.toString().trim()
        if(inputText.isNullOrBlank()){
            showToast("链接不能为空")
        }else {
            if(inputText.startsWith("\\")){
                parseCommand(inputText)
            }else{
                main(inputText)
            }
        }

    }
    private fun parseCommand(input: String){
        val parts = input.split(" ")
        val name = parts.firstOrNull()?.substring(1) ?: ""

        val params = mutableListOf<String>()
        val options = mutableMapOf<String, String>()

        // 解析参数和选项
        var index = 1
        while (index < parts.size) {
            val part = parts[index]

            if (part.startsWith("-")) {
                // 解析选项
                val option = part.substring(1)
                if (index + 1 < parts.size) {
                    options[option] = parts[index + 1]
                    index++
                }
            } else {
                // 解析参数
                params.add(part)
            }

            index++
        }
        val command = Commands(name, params, options)
        println(command.params.toString())
        appendTextView(input)
        when(command.name){
            "help"->{
                appendTextView(getString(R.string.help))
            }
            "setapi"->{
                if(command.params.size != 1 || command.options.isNotEmpty()){
                    appendTextView("您输入的指令有误，参数过多或过少")
                }else{
                    val editor = sharedPreferences.edit()
                    editor.putString("API", command.params[0])
                    editor.apply()
                    appendTextView("新的API地址已修改为:$apiURL")
                    init()
                }
                 // 存储一个字符串

            }
            "clear"->{
                textView.text = ""
            }
            else ->{
                appendTextView("您输入的指令有误")
            }
        }
    }

    private fun main(inputText:String){
        editText.text.clear()
        val pattern = "https://v.douyin.com/[a-zA-Z0-9]+/?".toRegex()
        val urls = pattern.findAll(inputText).map { it.value }.toList()
        if(urls.isEmpty()){
            return
        }
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
                                        appendTextView("下载完成,保存位置为:/storage/emulated/0/Download/DY")
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

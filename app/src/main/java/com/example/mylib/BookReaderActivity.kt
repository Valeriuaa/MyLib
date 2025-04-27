package com.example.mylib

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Website.URL
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.internal.bind.TypeAdapters.URL
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import kotlin.concurrent.thread

class BookReaderActivity : AppCompatActivity() {
    private lateinit var bookWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убираем панель навигации и делаем полноэкранный режим
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContentView(R.layout.activity_book_reader)

        bookWebView = findViewById(R.id.bookWebView)
        setupWebView()

        val bookTitle = intent.getStringExtra("BOOK_TITLE")
        if (!bookTitle.isNullOrEmpty()) {
            fetchBookLink(bookTitle)
        }
    }

    private fun setupWebView() {
        bookWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        bookWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e("WebView", "Ошибка загрузки: ${error?.description}")
            }
        }
    }

    private fun fetchBookLink(title: String) {
        thread {
            try {
                val apiKey = "AIzaSyDjkmHuXtJRPtNC6ZDDl_V1FoOXJVkSNy8"
                val query = title.replace(" ", "+")
                val searchUrl = "https://www.googleapis.com/books/v1/volumes?q=$query&key=$apiKey"

                val url = URL(searchUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.use { it.readText() }

                val jsonObject = JSONObject(responseText)
                val items = jsonObject.optJSONArray("items")

                if (items != null && items.length() > 0) {
                    val firstBook = items.getJSONObject(0)
                    val accessInfo = firstBook.optJSONObject("accessInfo")
                    val webReaderLink = accessInfo?.optString("webReaderLink", "")

                    runOnUiThread {
                        if (!webReaderLink.isNullOrEmpty()) {
                            val finalUrl = webReaderLink.replace("http://", "https://")
                            bookWebView.loadUrl(finalUrl)
                        } else {
                            Log.e("BookReaderActivity", "Книга найдена, но нет webReaderLink!")
                        }
                    }
                } else {
                    runOnUiThread {
                        Log.e("BookReaderActivity", "Книга не найдена в Google Books API!")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("BookReaderActivity", "Ошибка загрузки книги: ${e.message}")
                }
            }
        }
    }
}

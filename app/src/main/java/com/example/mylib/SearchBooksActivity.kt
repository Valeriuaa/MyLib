package com.example.mylib

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchBooksActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var btnBack: ImageButton
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bookAdapter: BookAdapter
    private lateinit var requestQueue: RequestQueue

    private var isLoading = false
    private var currentPage = 0
    private val pageSize = 10
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_books)



        val imgBtnBack = findViewById<ImageButton>(R.id.btnBack)
        val edtSearch = findViewById<EditText>(R.id.searchEditText)
        val btnSearch = findViewById<Button>(R.id.searchButton)
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        val txt1 = findViewById<TextView>(R.id.emptyTextView)








        // Загружаем тему
        val sharedPref = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val bgColor = sharedPref.getInt("bg_color", Color.WHITE)
        val textColor = sharedPref.getInt("text_color", Color.BLACK)


        val fltBtnBack = sharedPref.getString("flt_btn", "rounded_button")
        val fltBtn = resources.getIdentifier(fltBtnBack, "drawable", packageName)

        val edtTextBack = sharedPref.getString("edt_text", "rounded_edittext")
        val edtText = resources.getIdentifier(edtTextBack, "drawable", packageName)

        if (fltBtn != 0) {
            imgBtnBack?.setBackgroundResource(fltBtn)
            btnSearch?.setBackgroundResource(fltBtn)
        }

        if (edtText != 0) {
            edtSearch?.setBackgroundResource(edtText)
        }
        edtSearch?.setBackgroundResource(edtText)
        mainLayout?.setBackgroundColor(bgColor)
        txt1?.setTextColor(textColor)
        imgBtnBack?.setBackgroundResource(fltBtn)
        btnSearch?.setBackgroundResource(fltBtn)




        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        btnBack = findViewById(R.id.btnBack)
        booksRecyclerView = findViewById(R.id.recyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        progressBar = findViewById(R.id.progressBar)

        booksRecyclerView.layoutManager = LinearLayoutManager(this)
        requestQueue = Volley.newRequestQueue(this)
        bookAdapter = BookAdapter(mutableListOf(), true, false, this) // Передаем true для режима поиска

        booksRecyclerView.adapter = bookAdapter

        btnBack.setOnClickListener {
            finish()
        }


        searchButton.setOnClickListener {
            query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                resetSearch()
                searchBooks(query, currentPage)
            }
        }

        booksRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && totalItemCount <= lastVisibleItem + pageSize) {
                    searchBooks(query, currentPage)
                }
            }
        })
    }

    private fun resetSearch() {
        emptyTextView.visibility = View.GONE
        booksRecyclerView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        currentPage = 0
        bookAdapter.clearBooks()
    }

    private fun searchBooks(query: String, page: Int) {
        if (isLoading) return  // Предотвращаем многократные запросы

        isLoading = true
        val url = "https://www.googleapis.com/books/v1/volumes?q=$query&startIndex=$page&maxResults=$pageSize&projection=full"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                progressBar.visibility = View.GONE
                isLoading = false

                Log.d("SearchBooksActivity", "API Response: $response") // Логируем весь ответ

                val items = response.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val bookList = ArrayList<Book>()
                    for (i in 0 until items.length()) {
                        val bookJson = items.getJSONObject(i)
                        val volumeInfo = bookJson.optJSONObject("volumeInfo") ?: continue
                        val accessInfo = bookJson.optJSONObject("accessInfo") // Доступ к webReaderLink

                        val title = volumeInfo.optString("title", "Без названия")
                        val authorsArray = volumeInfo.optJSONArray("authors")
                        val authors = if (authorsArray != null && authorsArray.length() > 0) {
                            val authorsList = mutableListOf<String>()
                            for (j in 0 until authorsArray.length()) {
                                authorsList.add(authorsArray.getString(j))
                            }
                            authorsList.joinToString(", ")
                        } else {
                            "Неизвестный автор"
                        }

                        val imageLinks = volumeInfo.optJSONObject("imageLinks")
                        val thumbnail = imageLinks?.optString("thumbnail", "") ?: ""
                        val description = volumeInfo.optString("description", "")
                        val publisher = volumeInfo.optString("publisher", "Неизвестно")
                        val publishedDate = volumeInfo.optString("publishedDate", "Не указано")
                        val pageCount = volumeInfo.optInt("pageCount", 0)
                        val previewLink = volumeInfo.optString("previewLink", "")

                        // Получаем `webReaderLink`
                        var webReaderLink = accessInfo?.optString("webReaderLink") ?: ""


                        // Если `webReaderLink` отсутствует, пробуем сгенерировать его из `selfLink`
                        if (webReaderLink.isNullOrEmpty()) {
                            val selfLink = bookJson.optString("selfLink", "")
                            if (selfLink.isNotEmpty()) {
                                webReaderLink = "https://play.google.com/store/books/details?id=${selfLink.substringAfterLast("/")}"
                            }
                        }

                        Log.d("SearchBooksActivity", "webReaderLink: $webReaderLink") // Логируем webReaderLink

                        val book = Book(
                            title = title,
                            authors = authors,
                            thumbnail = thumbnail,
                            description = description,
                            publisher = publisher,
                            publishedDate = publishedDate,
                            pageCount = pageCount,
                            previewLink = previewLink,
                            webReaderLink = webReaderLink // ✅ Теперь ссылка всегда есть
                        )

                        bookList.add(book)
                    }

                    if (bookList.isEmpty()) {
                        Toast.makeText(this, "Книга не найдена в базе данных!", Toast.LENGTH_SHORT).show()
                    } else {
                        bookAdapter.addBooks(bookList)
                        currentPage += pageSize
                    }
                } else {
                    Toast.makeText(this, "Нет результатов по запросу!", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e("SearchBooksActivity", "Ошибка загрузки данных: ${error.message}")
                progressBar.visibility = View.GONE
                isLoading = false
                Toast.makeText(this, "Ошибка при поиске книги!", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(request)
    }






}

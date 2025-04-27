package com.example.mylib


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager


import com.example.mylib.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var allBooks: MutableList<Book>
    private lateinit var filteredBooks: MutableList<Book>
    private var currentSortBy: String = "title"
    private var currentSortOrder: String = "asc"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bookAdapter: BookAdapter
    private var books: MutableList<Book> = mutableListOf()

    private var currentGenre: String = "Все жанры"  // По умолчанию
    private var isReadable: Boolean = false
    private var isRead: Boolean = false
    private var isFavorite: Boolean = false

    private var isMenuVisible by Delegates.observable(false) { _, _, newValue ->
        binding.menuContainer.visibility = if (newValue) View.VISIBLE else View.GONE
        if (newValue) {
            showMenu()
        } else {
            hideMenu()
        }
    }
    private lateinit var binding: ActivityMainBinding

    companion object {
        private val REQUEST_CODE_NOTES = 1001

        private const val SCAN_ISBN_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        var instance: MainActivity? = null

        fun updateUiStatic(bookTitle: String) {
            instance?.updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        instance = this

        // Загружаем состояние фильтров
        loadFiltersState()


        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterBooks(s.toString()) // Фильтруем книги при изменении текста
            }

            override fun afterTextChanged(s: Editable?) {}
        })



        sharedPreferences = getSharedPreferences("saved_books", Context.MODE_PRIVATE)
        loadSavedBooks()

        allBooks = loadSavedBooks().toMutableList()
        filteredBooks = allBooks


        setupSortAndFilter()

        binding.booksRecyclerView.layoutManager = LinearLayoutManager(this)
        bookAdapter = BookAdapter(
            loadSavedBooks().toMutableList(),  // Список книг
            false, // Не в режиме поиска (сокрытие кнопки)
            true,  // В главном режиме (кнопка добавления скрыта)
            this
        )

        binding.booksRecyclerView.adapter = bookAdapter


        updateUI()


        val imageButton1 = findViewById<ImageButton>(R.id.imageButton1)
        imageButton1.setOnClickListener {
            val intent = Intent(this, SearchBooksActivity::class.java)
            startActivity(intent)
        }


        val imageButton2 = findViewById<ImageButton>(R.id.imageButton2)
        imageButton2.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Разрешение есть, запускаем сканирование
                startScanISBN()
            } else {
                // Запрашиваем разрешение
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }



        // Загружаем тему
        val sharedPref = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val bgColor = sharedPref.getInt("bg_color", Color.WHITE)
        val bottomNavColor = sharedPref.getInt("bottom_nav_color", Color.LTGRAY)


        val textViewBack = sharedPref.getString("txt_book", "txt_dark_gray")
        val txtBook = resources.getIdentifier(textViewBack, "drawable", packageName)
        val edtTextBack = sharedPref.getString("edt_text", "rounded_edittext")
        val edtText = resources.getIdentifier(edtTextBack, "drawable", packageName)
        val fltBtnBack = sharedPref.getString("flt_btn", "rounded_button")
        val fltBtn = resources.getIdentifier(fltBtnBack, "drawable", packageName)

        if (fltBtn != 0) {
            binding.filterButton.setBackgroundResource(fltBtn)
            binding.imageButton1.setBackgroundResource(fltBtn)
            binding.imageButton2.setBackgroundResource(fltBtn)
        }

        if (edtText != 0) {
            binding.searchEditText.setBackgroundResource(edtText)
        }

        if (txtBook != 0) {
            binding.booksFoundTextView.setBackgroundResource(txtBook)
            binding.menuContainer.setBackgroundResource(txtBook)
        }



        // Применяем цвета
        binding.imageButton1.setBackgroundResource(fltBtn)
        binding.imageButton2.setBackgroundResource(fltBtn)
        binding.main.setBackgroundColor(bgColor)
        binding.bottomNavigation.setBackgroundColor(bottomNavColor)
        binding.booksFoundTextView.setBackgroundResource(txtBook)
        binding.searchEditText.setBackgroundResource(edtText)
        binding.filterButton.setBackgroundResource(fltBtn)



        val addButton = findViewById<FloatingActionButton>(R.id.addButton)



        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }

        binding.refreshButton.setOnClickListener {
            animateRefreshButton()
        }

        addButton.setOnClickListener {
            isMenuVisible = !isMenuVisible // Переключение состояния видимости меню
        }
    }

    private fun showMenu() {
        binding.menuContainer.alpha = 0f // Начальная прозрачность контейнера
        binding.menuContainer.animate().alpha(1f).setDuration(300)
            .start() // Анимация появления контейнера
    }

    private fun hideMenu() {
        binding.menuContainer.animate().alpha(0f).setDuration(300).withEndAction {
            binding.menuContainer.visibility = View.GONE // Скрываем контейнер после анимации
        }.start()
    }

    private fun filterBooks(query: String) {
        // Применяем фильтрацию по запросу
        val filteredList = allBooks.filter { book ->
            book.title.contains(query, ignoreCase = true) ||
                    book.authors.contains(query, ignoreCase = true) // Проверка по строке авторов
        }.toMutableList() // Преобразуем результат в MutableList

        // Обновляем адаптер с отфильтрованными книгами
        bookAdapter.updateBooks(filteredList)

        // Обновляем текст с количеством найденных книг
        binding.booksFoundTextView.text = if (filteredList.isEmpty()) {
            "Ничего не найдено"
        } else {
            "Книг найдено: ${filteredList.size}"
        }
    }





    private fun animateRefreshButton() {
        updateUI()
        // 1. Rotate Animation
        val rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotateAnimation.duration = 500  // Half a second for rotation
        rotateAnimation.repeatCount = 0
        rotateAnimation.fillAfter = false // Don't keep the final rotation state

        // 2. Scale Animation (Increase and then decrease)
        val scaleAnimation = ScaleAnimation(
            1f, 1.2f,  // fromX, toX (1f = original size, 1.2f = 120% size)
            1f, 1.2f,  // fromY, toY
            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
            Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
        )
        scaleAnimation.duration = 200 // Quarter second to scale up
        scaleAnimation.repeatMode = Animation.REVERSE  // Scale back down
        scaleAnimation.repeatCount = 1 // Only scale up and then back down once

        // 3. Combine Animations
        val animationSet = AnimationSet(true) // true = share interpolator
        animationSet.addAnimation(rotateAnimation)
        animationSet.addAnimation(scaleAnimation)

        binding.refreshButton.startAnimation(animationSet)
    }

    private fun showFilterDialog() {
        // Восстановление состояния фильтров
        loadFiltersState()

        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val inflater = LayoutInflater.from(this)
        val dialogView: View = inflater.inflate(R.layout.filter_dialog, null)
        builder.setView(dialogView)

        // Инициализация компонентов диалога
        val genreSpinner: Spinner = dialogView.findViewById(R.id.genreSpinner)
        val sortByTitleRadioButton: RadioButton = dialogView.findViewById(R.id.sortByTitleRadioButton)
        val sortByAuthorRadioButton: RadioButton = dialogView.findViewById(R.id.sortByAuthorRadioButton)
        val sortOrderAscRadioButton: RadioButton = dialogView.findViewById(R.id.sortOrderAscRadioButton)
        val sortOrderDescRadioButton: RadioButton = dialogView.findViewById(R.id.sortOrderDescRadioButton)
        val readCheckBox: CheckBox = dialogView.findViewById(R.id.readCheckBox)
        val favoriteCheckBox: CheckBox = dialogView.findViewById(R.id.favoriteCheckBox)

        // Устанавливаем сохраненное состояние кнопок
        sortByTitleRadioButton.isChecked = currentSortBy == "title"
        sortByAuthorRadioButton.isChecked = currentSortBy == "author"
        sortOrderAscRadioButton.isChecked = currentSortOrder == "asc"
        sortOrderDescRadioButton.isChecked = currentSortOrder == "desc"
        readCheckBox.isChecked = isRead
        favoriteCheckBox.isChecked = isFavorite

        builder.setPositiveButton("Применить") { dialog, _ ->
            // Получаем выбранный жанр
            val selectedGenre = genreSpinner.selectedItem?.toString() ?: "Все жанры"

            // Получаем выбранное значение сортировки
            val sortBy = when {
                sortByTitleRadioButton.isChecked -> "title"
                sortByAuthorRadioButton.isChecked -> "author"
                else -> "title"
            }

            val sortOrder = if (sortOrderAscRadioButton.isChecked) "asc" else "desc"

            // Сохраняем состояние фильтров
            currentSortBy = sortBy
            currentSortOrder = sortOrder
            currentGenre = selectedGenre
            isRead = readCheckBox.isChecked
            isFavorite = favoriteCheckBox.isChecked

            // Сохраняем состояние в SharedPreferences
            saveFiltersState()

            // Применяем фильтры
            applyFilters(selectedGenre, sortBy, sortOrder, isReadable, isRead, isFavorite)

            // Закрытие диалога
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }

        // Отображаем диалог
        val alertDialog = builder.create()
        alertDialog.show()

        // Устанавливаем ширину диалога на 90% от ширины экрана
        val screenWidth = resources.displayMetrics.widthPixels
        alertDialog.window?.setLayout((screenWidth * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }



    // Обновляем адаптер с фильтрацией по состоянию `isRead`
    private fun applyFilters(
        genre: String,
        sortBy: String,
        sortOrder: String,
        isReadable: Boolean,
        isRead: Boolean,
        isFavorite: Boolean
    ) {
        var filteredBooks: MutableList<Book> = allBooks.toMutableList()

        // Фильтрация по жанру
        if (genre != "Все жанры") {
            filteredBooks = filteredBooks.filter { it.genre == genre }.toMutableList()
        }

        // Фильтрация по статусу читаемости (книги, которые читаются)
        if (isReadable) {
            filteredBooks = filteredBooks.filter { it.startDate != null && it.endDate == null }.toMutableList()
        }

        // Фильтрация по состоянию прочтения
        if (isRead) {
            filteredBooks = filteredBooks.filter { it.isRead }.toMutableList()
        }

        // Фильтрация по избранному
        if (isFavorite) {
            filteredBooks = filteredBooks.filter { book -> book.isFavorite }.toMutableList()
        }

        // Сортировка
        filteredBooks = when (sortBy) {
            "title" -> filteredBooks.sortedBy { it.title }.toMutableList()
            "author" -> filteredBooks.sortedBy { it.authors }.toMutableList()
            else -> filteredBooks.sortedBy { it.title }.toMutableList()
        }

        // Применяем сортировку по порядку
        if (sortOrder == "desc") {
            filteredBooks.reverse()
        }

        // Дополнительно сортируем, чтобы избранные были первыми
        filteredBooks = filteredBooks.sortedByDescending { it.isFavorite }.toMutableList()

        // Обновляем UI
        bookAdapter.updateBooks(filteredBooks)

        Log.d(
            "ApplyFilters",
            "Applied filters: genre=$genre, sortBy=$sortBy, sortOrder=$sortOrder, " +
                    "isReadable=$isReadable, isRead=$isRead, isFavorite=$isFavorite"
        )
    }












    private fun setupSortAndFilter() {
        // Устанавливаем текущие значения сортировки и фильтров при открытии фильтра
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }




    private fun loadFiltersState() {
        val sharedPreferences = getSharedPreferences("filters", MODE_PRIVATE)

        // Восстанавливаем сохраненное состояние фильтров
        currentSortBy = sharedPreferences.getString("currentSortBy", "title") ?: "title"
        currentSortOrder = sharedPreferences.getString("currentSortOrder", "asc") ?: "asc"
        currentGenre = sharedPreferences.getString("currentGenre", "Все жанры") ?: "Все жанры"
        isReadable = sharedPreferences.getBoolean("isReadable", false)
        isRead = sharedPreferences.getBoolean("isRead", false)
        isFavorite = sharedPreferences.getBoolean("isFavorite", false)
    }

    private fun saveFiltersState() {
        val sharedPreferences = getSharedPreferences("filters", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Сохраняем состояние фильтров и чекбоксов
        editor.putString("currentSortBy", currentSortBy)
        editor.putString("currentSortOrder", currentSortOrder)
        editor.putString("currentGenre", currentGenre)
        editor.putBoolean("isReadable", isReadable)
        editor.putBoolean("isRead", isRead)
        editor.putBoolean("isFavorite", isFavorite)

        editor.apply()
    }




    fun onNavButtonClick(view: View) {
        when (view.id) {
            R.id.nav_home -> {
                startActivity(Intent(this, StatisticsActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in_left, R.anim.slide_fade_out_left)
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in_right, R.anim.slide_fade_out_right)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, запускаем сканирование
                startScanISBN()
            } else {
                Log.e("MainActivity", "Разрешение на камеру отклонено")
            }
        }
    }

    private fun startScanISBN() {
        val intent = Intent(this, ScanISBNActivity::class.java)
        startActivityForResult(intent, SCAN_ISBN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SCAN_ISBN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val scannedISBN = data?.getStringExtra("ISBN_RESULT")
            Log.d("MainActivity", "Получен ISBN: $scannedISBN")

            if (!scannedISBN.isNullOrEmpty()) {
                Toast.makeText(this, "Книга успешно найдена!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, SearchBooksActivity::class.java)
                intent.putExtra("ISBN", scannedISBN)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Книга не найдена в базе данных!", Toast.LENGTH_SHORT).show()
            }
        }


    }









    override fun onResume() {
        super.onResume()
        updateUI() // Обновляем список книг при возвращении в активити
    }


    private fun updateUI() {
        // Получаем список сохраненных книг
        val bookList = loadSavedBooks()

        // Обновляем количество найденных книг

        val sharedPreferences = getSharedPreferences("book_count", Context.MODE_PRIVATE)
        val bookCount = sharedPreferences.getInt("count", 0)
        binding.booksFoundTextView.text = "Книг найдено: $bookCount"

        // Обновляем видимость элементов UI в зависимости от наличия книг
        if (bookList.isEmpty()) {
            binding.booksRecyclerView.visibility = View.GONE
            binding.emptyTextView.visibility = View.VISIBLE

        } else {
            binding.booksRecyclerView.visibility = View.VISIBLE
            binding.emptyTextView.visibility = View.GONE

            bookAdapter.updateBooks(bookList)

        }
    }

    private fun loadSavedBooks(): MutableList<Book> {
        val json = sharedPreferences.getString("books", null)

        Log.d("BookLoading", "Загружаем книги из SharedPreferences: $json")

        return if (json != null) {
            try {
                val type = object : TypeToken<List<Book>>() {}.type
                val books: List<Book> = Gson().fromJson(json, type) ?: emptyList()


                books.sortedByDescending { it.isFavorite }.toMutableList()

                Log.d("BookLoading", "Загруженные книги: $books")

                // Convert List<Book> to MutableList<Book>
                books.map { book ->
                    // Логируем поля книги
                    Log.d("BookLoading", "Книга: ${book.title}, Издательство: ${book.publisher}, Дата издания: ${book.publishedDate}, Страниц: ${book.pageCount}")

                    val publisher = book.publisher ?: "Неизвестно"
                    val publishedDate = book.publishedDate ?: "Не указано"
                    val pageCount = book.pageCount.takeIf { it > 0 } ?: 0

                    // Передаем параметр isRead, возможно с дефолтным значением
                    Book(
                        title = book.title,
                        authors = book.authors,
                        thumbnail = book.thumbnail,
                        description = book.description ?: "",
                        note = book.note,
                        genre = book.genre,
                        startDate = book.startDate,
                        endDate = book.endDate,
                        publisher = publisher,
                        publishedDate = publishedDate,
                        pageCount = pageCount,
                        previewLink = "",
                        webReaderLink = "",
                        isFavorite = book.isFavorite
                    )
                }.toMutableList()
            } catch (e: Exception) {
                Log.e("BookLoading", "Ошибка при загрузке книг: ${e.message}")
                mutableListOf()
            }
        } else {
            Log.d("BookLoading", "Нет данных в SharedPreferences, возвращаем пустой список.")
            mutableListOf()
        }

    }
















}

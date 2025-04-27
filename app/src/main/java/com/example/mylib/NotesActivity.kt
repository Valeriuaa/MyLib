package com.example.mylib

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale

class NotesActivity : AppCompatActivity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var bookTitleTextView: TextView
    private lateinit var noteEditText: EditText
    private lateinit var txt2: TextView
    private lateinit var startDateEditText: EditText
    private lateinit var txt3: TextView
    private lateinit var endDateEditText: EditText
    private lateinit var txt4: TextView
    private lateinit var genreSpinner: Spinner
    private lateinit var saveNoteButton: Button

    private var bookTitle: String? = null
    private val sharedPreferences by lazy {
        getSharedPreferences("book_notes", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        // Инициализируем все элементы
        mainLayout = findViewById(R.id.main)
        bookTitleTextView = findViewById(R.id.bookTitleTextView)
        noteEditText = findViewById(R.id.noteEditText)

        txt2 = findViewById(R.id.txt2)
        startDateEditText = findViewById(R.id.startDateEditText)
        txt3 = findViewById(R.id.txt3)
        endDateEditText = findViewById(R.id.endDateEditText)
        txt4 = findViewById(R.id.txt4)
        genreSpinner = findViewById(R.id.genreSpinner)
        saveNoteButton = findViewById(R.id.saveNoteButton)

        // Загружаем настройки темы
        val sharedPref = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val bgColor = sharedPref.getInt("bg_color", Color.WHITE)
        val textColor = sharedPref.getInt("text_color", Color.BLACK)

        val edtTextBack = sharedPref.getString("edt_text", "rounded_edittext")
        val edtText = resources.getIdentifier(edtTextBack, "drawable", packageName)
        val fltBtnBack = sharedPref.getString("flt_btn", "rounded_button")
        val fltBtn = resources.getIdentifier(fltBtnBack, "drawable", packageName)

        // Устанавливаем стили
        if (edtText != 0) {
            noteEditText.setBackgroundResource(edtText)
            startDateEditText.setBackgroundResource(edtText)
            endDateEditText.setBackgroundResource(edtText)
            genreSpinner.setBackgroundResource(edtText)
        }

        if (fltBtn != 0) {
            saveNoteButton.setBackgroundResource(fltBtn)
        }

        mainLayout.setBackgroundColor(bgColor)
        bookTitleTextView.setTextColor(textColor)
        txt2.setTextColor(textColor)
        txt3.setTextColor(textColor)
        txt4.setTextColor(textColor)

        // Обработчики для выбора дат
        startDateEditText.setOnClickListener { showDatePicker(startDateEditText) }
        endDateEditText.setOnClickListener { showDatePicker(endDateEditText) }

        // Настраиваем список жанров
        val genres = arrayOf("Фэнтези", "Детектив", "Роман", "Научная фантастика", "История", "Бизнес")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genres)
        genreSpinner.adapter = adapter

        // Получаем название книги из Intent
        bookTitle = intent.getStringExtra("BOOK_TITLE")
        bookTitleTextView.text = bookTitle

        // Загружаем сохранённые данные
        loadSavedData()

        saveNoteButton.setOnClickListener {
            saveNote()
            loadSavedData()
        }
    }



    private fun saveNote() {
        val noteText = noteEditText.text.toString()
        val startDate = startDateEditText.text.toString()
        val endDate = endDateEditText.text.toString()
        val genre = genreSpinner.selectedItem.toString()

        val editor = sharedPreferences.edit()
        editor.putString("${bookTitle}_note", noteText)
        editor.putString("${bookTitle}_start_date", startDate)
        editor.putString("${bookTitle}_end_date", endDate)
        editor.putString("${bookTitle}_genre", genre)
        editor.apply()

        // Обновляем статус книги
        updateBookReadStatus(bookTitle, endDate)

        // Обновляем UI
        noteEditText.setText(noteText)
        startDateEditText.setText(startDate)
        endDateEditText.setText(endDate)
        genreSpinner.setSelection((genreSpinner.adapter as ArrayAdapter<String>).getPosition(genre))

        // Показываем уведомление о сохранении
        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show()

        // Переходим на MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadSavedData() {
        noteEditText.setText(sharedPreferences.getString("${bookTitle}_note", ""))
        startDateEditText.setText(sharedPreferences.getString("${bookTitle}_start_date", ""))
        endDateEditText.setText(sharedPreferences.getString("${bookTitle}_end_date", ""))

        val savedEndDate = sharedPreferences.getString("${bookTitle}_end_date", null)
        if (!savedEndDate.isNullOrEmpty()) {
            try {
                val parts = savedEndDate.split(".") // Дата в формате "дд.ММ.гггг"
                val parsedDate = LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())

                // Сохраняем в SharedPreferences для доступа в других активностях
                val editor = sharedPreferences.edit()
                editor.putString("${bookTitle}_end_date_parsed", parsedDate.toString()) // Сохраняем в формате "yyyy-MM-dd"
                editor.apply()

                // Проверяем, прочитана ли книга при загрузке
                updateBookReadStatus(bookTitle, savedEndDate)
            } catch (e: Exception) {
                Log.e("NotesActivity", "Ошибка парсинга даты: $savedEndDate", e)
            }
        }

        val savedGenre = sharedPreferences.getString("${bookTitle}_genre", "")
        if (savedGenre != null) {
            val index = (genreSpinner.adapter as ArrayAdapter<String>).getPosition(savedGenre)
            genreSpinner.setSelection(index)
        }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay.${selectedMonth + 1}.$selectedYear"
            editText.setText(selectedDate)

            // Обновляем статус книги сразу, как только дата выбрана
            updateBookReadStatus(bookTitle, selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateBookReadStatus(title: String?, endDate: String?) {
        if (title == null || endDate.isNullOrEmpty()) return

        val sharedPreferences = getSharedPreferences("saved_books", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("books", "[]")
        val type = object : TypeToken<MutableList<Book>>() {}.type
        val books: MutableList<Book> = gson.fromJson(json, type) ?: mutableListOf()

        val dateFormatter = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.getDefault())

        books.find { it.title == title }?.let { book ->
            book.endDate = try {
                LocalDate.parse(endDate.trim(), dateFormatter)
            } catch (e: DateTimeParseException) {
                e.printStackTrace()
                null
            }

            // Проверяем, прочитана ли книга сразу
            book.isRead = book.endDate?.isBefore(LocalDate.now()) ?: false
        }

        val editor = sharedPreferences.edit()
        editor.putString("books", gson.toJson(books))
        editor.apply()
    }
}


package com.example.mylib

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StatisticsActivity : AppCompatActivity() {

    private lateinit var pieChartBooks: PieChart
    private lateinit var pieChartAuthors: PieChart
    private lateinit var pieChartPublishers: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val mainLayout = findViewById<NestedScrollView>(R.id.scrollView)
        val bottomNavigationLayout = findViewById<LinearLayout>(R.id.bottomNavigation)
        val txtView1 = findViewById<TextView>(R.id.knig)
        val txtView2 = findViewById<TextView>(R.id.avtor)
        val txtView3 = findViewById<TextView>(R.id.izdat)

        // Загружаем тему
        val sharedPref = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val bgColor = sharedPref.getInt("bg_color", Color.WHITE)
        val textColor = sharedPref.getInt("text_color", Color.BLACK)
        val bottomNavColor = sharedPref.getInt("bottom_nav_color", Color.LTGRAY)

        mainLayout?.setBackgroundColor(bgColor)
        bottomNavigationLayout?.setBackgroundColor(bottomNavColor)
        txtView1?.setTextColor(textColor)
        txtView2?.setTextColor(textColor)
        txtView3?.setTextColor(textColor)

        // Инициализация диаграмм
        pieChartBooks = findViewById(R.id.pieChartBooks)
        pieChartAuthors = findViewById(R.id.pieChartAuthors)
        pieChartPublishers = findViewById(R.id.pieChartPublishers)

        setupPieChart(pieChartBooks)
        setupPieChart(pieChartAuthors)
        setupPieChart(pieChartPublishers)

        loadStatistics()




















    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.apply {
            setUsePercentValues(false)  // Показываем точные числа
            description.isEnabled = false
            setDrawHoleEnabled(true)   // Вернем отверстие в центре
            setHoleColor(Color.WHITE)  // Белая середина
            setTransparentCircleAlpha(110)
            setHoleRadius(45f)  // Размер отверстия
            setTransparentCircleRadius(50f)

            setEntryLabelTextSize(14f)  // Увеличиваем текст подписей
            setEntryLabelColor(Color.WHITE) // Чёрные подписи
            setEntryLabelTypeface(Typeface.DEFAULT_BOLD) // Жирный шрифт подписей

            legend.isEnabled = false  // Включаем легенду
            legend.textSize = 14f     // Увеличиваем текст легенды
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM

            animateY(1500, Easing.EaseInOutQuad)  // Анимация
        }
    }



    private fun loadStatistics() {
        val books = getBooksFromPreferences()

        // Проверка, есть ли книги
        if (books.isEmpty()) {
            return
        }

        // 1. Подсчёт прочитанных и не прочитанных книг
        // Подсчёт прочитанных и не прочитанных книг
        val readBooksCount = books.count { it.isRead }
        val unreadBooksCount = books.count { !it.isRead }

        val bookEntries = mutableListOf<PieEntry>()
        if (readBooksCount > 0) bookEntries.add(PieEntry(readBooksCount.toFloat(), "Прочитано"))
        if (unreadBooksCount > 0) bookEntries.add(PieEntry(unreadBooksCount.toFloat(), "Не прочитано"))

        val bookDataSet = PieDataSet(bookEntries, "Статистика книг")
        bookDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        bookDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString() // Показываем точные числа
            }
        }
        pieChartBooks.data = PieData(bookDataSet)
        pieChartBooks.invalidate()


        // 2. Топ 5 авторов
        val authorCount = books.groupingBy { it.authors }.eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val authorEntries = authorCount.map { PieEntry(it.second.toFloat(), it.first) }
        val authorDataSet = PieDataSet(authorEntries, "Топ 5 авторов")
        authorDataSet.colors = ColorTemplate.VORDIPLOM_COLORS.toList()
        authorDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        pieChartAuthors.data = PieData(authorDataSet)
        pieChartAuthors.invalidate()

        // 3. Топ 5 издательств
        val publisherCount = books.groupingBy { it.publisher }.eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val publisherEntries = publisherCount.map { PieEntry(it.second.toFloat(), it.first) }
        val publisherDataSet = PieDataSet(publisherEntries, "Топ 5 издательств")
        publisherDataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()
        publisherDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        pieChartPublishers.data = PieData(publisherDataSet)
        pieChartPublishers.invalidate()
    }

    private fun getBooksFromPreferences(): List<Book> {
        val sharedPreferences = getSharedPreferences("saved_books", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("books", "[]") ?: "[]"
        return Gson().fromJson(json, object : TypeToken<List<Book>>() {}.type) ?: emptyList()
    }

    fun onNavButtonClick(view: View) {
        when (view.id) {
            R.id.nav_search -> {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in_right, R.anim.slide_fade_out_right)
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in_right, R.anim.slide_fade_out_right)
            }
        }
    }
}

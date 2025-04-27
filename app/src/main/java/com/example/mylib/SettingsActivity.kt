package com.example.mylib

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout

class SettingsActivity : AppCompatActivity() {





    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Получаем ссылки на кнопки
        val btnKhakiTheme = findViewById<AppCompatButton>(R.id.btnKhakiTheme)
        val btnLavenderTheme = findViewById<AppCompatButton>(R.id.btnLavenderTheme)
        val btnPaleGreenTheme = findViewById<AppCompatButton>(R.id.btnPaleGreenTheme)
        val btnDarkGrayTheme = findViewById<AppCompatButton>(R.id.btnDarkGrayTheme)
        val btnSlateBlueTheme = findViewById<AppCompatButton>(R.id.btnSlateBlueTheme)
        val btnDarkSlateGreenTheme = findViewById<AppCompatButton>(R.id.btnDarkSlateGreenTheme)

        val sharedPref = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val bgColor = sharedPref.getInt("bg_color", Color.WHITE)
        val textColor = sharedPref.getInt("text_color", Color.BLACK)
        val bottomNavColor = sharedPref.getInt("bottom_nav_color", Color.LTGRAY)


        // Получаем ссылки на TextView
        val textViewTitle = findViewById<TextView>(R.id.textViewTitle)


        // Получаем ссылку на LinearLayout
        val bottomNavigationLayout = findViewById<LinearLayout>(R.id.bottomNavigation)

        // Получаем ссылку на корневой layout (ConstraintLayout)
        val mainLayout = findViewById<LinearLayout>(R.id.main)

        mainLayout?.setBackgroundColor(bgColor)
        textViewTitle?.setTextColor(textColor)
        bottomNavigationLayout?.setBackgroundColor(bottomNavColor)


        // Функция для установки темы
        fun applyAndSaveColors(bgColor: Int, textColor: Int, bottomNavColor: Int, txtBook: String, edtText: String, fltBtn: String) {
            // Применяем цвета
            mainLayout.setBackgroundColor(bgColor)
            textViewTitle.setTextColor(textColor)
            bottomNavigationLayout.setBackgroundColor(bottomNavColor)

            // Сохраняем в SharedPreferences
            val sharedPref = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("bg_color", bgColor)
                putInt("text_color", textColor)
                putInt("bottom_nav_color", bottomNavColor)
                putString("txt_book", txtBook)
                putString("edt_text", edtText)
                putString("flt_btn", fltBtn)
                apply()
            }
        }

            mainLayout?.setBackgroundColor(bgColor)
            textViewTitle?.setTextColor(textColor)
            bottomNavigationLayout?.setBackgroundColor(bottomNavColor)

        // Устанавливаем слушатели кликов для кнопок
        btnKhakiTheme.setOnClickListener {
            applyAndSaveColors(
                resources.getColor(R.color.khakiBackground, theme),
                Color.BLACK,
                resources.getColor(R.color.khakiBottomNavigation, theme),
                "txt_khaki",
                "et_khaki",
                "btn_khaki"

            )
        }

        btnLavenderTheme.setOnClickListener {
            applyAndSaveColors(
                resources.getColor(R.color.lavenderBackground, theme),
                Color.BLACK,
                resources.getColor(R.color.lavenderBottomNavigation, theme),
                "txt_lavender",
                "et_lavender",
                "btn_lavender"
            )
        }

        btnDarkGrayTheme.setOnClickListener {
            applyAndSaveColors(
                resources.getColor(R.color.darkGrayBackground, theme),
                Color.WHITE,  // Или Color.BLACK
                resources.getColor(R.color.darkGrayBottomNavigation, theme),
                "txt_dark_gray",
                "et_dark_gray",
                "btn_dark_gray"
            )
        }

        btnSlateBlueTheme.setOnClickListener {
            applyAndSaveColors(
                resources.getColor(R.color.slateBlueBackground, theme),
                Color.WHITE,  // Или Color.BLACK
                resources.getColor(R.color.slateBlueBottomNavigation, theme),
                "txt_slate_blue",
                "et_slate_blue",
                "btn_slate_blue"
            )
        }

        btnDarkSlateGreenTheme.setOnClickListener {
            applyAndSaveColors(
                resources.getColor(R.color.darkSlateGreenBackground, theme),
                Color.WHITE,  // Или Color.BLACK
                resources.getColor(R.color.darkSlateGreenBottomNavigation, theme),
                "txt_dark_slate_green",
                "et_dark_slate_green",
                "btn_dark_slate_green"
            )
        }
        btnPaleGreenTheme.setOnClickListener {
            applyAndSaveColors(
                resources.getColor(R.color.paleGreenBackground, theme),
                Color.BLACK,  // Или Color.WHITE
                resources.getColor(R.color.paleGreenBottomNavigation, theme),
                "txt_pale_green",
                "et_pale_green",
                "btn_pale_green"
            )
        }
    }













    fun onNavButtonClick(view: View) {
        when (view.id) {
            R.id.nav_home -> {
                startActivity(Intent(this, StatisticsActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in_left, R.anim.slide_fade_out_left)
            }
            R.id.nav_search -> {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in_left, R.anim.slide_fade_out_left)
            }
        }
    }






}

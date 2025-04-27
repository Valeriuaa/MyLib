package com.example.mylib

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.view.View
import android.view.animation.AnimationUtils

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Установка загрузочного экрана (если используете Android 12 и выше)
        installSplashScreen()

        setContentView(R.layout.activity_splash)

        val logo = findViewById<View>(R.id.logo)

        // Применение анимации к логотипу
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        logo.startAnimation(fadeInAnimation)

        // Задержка перед переходом на главный экран
        Handler().postDelayed({
            // Переход к основному экрану (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Закрыть SplashActivity, чтобы пользователь не мог вернуться к нему
        }, 2000) // Задержка 3 секунды
    }
}

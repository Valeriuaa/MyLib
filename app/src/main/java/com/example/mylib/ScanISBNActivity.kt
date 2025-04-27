package com.example.mylib

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class ScanISBNActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Запуск сканера
        startBarcodeScanner()
    }

    private fun startBarcodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES) // Только штрих-коды
        integrator.setPrompt("Отсканируйте ISBN-код книги")
        integrator.setCameraId(0) // Камера по умолчанию
        integrator.setBeepEnabled(true) // Включить звук при сканировании
        integrator.setBarcodeImageEnabled(true) // Сохранение изображения штрих-кода
        integrator.setOrientationLocked(true) // Позволяет поворачивать экран
        integrator.initiateScan() // Запуск сканирования
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents != null) {
            Log.d("ScanISBNActivity", "Сканированный ISBN: ${result.contents}")

            // Отправляем ISBN обратно в MainActivity
            val intent = Intent()
            intent.putExtra("ISBN_RESULT", result.contents)
            setResult(Activity.RESULT_OK, intent)
        } else {
            Log.d("ScanISBNActivity", "Сканирование отменено")
            setResult(Activity.RESULT_CANCELED)
        }
        finish() // Закрываем активити после сканирования
    }
}

package com.example.pdfmerger

import android.app.Application
// import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
// import com.tom_roush.pdfbox.util.IterativeMergeSort

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // PDFBoxの初期化（これを忘れると例外が発生します）
        // PDFBoxResourceLoader.init(applicationContext)
        // init(applicationContext)
    }
}
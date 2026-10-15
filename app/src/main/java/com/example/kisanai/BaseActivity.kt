package com.example.kisanai

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Apply the saved locale to the context
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
package com.example.kisanai

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        window.statusBarColor = getColor(R.color.white)

        val language = LocaleHelper.getLanguage(this)
        val btnEnglish = findViewById<TextView>(R.id.btnEnglish)
        val btnUrdu = findViewById<TextView>(R.id.btnUrdu)

        if (language == "ur") {
            btnUrdu.setTypeface(null, Typeface.BOLD)
            btnEnglish.setTypeface(null, Typeface.NORMAL)
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        } else {
            btnEnglish.setTypeface(null, Typeface.BOLD)
            btnUrdu.setTypeface(null, Typeface.NORMAL)
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        findViewById<LinearLayout>(R.id.languageSwitcher).setOnClickListener {
            updateLanguage(if (language == "en") "ur" else "en")
        }

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_scan -> ScanFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_fertilizer -> FertilizerFragment()
                R.id.nav_irrigation -> IrrigationFragment()
                else -> null
            }
            selectedFragment?.let { loadFragment(it) } ?: false
        }

        // Load default fragment without animation on first launch
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()
            bottomNav.selectedItemId = R.id.nav_dashboard
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
        return true
    }

    private fun updateLanguage(lang: String) {
        LocaleHelper.setLocale(this, lang)
        finish()
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
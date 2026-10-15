package com.example.kisanai

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        window.statusBarColor = getColor(R.color.bg_cream)

        val language = LocaleHelper.getLanguage(this)

        val backgroundImageView = findViewById<ImageView>(R.id.backgroundImageView)
        val btnEnglish = findViewById<TextView>(R.id.btnEnglish)
        val btnUrdu = findViewById<TextView>(R.id.btnUrdu)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val tvSignup = findViewById<TextView>(R.id.tvSignup)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val emailLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)

        setupErrorClearance(etEmail, emailLayout)
        setupErrorClearance(etPassword, passwordLayout)

        if (language == "ur") {
            backgroundImageView.scaleX = -1f
            btnUrdu.setTypeface(null, Typeface.BOLD)
            btnEnglish.setTypeface(null, Typeface.NORMAL)
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        } else {
            backgroundImageView.scaleX = 1f
            btnEnglish.setTypeface(null, Typeface.BOLD)
            btnUrdu.setTypeface(null, Typeface.NORMAL)
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<LinearLayout>(R.id.languageSwitcher).setOnClickListener {
            updateLanguage(if (language == "en") "ur" else "en")
        }

        forgotPassword.setOnClickListener { showForgotPasswordDialog() }
        tvSignup.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }

        loginButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            var isValid = true
            val errorColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red))

            if (email.isEmpty()) {
                emailLayout.error = getString(R.string.error_email_required)
                emailLayout.setStartIconTintList(errorColor)
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = getString(R.string.error_invalid_email)
                emailLayout.setStartIconTintList(errorColor)
                isValid = false
            }

            if (password.isEmpty()) {
                passwordLayout.error = getString(R.string.error_password_required)
                passwordLayout.setStartIconTintList(errorColor)
                isValid = false
            }

            if (isValid) {
                loginUser(email, password, emailLayout, passwordLayout, errorColor)
            }
        }
    }

    private fun loginUser(email: String, pass: String, emailLayout: TextInputLayout, passwordLayout: TextInputLayout, errorColor: ColorStateList) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        emailLayout.error = getString(R.string.error_verify_email)
                        emailLayout.setStartIconTintList(errorColor)
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidUserException) {
                        emailLayout.error = getString(R.string.error_email_not_found)
                        emailLayout.setStartIconTintList(errorColor)
                    } else if (exception is FirebaseAuthInvalidCredentialsException) {
                        passwordLayout.error = getString(R.string.error_incorrect_password)
                        passwordLayout.setStartIconTintList(errorColor)
                    } else {
                        Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.loadingLayout).visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupErrorClearance(editText: TextInputEditText, layout: TextInputLayout) {
        val normalColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_green))
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                layout.error = null
                layout.setStartIconTintList(normalColor)
            }
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                layout.error = null
                layout.setStartIconTintList(normalColor)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showForgotPasswordDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_forgot_password)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)
        val btnSend = dialog.findViewById<MaterialButton>(R.id.btnSend)
        val etEmailReset = dialog.findViewById<TextInputEditText>(R.id.etEmail)
        val resetEmailLayout = dialog.findViewById<TextInputLayout>(R.id.emailInputLayout)
        val spinner = dialog.findViewById<ProgressBar>(R.id.progressBar)

        setupErrorClearance(etEmailReset, resetEmailLayout)

        btnClose.setOnClickListener { dialog.dismiss() }
        
        btnSend.setOnClickListener {
            val email = etEmailReset.text.toString().trim()
            val errorColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red))

            if (email.isEmpty()) {
                resetEmailLayout.error = getString(R.string.error_email_required)
                resetEmailLayout.setStartIconTintList(errorColor)
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                resetEmailLayout.error = getString(R.string.error_invalid_email)
                resetEmailLayout.setStartIconTintList(errorColor)
            } else {
                btnSend.text = "" 
                spinner.visibility = View.VISIBLE
                btnSend.isEnabled = false
                
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        dialog.dismiss()
                        showRegistrationSuccessDialog(email, isReset = true)
                    } else {
                        btnSend.text = getString(R.string.send_reset_link)
                        spinner.visibility = View.GONE
                        btnSend.isEnabled = true
                        
                        if (task.exception is FirebaseAuthInvalidUserException) {
                            resetEmailLayout.error = getString(R.string.error_email_not_found)
                            resetEmailLayout.setStartIconTintList(errorColor)
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showRegistrationSuccessDialog(email: String, isReset: Boolean = false) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_registration_success)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialog.findViewById<TextView>(R.id.dialogMessage)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnOk)

        if (isReset) {
            dialogTitle.text = getString(R.string.forgot_password_title)
            dialogMessage.text = getString(R.string.reset_link_sent)
        } else {
            dialogTitle.text = getString(R.string.registration_success_title)
            dialogMessage.text = getString(R.string.verification_link_sent, email)
        }
        
        btnOk.text = getString(R.string.ok)
        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun updateLanguage(lang: String) {
        LocaleHelper.setLocale(this, lang)
        finish()
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
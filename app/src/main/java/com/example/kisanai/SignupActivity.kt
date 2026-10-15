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
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class SignupActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        window.statusBarColor = getColor(R.color.bg_cream)

        val language = LocaleHelper.getLanguage(this)

        val backgroundImageView = findViewById<ImageView>(R.id.backgroundImageView)
        val btnEnglish = findViewById<TextView>(R.id.btnEnglish)
        val btnUrdu = findViewById<TextView>(R.id.btnUrdu)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val signupButton = findViewById<MaterialButton>(R.id.signupButton)

        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)

        val fullNameLayout = findViewById<TextInputLayout>(R.id.fullNameInputLayout)
        val emailLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.confirmPasswordInputLayout)

        setupErrorClearance(etFullName, fullNameLayout)
        setupErrorClearance(etEmail, emailLayout)
        setupErrorClearance(etPassword, passwordLayout)
        setupErrorClearance(etConfirmPassword, confirmPasswordLayout)

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

        btnBack.setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.languageSwitcher).setOnClickListener { updateLanguage(if (language == "en") "ur" else "en") }
        tvLogin.setOnClickListener { finish() }

        signupButton.setOnClickListener {
            validateFields(signupButton, fullNameLayout, emailLayout, passwordLayout, confirmPasswordLayout, etFullName, etEmail, etPassword, etConfirmPassword)
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            if (progressDialog == null) {
                progressDialog = Dialog(this).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setContentView(R.layout.layout_loading)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
        }
    }

    private fun validateFields(
        signupButton: MaterialButton,
        fullNameLayout: TextInputLayout, emailLayout: TextInputLayout,
        passwordLayout: TextInputLayout, confirmPasswordLayout: TextInputLayout,
        etFullName: TextInputEditText, etEmail: TextInputEditText,
        etPassword: TextInputEditText, etConfirmPassword: TextInputEditText
    ) {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        var isValid = true
        val errorColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red))

        if (name.isEmpty()) {
            fullNameLayout.error = getString(R.string.error_name_required)
            fullNameLayout.setStartIconTintList(errorColor)
            isValid = false
        }
        
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
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = getString(R.string.error_confirm_password_required)
            confirmPasswordLayout.setStartIconTintList(errorColor)
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordLayout.error = getString(R.string.error_passwords_dont_match)
            confirmPasswordLayout.setStartIconTintList(errorColor)
            isValid = false
        }

        if (isValid) {
            signupButton.isEnabled = false
            registerUser(signupButton, name, email, password)
        }
    }

    private fun registerUser(signupButton: MaterialButton, name: String, email: String, pass: String) {
        showLoading(true)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // 1. Send verification email
                        user.sendEmailVerification()
                        
                        // 2. Save to Firestore - We wait for this to complete
                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "uid" to user.uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("users").document(user.uid).set(userMap)
                            .addOnSuccessListener {
                                Log.d("SignupActivity", "User profile created in Firestore")
                                if (!isFinishing) {
                                    showLoading(false)
                                    signupButton.isEnabled = true
                                    showRegistrationSuccessDialog(email)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("SignupActivity", "Firestore Error: ${e.message}")
                                if (!isFinishing) {
                                    showLoading(false)
                                    signupButton.isEnabled = true
                                    Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    // Even if Firestore fails, we show success dialog for Auth
                                    showRegistrationSuccessDialog(email)
                                }
                            }
                    }
                } else {
                    showLoading(false)
                    signupButton.isEnabled = true
                    val errorMsg = task.exception?.message ?: "Unknown error"
                    Log.e("SignupActivity", "Auth Error: $errorMsg")
                    Toast.makeText(this, "Registration failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setupErrorClearance(editText: TextInputEditText, layout: TextInputLayout) {
        val normalColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_green))
        editText.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) { layout.error = null; layout.setStartIconTintList(normalColor) } }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { layout.error = null; layout.setStartIconTintList(normalColor) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showRegistrationSuccessDialog(email: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_registration_success)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialog.findViewById<TextView>(R.id.dialogMessage)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnOk)

        dialogTitle.text = getString(R.string.registration_success_title)
        dialogMessage.text = getString(R.string.verification_link_sent, email)
        btnOk.text = getString(R.string.ok)

        btnOk.setOnClickListener {
            dialog.dismiss()
            auth.signOut()
            finish()
        }

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
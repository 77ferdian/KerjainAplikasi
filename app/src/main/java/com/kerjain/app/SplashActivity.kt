package com.kerjain.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.kerjain.app.repository.KerjainRepository

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val repo = KerjainRepository.getInstance(this)

        // Delay 2 seconds to showcase the splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            // Load persistent session from SharedPreferences/Firestore
            repo.loadSession { user ->
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 2000)
    }
}

package com.kerjain.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kerjain.app.model.User
import com.kerjain.app.repository.KerjainRepository
import com.kerjain.app.ui.auth.LoginFragment
import com.kerjain.app.ui.pelajar.PelajarExploreFragment
import com.kerjain.app.ui.pelajar.PelajarHomeFragment
import com.kerjain.app.ui.pelajar.PelajarProfileFragment
import com.kerjain.app.ui.pelajar.PelajarProgressFragment
import com.kerjain.app.ui.mentor.MentorHomeFragment
import com.kerjain.app.ui.mentor.MentorProfileFragment
import com.kerjain.app.ui.mitra.MitraHomeFragment
import com.kerjain.app.ui.mitra.MitraManageFragment
import com.kerjain.app.ui.mitra.MitraProfileFragment
import com.kerjain.app.ui.mitra.MitraTalentFragment

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var repo: KerjainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottom_navigation)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportFragmentManager.addOnBackStackChangedListener {
            val hasBack = supportFragmentManager.backStackEntryCount > 0
            supportActionBar?.setDisplayHomeAsUpEnabled(hasBack)
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        repo = KerjainRepository.getInstance(this)
        
        // Async Seed Firestore on startup if empty
        repo.seedFirestoreIfNeeded { seeded ->
            if (seeded) {
                runOnUiThread {
                    Toast.makeText(this, "Firebase Firestore berhasil diseed dengan data awal!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Initially check login status
        if (repo.currentUser == null) {
            showBottomNav(false)
            showToolbar(false)
            navigateFragment(LoginFragment(), false)
        } else {
            onUserLoggedIn(repo.currentUser!!)
        }

        setupNavigationListener()
    }

    fun onUserLoggedIn(user: User) {
        showBottomNav(true)
        bottomNav.menu.clear()

        when (user.role) {
            "pelajar" -> {
                bottomNav.inflateMenu(R.menu.menu_pelajar)
                navigateFragment(PelajarHomeFragment(), false)
            }
            "mentor" -> {
                bottomNav.inflateMenu(R.menu.menu_mentor)
                navigateFragment(MentorHomeFragment(), false)
            }
            "mitra" -> {
                bottomNav.inflateMenu(R.menu.menu_mitra)
                navigateFragment(MitraHomeFragment(), false)
            }
            else -> {
                Toast.makeText(this, "Role tidak dikenal", Toast.LENGTH_SHORT).show()
                onUserLoggedOut()
            }
        }
    }

    fun onUserLoggedOut() {
        showBottomNav(false)
        navigateFragment(LoginFragment(), false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    fun navigateFragment(fragment: Fragment, addToBackstack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackstack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    fun updateToolbarTitle(title: String) {
        supportActionBar?.title = ""
    }

    fun showToolbar(visible: Boolean) {
        toolbar.visibility = if (visible) View.VISIBLE else View.GONE
        findViewById<View>(R.id.toolbar_divider).visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun showBottomNav(visible: Boolean) {
        bottomNav.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setupNavigationListener() {
        bottomNav.setOnItemSelectedListener { item ->
            val user = repo.currentUser ?: return@setOnItemSelectedListener false
            
            when (user.role) {
                "pelajar" -> {
                    when (item.itemId) {
                        R.id.menu_home -> {
                            navigateFragment(PelajarHomeFragment(), false)
                            true
                        }
                        R.id.menu_explore -> {
                            navigateFragment(PelajarExploreFragment(), false)
                            true
                        }
                        R.id.menu_progress -> {
                            navigateFragment(PelajarProgressFragment(), false)
                            true
                        }
                        R.id.menu_profile -> {
                            navigateFragment(PelajarProfileFragment(), false)
                            true
                        }
                        else -> false
                    }
                }
                "mentor" -> {
                    when (item.itemId) {
                        R.id.menu_mentor_review -> {
                            navigateFragment(MentorHomeFragment(), false)
                            true
                        }
                        R.id.menu_profile -> {
                            navigateFragment(MentorProfileFragment(), false)
                            true
                        }
                        else -> false
                    }
                }
                "mitra" -> {
                    when (item.itemId) {
                        R.id.menu_home -> {
                            navigateFragment(MitraHomeFragment(), false)
                            true
                        }
                        R.id.menu_mitra_programs -> {
                            navigateFragment(MitraManageFragment(), false)
                            true
                        }
                        R.id.menu_mitra_talent -> {
                            navigateFragment(MitraTalentFragment(), false)
                            true
                        }
                        R.id.menu_profile -> {
                            navigateFragment(MitraProfileFragment(), false)
                            true
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
    }
}
package com.kerjain.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kerjain.app.MainActivity
import com.kerjain.app.R
import com.kerjain.app.model.User
import com.kerjain.app.repository.KerjainRepository

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        (activity as? MainActivity)?.showToolbar(false)
        val repo = KerjainRepository.getInstance(requireContext())

        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnLogin = view.findViewById<Button>(R.id.btn_login)
        val tvRegisterLink = view.findViewById<TextView>(R.id.tv_register_link)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repo.login(email, password) { success, user, error ->
                if (success && user != null) {
                    (activity as? MainActivity)?.onUserLoggedIn(user)
                } else {
                    Toast.makeText(context, error ?: "Login gagal", Toast.LENGTH_LONG).show()
                }
            }
        }

        tvRegisterLink.setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(RegisterFragment(), false)
        }

        return view
    }
}

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        val repo = KerjainRepository.getInstance(requireContext())

        val etName = view.findViewById<EditText>(R.id.et_name)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val rgRole = view.findViewById<android.widget.RadioGroup>(R.id.rg_role)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val tvLoginLink = view.findViewById<TextView>(R.id.tv_login_link)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rb_mentor -> "mentor"
                R.id.rb_mitra -> "mitra"
                else -> "pelajar"
            }

            val newUser = User(
                namaLengkap = name,
                email = email,
                password = password,
                role = role
            )

            // Setup default fields depending on role
            when (role) {
                "pelajar" -> {
                    newUser.universitas = "Universitas Contoh"
                    newUser.jurusan = "Sistem Informasi"
                    newUser.angkatan = 2024
                    newUser.bio = "Halo, saya Pelajar baru di KerjaIn!"
                }
                "mentor" -> {
                    newUser.profesi = "Junior Mentor"
                    newUser.perusahaan = "KerjaIn Partner"
                    newUser.tahunPengalaman = 1
                    newUser.bio = "Senang bergabung sebagai mentor!"
                }
                "mitra" -> {
                    newUser.namaUsaha = "Usaha Mandiri"
                    newUser.bidangUsaha = "Teknologi"
                    newUser.kota = "Bandung"
                    newUser.website = "www.usaha-mandiri.com"
                    newUser.emailDomain = email.substringAfter("@")
                }
            }

            repo.register(newUser) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Registrasi Berhasil! " + (msg ?: ""), Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.onUserLoggedIn(newUser)
                } else {
                    Toast.makeText(context, "Registrasi gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvLoginLink.setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(LoginFragment(), false)
        }

        return view
    }
}

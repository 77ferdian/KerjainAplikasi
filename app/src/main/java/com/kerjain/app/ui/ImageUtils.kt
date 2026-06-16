package com.kerjain.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

object ImageUtils {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    fun getTempCameraUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "temp_photos").apply { mkdirs() }
        val tempFile = File(cacheDir, "temp_profile_capture.jpg")
        return FileProvider.getUriForFile(
            context,
            "com.kerjain.app.fileprovider",
            tempFile
        )
    }

    fun saveProfilePhoto(context: Context, sourceUri: Uri, userId: String): String? {
        return try {
            val photoDir = File(context.filesDir, "profile_photos").apply { mkdirs() }
            val destFile = File(photoDir, "profile_$userId.jpg")
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadProfileImage(fotoProfilUrl: String?, tvInitial: TextView, ivPhoto: ImageView) {
        if (!fotoProfilUrl.isNullOrEmpty()) {
            if (fotoProfilUrl.startsWith("http://") || fotoProfilUrl.startsWith("https://")) {
                ivPhoto.tag = fotoProfilUrl
                executor.execute {
                    val bitmap = try {
                        val inputStream = URL(fotoProfilUrl).openStream()
                        BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                    handler.post {
                        if (ivPhoto.tag == fotoProfilUrl) {
                            if (bitmap != null) {
                                ivPhoto.setImageBitmap(bitmap)
                                ivPhoto.visibility = View.VISIBLE
                                tvInitial.visibility = View.GONE
                            } else {
                                ivPhoto.visibility = View.GONE
                                tvInitial.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } else {
                try {
                    val uri = Uri.parse(fotoProfilUrl)
                    ivPhoto.setImageURI(uri)
                    ivPhoto.visibility = View.VISIBLE
                    tvInitial.visibility = View.GONE
                } catch (e: Exception) {
                    e.printStackTrace()
                    ivPhoto.visibility = View.GONE
                    tvInitial.visibility = View.VISIBLE
                }
            }
        } else {
            ivPhoto.visibility = View.GONE
            tvInitial.visibility = View.VISIBLE
        }
    }
}

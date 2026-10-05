package com.shotposeai.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.shotposeai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Copies selected photos into app storage; no gallery permission or remote upload. */
object AvatarStore {
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decode(context, uri, 1024) ?: error("Foto tidak dapat dibaca")
        val directory = File(context.filesDir, "profile_photos").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            Uri.fromFile(file).toString()
        } catch (e: Exception) { file.delete(); throw e }
        finally { bitmap.recycle() }
    }

    private fun decode(context: Context, uri: Uri, size: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > size * 2) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }
        }.getOrNull() ?: 1
        val matrix = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(270f); postScale(-1f, 1f) }
                8 -> setRotate(270f)
            }
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        val largest = maxOf(rotated.width, rotated.height)
        if (largest <= size) return rotated
        val scaled = Bitmap.createScaledBitmap(rotated, (rotated.width.toFloat() * size / largest).toInt().coerceAtLeast(1), (rotated.height.toFloat() * size / largest).toInt().coerceAtLeast(1), true)
        if (scaled !== rotated) rotated.recycle()
        return scaled
    }

    fun bind(view: ImageView, path: String?, scope: CoroutineScope) {
        val request = Any()
        view.tag = request
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        view.setBackgroundResource(R.drawable.bg_avatar_placeholder)
        val padding = (12 * view.resources.displayMetrics.density).toInt()
        view.setPadding(padding, padding, padding, padding)
        view.setImageResource(R.drawable.ic_user)
        if (path.isNullOrBlank()) return
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { decode(view.context.applicationContext, Uri.parse(path), 512) }.getOrNull()
            } ?: return@launch
            if (view.tag !== request) { bitmap.recycle(); return@launch }
            view.setPadding(0, 0, 0, 0)
            view.setImageDrawable(RoundedBitmapDrawableFactory.create(view.resources, bitmap).apply { isCircular = true })
        }
    }
}

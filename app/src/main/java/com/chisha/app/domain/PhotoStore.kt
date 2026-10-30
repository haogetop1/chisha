/*
 * 吃啥 · 离线单机做菜 App
 * Copyright (C) 2026 haogetop1
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.chisha.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 成品图统一落在 filesDir/photos/，数据库只存文件名，
 * 这样应用升级或数据库重建都不会让路径失效。
 * 长边压到 1280，JPEG 质量 85，单张约 150~300KB。
 */
object PhotoStore {

    private const val MAX_EDGE = 1280

    fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    fun newFileName(): String = "${UUID.randomUUID()}.jpg"

    fun cameraTarget(context: Context, fileName: String): File =
        File(dir(context), fileName)

    fun fileFor(context: Context, fileName: String): File =
        File(dir(context), fileName)

    /** 从相册 Uri 读入、压缩、落盘，返回文件名 */
    fun importFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, opts)
                input.close()

                val sample = calcInSampleSize(opts.outWidth, opts.outHeight)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(uri)?.use { again ->
                    BitmapFactory.decodeStream(again, null, decodeOpts)
                }
            } ?: return null

            val scaled = scaleDown(bitmap)
            val name = newFileName()
            FileOutputStream(File(dir(context), name)).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            name
        }.getOrNull()
    }

    /** 相机写在 filesDir/photos 下的原图，也压缩一遍 */
    fun compressInPlace(context: Context, fileName: String) {
        runCatching {
            val f = File(dir(context), fileName)
            if (!f.exists()) return
            val bitmap = BitmapFactory.decodeFile(f.absolutePath) ?: return
            val scaled = scaleDown(bitmap)
            FileOutputStream(f).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }
    }

    private fun calcInSampleSize(w: Int, h: Int): Int {
        if (w <= 0 || h <= 0) return 1
        var sample = 1
        val longest = maxOf(w, h)
        while (longest / sample > MAX_EDGE * 2) sample *= 2
        return sample
    }

    private fun scaleDown(src: Bitmap): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= MAX_EDGE) return src
        val ratio = MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}

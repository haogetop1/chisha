package com.chisha.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 菜谱成品图：全部打包在 assets/dish_photos/{id}.webp，离线可用。
 * 没有图的菜（抓取失败的）会走原来的菜系渐变插画，不会开天窗。
 */
object DishPhotos {

    private const val DIR = "dish_photos"
    private val available = HashSet<String>()
    private var appContext: Context? = null

    /** 24MB 内存上限，够缓存 20 来张 512×512，滚动时不重复解码 */
    private val mem = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun load(context: Context) {
        if (available.isNotEmpty()) return
        appContext = context.applicationContext
        runCatching {
            context.assets.list(DIR)?.forEach { name ->
                if (name.endsWith(".webp")) available.add(name.removeSuffix(".webp"))
            }
        }
    }

    fun has(id: String): Boolean = available.contains(id)
    val count: Int get() = available.size

    /** 在 IO 线程解码，结果进 LRU */
    suspend fun decode(id: String): Bitmap? = withContext(Dispatchers.IO) {
        mem.get(id)?.let { return@withContext it }
        val ctx = appContext ?: return@withContext null
        val bmp = runCatching {
            ctx.assets.open("$DIR/$id.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return@withContext null
        mem.put(id, bmp)
        bmp
    }
}

/** 有图就返回 ImageBitmap，没图返回 null（调用方负责画兜底插画） */
@Composable
fun rememberDishPhoto(id: String): ImageBitmap? {
    if (!DishPhotos.has(id)) return null
    return produceState<ImageBitmap?>(null, id) {
        value = DishPhotos.decode(id)?.asImageBitmap()
    }.value
}

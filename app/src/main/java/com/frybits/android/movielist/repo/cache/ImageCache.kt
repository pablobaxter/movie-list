package com.frybits.android.movielist.repo.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.lruCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "ImageCache"

/**
 * Simple image caching layer
 */
interface ImageCache {

    /**
     * Store the image to both disk and memory cache
     */
    suspend fun storeImage(key: String, bitmap: Bitmap)

    /**
     * Retrieve the image from disk or memory cache. Returns null if no image exists.
     */
    suspend fun retrieveImage(key: String): Bitmap?

    /**
     * Clears bitmaps from being referenced in the in-memory cache.
     */
    fun onLowMemory()
}

class ImageCacheImpl @Inject constructor(@ApplicationContext context: Context) : ImageCache {

    private val diskCacheFolder = File(context.cacheDir, "images")

    // Memory cache logic from https://developer.android.com/topic/performance/graphics/cache-bitmap#memory-cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val bitmapMemoryCache = lruCache<String, Bitmap>(
        maxSize = maxMemory / 8,
        sizeOf = { _, bitmap -> bitmap.byteCount / 1024 }
    )

    override suspend fun storeImage(key: String, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            ensureDirectoryCreated()
            File(diskCacheFolder, key).outputStream().buffered().use { outputStream ->
                // Currently saving as PNG, but might want to be smarter about this for other formats
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }

        // LRUCache is not thread safe, so always use the same thread when reading/writing to it
        withContext(Dispatchers.Main) {
            bitmapMemoryCache.put(key, bitmap)
        }
    }

    override suspend fun retrieveImage(key: String): Bitmap? {
        // Run in the same loop if we are already in the main thread
        return withContext(Dispatchers.Main.immediate) {
            Log.d(LOG_TAG, "Attempting memory cache...")
            return@withContext bitmapMemoryCache[key] ?: try {
                // Memory cache miss...
                Log.d(LOG_TAG, "Memory cache miss, attempting disk...")
                val bitmap = withContext(Dispatchers.IO) {
                    ensureDirectoryCreated()
                    File(diskCacheFolder, key).inputStream().buffered().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
                // Put image into cache
                bitmapMemoryCache.put(key, bitmap)
                return@withContext bitmap
            } catch (e: Exception) { // Disk errors should be considered disk cache misses. Just return null.
                Log.e(LOG_TAG, "Unable to retrieve image from disk", e)
                null
            }
        }
    }

    override fun onLowMemory() {
        // Do not recycle the bitmaps as they might still be in use.
        bitmapMemoryCache.evictAll()
    }

    private fun ensureDirectoryCreated() {
        if (!diskCacheFolder.exists() || !diskCacheFolder.isDirectory) {
            diskCacheFolder.mkdirs()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageCacheModule {

    @Singleton
    @Binds
    abstract fun bindImageCache(imageCacheImpl: ImageCacheImpl): ImageCache
}

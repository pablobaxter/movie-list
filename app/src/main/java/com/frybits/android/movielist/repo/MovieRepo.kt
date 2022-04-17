package com.frybits.android.movielist.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.frybits.android.movielist.repo.cache.ImageCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface MovieRepo {

    fun allMoviesFlow(): Flow<Result<List<GetMoviesQuery.Movie>>>

    suspend fun getMovie(id: Int): Result<GetMovieQuery.Movie>

    suspend fun getMoviePoster(id: Int): Result<Bitmap>

    fun onLowMemory()
}

class MovieRepoImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val okHttpClient: OkHttpClient,
    private val imageCache: ImageCache
): MovieRepo {

    override fun allMoviesFlow(): Flow<Result<List<GetMoviesQuery.Movie>>> {
        return apolloClient.query(GetMoviesQuery()).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().map { response ->
            return@map runCatching { requireNotNull(response.dataAssertNoErrors.movies) { "No movies found" }.filterNotNull() }
        }
    }

    override suspend fun getMovie(id: Int): Result<GetMovieQuery.Movie> {
        return runCatching {
            return@runCatching requireNotNull(apolloClient.query(GetMovieQuery(id)).execute().dataAssertNoErrors.movie) { "Movie $id not found" }
        }
    }

    override suspend fun getMoviePoster(id: Int): Result<Bitmap> {
        return getMovie(id).mapCatching { movie ->
            val posterPath = requireNotNull(movie.posterPath) { "No movie poster path found" }
            val imageKey = URI(posterPath).path.dropWhile { it == '/' }.replace('/', '_')
            val image = imageCache.retrieveImage(imageKey)
            if (image == null) {
                val networkImage = requireNotNull(getImageFromNetwork(posterPath)) { "No image poster found" }
                imageCache.storeImage(imageKey, networkImage)
                return@mapCatching networkImage
            } else {
                return@mapCatching image
            }
        }
    }

    private suspend fun getImageFromNetwork(posterPath: String): Bitmap? {
        val request = Request.Builder()
            .get()
            .url(posterPath)
            .build()

        return suspendCancellableCoroutine { cont ->
            val call = okHttpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val byteStream = requireNotNull(response.body?.byteStream()) { "Image network response gave no data" }
                    cont.resume(BitmapFactory.decodeStream(byteStream))
                }

                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }
            })
            cont.invokeOnCancellation { call.cancel() }
        }
    }

    override fun onLowMemory() {
        imageCache.onLowMemory()
    }
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class MovieRepoModule {
    
    @Binds
    @ViewModelScoped
    abstract fun bindMovieRepo(movieRepoImpl: MovieRepoImpl): MovieRepo
}

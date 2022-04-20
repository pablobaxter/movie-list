package com.frybits.android.movielist.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.frybits.android.movielist.repo.cache.ImageCache
import com.frybits.android.movielist.repo.type.Sort
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

/**
 * Movie repository for getting everything movie related
 */
interface MovieRepo {

    /**
     * Gets the movie object with the give id
     */
    suspend fun getMovie(id: Int): Result<GetMovieQuery.Movie>

    /**
     * Retrieve the movie poster image for the given movie id
     */
    suspend fun getMoviePoster(id: Int): Result<Bitmap>

    /**
     * Retrieve the cast profile image for the given cast
     */
    suspend fun getCastProfile(cast: GetMovieQuery.Cast): Result<Bitmap>

    /**
     * Get the list of all genres
     */
    suspend fun getMovieGenres(): Result<List<String>>

    /**
     * Get a list of movies with the given query parameters. Default is all movies.
     */
    suspend fun getMoviesByQuery(
        genre: String? = null,
        search: String? = null,
        limit: Int? = null,
        offset: Int? = null,
        orderBy: String? = null,
        sort: Sort? = null
    ): Result<List<GetMoviesQuery.Movie>>

    /**
     * Helper function to clear memory usage of this repo if the system memory gets too low
     */
    fun onLowMemory()
}

class MovieRepoImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val okHttpClient: OkHttpClient,
    private val imageCache: ImageCache
): MovieRepo {

    override suspend fun getMovie(id: Int): Result<GetMovieQuery.Movie> {
        return runCatching { // In all these cases, allow Apollo data throw exceptions. Kotlin results will handle everything else.
            return@runCatching requireNotNull(apolloClient.query(GetMovieQuery(id)).execute().dataAssertNoErrors.movie) { "Movie $id not found" }
        }
    }

    override suspend fun getMoviePoster(id: Int): Result<Bitmap> {
        return getMovie(id).mapCatching { movie -> // Get the latest movie info...
            val posterPath = requireNotNull(movie.posterPath) { "No movie poster path found" }
            return@mapCatching getImageFromCacheThenNetwork(posterPath) // Then get the image for the movie poster path
        }
    }

    override suspend fun getCastProfile(cast: GetMovieQuery.Cast): Result<Bitmap> {
        return runCatching {
            val profilePath = requireNotNull(cast.profilePath) { "No profile path found" }
            return@runCatching getImageFromCacheThenNetwork(profilePath)
        }
    }

    override suspend fun getMovieGenres(): Result<List<String>> {
        return runCatching {
            requireNotNull(apolloClient.query(GetMovieGenresQuery()).execute().dataAssertNoErrors.genres) { "No movies found" }
        }
    }

    override suspend fun getMoviesByQuery(
        genre: String?,
        search: String?,
        limit: Int?,
        offset: Int?,
        orderBy: String?,
        sort: Sort?
    ): Result<List<GetMoviesQuery.Movie>> {
        return runCatching {
            apolloClient.query(
                GetMoviesQuery( // All query parameters are optional, to allow for getting all movies
                    genre = Optional.presentIfNotNull(genre),
                    search = Optional.presentIfNotNull(search),
                    limit = Optional.presentIfNotNull(limit),
                    offset = Optional.presentIfNotNull(offset),
                    orderBy = Optional.presentIfNotNull(orderBy),
                    sort = Optional.presentIfNotNull(sort)
                )
            ).execute().dataAssertNoErrors.movies.orEmpty().filterNotNull()
        }
    }

    // Helper function to query the local cache before hitting the network for any images with the given path
    private suspend fun getImageFromCacheThenNetwork(urlPath: String): Bitmap {
        val imageKey = urlPath.generateKeyFromPath()
        val image = imageCache.retrieveImage(imageKey)
        return if (image == null) {
            val networkImage = requireNotNull(getImageFromNetwork(urlPath)) { "No image found" }
            imageCache.storeImage(imageKey, networkImage)
            networkImage
        } else {
            image
        }
    }

    // Helper function to get images from the network with the given url
    private suspend fun getImageFromNetwork(urlPath: String): Bitmap? {
        val request = Request.Builder()
            .get()
            .url(urlPath)
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

// This helps generate a unique key of a URL based off the path
private fun String.generateKeyFromPath(): String {
    return URI(this).path.dropWhile { it == '/' }.replace('/', '_')
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class MovieRepoModule {
    
    @Binds
    @ViewModelScoped
    abstract fun bindMovieRepo(movieRepoImpl: MovieRepoImpl): MovieRepo
}

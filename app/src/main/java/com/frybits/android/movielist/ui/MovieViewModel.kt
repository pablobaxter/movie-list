package com.frybits.android.movielist.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.frybits.android.movielist.repo.GetMovieQuery
import com.frybits.android.movielist.repo.GetMoviesQuery
import com.frybits.android.movielist.repo.MovieRepo
import com.frybits.android.movielist.repo.type.Sort
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val LOG_TAG = "DemoViewModel"

@HiltViewModel
class MovieViewModel @Inject constructor(private val movieRepo: MovieRepo) : ViewModel() {

    suspend fun getMovie(id: Int): GetMovieQuery.Movie? {
        return movieRepo.getMovie(id).onFailure {
            Log.d(LOG_TAG, "Unable to get movie $id", it)
        }.getOrNull()
    }

    suspend fun getMoviePoster(id: Int): Bitmap? {
        return movieRepo.getMoviePoster(id).onFailure {
            Log.d(LOG_TAG, "Unable to get movie poster", it)
        }.getOrNull()
    }

    suspend fun getCastProfile(cast: GetMovieQuery.Cast): Bitmap? {
        return movieRepo.getCastProfile(cast).onFailure {
            Log.d(LOG_TAG, "Unable to get cast profile", it)
        }.getOrNull()
    }

    suspend fun getTop5Movies(): List<GetMoviesQuery.Movie> {
        return movieRepo.getMoviesByQuery(limit = 5, orderBy = "voteAverage", sort = Sort.DESC).getOrElse {
            Log.d(LOG_TAG, "Unable to get top 5 movies", it)
            return@getOrElse emptyList()
        }
    }

    suspend fun getMovieGenres(): List<String> {
        return movieRepo.getMovieGenres().getOrElse {
            Log.d(LOG_TAG, "Unable to get movie genres", it)
            return@getOrElse emptyList()
        }
    }

    suspend fun getMoviesByQuery(genre: String?, orderBy: OrderCategory): List<GetMoviesQuery.Movie> {
        return movieRepo.getMoviesByQuery(genre = genre, orderBy = orderBy.toApolloStringQuery(), sort = orderBy.getOrderBy()).getOrElse {
            Log.d(LOG_TAG, "Unable to get movie genre $genre", it)
            return@getOrElse emptyList()
        }
    }

    suspend fun getAllMovies(): List<GetMoviesQuery.Movie> {
        return movieRepo.getMoviesByQuery().getOrElse {
            Log.d(LOG_TAG, "Movie list query failure", it)
            return@getOrElse emptyList()
        }
    }

    fun onLowMemory() {
        movieRepo.onLowMemory()
    }
}

enum class OrderCategory {
    POPULARITY, RUNTIME, TITLE, VOTER_AVERAGE
}

private fun OrderCategory.toApolloStringQuery(): String {
    return when (this) {
        OrderCategory.POPULARITY -> "popularity"
        OrderCategory.VOTER_AVERAGE -> "voteAverage"
        OrderCategory.TITLE -> "title"
        OrderCategory.RUNTIME -> "runtime"
    }
}

private fun OrderCategory.getOrderBy(): Sort {
    return when (this) {
        OrderCategory.POPULARITY,
        OrderCategory.VOTER_AVERAGE,
        OrderCategory.RUNTIME -> Sort.DESC
        else -> Sort.ASC
    }
}

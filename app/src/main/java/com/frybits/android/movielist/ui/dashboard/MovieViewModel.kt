package com.frybits.android.movielist.ui.dashboard

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frybits.android.movielist.repo.GetMovieQuery
import com.frybits.android.movielist.repo.GetMoviesQuery
import com.frybits.android.movielist.repo.MovieRepo
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "DemoViewModel"

@HiltViewModel
class DemoViewModel @Inject constructor(private val movieRepo: MovieRepo) : ViewModel() {

    val moviesFlow: Flow<List<GetMoviesQuery.Movie>> = movieRepo.allMoviesFlow().map { result ->
        return@map result.getOrElse {
            Log.d(LOG_TAG, "Movie flow failure", it)
            emptyList()
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

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

    fun onLowMemory() {
        movieRepo.onLowMemory()
    }
}

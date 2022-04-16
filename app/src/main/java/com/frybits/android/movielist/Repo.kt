package com.frybits.android.movielist

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class Repo {

    suspend fun getMovies(): Result<GetMoviesQuery.Data?> {
        val response = ApiClient.getInstance().movieClient.query(GetMoviesQuery()).execute()
        return if (response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(java.lang.Exception())
        }
    }

    companion object {
        private var INSTANCE: Repo? = null
        fun getInstance() = INSTANCE
            ?: Repo().also {
                INSTANCE = it
            }
    }
}
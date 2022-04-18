package com.frybits.android.movielist.ui.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.frybits.android.movielist.R
import com.frybits.android.movielist.databinding.ViewMovieItemBinding
import com.frybits.android.movielist.repo.GetMoviesQuery
import com.frybits.android.movielist.ui.dashboard.MovieViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TITLE_VIEW_TYPE = 1
private const val CHILD_RECYCLERVIEW_VIEW_TYPE = 2
private const val MOVIE_VIEW_TYPE = 3

class MovieRecyclerViewAdapter(
    private val scope: CoroutineScope,
    private val movieViewModel: MovieViewModel
): ListAdapter<GetMoviesQuery.Movie, MovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {

    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0, 2, 4 -> TITLE_VIEW_TYPE
            1, 3 -> CHILD_RECYCLERVIEW_VIEW_TYPE
            else -> MOVIE_VIEW_TYPE
        }
    }
}

private class ChildMovieRecyclerViewAdapter(
    private val scope: CoroutineScope,
    private val movieViewModel: MovieViewModel
) : ListAdapter<GetMoviesQuery.Movie, MovieViewHolder.ChildMovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder.ChildMovieViewHolder {
        return MovieViewHolder.ChildMovieViewHolder(
            scope,
            movieViewModel,
            ViewMovieItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MovieViewHolder.ChildMovieViewHolder, position: Int) {
        val movieItem = getItem(position)
        holder.bindMovie(movieItem)
    }
}

sealed class MovieViewHolder(rootView: View): RecyclerView.ViewHolder(rootView) {

    private class TitleViewHolder(private val textView: TextView): MovieViewHolder(textView) {

        fun setTitle(title: String) {
            textView.text = title
        }
    }

    private class ChildListViewHolder(
        private val childMovieRecyclerViewAdapter: ChildMovieRecyclerViewAdapter,
        recyclerView: RecyclerView
    ): MovieViewHolder(recyclerView) {

        init {
            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.adapter = childMovieRecyclerViewAdapter
        }

        fun submitMovieList(movieList: List<GetMoviesQuery.Movie>) {
            childMovieRecyclerViewAdapter.submitList(movieList)
        }
    }

    class ChildMovieViewHolder(
        private val scope: CoroutineScope,
        private val movieViewModel: MovieViewModel,
        private val binding: ViewMovieItemBinding
    ): MovieViewHolder(binding.root) {

        private var bindingJob: Job? = null

        fun bindMovie(movie: GetMoviesQuery.Movie) {
            bindingJob?.cancel()
            bindingJob = scope.launch {
                try {
                    binding.imageLoadingProgressBar.visibility = View.VISIBLE
                    binding.moviePosterImageView.setImageResource(R.drawable.ic_baseline_person_24)
                    binding.movieTitleTextView.text = movie.title
                    val movieImage = movieViewModel.getMoviePoster(movie.id)
                    if (movieImage != null) {
                        binding.moviePosterImageView.setImageBitmap(movieImage)
                    }
                } finally {
                    binding.imageLoadingProgressBar.visibility = View.GONE
                }
            }
        }
    }
}

// Helper class for the ListAdapter
private object MovieDiffCallback : DiffUtil.ItemCallback<GetMoviesQuery.Movie>() {
    override fun areItemsTheSame(oldItem: GetMoviesQuery.Movie, newItem: GetMoviesQuery.Movie): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: GetMoviesQuery.Movie, newItem: GetMoviesQuery.Movie): Boolean {
        return oldItem == newItem
    }
}

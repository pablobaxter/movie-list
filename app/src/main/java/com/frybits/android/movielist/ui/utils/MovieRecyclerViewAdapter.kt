package com.frybits.android.movielist.ui.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.frybits.android.movielist.R
import com.frybits.android.movielist.databinding.ViewMovieItemBinding
import com.frybits.android.movielist.repo.GetMoviesQuery
import com.frybits.android.movielist.ui.MovieViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Movie RecyclerViewAdapter for creating the card views for each movie object
 */
class MovieRecyclerViewAdapter(
    private val scope: CoroutineScope, // RecyclerViews should not create their own scope
    private val movieViewModel: MovieViewModel,
    private val movieClickDelegate: (movieId: Int) -> Unit
): ListAdapter<GetMoviesQuery.Movie, MovieRecyclerViewAdapter.MovieViewHolder>(MovieDiffCallback) {

    inner class MovieViewHolder(private val binding: ViewMovieItemBinding): RecyclerView.ViewHolder(binding.root) {

        // Keep reference to previous view coroutine job, in order to cancel if this viewholder is recycled
        private var bindingJob: Job? = null
        fun bindMovie(movie: GetMoviesQuery.Movie) {
            bindingJob?.cancel() // Cancel last job
            bindingJob = scope.launch { // Launch new job for this movie
                binding.moviePosterImageView.setImageResource(R.drawable.ic_baseline_movies_24)
                binding.movieTitleTextView.text = movie.title
                try {
                    binding.imageLoadingProgressBar.visibility = View.VISIBLE
                    val movieImage = movieViewModel.getMoviePoster(movie.id) ?: return@launch
                    binding.moviePosterImageView.setImageBitmap(movieImage)
                } finally { // Never leave the progress bar running indefinitely
                    binding.imageLoadingProgressBar.visibility = View.GONE
                }
            }
            binding.root.setOnClickListener {
                movieClickDelegate(movie.id) // Delegate the click listener to the object creator
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return MovieViewHolder(ViewMovieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bindMovie(getItem(position))
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

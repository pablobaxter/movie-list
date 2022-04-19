package com.frybits.android.movielist.ui.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.frybits.android.movielist.R
import com.frybits.android.movielist.databinding.ViewMovieItemBinding
import com.frybits.android.movielist.databinding.ViewRecyclerviewItemBinding
import com.frybits.android.movielist.databinding.ViewTitleItemBinding
import com.frybits.android.movielist.repo.GetMoviesQuery
import com.frybits.android.movielist.ui.dashboard.MovieViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TITLE_VIEW_TYPE = 1
private const val CHILD_RECYCLERVIEW_VIEW_TYPE = 2
private const val MOVIE_VIEW_TYPE = 3

private const val TOP_ITEMS_SIZE = 5

class MovieRecyclerViewAdapter(
    private val scope: CoroutineScope,
    private val movieViewModel: MovieViewModel
): ListAdapter<GetMoviesQuery.Movie, MovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TITLE_VIEW_TYPE -> TitleViewHolder(ViewTitleItemBinding.inflate(layoutInflater, parent, false))
            CHILD_RECYCLERVIEW_VIEW_TYPE -> ChildListViewHolder(ChildRecyclerViewAdapter(scope, movieViewModel), ViewRecyclerviewItemBinding.inflate(layoutInflater, parent, false))
            MOVIE_VIEW_TYPE -> ChildMovieViewHolder(scope, movieViewModel, ViewMovieItemBinding.inflate(layoutInflater, parent, false))
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    private var top5MoviesJob: Job? = null
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        when (position) {
            0 -> {
                require(holder is TitleViewHolder) { "Expected ${TitleViewHolder::class.java.simpleName}, but got ${holder::class.java.simpleName}" }
                holder.setTitle("Movies: Top 5")
            }
            1 -> {
                require(holder is ChildListViewHolder) { "Expected ${ChildListViewHolder::class.java.simpleName}, but got ${holder::class.java.simpleName}" }
                top5MoviesJob?.cancel()
                top5MoviesJob = scope.launch {
                    holder.submitMovieList(movieViewModel.getTop5Movies())
                }
            }
            2 -> {
                require(holder is TitleViewHolder) { "Expected ${TitleViewHolder::class.java.simpleName}, but got ${holder::class.java.simpleName}" }
                holder.setTitle("Genres")
                // TODO
            }
            3 -> {
                require(holder is ChildListViewHolder) { "Expected ${ChildListViewHolder::class.java.simpleName}, but got ${holder::class.java.simpleName}" }
                holder.submitMovieList(emptyList())
                // TODO
            }
            4 -> {
                require(holder is TitleViewHolder) { "Expected ${TitleViewHolder::class.java.simpleName}, but got ${holder::class.java.simpleName}" }
                holder.setTitle("All Movies")
            }
            else -> {
                require(holder is ChildMovieViewHolder) { "Expected ${ChildMovieViewHolder::class.java.simpleName}, but got ${holder::class.java.simpleName}" }
                holder.bindMovie(getItem(position - TOP_ITEMS_SIZE))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0, 2, 4 -> TITLE_VIEW_TYPE
            1, 3 -> CHILD_RECYCLERVIEW_VIEW_TYPE
            else -> MOVIE_VIEW_TYPE
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + TOP_ITEMS_SIZE
    }
}

sealed class MovieViewHolder(rootView: View): RecyclerView.ViewHolder(rootView)

private class TitleViewHolder(private val binding: ViewTitleItemBinding): MovieViewHolder(binding.root) {

    fun setTitle(title: String) {
        binding.titleTextView.text = title
    }
}

private class ChildListViewHolder(
    private val childRecyclerViewAdapter: ChildRecyclerViewAdapter,
    binding: ViewRecyclerviewItemBinding
): MovieViewHolder(binding.root) {

    init {
        binding.childRecyclerView.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
        binding.childRecyclerView.adapter = childRecyclerViewAdapter
    }

    fun submitMovieList(movieList: List<GetMoviesQuery.Movie>) {
        childRecyclerViewAdapter.submitList(movieList)
    }
}

private class ChildMovieViewHolder(
    private val scope: CoroutineScope,
    private val movieViewModel: MovieViewModel,
    private val binding: ViewMovieItemBinding
): MovieViewHolder(binding.root) {

    private var bindingJob: Job? = null
    fun bindMovie(movie: GetMoviesQuery.Movie) {
        bindingJob?.cancel()
        bindingJob = scope.launch {
            binding.moviePosterImageView.setImageResource(R.drawable.ic_baseline_movies_24)
            binding.movieTitleTextView.text = movie.title
            try {
                binding.imageLoadingProgressBar.visibility = View.VISIBLE
                val movieImage = movieViewModel.getMoviePoster(movie.id) ?: return@launch
                binding.moviePosterImageView.setImageBitmap(movieImage)
            } finally {
                binding.imageLoadingProgressBar.visibility = View.GONE
            }
        }
    }
}

private class ChildRecyclerViewAdapter(
    private val scope: CoroutineScope,
    private val movieViewModel: MovieViewModel
): ListAdapter<GetMoviesQuery.Movie, ChildMovieViewHolder>(MovieDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildMovieViewHolder {
        return ChildMovieViewHolder(scope, movieViewModel, ViewMovieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ChildMovieViewHolder, position: Int) {
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

package com.frybits.android.movielist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.frybits.android.movielist.databinding.FragmentMovieDetailBinding
import com.frybits.android.movielist.databinding.ViewActorItemBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@AndroidEntryPoint
class MovieDetailFragment : Fragment() {

    private val viewModel by activityViewModels<MovieViewModel>()
    private val args by navArgs<MovieDetailFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMovieDetailBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            val movie = try {
                binding.movieDetailLoadProgressBar.visibility = View.VISIBLE
                viewModel.getMovie(args.movieId) ?: throw IllegalStateException("Unable to retrieve movie")
            } finally {
                binding.movieDetailLoadProgressBar.visibility = View.GONE
            }

            val postLoadJob = launch {
                try {
                    binding.posterImageProgressBar.visibility = View.VISIBLE
                    binding.moviePosterImageView.setImageBitmap(viewModel.getMoviePoster(args.movieId))
                } finally {
                    binding.posterImageProgressBar.visibility = View.GONE
                }
            }

            binding.movieTitleTextView.text = movie.title
            println(movie.voteAverage.toFloat())
            binding.movieRatingBar.rating = movie.voteAverage.toFloat() / 2
            binding.overviewTextView.text = movie.overview
            binding.directorNameTextView.text = movie.director.name
            val castProfileLoadJobs = movie.cast.map { cast ->
                val castView = ViewActorItemBinding.inflate(inflater, binding.actorLinearLayout, true)
                castView.actorNameTextView.text = cast.name
                castView.characterNameTextView.text = cast.character
                return@map launch { castView.imageView.setImageBitmap(viewModel.getCastProfile(cast)) }
            }

            postLoadJob.join()
            castProfileLoadJobs.joinAll()

            binding.root.invalidate()
        }

        return binding.root
    }
}

package com.frybits.android.movielist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.frybits.android.movielist.R
import com.frybits.android.movielist.databinding.FragmentMovieDetailBinding
import com.frybits.android.movielist.databinding.ViewActorItemBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.time.Duration

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

            val postLoadJob = launch poster@{
                binding.moviePosterImageView.setImageResource(R.drawable.ic_baseline_movies_24)
                try {
                    binding.posterImageProgressBar.visibility = View.VISIBLE
                    val postImage = viewModel.getMoviePoster(args.movieId) ?: return@poster
                    binding.moviePosterImageView.setImageBitmap(postImage)
                } finally {
                    binding.posterImageProgressBar.visibility = View.GONE
                }
            }

            binding.movieTitleTextView.text = movie.title
            binding.movieRatingBar.rating = movie.voteAverage.toFloat() / 2
            binding.overviewTextView.text = movie.overview
            binding.directorNameTextView.text = movie.director.name
            binding.genresTextView.text = movie.genres.joinToString(", ")
            val duration = Duration.ofMinutes(movie.runtime.toLong())
            binding.runtimeTextView.text = resources.getString(R.string.duration, duration.toHours() % 24, duration.toMinutes() % 60)
            val castProfileLoadJobs = movie.cast.map { cast ->
                val castView = ViewActorItemBinding.inflate(inflater, binding.actorLinearLayout, true)
                castView.actorNameTextView.text = cast.name
                castView.characterNameTextView.text = cast.character
                return@map launch cast@{
                    castView.imageView.setImageResource(R.drawable.ic_baseline_person_24)
                    try {
                        castView.actorImageProgressBar.visibility = View.VISIBLE
                        val profileImage = viewModel.getCastProfile(cast) ?: return@cast
                        castView.imageView.setImageBitmap(profileImage)
                    } finally {
                        castView.actorImageProgressBar.visibility = View.GONE
                    }
                }
            }

            postLoadJob.join()
            castProfileLoadJobs.joinAll()

            binding.root.invalidate()
        }

        return binding.root
    }
}

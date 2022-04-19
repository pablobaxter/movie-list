package com.frybits.android.movielist.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.frybits.android.movielist.R
import com.frybits.android.movielist.databinding.FragmentMoviesBinding
import com.frybits.android.movielist.ui.utils.MovieRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieListFragment : Fragment() {

    private val viewModel by activityViewModels<MovieViewModel>()
    private val args by navArgs<MovieListFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMoviesBinding.inflate(inflater, container, false)
        val adapter = MovieRecyclerViewAdapter(lifecycleScope, viewModel) {
            findNavController().navigate(MovieListFragmentDirections.actionMovieListFragmentToMovieDetailFragment(it))
        }

        val spanCount = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, resources.configuration.screenWidthDp.toFloat(), resources.displayMetrics) / resources.getDimension(R.dimen.movie_card_width)).toInt()

        binding.movieListTextView.text = args.genre ?: "All Movies"
        binding.movieListRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        binding.movieListRecyclerView.adapter = adapter

        lifecycleScope.launch {
            try {
                binding.movieListProgressBar.visibility = View.VISIBLE
                adapter.submitList(viewModel.getMoviesByGenre(args.genre))
            } finally {
                binding.movieListProgressBar.visibility = View.GONE
            }
        }

        return binding.root
    }
}

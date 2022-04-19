package com.frybits.android.movielist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.frybits.android.movielist.databinding.FragmentDashboardBinding
import com.frybits.android.movielist.ui.utils.GenresRecyclerViewAdapter
import com.frybits.android.movielist.ui.utils.MovieRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private val viewModel by activityViewModels<MovieViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val movieAdapter = MovieRecyclerViewAdapter(lifecycleScope, viewModel) {
            findNavController().navigate(DashboardFragmentDirections.actionNavigationDashboardToMovieDetailFragment(it))
        }
        val genreAdapter = GenresRecyclerViewAdapter {
            findNavController().navigate(DashboardFragmentDirections.actionNavigationDashboardToMovieListFragment(it))
        }

        binding.topMoviesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.topMoviesRecyclerView.adapter = movieAdapter

        binding.genresRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.genresRecyclerView.adapter = genreAdapter

        binding.allMoviesButton.setOnClickListener {
            findNavController().navigate(DashboardFragmentDirections.actionNavigationDashboardToMovieListFragment())
        }

        lifecycleScope.launch {
            launch { movieAdapter.submitList(viewModel.getTop5Movies()) }
            launch { genreAdapter.submitList(viewModel.getMovieGenres()) }
        }

        return binding.root
    }
}

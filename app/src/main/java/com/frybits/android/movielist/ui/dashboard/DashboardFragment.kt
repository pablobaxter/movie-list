package com.frybits.android.movielist.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.frybits.android.movielist.databinding.FragmentDashboardBinding
import com.frybits.android.movielist.ui.utils.MovieRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private val viewModel: MovieViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val spanCount = resources.configuration.screenWidthDp / 120
        val adapter = MovieRecyclerViewAdapter(lifecycleScope, viewModel)

        binding.mainRecyclerView.layoutManager = GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position <= 4) spanCount else 1
                }
            }
        }
        binding.mainRecyclerView.adapter = adapter
        lifecycleScope.launch {
            adapter.submitList(viewModel.getAllMovies())
        }
        return binding.root
    }
}

package com.frybits.android.movielist.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
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
            // This callback flow handles update to the dropdown menu in the movie list view
            callbackFlow {
                // Don't read the resources each time a new item is selected
                val runtimeText = resources.getString(R.string.runtime)
                val titleText = resources.getString(R.string.title)
                val voterAverageText = resources.getString(R.string.voter_average)

                val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        // Get the string array populating the spinner
                        val selectionList = resources.getStringArray(R.array.sort_options)

                        // Map the string to the OrderCategory and send it downstream
                        when (selectionList[position]) {
                            // The name says blocking, but in this case, it won't be since we are sending it all using the main thread
                            runtimeText -> trySendBlocking(OrderCategory.RUNTIME)
                            titleText -> trySendBlocking(OrderCategory.TITLE)
                            voterAverageText -> trySendBlocking(OrderCategory.VOTER_AVERAGE)
                            else -> trySendBlocking(OrderCategory.POPULARITY)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        trySendBlocking(OrderCategory.POPULARITY)
                    }
                }

                // Set the item selected listener
                binding.sortSpinner.onItemSelectedListener = onItemSelectedListener

                // When this coroutine is cancelled, remove the listener
                awaitClose { binding.sortSpinner.onItemSelectedListener = null }
            }.collectLatest { category -> // Each emit will cancel the previous coroutine and begin the movie query again
                try {
                    binding.movieListProgressBar.visibility = View.VISIBLE
                    adapter.submitList(null) // Blank out the page while the movies load
                    adapter.submitList(viewModel.getMoviesByQuery(args.genre, category))
                } finally {
                    binding.movieListProgressBar.visibility = View.GONE
                }
            }
        }

        return binding.root
    }
}

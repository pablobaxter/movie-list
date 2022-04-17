package com.frybits.android.movielist.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.frybits.android.movielist.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private val viewModel: DemoViewModel by activityViewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var tvTitle: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = binding.tvTitle
        tvTitle.text = "Movie List"

        viewModel.moviesFlow.onEach {
            println(it.size)
        }.launchIn(lifecycleScope)
    }
}

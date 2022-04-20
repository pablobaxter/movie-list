package com.frybits.android.movielist.ui.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.frybits.android.movielist.databinding.ViewGenreItemBinding

/**
 * Simple RecyclerViewAdapter for getting the list of genres to display with a given string list
 */
class GenresRecyclerViewAdapter(private val itemClickDelegate: (genre: String?) -> Unit) : ListAdapter<String, GenresRecyclerViewAdapter.GenreViewHolder>(StringDiffUtil) {

    inner class GenreViewHolder(private val binding: ViewGenreItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun setGenre(genre: String) {
            binding.textView.text = genre
            binding.root.setOnClickListener { // Don't handle click events here... pass them to the object creator
                itemClickDelegate(genre)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        return GenreViewHolder(ViewGenreItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        holder.setGenre(getItem(position))
    }
}

private object StringDiffUtil: DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}

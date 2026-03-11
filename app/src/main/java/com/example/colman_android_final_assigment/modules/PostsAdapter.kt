package com.example.colman_android_final_assigment.modules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.databinding.PostListItemBinding
import com.example.colman_android_final_assigment.model.Post
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostsAdapter(
    private val onPostClick: (Post) -> Unit = {}
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    inner class PostViewHolder(private val binding: PostListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var cityJob: Job? = null

        fun bind(post: Post) {
            binding.postTitle.text = post.title
            binding.postCategory.text = post.category

            // Cancel any in-flight city lookup from a recycled ViewHolder
            cityJob?.cancel()
            binding.postLocation.text = "" // clear while loading

            cityJob = CoroutineScope(Dispatchers.Main).launch {
                val cityName = CityApiService.getCityNameById(post.cityId)
                binding.postLocation.text = cityName
            }

            if (post.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(post.imageUrl)
                    .fit()
                    .centerCrop()
                    .into(binding.postImage)
            } else {
                binding.postImage.setImageDrawable(null)
            }

            binding.root.setOnClickListener { onPostClick(post) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}


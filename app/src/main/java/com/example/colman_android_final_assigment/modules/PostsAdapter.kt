package com.example.colman_android_final_assigment.modules

import android.view.LayoutInflater
import android.view.View
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PostsAdapter(
    private val onPostClick: (Post) -> Unit = {},
    initialCategoryNameById: Map<String, String> = emptyMap()
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    companion object {
        private const val PAYLOAD_CATEGORY_CHANGED = "payload_category_changed"
    }

    private var categoryNameById: Map<String, String> = initialCategoryNameById

    inner class PostViewHolder(private val binding: PostListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val viewHolderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var cityJob: Job? = null
        private var boundPostId: String? = null

        fun bind(post: Post) {
            boundPostId = post.id
            binding.postTitle.text = post.title

            // Set category synchronously from the provided map to avoid per-row DB lookups.
            val categoryName = categoryNameById[post.categoryId] ?: post.categoryId
            binding.postCategory.text = categoryName

            cityJob?.cancel()
            binding.postLocation.text = ""
            binding.cityLoadingSpinner.visibility = View.VISIBLE

            cityJob = viewHolderScope.launch {
                val cityName = CityApiService.getCityNameById(post.cityId)
                if (boundPostId == post.id) {
                    binding.postLocation.text = cityName
                    binding.cityLoadingSpinner.visibility = View.GONE
                }
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

        fun bindCategoryOnly(post: Post) {
            val categoryName = categoryNameById[post.categoryId] ?: post.categoryId
            binding.postCategory.text = categoryName
        }

        fun onRecycled() {
            cityJob?.cancel()
            binding.cityLoadingSpinner.visibility = View.GONE
            boundPostId = null
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

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_CATEGORY_CHANGED)) {
            holder.bindCategoryOnly(getItem(position))
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }

    fun updateCategoryNameMap(newMap: Map<String, String>) {
        if (newMap == categoryNameById) return

        val oldMap = categoryNameById
        categoryNameById = newMap

        currentList.forEachIndexed { index, post ->
            val oldName = oldMap[post.categoryId] ?: post.categoryId
            val newName = newMap[post.categoryId] ?: post.categoryId
            if (oldName != newName) {
                notifyItemChanged(index, PAYLOAD_CATEGORY_CHANGED)
            }
        }
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}

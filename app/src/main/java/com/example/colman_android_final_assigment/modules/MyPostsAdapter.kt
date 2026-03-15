package com.example.colman_android_final_assigment.modules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.database.AppLocalDb
import com.example.colman_android_final_assigment.databinding.MyPostListItemBinding
import com.example.colman_android_final_assigment.model.Post
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyPostsAdapter(
    private val onToggleStatus: (Post) -> Unit,
    private val onDelete: (Post) -> Unit,
    private val onEdit: (Post) -> Unit
) : ListAdapter<Post, MyPostsAdapter.ViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MyPostListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    inner class ViewHolder(private val binding: MyPostListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val uiScope = CoroutineScope(Dispatchers.Main)
        private var cityJob: Job? = null
        private var categoryJob: Job? = null
        private var boundPostId: String? = null

        fun bind(post: Post) {
            boundPostId = post.id
            binding.postTitle.text = post.title

            // Category loading logic
            categoryJob?.cancel()
            binding.postCategory.text = ""
            categoryJob = uiScope.launch {
                val categoryName = withContext(Dispatchers.IO) {
                    AppLocalDb.getDatabase(binding.root.context)
                        .categoryDao()
                        .getCategoryNameById(post.categoryId)
                        ?: post.categoryId
                }
                if (boundPostId == post.id) {
                    binding.postCategory.text = categoryName
                }
            }

            // City loading logic
            cityJob?.cancel()
            binding.postLocation.text = ""
            binding.cityLoadingSpinner.visibility = View.VISIBLE

            cityJob = uiScope.launch {
                val cityName = withContext(Dispatchers.IO) {
                    CityApiService.getCityNameById(post.cityId)
                }
                if (boundPostId == post.id) {
                    binding.postLocation.text = cityName
                    binding.cityLoadingSpinner.visibility = View.GONE
                }
            }

            if (post.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(post.imageUrl)
                    .placeholder(R.drawable.bg_input)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.postImage)
            } else {
                binding.postImage.setImageResource(R.drawable.bg_input)
            }

            binding.postGivenBadge.visibility = if (post.isTaken) View.VISIBLE else View.GONE
            binding.markAsGivenButton.text = if (post.isTaken) "Mark as Available" else "Mark as Given"

            binding.markAsGivenButton.setOnClickListener { onToggleStatus(post) }
            binding.deletePostButton.setOnClickListener { onDelete(post) }
            binding.editPostButton.setOnClickListener { onEdit(post) }
        }

        fun clear() {
            boundPostId = null
            cityJob?.cancel()
            cityJob = null
            categoryJob?.cancel()
            categoryJob = null
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}

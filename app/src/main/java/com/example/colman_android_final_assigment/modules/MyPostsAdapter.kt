package com.example.colman_android_final_assigment.modules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.databinding.MyPostListItemBinding
import com.example.colman_android_final_assigment.model.Post
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    inner class ViewHolder(private val binding: MyPostListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var cityJob: Job? = null
        private var boundPostId: String? = null

        fun bind(post: Post) {
            boundPostId = post.id
            binding.postTitle.text = post.title
            binding.postCategory.text = post.categoryId
            
            // City loading logic
            cityJob?.cancel()
            binding.postLocation.text = ""
            binding.cityLoadingSpinner.visibility = View.VISIBLE

            cityJob = CoroutineScope(Dispatchers.Main).launch {
                val cityName = CityApiService.getCityNameById(post.cityId)
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
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}

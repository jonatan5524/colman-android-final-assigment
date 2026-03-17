package com.example.colman_android_final_assigment.modules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.databinding.FragmentPostDetailsBinding
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.viewmodel.PostDetailsViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostDetailsFragment : Fragment() {
    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailsViewModel by viewModels()
    private val args: PostDetailsFragmentArgs by navArgs()

    private var categoryNameById: Map<String, String> = emptyMap()
    private var cityJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        viewModel.setPostId(args.postId)
    }

    private fun observeViewModel() {
        viewModel.categoryNameById.observe(viewLifecycleOwner) { map ->
            categoryNameById = map
            viewModel.post.value?.let(::bindPost)
        }

        viewModel.post.observe(viewLifecycleOwner) { post ->
            binding.detailsProgressBar.visibility = if (post == null) View.VISIBLE else View.GONE
            post?.let(::bindPost)
        }
    }

    private fun bindPost(post: Post) {
        binding.detailsTitle.text = post.title
        binding.detailsDescription.text = post.description
        binding.detailsCategory.text = getString(
            R.string.post_details_category,
            categoryNameById[post.categoryId] ?: post.categoryId
        )

        if (post.imageUrl.isNotBlank()) {
            Picasso.get()
                .load(post.imageUrl)
                .placeholder(R.drawable.bg_input)
                .error(R.drawable.bg_input)
                .into(binding.detailsImage)
        } else {
            binding.detailsImage.setImageResource(R.drawable.bg_input)
        }

        cityJob?.cancel()
        binding.detailsCity.text = getString(R.string.post_details_city_loading)
        cityJob = viewLifecycleOwner.lifecycleScope.launch {
            val cityName = CityApiService.getCityNameById(post.cityId)
            binding.detailsCity.text = getString(R.string.post_details_city, cityName)
        }
    }

    override fun onDestroyView() {
        cityJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}

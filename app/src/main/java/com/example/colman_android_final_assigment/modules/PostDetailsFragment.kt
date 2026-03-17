package com.example.colman_android_final_assigment.modules

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.databinding.FragmentPostDetailsBinding
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.model.User
import com.example.colman_android_final_assigment.viewmodel.PostDetailsViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class PostDetailsFragment : Fragment() {
    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailsViewModel by viewModels()
    private val args: PostDetailsFragmentArgs by navArgs()

    private var categoryNameById: Map<String, String> = emptyMap()
    private var cityJob: Job? = null
    private var currentPost: Post? = null
    private var ownerUser: User? = null
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.contactButton.setOnClickListener {
            contactOwnerOnWhatsApp()
        }

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
            currentPost = post
            post?.let {
                bindPost(it)
                viewModel.setOwnerUserId(it.userId)
            }
        }

        viewModel.ownerUser.observe(viewLifecycleOwner) { owner ->
            ownerUser = owner
            val ownerName = owner?.name?.ifBlank { getString(R.string.unknown_user) } ?: getString(R.string.unknown_user)
            val ownerPhone = owner?.phone?.ifBlank { getString(R.string.no_phone_number) } ?: getString(R.string.no_phone_number)
            binding.detailsOwnerName.text = getString(R.string.post_details_owner_name, ownerName)
            binding.detailsOwnerPhone.text = getString(R.string.post_details_owner_phone, ownerPhone)
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            currentUser = user
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

    private fun contactOwnerOnWhatsApp() {
        val post = currentPost ?: run {
            Toast.makeText(requireContext(), R.string.post_not_loaded, Toast.LENGTH_SHORT).show()
            return
        }

        val ownerName = ownerUser?.name?.takeIf { it.isNotBlank() } ?: getString(R.string.unknown_user)
        val currentUserName = currentUser?.name?.takeIf { it.isNotBlank() } ?: getString(R.string.default_current_user_name)
        val rawPhone = ownerUser?.phone
        val phone = rawPhone?.filter { it.isDigit() }.orEmpty()

        if (phone.isBlank()) {
            Toast.makeText(requireContext(), R.string.whatsapp_phone_missing, Toast.LENGTH_SHORT).show()
            return
        }

        val message = getString(
            R.string.whatsapp_contact_message,
            ownerName,
            currentUserName,
            post.title
        )
        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val whatsappUri = "https://wa.me/$phone?text=$encodedMessage".toUri()
        val intent = Intent(Intent.ACTION_VIEW, whatsappUri)

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        cityJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}

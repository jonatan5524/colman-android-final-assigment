package com.example.colman_android_final_assigment.modules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.colman_android_final_assigment.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.databinding.FragmentMyPostsBinding
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.viewmodel.MyPostsViewModel

class MyPostsFragment : Fragment() {
    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPostsViewModel by viewModels()
    private lateinit var adapter: MyPostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MyPostsAdapter(
            onToggleStatus = { post ->
                viewModel.togglePostStatus(post)
            },
            onDelete = { post ->
                showDeleteConfirmation(post)
            },
            onEdit = { post ->
                val action = MyPostsFragmentDirections.actionMyPostsFragmentToEditPostFragment(post.id)
                findNavController().navigate(action)
            }
        )
        binding.myPostsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@MyPostsFragment.adapter
        }
    }

    private fun showDeleteConfirmation(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_dialog_title))
            .setMessage(getString(R.string.delete_dialog_message))
            .setPositiveButton(getString(R.string.delete_confirm)) { _, _ ->
                viewModel.deletePost(post)
            }
            .setNegativeButton(getString(R.string.filter_action_cancel), null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.myPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.myPostsProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.myPostsProgressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.myPostsProgressBar.visibility = View.GONE
                    Toast.makeText(context, "Operation successful", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.myPostsProgressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

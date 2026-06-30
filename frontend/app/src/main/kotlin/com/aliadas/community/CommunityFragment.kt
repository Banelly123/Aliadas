package com.aliadas.community

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliadas.R
import com.aliadas.databinding.FragmentCommunityBinding
import com.aliadas.databinding.ItemPostBinding
import com.aliadas.network.PostRequest
import com.aliadas.network.PostResponse
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!
    private val posts = mutableListOf<PostResponse>()
    private lateinit var adapter: PostAdapter
    private var currentFilter = "recent"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostAdapter(posts,
            onLike = { post -> likePost(post) },
            onComment = { post -> showCommentsDialog(post) }
        )

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        binding.chipRecent.setOnClickListener {
            currentFilter = "recent"
            updateFilterButtonsVisuals(binding.chipRecent)
            loadPosts()
        }
        binding.chipPopular.setOnClickListener {
            currentFilter = "popular"
            updateFilterButtonsVisuals(binding.chipPopular)
            loadPosts()
        }
        binding.chipApoyo.setOnClickListener {
            currentFilter = "apoyo"
            updateFilterButtonsVisuals(binding.chipApoyo)
            loadPosts()
        }

        binding.btnPublish.setOnClickListener { publishPost() }
        binding.swipeRefresh.setOnRefreshListener { loadPosts() }

        loadPosts()
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                val res = RetrofitClient.api.getPosts(token, currentFilter)
                if (res.isSuccessful) {
                    posts.clear()
                    posts.addAll(res.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun publishPost() {
        val content = binding.etPost.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, escribe un testimonio para publicar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mapeo dinámico e inteligente según el Chip Bento seleccionado por la usuaria
        val category = when (binding.chipCategory.checkedChipId) {
            R.id.chipCatApoyo -> "apoyo"
            R.id.chipCatDuda -> "duda"
            R.id.chipCatReporte -> "reporte"
            else -> "general"
        }

        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                val res = RetrofitClient.api.createPost(token, PostRequest(content, category))
                if (res.isSuccessful) {
                    binding.etPost.setText("")
                    Toast.makeText(requireContext(), "✅ Publicado anónimamente", Toast.LENGTH_SHORT).show()
                    loadPosts()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Error al subir la publicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFilterButtonsVisuals(selectedButton: View) {
        binding.chipRecent.setBackgroundResource(R.drawable.bg_pill_inactive)
        binding.chipRecent.setTextColor(resources.getColor(android.R.color.black, null))
        binding.chipPopular.setBackgroundResource(R.drawable.bg_pill_inactive)
        binding.chipPopular.setTextColor(resources.getColor(android.R.color.black, null))
        binding.chipApoyo.setBackgroundResource(R.drawable.bg_pill_inactive)
        binding.chipApoyo.setTextColor(resources.getColor(android.R.color.black, null))

        selectedButton.setBackgroundResource(R.drawable.bg_pill_active)
        (selectedButton as? android.widget.Button)?.setTextColor(resources.getColor(R.color.white, null))
    }

    private fun likePost(post: PostResponse) {
        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                RetrofitClient.api.likePost(token, post.id)
                loadPosts()
            } catch (_: Exception) { }
        }
    }

    private fun showCommentsDialog(post: PostResponse) {
        val dialog = CommentsBottomSheet.newInstance(post.id)
        dialog.show(childFragmentManager, "comments")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
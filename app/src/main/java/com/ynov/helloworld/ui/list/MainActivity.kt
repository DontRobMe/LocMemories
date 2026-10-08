package com.ynov.helloworld.ui.list

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.ynov.helloworld.R
import com.ynov.helloworld.databinding.ActivityMainBinding
import com.ynov.helloworld.ui.add.AddNoteActivity
import com.ynov.helloworld.ui.detail.NoteDetailActivity
import com.ynov.helloworld.ui.map.MapActivity
import com.ynov.helloworld.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

/** Écran d'accueil (vue de [NoteListViewModel]) ; ouvre l'introduction au tout premier lancement. */
class MainActivity : AppCompatActivity() {

    private val viewModel: NoteListViewModel by viewModels { NoteListViewModel.Factory }
    private lateinit var binding: ActivityMainBinding
    private val adapter = NoteAdapter { NoteDetailActivity.start(this, it.id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null && viewModel.onboardingNeeded) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_map -> startActivity(Intent(this, MapActivity::class.java))
                R.id.action_onboarding -> startActivity(Intent(this, OnboardingActivity::class.java))
            }
            true
        }
        binding.list.adapter = adapter
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (recyclerView.canScrollVertically(-1)) binding.add.shrink() else binding.add.extend()
            }
        })
        val openEditor = View.OnClickListener { startActivity(Intent(this, AddNoteActivity::class.java)) }
        binding.add.setOnClickListener(openEditor)
        binding.emptyAdd.setOnClickListener(openEditor)
        binding.searchInput.doAfterTextChanged { viewModel.search(it.toString()) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: NoteListState) {
        val showList = !state.loading && !state.empty
        binding.content.visibility = if (showList) View.VISIBLE else View.GONE
        binding.add.visibility = binding.content.visibility
        binding.empty.visibility = if (state.empty) View.VISIBLE else View.GONE
        if (!showList) return

        adapter.submitList(state.notes)
        binding.count.text = if (state.query.isBlank()) {
            resources.getQuantityString(R.plurals.list_count, state.total, state.total)
        } else {
            resources.getQuantityString(R.plurals.list_results, state.notes.size, state.notes.size)
        }
        binding.noResult.visibility = if (state.notes.isEmpty()) View.VISIBLE else View.GONE
        binding.noResult.text = getString(R.string.list_no_result, state.query)
    }
}

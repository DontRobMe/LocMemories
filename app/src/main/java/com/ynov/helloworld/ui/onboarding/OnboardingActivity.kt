package com.ynov.helloworld.ui.onboarding

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.ynov.helloworld.R
import com.ynov.helloworld.databinding.ActivityOnboardingBinding
import com.ynov.helloworld.databinding.ItemOnboardingPageBinding
import kotlin.math.abs

private data class OnboardingPage(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
)

private val pages = listOf(
    OnboardingPage(R.drawable.ic_edit_note, R.string.onboarding_title_1, R.string.onboarding_text_1),
    OnboardingPage(R.drawable.ic_photo_camera, R.string.onboarding_title_2, R.string.onboarding_text_2),
    OnboardingPage(R.drawable.ic_my_location, R.string.onboarding_title_3, R.string.onboarding_text_3),
    OnboardingPage(R.drawable.ic_map, R.string.onboarding_title_4, R.string.onboarding_text_4),
)

// region Écran

/**
 * Introduction en carrousel, au premier lancement puis à la demande depuis la liste.
 * L'indicateur de page est annoncé par TalkBack (« Page 2 sur 4 »).
 */
class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels { OnboardingViewModel.Factory }
    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = PageAdapter()
        binding.pager.setPageTransformer { page, position ->
            val progress = abs(position).coerceAtMost(1f)
            page.findViewById<View>(R.id.illustration).apply {
                scaleX = 1f - 0.25f * progress
                scaleY = 1f - 0.25f * progress
                alpha = 1f - 0.6f * progress
            }
        }
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showPage(position)
        })
        repeat(pages.size) {
            binding.indicator.addView(View(this).apply { setBackgroundResource(R.drawable.bg_pill) })
        }

        binding.skip.setOnClickListener { finishOnboarding() }
        binding.next.setOnClickListener {
            val current = binding.pager.currentItem
            if (current == pages.lastIndex) finishOnboarding() else binding.pager.currentItem = current + 1
        }
        showPage(binding.pager.currentItem)
    }

    private fun showPage(position: Int) {
        val last = position == pages.lastIndex
        binding.skip.visibility = if (last) View.INVISIBLE else View.VISIBLE
        binding.next.setText(if (last) R.string.onboarding_start else R.string.onboarding_next)
        binding.next.setIconResource(if (last) R.drawable.ic_check else R.drawable.ic_arrow_forward)

        val density = resources.displayMetrics.density
        val selected = MaterialColors.getColor(binding.indicator, androidx.appcompat.R.attr.colorPrimary)
        val other = MaterialColors.getColor(binding.indicator, com.google.android.material.R.attr.colorOutlineVariant)
        for (index in pages.indices) {
            binding.indicator.getChildAt(index).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ((if (index == position) 28 else 8) * density).toInt(),
                    (8 * density).toInt(),
                ).apply { marginEnd = if (index < pages.lastIndex) (8 * density).toInt() else 0 }
                backgroundTintList = ColorStateList.valueOf(
                    if (index == position) selected else other
                )
            }
        }
        binding.indicator.contentDescription = getString(R.string.onboarding_page, position + 1, pages.size)
    }

    private fun finishOnboarding() {
        viewModel.complete()
        finish()
    }
}

// endregion

// region Adaptateur

private class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {

    class Holder(val binding: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val page = pages[position]
        holder.binding.icon.setImageResource(page.icon)
        holder.binding.title.setText(page.title)
        holder.binding.text.setText(page.text)
    }
}

// endregion

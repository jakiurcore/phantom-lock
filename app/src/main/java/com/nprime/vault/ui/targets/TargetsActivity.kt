package com.nprime.vault.ui.targets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nprime.vault.R

class TargetsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_targets)

        val pager = findViewById<ViewPager2>(R.id.targets_pager)
        val tabs  = findViewById<TabLayout>(R.id.targets_tabs)

        pager.adapter = TargetsPagerAdapter(this)
        TabLayoutMediator(tabs, pager) { tab, pos ->
            tab.text = if (pos == 0) getString(R.string.tab_apps) else getString(R.string.tab_files)
        }.attach()
    }
}

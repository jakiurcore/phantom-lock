package com.nprime.vault.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.ui.permissions.PermissionSetupActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var dots: TabLayout
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.onboarding_pager)
        dots = findViewById(R.id.onboarding_dots)
        btnNext = findViewById(R.id.btn_next)

        pager.adapter = OnboardingAdapter()

        TabLayoutMediator(dots, pager) { _, _ -> }.attach()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnNext.text = if (position == 2) getString(R.string.get_started) else getString(R.string.next)
            }
        })

        btnNext.setOnClickListener {
            if (pager.currentItem < 2) {
                pager.setCurrentItem(pager.currentItem + 1, true)
            } else {
                VaultPrefs.markOnboardingDone(this)
                startActivity(Intent(this, PermissionSetupActivity::class.java))
                finish()
            }
        }
    }
}

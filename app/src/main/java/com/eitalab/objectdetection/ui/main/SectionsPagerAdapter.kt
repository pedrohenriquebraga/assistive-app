package com.eitalab.objectdetection.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SectionsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeDashboardFragment.newInstance()
            1 -> CameraDetectionFragment.newInstance()
            2 -> HistoryFragment.newInstance()
            3 -> SettingsFragment.newInstance()
            else -> HomeDashboardFragment.newInstance()
        }
    }
}

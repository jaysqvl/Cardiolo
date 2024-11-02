package com.example.myruns.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyFragmentStateAdapter(
    fragmentActivity: FragmentActivity,
    private val fragments: ArrayList<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    // Returns the fragment at the given position
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    // Returns the total number of fragments
    override fun getItemCount(): Int {
        return fragments.size
    }
}

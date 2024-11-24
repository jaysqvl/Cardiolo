package com.example.myruns.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyFragmentStateAdapter(
    fragmentActivity: FragmentActivity,
    private val fragments: ArrayList<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    private val fragmentStates: ArrayList<Fragment?> = ArrayList(fragments)

    // Returns the fragment at the given position
    override fun createFragment(position: Int): Fragment {
        // Use the recreated fragment if present
        return fragmentStates[position] ?: fragments[position]
    }

    // Returns the total number of fragments
    override fun getItemCount(): Int {
        return fragments.size
    }

    // Method to refresh a specific fragment
    fun refreshFragment(position: Int, newFragment: Fragment) {
        fragmentStates[position] = newFragment
        notifyItemChanged(position)
    }
}
package com.example.myruns.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.myruns.ui.fragments.HistoryFragment
import com.example.myruns.R
import com.example.myruns.adapter.MyFragmentStateAdapter
import com.example.myruns.ui.fragments.SettingsFragment
import com.example.myruns.ui.fragments.StartFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    // Declare all variables as private var at the top
    private lateinit var viewPager2: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fragmentStart: StartFragment
    private lateinit var fragmentSettings: SettingsFragment
    private lateinit var myFragmentStateAdapter: MyFragmentStateAdapter
    private lateinit var fragments: ArrayList<Fragment>
    private val tabTitles = arrayOf("Start", "History", "Settings") // Tab titles
    private lateinit var tabLayoutMediator: TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle("MyRuns5")

        // Set up the Toolbar as the ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set the title for the ActionBar
        supportActionBar?.title = "MyRuns5"

        // Initialize view components
        viewPager2 = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Set the fragments and initialize them
        fragmentStart = StartFragment()
        val initialHistoryFragment = HistoryFragment()
        fragmentSettings = SettingsFragment()

        // Add them to the fragments list so that they can be used in the ViewPager adapter
        fragments = ArrayList<Fragment>()
        fragments.add(fragmentStart)
        fragments.add(initialHistoryFragment)
        fragments.add(fragmentSettings)

        // Initialize and set up the ViewPager adapter
        myFragmentStateAdapter = MyFragmentStateAdapter(this, fragments)
        viewPager2.adapter = myFragmentStateAdapter

        // Set up TabLayoutMediator to link tabs with ViewPager2
        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = tabTitles[position]
        }

        // Attach the TabLayoutMediator to sync TabLayout with ViewPager2
        tabLayoutMediator.attach()
    }

    // Method to refresh the HistoryFragment
    fun refreshHistoryFragment() {
        val newHistoryFragment = HistoryFragment()
        fragments[1] = newHistoryFragment // Replace the existing HistoryFragment
        myFragmentStateAdapter.refreshFragment(1, newHistoryFragment) // Notify the adapter
    }
}
package com.example.myruns

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    // Declare all variables as private var at the top
    private lateinit var viewPager2: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fragmentStart: StartFragment
    private lateinit var fragmentHistory: HistoryFragment
    private lateinit var fragmentSettings: SettingsFragment
    private lateinit var myFragmentStateAdapter: MyFragmentStateAdapter
    private lateinit var fragments: ArrayList<Fragment>
    private val tabTitles = arrayOf("Start", "History", "Settings") // Tab titles
    private lateinit var tabLayoutMediator: TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle("MyRuns2")

        // Set up the Toolbar as the ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set the title for the ActionBar
        supportActionBar?.title = "MyRuns2"

        // Initialize view components
        viewPager2 = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Set the fragments and initialize them
        fragmentStart = StartFragment()
        fragmentHistory = HistoryFragment()
        fragmentSettings = SettingsFragment()

        // Add them to the fragments list so that they can be used in the ViewPager adapter
        fragments = ArrayList<Fragment>()
        fragments.add(fragmentStart)
        fragments.add(fragmentHistory)
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
}

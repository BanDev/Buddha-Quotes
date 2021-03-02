/**

Buddha Quotes
Copyright (C) 2021  BanDev

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

package org.bandev.buddhaquotes.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.Validation
import com.maxkeppeler.sheets.input.type.InputEditText
import nl.joery.animatedbottombar.AnimatedBottomBar
import org.bandev.buddhaquotes.R
import org.bandev.buddhaquotes.core.*
import org.bandev.buddhaquotes.core.Notify
import org.bandev.buddhaquotes.databinding.MainActivityBinding
import org.bandev.buddhaquotes.fragments.FragmentAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Main is the main page of Buddha Quotes
 *
 * It has a ViewPager and allows the user to scroll between its fragments.
 * It uses [FragmentAdapter] as a fragment adapter and
 * https://github.com/Droppers/AnimatedBottomBar for its nice bottom bar.
 * @since v1.0.0
 * @author jack.txt & Fennec_exe
 */

class Main : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    /**
     * On activity created
     *
     * @param savedInstanceState [Bundle]
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme, navigation bar and language
        Colours().setAccentColour(this, window, resources)
        Compatibility().setNavigationBarColourMain(this, window, resources)
        Languages(baseContext).setLanguage()

        val sharedPrefs = getSharedPreferences("Settings", 0)
        val editor = sharedPrefs.edit()

        if (sharedPrefs.getBoolean("first_time", true)) {
            editor.putBoolean("first_time", false)
            editor.apply()
            startActivity(Intent(this, Intro::class.java))
        }

        // Setup view binding
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)

        // Setup viewPager with FragmentAdapter
        binding.viewPager.adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        binding.viewPager.setCurrentItem(Store(this).fragment, false)
        binding.bottomBar.setupWithViewPager2(binding.viewPager)

        binding.bottomBar.setOnTabInterceptListener(object :
            AnimatedBottomBar.OnTabInterceptListener {
            override fun onTabIntercepted(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ): Boolean {
                Store(applicationContext).fragment = newIndex
                return true
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotifyReceive(event: Notify) {
    }

    /**
     * On options menu created
     * @param menu [Menu]
     * @return [Boolean]
     */

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                showCreateListSheet()
                true
            }
            R.id.settings -> {
                val intent = Intent(this, Settings::class.java)
                intent.putExtra("from", Activities.MAIN)
                startActivity(intent)
                finish()
                overridePendingTransition(
                    R.anim.anim_slide_in_left,
                    R.anim.anim_slide_out_left
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * On options menu item selected
     * @return [Boolean]
     */

    // Build the input bottom sheet that allows creation of a new list
    private fun showCreateListSheet() {
        // Retrieve the lists
        val lists = ListsV2(this).getMasterList()

        InputSheet().show(this) {
            title(R.string.createNewList)
            with(
                InputEditText {
                    required()
                    hint(R.string.insertName)
                    validationListener { value ->
                        when {
                            value.contains("//") -> {
                                Validation.failed(getString(R.string.validationRule1))
                            }
                            value.toLowerCase(Locale.ROOT) == "favourites" -> {
                                Validation.failed(getString(R.string.validationRule2))
                            }
                            lists.contains(value.toLowerCase(Locale.ROOT)) -> {
                                Validation.failed(getString(R.string.validationRule3) + " $value")
                            }
                            else -> Validation.success()
                        }
                    }
                    resultListener { value ->
                        EventBus.getDefault().post(Notify.NotifyNewList(value.toString()))
                    }
                }
            )
            onNegative { binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
            onPositive(R.string.add) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}

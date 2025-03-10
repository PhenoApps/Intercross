package org.phenoapps.intercross.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment.Companion.createInstance
import org.phenoapps.intercross.R
import org.phenoapps.intercross.fragments.app_intro.GallerySlideFragment
import org.phenoapps.intercross.fragments.app_intro.OptionalSetupFragment
import org.phenoapps.intercross.fragments.app_intro.RequiredSetupPolicyFragment

class AppIntroActivity :  AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setWindowInsetListener()

        val context = applicationContext

        isSkipButtonEnabled = false

        // intro slide
        addSlide(
            createInstance(
                context.getString(R.string.app_intro_intro_title_slide1),
                context.getString(R.string.app_intro_intro_summary_slide1),
                R.mipmap.ic_launcher,
                R.color.colorPrimaryDark
            )
        )

        // gallery slide
        /*
       addSlide(
           GallerySlideFragment.newInstance(
               context.getString(R.string.app_intro_intro_title_slide2),
               context.getString(R.string.app_intro_intro_summary_slide2),
               R.color.colorPrimaryDark
           )
       )
        */

        // required setup
        addSlide(
            RequiredSetupPolicyFragment.newInstance(
                context.getString(R.string.app_intro_required_setup_title),
                context.getString(R.string.app_intro_required_setup_summary),
                R.color.colorPrimaryDark
            )
        )

        // optional setup
        addSlide(
            OptionalSetupFragment.newInstance(
                context.getString(R.string.app_intro_optional_setup_title),
                context.getString(R.string.app_intro_optional_setup_summary),
                R.color.colorPrimaryDark
            )
        )
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        setResult(RESULT_OK)
        finish()
    }

    private fun setWindowInsetListener() {
        window.decorView.findViewById<View>(android.R.id.content)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, windowInsets ->
                val insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
                )

                it.setPadding(0, 0, 0, insets.bottom)

                windowInsets
            }
        }
    }
}
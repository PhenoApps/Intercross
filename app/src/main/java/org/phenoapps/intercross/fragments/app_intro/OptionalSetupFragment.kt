package org.phenoapps.intercross.fragments.app_intro

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.phenoapps.intercross.R
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.views.OptionalSetupItem

class OptionalSetupFragment : Fragment(){
    private var slideTitle: String? = null
    private var slideSummary: String? = null
    private var slideBackgroundColor: Int? = null

    private var loadSampleParents: OptionalSetupItem? = null
    private var loadSampleWishlist: OptionalSetupItem? = null

    private var prefs: SharedPreferences? = null

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.app_intro_optional_setup_slide, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }

        val slideTitle = view.findViewById<TextView>(R.id.slide_title)
        val slideSummary = view.findViewById<TextView>(R.id.slide_summary)

        slideTitle?.text = this.slideTitle
        slideSummary?.text = this.slideSummary

        slideBackgroundColor?.let { view.setBackgroundResource(it) }

        loadSampleParents = view.findViewById(R.id.load_sample_parents)

        loadSampleWishlist = view.findViewById(R.id.load_sample_wishlist)

        initSetupItems()
    }

    private fun initSetupItems() {
        loadSampleParents?.apply {
            setTitle(getString(R.string.app_intro_load_sample_parents_title))
            setSummary(getString(R.string.app_intro_load_sample_parents_summary))
            setOnClickListener {
                loadSampleToggle(this, mKeyUtil.loadSampleParents)
            }
            setTitleTextSize(24f)
        }

        loadSampleWishlist?.apply {
            setTitle(getString(R.string.app_intro_load_sample_wishlist_title))
            setSummary(getString(R.string.app_intro_load_sample_wishlist_summary))
            setOnClickListener {
                loadSampleToggle(this, mKeyUtil.loadSampleWishlist)
            }
            setTitleTextSize(24f)
        }
    }

    private fun loadSampleToggle(optionalSetupItemView: OptionalSetupItem, prefKey: String) {
        optionalSetupItemView.let {
            it.setCheckbox(!it.isChecked())

            prefs?.edit()?.putBoolean(prefKey, it.isChecked())?.apply()
        }
    }

    companion object {
        fun newInstance(
            slideTitle: String,
            slideSummary: String,
            slideBackgroundColor: Int
        ): OptionalSetupFragment {
            val fragment = OptionalSetupFragment()
            fragment.slideTitle = slideTitle
            fragment.slideSummary = slideSummary
            fragment.slideBackgroundColor = slideBackgroundColor
            return fragment
        }
    }
}
package org.phenoapps.intercross.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.util.SnackbarQueue

abstract class IntercrossBaseFragment<T : ViewDataBinding>(
        private val layoutId: Int) : Fragment() {

    lateinit var mSnackbar: SnackbarQueue

    protected lateinit var mBinding: T

    val db by lazy {
        IntercrossDatabase.getInstance(requireContext())
    }

    abstract fun T.afterCreateView()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, layoutId, container, false)

        (activity as? MainActivity)?.supportActionBar?.hide()
        (activity as? MainActivity)?.setSupportActionBar(null)

        return with(mBinding) {

            afterCreateView()

            root
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding.unbind()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSnackbar = SnackbarQueue()
    }

    fun closeKeyboard() {
        activity?.let {
            val imm = it.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.currentFocus?.windowToken, 0)
        }
    }
}
package org.phenoapps.intercross.activities

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.R
import org.phenoapps.utils.BaseDocumentTreeUtil

@AndroidEntryPoint
class DefineStorageActivity: AppCompatActivity() {

    private var mBackButtonEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.apply {
            WindowCompat.getInsetsController(this, decorView).apply {
                isAppearanceLightStatusBars = true
            }
        }

        setContentView(R.layout.activity_define_storage)

        setWindowInsetListener()
    }

    override fun onBackPressed() {

        if (mBackButtonEnabled) {
            super.onBackPressed()
            setResult(if (BaseDocumentTreeUtil.isEnabled(this)) {
                Activity.RESULT_OK
            } else Activity.RESULT_CANCELED)
            finish()
        }
    }

    fun enableBackButton(enable: Boolean) {
        mBackButtonEnabled = enable
    }

    private fun setWindowInsetListener() {
        window.decorView.findViewById<View>(android.R.id.content)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, windowInsets ->
                val insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
                )

                it.setPadding(0, insets.top, 0, insets.bottom)

                windowInsets
            }
        }
    }
}
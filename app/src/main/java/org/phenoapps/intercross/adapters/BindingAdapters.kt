package org.phenoapps.intercross.adapters

import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Settings
import org.phenoapps.intercross.util.AsyncLoadBarcode
import java.util.*

@BindingAdapter("setImageByCrossType")
fun bindCrossTypeImage(view: ImageView, event: Event?) {

    if (event == null) {

        view.setImageResource(R.drawable.ic_cross_unknown)

    } else {

        view.setImageResource(when(event.type) {

            CrossType.BIPARENTAL -> R.drawable.ic_cross_biparental

            CrossType.POLY -> R.drawable.ic_cross_poly

            CrossType.SELF -> R.drawable.ic_cross_self

            CrossType.OPEN -> R.drawable.ic_cross_open

            CrossType.UNKNOWN -> R.drawable.ic_cross_unknown

        })
    }
}

@BindingAdapter("setQRCode")
fun bindQRCodeImage(view: ImageView, code: String?) {
    if (code.isNullOrBlank()) {
        view.tag = null
        view.setImageDrawable(null)
        return
    }

    view.tag = code
    AsyncLoadBarcode(view, code).execute(code)
}

@BindingAdapter("layoutMarginStart")
fun bindLayoutMarginStart(view: TextView, spacing: Float) {

    view.layoutParams = (view.layoutParams as ViewGroup.MarginLayoutParams).apply {

        marginStart = spacing.toInt()

    }
}

@BindingAdapter("setCrossId")
fun bindCrossId(view: EditText, settings: Settings?) {

    settings?.let {

        with (it) {

            when {

                isPattern -> {

                    view.setText(pattern)

                }

                isUUID -> {

                    view.setText(UUID.randomUUID().toString())

                }
            }
        }
    }
}
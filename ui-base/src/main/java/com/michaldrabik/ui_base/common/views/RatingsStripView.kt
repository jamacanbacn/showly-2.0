package com.michaldrabik.ui_base.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import com.michaldrabik.ui_base.R
import com.michaldrabik.ui_base.utilities.extensions.colorFromAttr
import com.michaldrabik.ui_base.utilities.extensions.onClick
import com.michaldrabik.ui_base.utilities.extensions.visibleIf
import com.michaldrabik.ui_model.Ratings
import kotlinx.android.synthetic.main.view_ratings_strip.view.*

class RatingsStripView : LinearLayout {

  companion object {
    private const val EMPTY_SYMBOL = "---"
  }

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  var onTraktClick: (() -> Unit)? = null
  var onImdbClick: (() -> Unit)? = null
  var onMetaClick: (() -> Unit)? = null
  var onRottenClick: (() -> Unit)? = null

  private val colorPrimary by lazy { context.colorFromAttr(android.R.attr.textColorPrimary) }
  private val colorSecondary by lazy { context.colorFromAttr(android.R.attr.textColorSecondary) }

  init {
    inflate(context, R.layout.view_ratings_strip, this)
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    orientation = HORIZONTAL
    gravity = Gravity.TOP

    viewRatingsStripTrakt.onClick { onTraktClick?.invoke() }
    viewRatingsStripImdb.onClick { onImdbClick?.invoke() }
    viewRatingsStripMeta.onClick { onMetaClick?.invoke() }
    viewRatingsRotten.onClick { onRottenClick?.invoke() }
  }

  fun bind(ratings: Ratings) {

    fun bindValue(
      ratings: Ratings.Value?,
      valueView: TextView,
      progressView: View,
    ) {
      val rating = ratings?.value
      val isLoading = ratings?.isLoading == true
      with(valueView) {
        visibleIf(!isLoading, gone = false)
        text = if (rating.isNullOrBlank()) EMPTY_SYMBOL else rating
        setTextColor(if (rating != null) colorPrimary else colorSecondary)
      }
      progressView.visibleIf(isLoading)
    }

    bindValue(ratings.trakt, viewRatingsStripTraktValue, viewRatingsStripTraktProgress)
    bindValue(ratings.imdb, viewRatingsStripImdbValue, viewRatingsStripImdbProgress)
    bindValue(ratings.metascore, viewRatingsStripMetaValue, viewRatingsStripMetaProgress)
    bindValue(ratings.rottenTomatoes, viewRatingsStripRottenValue, viewRatingsStripRottenProgress)
  }
}
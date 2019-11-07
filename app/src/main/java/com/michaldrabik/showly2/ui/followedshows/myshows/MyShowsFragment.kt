package com.michaldrabik.showly2.ui.followedshows.myshows

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.michaldrabik.showly2.R
import com.michaldrabik.showly2.appComponent
import com.michaldrabik.showly2.model.MyShowsSection
import com.michaldrabik.showly2.model.MyShowsSection.COMING_SOON
import com.michaldrabik.showly2.model.MyShowsSection.ENDED
import com.michaldrabik.showly2.model.MyShowsSection.RUNNING
import com.michaldrabik.showly2.model.Show
import com.michaldrabik.showly2.model.SortOrder
import com.michaldrabik.showly2.ui.common.OnScrollResetListener
import com.michaldrabik.showly2.ui.common.OnTabReselectedListener
import com.michaldrabik.showly2.ui.common.base.BaseFragment
import com.michaldrabik.showly2.ui.followedshows.FollowedShowsFragment
import com.michaldrabik.showly2.ui.followedshows.myshows.recycler.MyShowsListItem
import com.michaldrabik.showly2.ui.followedshows.myshows.views.MyShowFanartView
import com.michaldrabik.showly2.utilities.extensions.dimenToPx
import com.michaldrabik.showly2.utilities.extensions.fadeIf
import com.michaldrabik.showly2.utilities.extensions.gone
import com.michaldrabik.showly2.utilities.extensions.visible
import com.michaldrabik.showly2.utilities.extensions.visibleIf
import kotlinx.android.synthetic.main.fragment_my_shows.*
import java.util.ResourceBundle.clearCache

class MyShowsFragment : BaseFragment<MyShowsViewModel>(), OnTabReselectedListener, OnScrollResetListener {

  override val layoutResId = R.layout.fragment_my_shows

  override fun onCreate(savedInstanceState: Bundle?) {
    appComponent().inject(this)
    super.onCreate(savedInstanceState)
  }

  override fun createViewModel(provider: ViewModelProvider) =
    provider.get(MyShowsViewModel::class.java)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupSectionsViews()
    viewModel.run {
      uiStream.observe(viewLifecycleOwner, Observer { render(it!!) })
      clearCache()
      loadMyShows()
    }
  }

  private fun setupSectionsViews() {
    val onSectionItemClick: (MyShowsListItem) -> Unit = { openShowDetails(it.show) }
    val onSectionMissingImageListener: (MyShowsListItem, Boolean) -> Unit = { item, force ->
      viewModel.loadMissingImage(item, force)
    }
    val onSectionSortOrderChange: (MyShowsSection, SortOrder) -> Unit = { section, order ->
      viewModel.loadSortedSection(section, order)
    }

    myShowsRunningSection.itemClickListener = onSectionItemClick
    myShowsRunningSection.missingImageListener = onSectionMissingImageListener
    myShowsRunningSection.sortSelectedListener = onSectionSortOrderChange

    myShowsEndedSection.itemClickListener = onSectionItemClick
    myShowsEndedSection.missingImageListener = onSectionMissingImageListener
    myShowsEndedSection.sortSelectedListener = onSectionSortOrderChange

    myShowsIncomingSection.itemClickListener = onSectionItemClick
    myShowsIncomingSection.missingImageListener = onSectionMissingImageListener
    myShowsIncomingSection.sortSelectedListener = onSectionSortOrderChange
  }

  private fun render(uiModel: MyShowsUiModel) {
    uiModel.run {
      recentShows?.let {
        myShowsSearchContainer.gone()
        myShowsRecentsLabel.visible()
        myShowsRecentsContainer.visible()
        myShowsRootContent.fadeIf(it.isNotEmpty())
        myShowsEmptyView.fadeIf(it.isEmpty())
        renderFanartContainer(it, myShowsRecentsContainer)
        (parentFragment as FollowedShowsFragment).enableSearch(it.isNotEmpty())
      }
      runningShows?.let {
        myShowsRunningSection.bind(it.items, it.section, it.sortOrder, R.string.textRunning)
        myShowsRunningSection.visibleIf(it.items.isNotEmpty())
      }
      endedShows?.let {
        myShowsEndedSection.bind(it.items, it.section, it.sortOrder, R.string.textEnded)
        myShowsEndedSection.visibleIf(it.items.isNotEmpty())
      }
      incomingShows?.let {
        myShowsIncomingSection.bind(it.items, it.section, it.sortOrder, R.string.textIncoming)
        myShowsIncomingSection.visibleIf(it.items.isNotEmpty())
      }
      updateListItem?.let { item ->
        myShowsRunningSection.updateItem(item)
        myShowsEndedSection.updateItem(item)
        myShowsIncomingSection.updateItem(item)
      }
      sectionsPositions?.let {
        myShowsRunningSection.scrollToPosition(it[RUNNING]?.first ?: 0, it[RUNNING]?.second ?: 0)
        myShowsEndedSection.scrollToPosition(it[ENDED]?.first ?: 0, it[ENDED]?.second ?: 0)
        myShowsIncomingSection.scrollToPosition(it[COMING_SOON]?.first ?: 0, it[COMING_SOON]?.second ?: 0)
      }
    }
  }

  private fun renderFanartContainer(items: List<MyShowsListItem>, container: GridLayout) {
    container.removeAllViews()

    val context = requireContext()
    val itemHeight = context.dimenToPx(R.dimen.myShowsFanartHeight)
    val itemMargin = context.dimenToPx(R.dimen.spaceTiny)

    val clickListener: (Show) -> Unit = { openShowDetails(it) }

    items.forEachIndexed { index, item ->
      val view = MyShowFanartView(context).apply {
        layoutParams = FrameLayout.LayoutParams(0, MATCH_PARENT)
        bind(item.show, item.image, clickListener)
      }
      val layoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = itemHeight
        columnSpec = GridLayout.spec(index % 2, 1F)
        setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
      }
      container.addView(view, layoutParams)
    }
  }

  private fun openShowDetails(show: Show) {
    saveToUiCache()
    (parentFragment as? FollowedShowsFragment)?.openShowDetails(show)
  }

  private fun saveToUiCache() {
    val sectionPositions = mapOf(
      RUNNING to myShowsRunningSection.getListPosition(),
      ENDED to myShowsEndedSection.getListPosition(),
      COMING_SOON to myShowsIncomingSection.getListPosition()
    )
    viewModel.saveListPosition(sectionPositions)
  }

  override fun onTabReselected() = onScrollReset()

  override fun onScrollReset() = myShowsRootScroll.smoothScrollTo(0, 0)
}
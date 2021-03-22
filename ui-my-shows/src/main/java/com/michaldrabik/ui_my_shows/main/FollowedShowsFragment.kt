package com.michaldrabik.ui_my_shows.main

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import com.michaldrabik.ui_base.BaseFragment
import com.michaldrabik.ui_base.common.OnScrollResetListener
import com.michaldrabik.ui_base.common.OnTabReselectedListener
import com.michaldrabik.ui_base.common.OnTraktSyncListener
import com.michaldrabik.ui_base.common.views.exSearchViewIcon
import com.michaldrabik.ui_base.common.views.exSearchViewInput
import com.michaldrabik.ui_base.common.views.exSearchViewText
import com.michaldrabik.ui_base.utilities.extensions.add
import com.michaldrabik.ui_base.utilities.extensions.dimenToPx
import com.michaldrabik.ui_base.utilities.extensions.disableUi
import com.michaldrabik.ui_base.utilities.extensions.doOnApplyWindowInsets
import com.michaldrabik.ui_base.utilities.extensions.enableUi
import com.michaldrabik.ui_base.utilities.extensions.fadeIf
import com.michaldrabik.ui_base.utilities.extensions.fadeOut
import com.michaldrabik.ui_base.utilities.extensions.gone
import com.michaldrabik.ui_base.utilities.extensions.hideKeyboard
import com.michaldrabik.ui_base.utilities.extensions.nextPage
import com.michaldrabik.ui_base.utilities.extensions.onClick
import com.michaldrabik.ui_base.utilities.extensions.showKeyboard
import com.michaldrabik.ui_base.utilities.extensions.updateTopMargin
import com.michaldrabik.ui_base.utilities.extensions.visible
import com.michaldrabik.ui_base.utilities.extensions.visibleIf
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_my_shows.R
import com.michaldrabik.ui_my_shows.di.UiMyShowsComponentProvider
import com.michaldrabik.ui_my_shows.main.utilities.OnSortClickListener
import com.michaldrabik.ui_my_shows.myshows.helpers.MyShowsSearchResult
import com.michaldrabik.ui_my_shows.myshows.helpers.ResultType.EMPTY
import com.michaldrabik.ui_my_shows.myshows.helpers.ResultType.NO_RESULTS
import com.michaldrabik.ui_my_shows.myshows.helpers.ResultType.RESULTS
import com.michaldrabik.ui_my_shows.myshows.recycler.MyShowsItem
import com.michaldrabik.ui_my_shows.myshows.views.MyShowFanartView
import com.michaldrabik.ui_navigation.java.NavigationArgs.ARG_SHOW_ID
import kotlinx.android.synthetic.main.fragment_followed_shows.*

class FollowedShowsFragment :
  BaseFragment<FollowedShowsViewModel>(R.layout.fragment_followed_shows),
  OnTabReselectedListener,
  OnTraktSyncListener {

  override val viewModel by viewModels<FollowedShowsViewModel> { viewModelFactory }
  private var currentPage = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    (requireActivity() as UiMyShowsComponentProvider).provideMyShowsComponent().inject(this)
    super.onCreate(savedInstanceState)
    setupBackPressed()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupPager()
    setupStatusBar()

    viewModel.run {
      uiLiveData.observe(viewLifecycleOwner, { render(it!!) })
      clearCache()
    }
  }

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onPause() {
    enableUi()
    super.onPause()
  }

  override fun onDestroyView() {
    followedShowsPager.removeOnPageChangeListener(pageChangeListener)
    super.onDestroyView()
  }

  private fun setupView() {
    followedShowsSearchView.run {
      hint = getString(R.string.textSearchFor)
      statsIconVisible = true
      onClick { enterSearch() }
      onSettingsClickListener = { openSettings() }
      onStatsClickListener = { openStatistics() }
      if (isTraktSyncing()) setTraktProgress(true)
    }
    followedShowsModeTabs.run {
      onModeSelected = { mode = it }
      onListsSelected = { navigateTo(R.id.actionNavigateListsFragment) }
      showMovies(moviesEnabled)
      showLists(true, anchorEnd = moviesEnabled)
      selectShows()
    }
    followedShowsSortIcon.run {
      visibleIf(currentPage != 0)
      onClick {
        val currentIndex = followedShowsPager.currentItem
        (childFragmentManager.fragments[currentIndex] as? OnSortClickListener)?.onSortClick(currentIndex)
      }
    }
    exSearchViewInput.run {
      imeOptions = EditorInfo.IME_ACTION_DONE
      setOnEditorActionListener { _, _, _ ->
        clearFocus()
        hideKeyboard()
        true
      }
    }

    followedShowsTabs.translationY = viewModel.tabsTranslation
    followedShowsModeTabs.translationY = viewModel.tabsTranslation
    followedShowsSearchView.translationY = viewModel.searchViewTranslation
    followedShowsSortIcon.translationY = viewModel.tabsTranslation
  }

  private fun setupPager() {
    followedShowsPager.run {
      offscreenPageLimit = FollowedPagesAdapter.PAGES_COUNT
      adapter = FollowedPagesAdapter(childFragmentManager, requireAppContext())
      addOnPageChangeListener(pageChangeListener)
    }
    followedShowsTabs.setupWithViewPager(followedShowsPager)
  }

  private fun setupStatusBar() {
    followedShowsRoot.doOnApplyWindowInsets { _, insets, _, _ ->
      val statusBarSize = insets.systemWindowInsetTop
      followedShowsSearchView.applyWindowInsetBehaviour(dimenToPx(R.dimen.spaceNormal) + statusBarSize)
      followedShowsSearchView.updateTopMargin(dimenToPx(R.dimen.spaceSmall) + statusBarSize)
      followedShowsModeTabs.updateTopMargin(dimenToPx(R.dimen.collectionTabsMargin) + statusBarSize)
      followedShowsTabs.updateTopMargin(dimenToPx(R.dimen.myShowsSearchViewPadding) + statusBarSize)
      followedShowsSortIcon.updateTopMargin(dimenToPx(R.dimen.myShowsSearchViewPadding) + statusBarSize)
      followedShowsSearchEmptyView.updateTopMargin(dimenToPx(R.dimen.searchViewHeightPadded) + statusBarSize)
      followedShowsSearchWrapper.updateTopMargin(dimenToPx(R.dimen.searchViewHeightPadded) + statusBarSize)
    }
  }

  private fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(this) {
      if (followedShowsSearchView.isSearching) {
        exitSearch()
      } else {
        remove()
        dispatcher.onBackPressed()
      }
    }
  }

  private fun enterSearch() {
    if (followedShowsSearchView.isSearching) return
    followedShowsSearchView.isSearching = true
    exSearchViewText.gone()
    exSearchViewInput.run {
      setText("")
      doAfterTextChanged { viewModel.searchFollowedShows(it?.toString() ?: "") }
      visible()
      showKeyboard()
      requestFocus()
    }
    (exSearchViewIcon.drawable as Animatable).start()
    exSearchViewIcon.onClick { exitSearch() }
    hideNavigation(false)
  }

  private fun exitSearch(showNavigation: Boolean = true) {
    followedShowsSearchView.isSearching = false
    exSearchViewText.visible()
    exSearchViewInput.run {
      setText("")
      gone()
      hideKeyboard()
      clearFocus()
    }
    exSearchViewIcon.setImageResource(R.drawable.ic_anim_search_to_close)
    if (showNavigation) showNavigation()
  }

  private fun render(uiModel: FollowedShowsUiModel) {
    uiModel.run {
      searchResult?.let { renderSearchResults(it) }
    }
  }

  private fun renderSearchResults(result: MyShowsSearchResult) {
    when (result.type) {
      RESULTS -> {
        followedShowsSearchWrapper.visible()
        followedShowsPager.gone()
        followedShowsTabs.gone()
        followedShowsModeTabs.gone()
        followedShowsSearchEmptyView.gone()
        renderSearchContainer(result.items)
      }
      NO_RESULTS -> {
        followedShowsSearchWrapper.gone()
        followedShowsPager.gone()
        followedShowsTabs.gone()
        followedShowsModeTabs.gone()
        followedShowsSearchEmptyView.visible()
      }
      EMPTY -> {
        followedShowsSearchWrapper.gone()
        followedShowsPager.visible()
        followedShowsTabs.visible()
        followedShowsModeTabs.visible()
        followedShowsSearchEmptyView.gone()
      }
    }

    if (result.type != EMPTY) {
      followedShowsSearchView.translationY = 0F
      followedShowsTabs.translationY = 0F
      followedShowsModeTabs.translationY = 0F
      followedShowsSortIcon.translationY = 0F
      childFragmentManager.fragments.forEach {
        (it as? OnScrollResetListener)?.onScrollReset()
      }
    }
  }

  private fun renderSearchContainer(items: List<MyShowsItem>) {
    followedShowsSearchContainer.removeAllViews()

    val context = requireContext()
    val itemHeight = context.dimenToPx(R.dimen.myShowsFanartHeight)
    val itemMargin = context.dimenToPx(R.dimen.spaceTiny)

    val clickListener: (MyShowsItem) -> Unit = {
      followedShowsRoot.hideKeyboard()
      openShowDetails(it.show)
    }

    items.forEachIndexed { index, item ->
      val view = MyShowFanartView(context).apply {
        layoutParams = FrameLayout.LayoutParams(0, MATCH_PARENT)
        bind(item, clickListener)
      }
      val layoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = itemHeight
        columnSpec = GridLayout.spec(index % 2, 1F)
        setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
      }
      followedShowsSearchContainer.addView(view, layoutParams)
    }
  }

  fun openShowDetails(show: Show) {
    disableUi()
    hideNavigation()
    followedShowsRoot.fadeOut(150) {
      exitSearch(false)
      val bundle = Bundle().apply { putLong(ARG_SHOW_ID, show.traktId) }
      navigateTo(R.id.actionFollowedShowsFragmentToShowDetailsFragment, bundle)
    }.add(animations)
    viewModel.tabsTranslation = followedShowsTabs.translationY
    viewModel.searchViewTranslation = followedShowsSearchView.translationY
  }

  private fun openSettings() {
    hideNavigation()
    navigateTo(R.id.actionFollowedShowsFragmentToSettingsFragment)

    viewModel.tabsTranslation = followedShowsTabs.translationY
    viewModel.searchViewTranslation = followedShowsSearchView.translationY
  }

  private fun openStatistics() {
    hideNavigation()
    navigateTo(R.id.actionFollowedShowsFragmentToStatisticsFragment)

    viewModel.tabsTranslation = followedShowsTabs.translationY
    viewModel.searchViewTranslation = followedShowsSearchView.translationY
  }

  fun enableSearch(enable: Boolean) {
    followedShowsSearchView.isClickable = enable
    followedShowsSearchView.isEnabled = enable
  }

  override fun onTabReselected() {
    followedShowsSearchView.translationY = 0F
    followedShowsTabs.translationY = 0F
    followedShowsModeTabs.translationY = 0F
    followedShowsSortIcon.translationY = 0F
    followedShowsPager.nextPage()
    childFragmentManager.fragments.forEach {
      (it as? OnScrollResetListener)?.onScrollReset()
    }
  }

  override fun onTraktSyncProgress() =
    followedShowsSearchView.setTraktProgress(true)

  override fun onTraktSyncComplete() {
    followedShowsSearchView.setTraktProgress(false)
    childFragmentManager.fragments.forEach {
      (it as? OnTraktSyncListener)?.onTraktSyncComplete()
    }
  }

  private val pageChangeListener = object : ViewPager.OnPageChangeListener {
    override fun onPageSelected(position: Int) {
      if (currentPage == position) return

      followedShowsSortIcon.fadeIf(position != 0, duration = 150)
      if (followedShowsTabs.translationY != 0F) {
        followedShowsSearchView.animate().translationY(0F).setDuration(225L).start()
        followedShowsTabs.animate().translationY(0F).setDuration(225L).start()
        followedShowsModeTabs.animate().translationY(0F).setDuration(225L).start()
        followedShowsSortIcon.animate().translationY(0F).setDuration(225L).start()
        requireView().postDelayed(
          {
            childFragmentManager.fragments.forEach {
              (it as? OnScrollResetListener)?.onScrollReset()
            }
          },
          225L
        )
      }

      currentPage = position
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
    override fun onPageScrollStateChanged(state: Int) = Unit
  }
}

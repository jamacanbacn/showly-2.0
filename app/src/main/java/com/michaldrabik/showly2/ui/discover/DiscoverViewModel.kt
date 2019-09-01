package com.michaldrabik.showly2.ui.discover

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.michaldrabik.showly2.model.Image
import com.michaldrabik.showly2.model.ImageType.FANART
import com.michaldrabik.showly2.model.ImageType.POSTER
import com.michaldrabik.showly2.model.Show
import com.michaldrabik.showly2.ui.common.base.BaseViewModel
import com.michaldrabik.showly2.ui.discover.recycler.DiscoverListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class DiscoverViewModel @Inject constructor(
  private val interactor: DiscoverInteractor
) : BaseViewModel() {

  val uiStream by lazy { MutableLiveData<DiscoverUiModel>() }

  fun loadTrendingShows() {
    viewModelScope.launch {
      val progress = launch {
        delay(250)
        uiStream.value = DiscoverUiModel(showLoading = true)
      }
      try {
        val shows = interactor.loadTrendingShows()
        onShowsLoaded(shows)
      } catch (t: Throwable) {
        onError(t)
      } finally {
        uiStream.value = DiscoverUiModel(showLoading = false)
        progress.cancel()
      }
    }
  }

  private suspend fun onShowsLoaded(shows: List<Show>) {
    val items = shows.mapIndexed { index, show ->
      val itemType =
        when (index) {
          in (0..200 step 16), in (9..200 step 16) -> FANART
          else -> POSTER
        }
      val image = interactor.findCachedImage(show, itemType)
      DiscoverListItem(show, image)
    }
    uiStream.value = DiscoverUiModel(trendingShows = items)
  }

  fun loadMissingImage(item: DiscoverListItem, force: Boolean) {
    viewModelScope.launch {
      uiStream.value = DiscoverUiModel(updateListItem = item.copy(isLoading = true))
      try {
        val image = interactor.loadMissingImage(item.show, item.image.type, force)
        uiStream.value =
          DiscoverUiModel(updateListItem = item.copy(isLoading = false, image = image))
      } catch (t: Throwable) {
        uiStream.value =
          DiscoverUiModel(updateListItem = item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
        onError(t)
      }
    }
  }

  private fun onError(error: Throwable) {
    //TODO
  }
}
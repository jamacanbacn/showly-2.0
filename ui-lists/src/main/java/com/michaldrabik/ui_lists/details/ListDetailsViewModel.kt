package com.michaldrabik.ui_lists.details

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.michaldrabik.common.Config
import com.michaldrabik.common.Mode
import com.michaldrabik.ui_base.BaseViewModel
import com.michaldrabik.ui_base.Logger
import com.michaldrabik.ui_base.images.MovieImagesProvider
import com.michaldrabik.ui_base.images.ShowImagesProvider
import com.michaldrabik.ui_base.utilities.ActionEvent
import com.michaldrabik.ui_base.utilities.MessageEvent
import com.michaldrabik.ui_base.utilities.extensions.findReplace
import com.michaldrabik.ui_lists.R
import com.michaldrabik.ui_lists.details.cases.ListDetailsItemsCase
import com.michaldrabik.ui_lists.details.cases.ListDetailsMainCase
import com.michaldrabik.ui_lists.details.cases.ListDetailsSortCase
import com.michaldrabik.ui_lists.details.cases.ListDetailsTipsCase
import com.michaldrabik.ui_lists.details.cases.ListDetailsTranslationsCase
import com.michaldrabik.ui_lists.details.recycler.ListDetailsItem
import com.michaldrabik.ui_model.Image
import com.michaldrabik.ui_model.SortOrderList
import com.michaldrabik.ui_model.Tip
import kotlinx.coroutines.launch
import javax.inject.Inject

class ListDetailsViewModel @Inject constructor(
  private val mainCase: ListDetailsMainCase,
  private val itemsCase: ListDetailsItemsCase,
  private val translationsCase: ListDetailsTranslationsCase,
  private val sortCase: ListDetailsSortCase,
  private val tipsCase: ListDetailsTipsCase,
  private val showImagesProvider: ShowImagesProvider,
  private val movieImagesProvider: MovieImagesProvider,
) : BaseViewModel<ListDetailsUiModel>() {

  fun loadDetails(id: Long) {
    viewModelScope.launch {
      val list = mainCase.loadDetails(id)
      val listItems = itemsCase.loadItems(list)
      val quickRemoveEnabled = mainCase.isQuickRemoveEnabled(list)

      uiState = ListDetailsUiModel(
        details = list,
        items = listItems,
        isManageMode = false,
        isQuickRemoveEnabled = quickRemoveEnabled
      )

      val tip = Tip.LIST_ITEM_SWIPE_DELETE
      if (listItems.isNotEmpty() && !tipsCase.isTipShown(tip)) {
        _messageLiveData.value = MessageEvent.info(tip.textResId, indefinite = true)
        tipsCase.setTipShown(tip)
      }
    }
  }

  fun loadMissingImage(item: ListDetailsItem, force: Boolean) {
    viewModelScope.launch {
      updateItem(item.copy(isLoading = true))
      try {
        val image =
          when {
            item.isShow() -> showImagesProvider.loadRemoteImage(item.requireShow(), item.image.type, force)
            item.isMovie() -> movieImagesProvider.loadRemoteImage(item.requireMovie(), item.image.type, force)
            else -> throw IllegalStateException()
          }
        updateItem(item.copy(isLoading = false, image = image))
      } catch (t: Throwable) {
        updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
      }
    }
  }

  fun loadMissingTranslation(item: ListDetailsItem) {
    if (item.translation != null || translationsCase.language == Config.DEFAULT_LANGUAGE) return
    viewModelScope.launch {
      try {
        val translation = translationsCase.loadTranslation(item, false)
        updateItem(item.copy(translation = translation))
      } catch (error: Throwable) {
        Logger.record(error, "Source" to "ListDetailsViewModel::loadMissingTranslation()")
      }
    }
  }

  fun setReorderMode(listId: Long, isReorderMode: Boolean) {
    viewModelScope.launch {
      uiState = if (isReorderMode) {
        val list = mainCase.loadDetails(listId).copy(
          sortByLocal = SortOrderList.RANK,
          filterTypeLocal = Mode.getAll()
        )
        val listItems = itemsCase.loadItems(list).map { it.copy(isManageMode = true) }
        ListDetailsUiModel(items = listItems, isManageMode = true, resetScroll = ActionEvent(false))
      } else {
        val list = mainCase.loadDetails(listId)
        val listItems = itemsCase.loadItems(list).map { it.copy(isManageMode = false) }
        ListDetailsUiModel(items = listItems, isManageMode = false, resetScroll = ActionEvent(false))
      }
    }
  }

  fun updateRanks(listId: Long, items: List<ListDetailsItem>) {
    viewModelScope.launch {
      val updatedItems = mainCase.updateRanks(listId, items)
      uiState = ListDetailsUiModel(items = updatedItems)
    }
  }

  fun setSortOrder(id: Long, sortOrder: SortOrderList) {
    viewModelScope.launch {
      val list = sortCase.setSortOrder(id, sortOrder)

      val currentItems = uiState?.items?.toList() ?: emptyList()
      val sortedItems = itemsCase.sortItems(currentItems, list.sortByLocal, list.filterTypeLocal)

      uiState = ListDetailsUiModel(
        details = list,
        items = sortedItems,
        resetScroll = ActionEvent(true)
      )
    }
  }

  fun setSortTypes(listId: Long, types: List<Mode>) {
    if (types.isEmpty()) return

    viewModelScope.launch {
      val list = sortCase.setSortTypes(listId, types)
      val sortedItems = itemsCase.loadItems(list)

      uiState = ListDetailsUiModel(
        details = list,
        items = sortedItems,
        resetScroll = ActionEvent(true)
      )
    }
  }

  fun deleteList(listId: Long, removeFromTrakt: Boolean) {
    viewModelScope.launch {
      try {
        if (removeFromTrakt) {
          uiState = ListDetailsUiModel(isLoading = true)
        }
        mainCase.deleteList(listId, removeFromTrakt)
        uiState = ListDetailsUiModel(isLoading = false, deleteEvent = ActionEvent(true))
      } catch (error: Throwable) {
        _messageLiveData.value = MessageEvent.error(R.string.errorCouldNotDeleteList)
        uiState = ListDetailsUiModel(isLoading = false)
      }
    }
  }

  fun deleteListItem(
    context: Context,
    listId: Long,
    item: ListDetailsItem,
  ) {
    viewModelScope.launch {
      val type =
        when {
          item.isShow() -> Mode.SHOWS
          item.isMovie() -> Mode.MOVIES
          else -> throw IllegalStateException()
        }
      itemsCase.deleteListItem(context, listId, item.getTraktId(), type)
      loadDetails(listId)
    }
  }

  private fun updateItem(newItem: ListDetailsItem) {
    val currentItems = uiState?.items?.toMutableList()
    currentItems?.findReplace(newItem) { it.id == newItem.id }
    uiState = uiState?.copy(items = currentItems)
  }
}

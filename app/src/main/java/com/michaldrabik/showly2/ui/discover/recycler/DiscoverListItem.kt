package com.michaldrabik.showly2.ui.discover.recycler

import com.michaldrabik.showly2.model.Image
import com.michaldrabik.showly2.model.Show

data class DiscoverListItem(
  override val show: Show,
  override val image: Image,
  var isLoading: Boolean = false
) : ListItem
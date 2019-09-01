package com.michaldrabik.showly2.ui.shows

import com.michaldrabik.network.Cloud
import com.michaldrabik.showly2.ui.common.base.BaseViewModel
import com.michaldrabik.storage.repository.UserRepository
import javax.inject.Inject

class ShowDetailsViewModel @Inject constructor(
  private val cloud: Cloud,
  private val userRepository: UserRepository
) : BaseViewModel()

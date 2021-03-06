package com.michaldrabik.showly2.ui.main.cases

import android.content.Context
import com.michaldrabik.repository.RatingsRepository
import com.michaldrabik.repository.SettingsRepository
import com.michaldrabik.showly2.BuildConfig
import com.michaldrabik.ui_base.images.MovieImagesProvider
import com.michaldrabik.ui_base.images.ShowImagesProvider
import com.michaldrabik.ui_base.notifications.AnnouncementManager
import timber.log.Timber
import javax.inject.Inject

class MainMiscCase @Inject constructor(
  private val ratingsRepository: RatingsRepository,
  private val settingsRepository: SettingsRepository,
  private val showImagesProvider: ShowImagesProvider,
  private val movieImagesProvider: MovieImagesProvider,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun refreshAnnouncements(context: Context) {
    announcementManager.refreshShowsAnnouncements(context)
    announcementManager.refreshMoviesAnnouncements(context)
  }

  fun moviesEnabled() = settingsRepository.isMoviesEnabled

  fun newsEnabled(): Boolean {
    if (BuildConfig.DEBUG) return true
    return settingsRepository.isNewsEnabled && settingsRepository.isPremium
  }

  fun clear() {
    ratingsRepository.clear()
    showImagesProvider.clear()
    movieImagesProvider.clear()
    Timber.d("Clearing...")
  }
}

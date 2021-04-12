package com.michaldrabik.showly2.common

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.michaldrabik.showly2.common.movies.MoviesSyncRunner
import com.michaldrabik.showly2.common.shows.ShowsSyncRunner
import com.michaldrabik.showly2.serviceComponent
import com.michaldrabik.ui_base.Logger
import com.michaldrabik.ui_base.events.EventsManager
import com.michaldrabik.ui_base.events.ShowsMoviesSyncComplete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class ShowsMoviesSyncService : JobIntentService(), CoroutineScope {

  companion object {
    private const val JOB_ID = 999

    fun initialize(context: Context) {
      enqueueWork(context, ShowsMoviesSyncService::class.java, JOB_ID, Intent())
    }
  }

  override val coroutineContext = Job() + Dispatchers.Default

  @Inject lateinit var showsSyncRunner: ShowsSyncRunner
  @Inject lateinit var moviesSyncRunner: MoviesSyncRunner

  override fun onHandleWork(intent: Intent) {
    Timber.d("Sync service initialized")
    serviceComponent().inject(this)

    val showsAsync = async {
      try {
        return@async showsSyncRunner.run()
      } catch (t: Throwable) {
        Logger.record(t, "Source" to "ShowsSyncRunner")
        return@async 0
      }
    }

    val moviesAsync = async {
      try {
        return@async moviesSyncRunner.run()
      } catch (t: Throwable) {
        Logger.record(t, "Source" to "MoviesSyncRunner")
        return@async 0
      }
    }

    val syncCount = runBlocking {
      val (count1, count2) = awaitAll(showsAsync, moviesAsync)
      count1 + count2
    }

    if (syncCount > 0) {
      EventsManager.sendEvent(ShowsMoviesSyncComplete)
    }
  }

  override fun onDestroy() {
    coroutineContext.cancel()
    Timber.d("Sync service destroyed")
    super.onDestroy()
  }
}

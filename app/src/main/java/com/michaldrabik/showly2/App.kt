package com.michaldrabik.showly2

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.StrictMode
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.processphoenix.ProcessPhoenix
import com.jakewharton.threetenabp.AndroidThreeTen
import com.michaldrabik.common.Config.DEFAULT_LANGUAGE
import com.michaldrabik.network.di.DaggerCloudComponent
import com.michaldrabik.showly2.common.ShowsMoviesSyncActivityCallbacks
import com.michaldrabik.showly2.di.component.AppComponent
import com.michaldrabik.showly2.di.component.DaggerAppComponent
import com.michaldrabik.showly2.di.module.PreferencesModule
import com.michaldrabik.showly2.utilities.NetworkMonitorCallbacks
import com.michaldrabik.storage.di.DaggerStorageComponent
import com.michaldrabik.storage.di.StorageModule
import com.michaldrabik.ui_base.common.OnTraktSyncListener
import com.michaldrabik.ui_base.common.OnlineStatusProvider
import com.michaldrabik.ui_base.common.WidgetsProvider
import com.michaldrabik.ui_base.di.UiBaseComponentProvider
import com.michaldrabik.ui_base.events.EventsActivityCallbacks
import com.michaldrabik.ui_base.utilities.extensions.notificationManager
import com.michaldrabik.ui_model.Settings
import com.michaldrabik.ui_repository.SettingsRepository
import com.michaldrabik.ui_widgets.WidgetSettings
import com.michaldrabik.ui_widgets.calendar.CalendarWidgetProvider
import com.michaldrabik.ui_widgets.calendar_movies.CalendarMoviesWidgetProvider
import com.michaldrabik.ui_widgets.di.UiWidgetsComponentProvider
import com.michaldrabik.ui_widgets.progress.ProgressWidgetProvider
import com.michaldrabik.ui_widgets.progress_movies.ProgressMoviesWidgetProvider
import com.yariksoffice.lingver.Lingver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import com.michaldrabik.ui_base.fcm.NotificationChannel as AppNotificationChannel

class App :
  Application(),
  UiBaseComponentProvider,
  UiWidgetsComponentProvider,
  OnlineStatusProvider,
  WidgetsProvider,
  OnTraktSyncListener {

  @Inject lateinit var settingsRepository: SettingsRepository

  var isAppOnline = true
  lateinit var appComponent: AppComponent
  private var isSyncRunning = false

  private val activityCallbacks by lazy {
    listOf(
      EventsActivityCallbacks(),
      ShowsMoviesSyncActivityCallbacks(),
      NetworkMonitorCallbacks(connectivityManager())
    )
  }

  private val appScope = MainScope()

  override fun onCreate() {
    super.onCreate()
    if (ProcessPhoenix.isPhoenixProcess(this)) return

    activityCallbacks.forEach { registerActivityLifecycleCallbacks(it) }

    AndroidThreeTen.init(this)
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      FirebaseMessaging.getInstance().token.addOnCompleteListener {
        Timber.d("FCM Token: ${it.result}")
      }
    }

    setupComponents()
    setupSettings()
    setupStrictMode()
    setupNotificationChannels()
    setupLanguage()
  }

  private fun setupLanguage() {
    Lingver.init(this, DEFAULT_LANGUAGE)
    val language = settingsRepository.getLanguage()
    Lingver.getInstance().setLocale(this, language)
  }

  private fun setupComponents() {
    appComponent = DaggerAppComponent.builder()
      .cloudMarker(DaggerCloudComponent.create())
      .storageMarker(
        DaggerStorageComponent.builder()
          .storageModule(StorageModule(applicationContext))
          .build()
      )
      .preferencesModule(PreferencesModule(applicationContext))
      .build()
    appComponent.inject(this)
  }

  private fun setupSettings() = runBlocking {
    if (!settingsRepository.isInitialized()) {
      settingsRepository.update(Settings.createInitial())
    }
  }

  private fun setupStrictMode() {
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
          .detectAll()
          .penaltyLog()
          .build()
      )
    }
  }

  private fun setupNotificationChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    fun createChannel(channel: AppNotificationChannel) =
      NotificationChannel(channel.name, channel.displayName, channel.importance).apply {
        description = channel.description
      }

    notificationManager().run {
      createNotificationChannel(createChannel(AppNotificationChannel.GENERAL_INFO))
      createNotificationChannel(createChannel(AppNotificationChannel.SHOWS_INFO))
      createNotificationChannel(createChannel(AppNotificationChannel.EPISODES_ANNOUNCEMENTS))
      createNotificationChannel(createChannel(AppNotificationChannel.MOVIES_ANNOUNCEMENTS))
    }
  }

  override fun isOnline() = isAppOnline

  override fun requestShowsWidgetsUpdate() {
    appScope.launch {
      val settings = if (settingsRepository.isInitialized()) settingsRepository.load() else null
      val widgetSettings = settings?.let {
        WidgetSettings(showLabel = it.widgetsShowLabel)
      } ?: WidgetSettings.createInitial()

      ProgressWidgetProvider.requestUpdate(applicationContext, widgetSettings)
      CalendarWidgetProvider.requestUpdate(applicationContext, widgetSettings)
    }
  }

  override fun requestMoviesWidgetsUpdate() {
    appScope.launch {
      val settings = if (settingsRepository.isInitialized()) settingsRepository.load() else null
      val widgetSettings = settings?.let {
        WidgetSettings(showLabel = it.widgetsShowLabel)
      } ?: WidgetSettings.createInitial()

      ProgressMoviesWidgetProvider.requestUpdate(applicationContext, widgetSettings)
      CalendarMoviesWidgetProvider.requestUpdate(applicationContext, widgetSettings)
    }
  }

  override fun provideBaseComponent() = appComponent.uiBaseComponent().create()
  override fun provideWidgetsComponent() = appComponent.uiWidgetsComponent().create()

  override fun onTraktSyncProgress() = run { isSyncRunning = true }
  override fun onTraktSyncComplete() = run { isSyncRunning = false }
  override fun isTraktSyncActive() = isSyncRunning
}

fun Context.connectivityManager() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

fun Activity.appComponent() = (application as App).appComponent

fun Service.serviceComponent() = (application as App).appComponent.serviceComponent().create()

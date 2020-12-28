package com.michaldrabik.ui_widgets.calendar_movies

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import com.michaldrabik.common.Config
import com.michaldrabik.ui_base.utilities.extensions.dimenToPx
import com.michaldrabik.ui_widgets.BaseWidgetProvider
import com.michaldrabik.ui_widgets.R
import com.michaldrabik.ui_widgets.WidgetSettings
import timber.log.Timber

class CalendarMoviesWidgetProvider : BaseWidgetProvider() {

  companion object {
    fun requestUpdate(context: Context, widgetSettings: WidgetSettings) {
      val applicationContext = context.applicationContext
      val intent = Intent(applicationContext, CalendarMoviesWidgetProvider::class.java).apply {
        val ids: IntArray = AppWidgetManager.getInstance(applicationContext)
          .getAppWidgetIds(ComponentName(applicationContext, CalendarMoviesWidgetProvider::class.java))
        action = ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        putExtra(EXTRA_SETTINGS, widgetSettings)
      }
      applicationContext.sendBroadcast(intent)
      Timber.d("Widget update requested.")
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray?
  ) {
    appWidgetIds?.forEach { updateWidget(context, appWidgetManager, it) }
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
    val intent = Intent(context, CalendarMoviesWidgetService::class.java).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
      data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }

    val remoteViews = RemoteViews(context.packageName, R.layout.widget_movies_calendar).apply {
      setRemoteAdapter(R.id.calendarWidgetMoviesList, intent)
      setEmptyView(R.id.calendarWidgetMoviesList, R.id.calendarWidgetMoviesEmptyView)

      val spaceTiny = context.dimenToPx(R.dimen.spaceTiny)
      val paddingTop = if (settings?.showLabel == false) spaceTiny else context.dimenToPx(R.dimen.widgetPaddingTop)
      val labelVisibility = if (settings?.showLabel == false) GONE else VISIBLE
      setViewPadding(R.id.calendarWidgetMoviesList, 0, paddingTop, 0, spaceTiny)
      setViewVisibility(R.id.calendarWidgetMoviesLabel, labelVisibility)
    }

    val listClickIntent = Intent(context, CalendarMoviesWidgetProvider::class.java).apply {
      action = ACTION_LIST_CLICK
      data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
    }

    val listIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setPendingIntentTemplate(R.id.calendarWidgetMoviesList, listIntent)

    appWidgetManager.updateAppWidget(widgetId, remoteViews)
    appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.calendarWidgetMoviesList)
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_LIST_CLICK) {
      when {
        intent.extras?.containsKey(EXTRA_MOVIE_ID) == true -> {
          val movieId = intent.getLongExtra(EXTRA_MOVIE_ID, -1L)
          context.startActivity(
            Intent().apply {
              setClassName(context, Config.HOST_ACTIVITY_NAME)
              putExtra(EXTRA_MOVIE_ID, movieId.toString())
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
          )
        }
      }
    }
    super.onReceive(context, intent)
  }
}
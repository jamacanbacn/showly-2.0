package com.michaldrabik.ui_widgets.calendar

import android.content.Intent
import android.widget.RemoteViewsService
import com.michaldrabik.ui_base.images.ShowImagesProvider
import com.michaldrabik.ui_progress.calendar.cases.ProgressCalendarCase
import com.michaldrabik.ui_progress.main.cases.ProgressLoadItemsCase
import com.michaldrabik.ui_widgets.di.UiWidgetsComponentProvider
import javax.inject.Inject

class CalendarWidgetService : RemoteViewsService() {

  @Inject lateinit var progressLoadItemsCase: ProgressLoadItemsCase
  @Inject lateinit var calendarCase: ProgressCalendarCase
  @Inject lateinit var imagesProvider: ShowImagesProvider

  override fun onCreate() {
    super.onCreate()
    (application as UiWidgetsComponentProvider).provideWidgetsComponent().inject(this)
  }

  override fun onGetViewFactory(intent: Intent?) =
    CalendarWidgetViewsFactory(
      applicationContext,
      progressLoadItemsCase,
      calendarCase,
      imagesProvider
    )
}
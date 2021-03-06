package com.michaldrabik.ui_progress_movies.calendar.cases

import androidx.annotation.StringRes
import com.michaldrabik.common.extensions.nowUtcDay
import com.michaldrabik.ui_progress_movies.ProgressMovieItem
import com.michaldrabik.ui_progress_movies.R
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.LATER
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.NEXT_MONTH
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.NEXT_WEEK
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.NEXT_YEAR
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.THIS_WEEK
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.THIS_YEAR
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.TODAY
import com.michaldrabik.ui_progress_movies.calendar.cases.ProgressMoviesCalendarCase.Section.TOMORROW
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

class ProgressMoviesCalendarCase @Inject constructor() {

  fun prepareItems(items: List<ProgressMovieItem>): List<ProgressMovieItem> {
    val calendarItems = items
      .filter { !it.movie.hasAired() || it.movie.isToday() }
      .sortedBy { it.movie.released?.toEpochDay() }
      .toMutableList()

    return groupByTime(calendarItems)
  }

  private fun groupByTime(items: MutableList<ProgressMovieItem>): List<ProgressMovieItem> {
    val today = nowUtcDay()
    val nextWeekStart = today.plusDays(((DayOfWeek.SUNDAY.value - today.dayOfWeek.value) + 1L))

    val timeMap = mutableMapOf<Section, MutableList<ProgressMovieItem>>()
    val sectionsList = mutableListOf<ProgressMovieItem>()

    items
      .filter { it.movie.released != null }
      .forEach { item ->
        val time = item.movie.released!!
        val isSameYear = time.year == today.year
        when {
          isSameYear && time.dayOfYear == today.dayOfYear ->
            timeMap.getOrPut(TODAY, { mutableListOf() }).add(item)
          isSameYear && time.dayOfYear == today.plusDays(1).dayOfYear ->
            timeMap.getOrPut(TOMORROW, { mutableListOf() }).add(item)
          time.isBefore(nextWeekStart) ->
            timeMap.getOrPut(THIS_WEEK, { mutableListOf() }).add(item)
          time.isBefore(nextWeekStart.plusWeeks(1)) ->
            timeMap.getOrPut(NEXT_WEEK, { mutableListOf() }).add(item)
          isSameYear && time.month == today.plusMonths(1).month ->
            timeMap.getOrPut(NEXT_MONTH, { mutableListOf() }).add(item)
          isSameYear ->
            timeMap.getOrPut(THIS_YEAR, { mutableListOf() }).add(item)
          time.year == today.plusYears(1).year ->
            timeMap.getOrPut(NEXT_YEAR, { mutableListOf() }).add(item)
          else ->
            timeMap.getOrPut(LATER, { mutableListOf() }).add(item)
        }
      }

    timeMap.entries
      .sortedBy { it.key.order }
      .forEach { (section, items) ->
        sectionsList.run {
          add(ProgressMovieItem.EMPTY.copy(headerTextResId = section.headerRes))
          addAll(items.toList())
        }
      }

    return sectionsList
  }

  enum class Section(@StringRes val headerRes: Int, val order: Int) {
    TODAY(R.string.textToday, 0),
    TOMORROW(R.string.textTomorrow, 1),
    THIS_WEEK(R.string.textThisWeek, 2),
    NEXT_WEEK(R.string.textNextWeek, 3),
    NEXT_MONTH(R.string.textNextMonth, 4),
    THIS_YEAR(R.string.textThisYear, 5),
    NEXT_YEAR(R.string.textNextYear, 6),
    LATER(R.string.textLater, 7)
  }
}

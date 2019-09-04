package com.michaldrabik.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.michaldrabik.storage.database.dao.ImagesDao
import com.michaldrabik.storage.database.dao.ShowsDao
import com.michaldrabik.storage.database.dao.TrendingShowsDao
import com.michaldrabik.storage.database.dao.UserDao
import com.michaldrabik.storage.database.model.Image
import com.michaldrabik.storage.database.model.Show
import com.michaldrabik.storage.database.model.TrendingShow
import com.michaldrabik.storage.database.model.User

private const val DATABASE_VERSION = 1

@Database(entities = [Show::class, TrendingShow::class, Image::class, User::class], version = DATABASE_VERSION)
abstract class AppDatabase : RoomDatabase() {

  abstract fun showsDao(): ShowsDao

  abstract fun trendingShowsDao(): TrendingShowsDao

  abstract fun imagesDao(): ImagesDao

  abstract fun userDao(): UserDao
}
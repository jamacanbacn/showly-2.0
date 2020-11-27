package com.michaldrabik.ui_base.images

import com.michaldrabik.common.di.AppScope
import com.michaldrabik.network.Cloud
import com.michaldrabik.network.tmdb.model.TmdbImages
import com.michaldrabik.storage.database.AppDatabase
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.IdTrakt
import com.michaldrabik.ui_model.ImageSource.TMDB
import com.michaldrabik.ui_model.ImageStatus.AVAILABLE
import com.michaldrabik.ui_model.ImageStatus.UNAVAILABLE
import com.michaldrabik.ui_model.ImageType
import com.michaldrabik.ui_model.ImageType.FANART
import com.michaldrabik.ui_model.ImageType.FANART_WIDE
import com.michaldrabik.ui_model.ImageType.POSTER
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.MovieImage
import com.michaldrabik.ui_repository.mappers.Mappers
import javax.inject.Inject

@AppScope
class MovieImagesProvider @Inject constructor(
  private val cloud: Cloud,
  private val database: AppDatabase,
  private val mappers: Mappers
) {

  private val unavailableCache = mutableListOf<IdTrakt>()

  suspend fun findCachedImage(movie: Movie, type: ImageType): MovieImage {
    val image = database.movieImagesDao().getByMovieId(movie.ids.tmdb.id, type.key)
    return when (image) {
      null ->
        if (unavailableCache.contains(movie.ids.trakt)) {
          MovieImage.createUnavailable(type)
        } else {
          MovieImage.createUnknown(type)
        }
      else -> mappers.image.fromDatabase(image).copy(type = type)
    }
  }

  suspend fun loadRemoteImage(movie: Movie, type: ImageType, force: Boolean = false): MovieImage {
    val tmdbId = movie.ids.tmdb
    val cachedImage = findCachedImage(movie, type)
    if (cachedImage.status == AVAILABLE && !force) {
      return cachedImage
    }

    val images = cloud.tmdbApi.fetchMovieImages(tmdbId.id)
    val typeImages = when (type) {
      POSTER -> images.posters ?: emptyList()
      FANART, FANART_WIDE -> images.backdrops ?: emptyList()
    }

    val remoteImage = typeImages.firstOrNull()
    val image = when (remoteImage) {
      null -> MovieImage.createUnavailable(type)
      else -> MovieImage(
        -1,
        tmdbId,
        type,
        remoteImage.file_path,
        AVAILABLE,
        TMDB
      )
    }

    when (image.status) {
      UNAVAILABLE -> {
        unavailableCache.add(movie.ids.trakt)
        database.movieImagesDao().deleteByMovieId(tmdbId.id, image.type.key)
      }
      else -> {
        database.movieImagesDao().insertMovieImage(mappers.image.toDatabase(image))
        storeExtraImage(tmdbId, images, type)
      }
    }

    return image
  }

  private suspend fun storeExtraImage(
    id: IdTmdb,
    images: TmdbImages,
    targetType: ImageType
  ) {
    val extraType = if (targetType == POSTER) FANART else POSTER
    val typeImages = when (extraType) {
      POSTER -> images.posters ?: emptyList()
      FANART, FANART_WIDE -> images.backdrops ?: emptyList()
    }
    typeImages
      .firstOrNull()
      ?.let {
        val extraImage = MovieImage(-1, id, extraType, it.file_path, AVAILABLE, TMDB)
        database.movieImagesDao().insertMovieImage(mappers.image.toDatabase(extraImage))
      }
  }

  suspend fun loadRemoteImages(movie: Movie, type: ImageType): List<MovieImage> {
    val tmdbId = movie.ids.tmdb
    val remoteImages = cloud.tmdbApi.fetchMovieImages(tmdbId.id)
    val typeImages = when (type) {
      POSTER -> remoteImages.posters ?: emptyList()
      FANART, FANART_WIDE -> remoteImages.backdrops ?: emptyList()
    }

    return typeImages
      .map {
        MovieImage(-1, tmdbId, type, it.file_path, AVAILABLE, TMDB)
      }
  }

  suspend fun deleteLocalCache() = database.movieImagesDao().deleteAll()
}
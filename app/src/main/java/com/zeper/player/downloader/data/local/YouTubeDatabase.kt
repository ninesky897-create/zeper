package com.zeper.player.downloader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SearchHistoryEntity::class, LikedVideoEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class YouTubeDatabase : RoomDatabase() {

    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun youtubeDao(): YouTubeDao

    companion object {
        @Volatile
        private var INSTANCE: YouTubeDatabase? = null

        fun getDatabase(context: Context): YouTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YouTubeDatabase::class.java,
                    "youtube_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

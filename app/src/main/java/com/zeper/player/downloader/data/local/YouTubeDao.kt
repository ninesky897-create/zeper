package com.zeper.player.downloader.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubeDao {
    @Query("SELECT * FROM liked_videos ORDER BY timestamp DESC")
    fun getLikedVideos(): Flow<List<LikedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiked(video: LikedVideoEntity)

    @Delete
    suspend fun deleteLiked(video: LikedVideoEntity)

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTime DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

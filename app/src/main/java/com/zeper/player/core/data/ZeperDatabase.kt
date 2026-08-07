package com.zeper.player.core.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteEntity)

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistory(type: String): Flow<List<HistoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uri = :uri LIMIT 1)")
    fun isFavorite(uri: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(history: HistoryEntity)

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY timestamp DESC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE fileName LIKE '%' || :query || '%' OR format LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchDownloads(query: String): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: DownloadStatus)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("UPDATE downloads SET fileName = :newName WHERE id = :id")
    suspend fun renameDownload(id: String, newName: String)

    @Query("SELECT * FROM trash ORDER BY deleteTimestamp DESC")
    fun getTrashItems(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToTrash(item: TrashEntity)

    @Delete
    suspend fun removeFromTrash(item: TrashEntity)

    @Query("DELETE FROM trash WHERE originalPath = :path")
    suspend fun deleteTrashByOriginalPath(path: String)

    // --- Vault ---
    @Query("SELECT * FROM vault ORDER BY timestamp DESC")
    fun getVaultItems(): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToVault(item: VaultEntity)

    @Delete
    suspend fun removeFromVault(item: VaultEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM vault WHERE originalPath = :path LIMIT 1)")
    suspend fun isVaulted(path: String): Boolean
}

@Database(entities = [FavoriteEntity::class, HistoryEntity::class, DownloadEntity::class, TrashEntity::class, VaultEntity::class], version = 5, exportSchema = false)
abstract class ZeperDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: ZeperDatabase? = null

        fun getInstance(context: android.content.Context): ZeperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZeperDatabase::class.java,
                    "zeper_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

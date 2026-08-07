package com.zeper.player.core.data

/**
 * DummyData.kt — Sample data for Phase 1 UI development.
 *
 * This file provides pre-populated lists of videos, folders, and history items
 * so the Home Screen can render a realistic preview without needing MediaStore access.
 * These will be replaced with real data from MediaScanner in Phase 2.
 */

// ── Data Classes ────────────────────────────────────────────────

/**
 * Represents a sample video for dummy display.
 * @param id Unique identifier
 * @param name Display name of the video
 * @param durationMs Duration in milliseconds
 * @param sizeBytes File size in bytes
 * @param folderName The folder this video belongs to
 * @param path Fake path for display purposes
 */
data class DummyVideo(
    val id: Long,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val folderName: String,
    val path: String
)

/**
 * Represents a folder containing videos.
 * @param name Folder display name
 * @param videoCount Number of videos in this folder
 * @param isExternal Whether the folder is on external storage (SD card)
 */
data class DummyFolder(
    val name: String,
    val videoCount: Int,
    val isExternal: Boolean = false,
    val subtitle: String? = null
)

/**
 * Represents a history entry — a video the user has watched.
 * @param video The video that was watched
 * @param progressPercent How much of the video was watched (0.0 to 1.0)
 */
data class DummyHistoryItem(
    val video: DummyVideo,
    val progressPercent: Float
)

// ── Sample Data ─────────────────────────────────────────────────

object DummyDataProvider {

    /**
     * 10 sample videos spread across different folders.
     */
    val sampleVideos: List<DummyVideo> = listOf(
        DummyVideo(1, "Nature Calm 1min.mp4", 60_000, 15_000_000, "Download", "/storage/emulated/0/Download/Nature Calm 1min.mp4"),
        DummyVideo(2, "City Life 1min.mp4", 60_000, 22_000_000, "Download", "/storage/emulated/0/Download/City Life 1min.mp4"),
        DummyVideo(3, "Ocean Waves 1min.mp4", 60_000, 18_000_000, "Download", "/storage/emulated/0/Download/Ocean Waves 1min.mp4"),
        DummyVideo(4, "Gaming Montage 2024.mp4", 425_000, 680_000_000, "Gaming Clips", "/storage/emulated/0/Gaming Clips/Gaming Montage 2024.mp4"),
        DummyVideo(5, "Sunset Timelapse.mp4", 62_000, 150_000_000, "Camera", "/storage/emulated/0/DCIM/Camera/Sunset Timelapse.mp4"),
        DummyVideo(6, "Product Review - Phone.mp4", 540_000, 720_000_000, "Download", "/storage/emulated/0/Download/Product Review - Phone.mp4"),
        DummyVideo(7, "Workout Routine Day 1.mp4", 1_200_000, 560_000_000, "Fitness", "/storage/emulated/0/Fitness/Workout Routine Day 1.mp4"),
        DummyVideo(8, "Travel Vlog - Cox's Bazar.mp4", 780_000, 1_200_000_000, "Travel", "/storage/emulated/0/Travel/Travel Vlog - Cox's Bazar.mp4"),
        DummyVideo(9, "Cooking Recipe - Biryani.mp4", 920_000, 480_000_000, "Recipes", "/storage/emulated/0/Recipes/Cooking Recipe - Biryani.mp4"),
        DummyVideo(10, "Music Video - Arijit.mp4", 245_000, 320_000_000, "Music Videos", "/storage/emulated/0/Music Videos/Music Video - Arijit.mp4")
    )

    /**
     * Folders derived from the sample videos + some extras.
     */
    val sampleFolders: List<DummyFolder> = listOf(
        DummyFolder("Camera", 3),
        DummyFolder("Download", 2),
        DummyFolder("Gaming Clips", 1),
        DummyFolder("Fitness", 1),
        DummyFolder("Travel", 1),
        DummyFolder("Recipes", 1),
        DummyFolder("Music Videos", 1),
        DummyFolder("WhatsApp Video", 12),
        DummyFolder("Telegram", 5),
        DummyFolder("Screen Recordings", 8)
    )

    /**
     * 5 recently watched videos with progress bars.
     */
    val sampleHistory: List<DummyHistoryItem> = listOf(
        DummyHistoryItem(sampleVideos[0], 0.72f),  // 72% watched
        DummyHistoryItem(sampleVideos[2], 0.35f),   // 35% watched
        DummyHistoryItem(sampleVideos[7], 0.88f),   // 88% watched
        DummyHistoryItem(sampleVideos[4], 1.0f),    // Fully watched
        DummyHistoryItem(sampleVideos[9], 0.15f)    // 15% watched
    )

    /**
     * Total count of all videos across all folders.
     */
    val totalVideoCount: Int = sampleFolders.sumOf { it.videoCount }
}

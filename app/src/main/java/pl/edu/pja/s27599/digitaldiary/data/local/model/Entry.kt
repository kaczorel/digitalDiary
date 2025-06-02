package pl.edu.pja.s27599.digitaldiary.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date


@Entity(tableName = "diary_entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val location: String?,
    val photoUri: String?,
    val audioUri: String?,
    val timestamp: Date = Date()
)
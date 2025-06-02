package pl.edu.pja.s27599.digitaldiary.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.edu.pja.s27599.digitaldiary.data.local.database.AppDatabase
import pl.edu.pja.s27599.digitaldiary.data.local.database.EntryDao
import pl.edu.pja.s27599.digitaldiary.data.local.model.Entry
import java.util.Date
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {


    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "digital_diary_db"
        )

            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    scope.launch(Dispatchers.IO) {
                        val entryDao = provideDatabase(context, scope).entryDao()
                        populateInitialData(entryDao)
                    }
                }
            })
            .build()
    }


    @Provides
    @Singleton
    fun provideEntryDao(database: AppDatabase): EntryDao {
        return database.entryDao()
    }

    private suspend fun populateInitialData(entryDao: EntryDao) {
        val entry1 = Entry(
            title = "My First Diary Entry",
            content = "Today I started my digital diary project. It's exciting!",
            location = "Warsaw",
            photoUri = null,
            audioUri = null,
            timestamp = Date(System.currentTimeMillis() - 86400000 * 3)
        )
        entryDao.insertEntry(entry1)

        val entry2 = Entry(
            title = "Learning Jetpack Compose",
            content = "Compose makes UI development so much faster and more intuitive. Still learning!",
            location = "Krakow",
            photoUri = null,
            audioUri = null,
            timestamp = Date(System.currentTimeMillis() - 86400000)
        )
        entryDao.insertEntry(entry2)

        val entry3 = Entry(
            title = "Project Progress",
            content = "Successfully implemented Room database and Hilt. Feeling good about the progress!",
            location = "Gdansk",
            photoUri = null,
            audioUri = null,
            timestamp = Date()
        )
        entryDao.insertEntry(entry3)
    }
}
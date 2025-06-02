package pl.edu.pja.s27599.digitaldiary.data.local.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pja.s27599.digitaldiary.data.local.database.EntryDao
import pl.edu.pja.s27599.digitaldiary.data.local.model.Entry
import javax.inject.Inject

class EntryRepository @Inject constructor(private val entryDao: EntryDao) {
    val allEntries: Flow<List<Entry>> = entryDao.getAllEntries()

    suspend fun insert(entry: Entry) {
        entryDao.insertEntry(entry)
    }

    suspend fun update(entry: Entry) {
        entryDao.updateEntry(entry)
    }

    suspend fun delete(entry: Entry) {
        entryDao.deleteEntry(entry)
    }

    suspend fun getEntryById(id: Int): Entry? {
        return entryDao.getEntryById(id)
    }
}

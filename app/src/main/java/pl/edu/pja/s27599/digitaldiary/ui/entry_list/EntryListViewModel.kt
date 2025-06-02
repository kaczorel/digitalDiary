package pl.edu.pja.s27599.digitaldiary.ui.entry_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.edu.pja.s27599.digitaldiary.data.local.model.Entry
import pl.edu.pja.s27599.digitaldiary.data.local.repository.EntryRepository
import javax.inject.Inject

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val repository: EntryRepository
) : ViewModel() {

    val uiState: StateFlow<EntryListUiState> = repository.allEntries
        .map { entries -> EntryListUiState(entries = entries) }
        .stateIn(
            scope = viewModelScope,

            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EntryListUiState(emptyList())
        )

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}


data class EntryListUiState(
    val entries: List<Entry>
)

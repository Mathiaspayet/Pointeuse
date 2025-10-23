package com.mapointeuse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapointeuse.data.Pointage
import com.mapointeuse.data.PointageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoriqueUiState(
    val pointages: List<Pointage> = emptyList(),
    val isLoading: Boolean = true
)

class HistoriqueViewModel(
    private val repository: PointageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoriqueUiState())
    val uiState: StateFlow<HistoriqueUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPointages().collect { pointages ->
                _uiState.value = _uiState.value.copy(
                    pointages = pointages,
                    isLoading = false
                )
            }
        }
    }
}

class HistoriqueViewModelFactory(
    private val repository: PointageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoriqueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoriqueViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

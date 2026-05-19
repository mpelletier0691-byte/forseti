package com.forseti.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.glossary.GlossaryRepository
import com.forseti.glossary.GlossaryTerm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val repository: GlossaryRepository
) : ViewModel() {
    val terms = MutableStateFlow<List<GlossaryTerm>>(emptyList())

    init { viewModelScope.launch { terms.value = repository.load() } }
}

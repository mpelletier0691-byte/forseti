package com.forseti.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.guides.GuideMeta
import com.forseti.guides.GuideRepository
import com.forseti.util.LocalizedAssets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class GuidesViewModel @Inject constructor(
    private val repository: GuideRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val guides = MutableStateFlow<List<GuideMeta>>(emptyList())

    private val bodyCache = ConcurrentHashMap<String, MutableStateFlow<String>>()

    init {
        viewModelScope.launch { guides.value = repository.loadIndex() }
    }

    fun bodyFor(meta: GuideMeta?): StateFlow<String> {
        if (meta == null) return EmptyBody
        val key = "${LocalizedAssets.languageTag(context)}:${meta.id}"
        return bodyCache.getOrPut(key) {
            val state = MutableStateFlow("")
            viewModelScope.launch { state.value = repository.loadBody(meta) }
            state
        }.asStateFlow()
    }

    private companion object {
        val EmptyBody: StateFlow<String> = MutableStateFlow("").asStateFlow()
    }
}

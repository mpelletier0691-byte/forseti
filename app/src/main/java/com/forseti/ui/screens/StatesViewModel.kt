package com.forseti.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.forseti.states.StateCacheManager
import com.forseti.states.StateDownloadWorker
import com.forseti.states.StateRule
import com.forseti.states.StateRulesCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val cacheManager: StateCacheManager
) : ViewModel() {
    val cached: StateFlow<Set<String>> = cacheManager.cached

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshSummary = MutableStateFlow<StateCacheManager.RefreshSummary?>(null)
    val refreshSummary: StateFlow<StateCacheManager.RefreshSummary?> = _refreshSummary.asStateFlow()

    fun toggleDownload(rule: StateRule) {
        if (cacheManager.isCached(rule)) {
            cacheManager.delete(rule)
            return
        }
        val req = OneTimeWorkRequestBuilder<StateDownloadWorker>()
            .setInputData(Data.Builder().putString(StateDownloadWorker.KEY_ABBR, rule.abbreviation).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }

    /** Re-fetch every locally cached rule from its official .gov source. */
    fun checkForUpdates() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            val all = StateRulesCatalog.states + StateRulesCatalog.circuits
            val summary = cacheManager.refreshAllCached(all)
            _refreshSummary.value = summary
            _refreshing.value = false
        }
    }

    fun consumeRefreshSummary() { _refreshSummary.value = null }
}

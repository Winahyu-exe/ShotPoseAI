package com.shotposeai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shotposeai.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val _dashboard = MutableLiveData<AppRepository.DashboardCounts>()
    val dashboard: LiveData<AppRepository.DashboardCounts> = _dashboard
    fun refresh() = viewModelScope.launch(Dispatchers.IO) { _dashboard.postValue(repository.dashboard()) }
}

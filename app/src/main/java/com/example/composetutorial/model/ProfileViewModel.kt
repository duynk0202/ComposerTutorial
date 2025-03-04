package com.example.composetutorial.model

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composetutorial.Profile
import com.example.composetutorial.service.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * @Author: NGUYỄN KHÁNH DUY
 * @Date: 4/3/25
 */
// ViewModel để quản lý danh sách Profile
class ProfileViewModel : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    var profileState: StateFlow<ProfileState> = _profileState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private var searchJob: Job? = null // Để hủy job cũ khi có query mới
    private var fullProfileList: List<Profile> = emptyList() // Lưu danh sách gốc

    init {
        fetchProfiles()
    }

    fun refreshProfiles() {
        fetchProfiles()
    }

    fun getProfileById(id: Int): Profile? {
        return fullProfileList.find { it.id == id }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            filterProfiles(query)
        }
    }

    fun updateProfile(updatedProfile: Profile) {
        val currentList = fullProfileList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedProfile.id } // Giả lập dựa trên name
        if (index != -1) {
            currentList[index] = updatedProfile
            fullProfileList = currentList
            filterProfiles("") // Cập nhật danh sách hiển thị
        }
    }



    fun fetchProfiles() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val response = RetrofitClient.profileApiService.getProfiles()
                fullProfileList =
                    response.map { profileResponse ->
                        // Chuyển từ ProfileResponse sang Profile
                        Profile(
                            id = profileResponse.id,
                            name = profileResponse.name,
                            age = (20 + profileResponse.id), // Giả lập tuổi
                            imageUrl = "https://via.placeholder.com/50" // Giả lập ảnh
                        )
                    }
                filterProfiles(_searchQuery.value)
            } catch (e: Exception) {
                // Xử lý lỗi (có thể thêm thông báo sau)
                _profileState.value = ProfileState.Error("Failed to load profiles: ${e.message}")
            }
        }
    }

    private fun filterProfiles(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullProfileList
        } else {
            fullProfileList.filter { it.name.contains(query, ignoreCase = true) }
        }
        _profileState.value = ProfileState.Success(filteredList)
        Log.d("sdfjsdghfjsdghfjd ",Gson().toJson(profileState.value))
    }
}

sealed class ProfileState {
    data object Loading : ProfileState()
    data class Success(val profiles: List<Profile>) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
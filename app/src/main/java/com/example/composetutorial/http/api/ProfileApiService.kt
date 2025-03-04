package com.example.composetutorial.http.api

import retrofit2.http.GET

/**
 * @Author: NGUYỄN KHÁNH DUY
 * @Date: 4/3/25
 */
interface ProfileApiService {
    @GET("users")
    suspend fun getProfiles(): List<ProfileResponse>
}

data class ProfileResponse(
    val id: Int,
    val name: String,
    val email: String // Dùng email làm imageUrl giả lập
)
package ru.social.nework.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.social.nework.data.dto.PostResponse
import ru.social.nework.data.dto.Event
import ru.social.nework.data.dto.Job
import ru.social.nework.data.dto.Media
import ru.social.nework.data.dto.PostRequest
import ru.social.nework.data.dto.Token
import ru.social.nework.data.dto.User

interface ApiService {
    @GET("api/posts")
    suspend fun getAllPosts(): Response<List<PostResponse>>

    @GET("api/posts/{id}")
    suspend fun getPostById(@Path("id") id: Long): Response<PostResponse>

    @POST("api/posts")
    suspend fun savePost(@Body post: PostRequest): Response<PostResponse>

    @DELETE("api/posts/{id}")
    suspend fun deletePost(@Path("id") id: Long): Response<Unit>

    @POST("api/posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<PostResponse>

    @DELETE("api/posts/{id}/likes")
    suspend fun dislikePost(@Path("id") id: Long): Response<PostResponse>

    @GET("api/events")
    suspend fun getAllEvents(): Response<List<Event>>

    @GET("api/users")
    suspend fun getAllUsers(): Response<List<User>>

    @FormUrlEncoded
    @POST("api/users/authentication")
    suspend fun authenticateUser(
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Response<Token>

    @Multipart
    @POST("api/users/registration")
    suspend fun registerUser(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part? = null
    ): Response<Token>

    @GET("api/my/jobs")
    suspend fun getMyJobs(): Response<List<Job>>

    @GET("api/{userId}/jobs")
    suspend fun getUserJobs(@Path("userId") userId: Long): Response<List<Job>>

    @POST("api/my/jobs")
    suspend fun saveJob(@Body job: Job): Response<Job>

    @DELETE("api/my/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: Long): Response<Unit>

    @Multipart
    @POST("api/media")
    suspend fun upload(@Part file: MultipartBody.Part): Response<Media>

    @GET("api/{authorId}/wall")
    suspend fun getUserWall(@Path("authorId") authorId: Long): Response<List<PostResponse>>
}
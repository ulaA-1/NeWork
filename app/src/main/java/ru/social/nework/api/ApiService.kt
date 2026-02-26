package ru.social.nework.api

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.social.nework.BuildConfig
import ru.social.nework.auth.AuthState
import ru.social.nework.dto.Event
import ru.social.nework.dto.EventApi
import ru.social.nework.dto.Job
import ru.social.nework.dto.Media
import ru.social.nework.dto.Post
import ru.social.nework.dto.PostApi
import ru.social.nework.dto.User

private const val BASE_URL = "${BuildConfig.BASE_URL}/api/"

fun okhttp(vararg interceptors: Interceptor): OkHttpClient = OkHttpClient.Builder()
    .apply {
        interceptors.forEach {
            this.addInterceptor(it)
        }
    }
    .build()

fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface ApiService {
    @GET("posts")
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getPostsLatest(@Query("count") count: Int): Response<List<Post>>

    @GET("posts/{id}/before")
    suspend fun getPostsBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("posts/{id}/after")
    suspend fun getPostsAfter(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("posts/{id}")
    suspend fun getPostById(@Path("id") id: Long): Response<Post>

    @POST("posts")
    suspend fun savePost(@Body post: PostApi): Response<Post>

    @Multipart
    @POST("media")
    suspend fun upload(@Part media: MultipartBody.Part): Response<Media>

    @DELETE("posts/{id}")
    suspend fun removePostById(@Path("id") id: Long): Response<Unit>

    @POST("posts/{id}/likes")
    suspend fun likePostById(@Path("id") id: Long): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikePostById(@Path("id") id: Long): Response<Post>

    @GET("posts/{id}/newer")
    suspend fun getNewerPosts(@Path("id") id: Long): Response<List<Post>>

    @GET("events")
    suspend fun getAllEvents(): Response<List<Event>>

    @GET("events/latest")
    suspend fun getEventsLatest(@Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}/before")
    suspend fun getEventsBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Event>>

    @GET("events/{id}/after")
    suspend fun getEventsAfter(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Event>>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: Long): Response<Event>

    @POST("events")
    suspend fun saveEvent(@Body event: EventApi): Response<Event>

    @DELETE("events/{id}")
    suspend fun removeEventById(@Path("id") id: Long): Response<Unit>

    @POST("events/{id}/likes")
    suspend fun likeEventById(@Path("id") id: Long): Response<Event>

    @DELETE("events/{id}/likes")
    suspend fun dislikeEventById(@Path("id") id: Long): Response<Event>

    @POST("events/{id}/participants")
    suspend fun participateEventById(@Path("id") id: Long): Response<Event>

    @DELETE("events/{id}/participants")
    suspend fun notParticipateEventById(@Path("id") id: Long): Response<Event>

    @GET("events/{id}/newer")
    suspend fun getNewerEvents(@Path("id") id: Long): Response<List<Event>>

    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun updateUser(
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Response<AuthState>

    @Multipart
    @POST("users/registration")
    suspend fun registerUserWithAvatar(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part media: MultipartBody.Part
    ): Response<AuthState>

    @FormUrlEncoded
    @POST("users/registration")
    suspend fun registerUser(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Field("name") name: String
    ): Response<AuthState>

    @GET("users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<User>

    @GET("{authorId}/wall")
    suspend fun getUserWall(@Path("authorId") id: Long): Response<List<Post>>

    @GET("{authorId}/wall/latest")
    suspend fun getUserWallLatest(
        @Path("authorId") authorId: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("{authorId}/wall/{id}/newer")
    suspend fun getUserWallNewer(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long
    ): Response<List<Post>>

    @GET("{authorId}/wall/{id}/before")
    suspend fun getUserWallBefore(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("{authorId}/wall/{id}/after")
    suspend fun getUserWallAfter(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("my/wall")
    suspend fun getMyWall(): Response<List<Post>>

    @GET("my/wall/latest")
    suspend fun getMyWallLatest(@Query("count") count: Int): Response<List<Post>>

    @GET("my/wall/{id}/newer")
    suspend fun getMyWallNewer(@Path("id") id: Long): Response<List<Post>>

    @GET("my/wall/{id}/before")
    suspend fun getMyWallBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("my/wall/{id}/after")
    suspend fun getMyWallAfter(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @POST("{authorId}/wall/{id}/likes")
    suspend fun likeUserPostById(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long
    ): Response<Post>

    @DELETE("{authorId}/wall/{id}/likes")
    suspend fun dislikeUserPostById(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long
    ): Response<Post>

    @GET("{authorId}/jobs")
    suspend fun getUserJobs(@Path("authorId") id: Long): Response<List<Job>>

    @POST("my/jobs")
    suspend fun saveJob(@Body job: Job): Response<Job>

    @DELETE("my/jobs/{id}")
    suspend fun removeJobById(@Path("id") jobId: Long): Response<Unit>
}


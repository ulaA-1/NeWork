package ru.social.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagingData
import androidx.paging.map
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import ru.social.nework.auth.AppAuth
import ru.social.nework.dto.Post
import ru.social.nework.repository.PostRepoUserWall
import ru.social.nework.repository.PostRepository

class WallViewModel @AssistedInject constructor(
    @PostRepoUserWall private val repository: PostRepository,
    auth: AppAuth,
    @Assisted private val userId: Long,
) : PostViewModel(repository, auth) {

    override var data: Flow<PagingData<Post>>

    @AssistedFactory
    interface Factory {
        fun create(userId: Long): WallViewModel
    }

    companion object {
        fun provideWallViewModelFactory(factory: Factory, userId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return factory.create(userId) as T
                }
            }
        }
    }

    init {
        repository.setUser(userId)
        @OptIn(ExperimentalCoroutinesApi::class)
        data = auth.authStateFlow
            .flatMapLatest { (myId, _) ->
                repository.data.map { pagingData ->
                    pagingData.map { post ->
                        post.copy(ownedByMe = post.authorId == myId)
                    }
                }
            }
    }
}
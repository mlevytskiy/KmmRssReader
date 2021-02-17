package com.github.jetbrains.app

import com.github.aakira.napier.Napier
import com.github.jetbrains.rssreader.RssReader
import com.github.jetbrains.rssreader.entity.Feed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FeedState(
    val progress: Boolean,
    val feeds: List<Feed>,
    val selectedFeed: Feed? = null //null means selected all
) : State

fun FeedState.mainFeedPosts() = (selectedFeed?.posts ?: feeds.flatMap { it.posts }).sortedByDescending { it.date }

sealed class FeedAction : Action {
    data class Refresh(val forceLoad: Boolean) : FeedAction()
    data class Add(val url: String) : FeedAction()
    data class Delete(val url: String) : FeedAction()
    data class SelectFeed(val feed: Feed?) : FeedAction()
    data class Data(val feeds: List<Feed>) : FeedAction()
    data class Error(val error: Exception) : FeedAction()
}

sealed class FeedSideEffect : Effect {
    data class Error(val error: Exception) : FeedSideEffect()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FeedStore(
    private val rssReader: RssReader
) : Store<FeedState, FeedAction, FeedSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(FeedState(false, emptyList()))
    private val sideEffect = MutableSharedFlow<FeedSideEffect>()

    override fun observeState(): StateFlow<FeedState> = state

    override fun observeSideEffect(): Flow<FeedSideEffect> = sideEffect

    override fun dispatch(action: FeedAction) {
        Napier.d(tag = "FeedStore", message = "Action: $action")
        sideEffect.tryEmit(FeedSideEffect.Error(Exception("Dispatch action")))

        val oldState = state.value

        val newState = when (action) {
            is FeedAction.Refresh -> {
                if (oldState.progress) {
                    sideEffect.tryEmit(FeedSideEffect.Error(Exception("In progress")))
                    oldState
                } else {
                    launch { loadAllFeeds(action.forceLoad) }
                    oldState.copy(progress = true)
                }
            }
            is FeedAction.Add -> {
                if (oldState.progress) {
                    sideEffect.tryEmit(FeedSideEffect.Error(Exception("In progress")))
                    oldState
                } else {
                    launch { addFeed(action.url) }
                    FeedState(true, oldState.feeds)
                }
            }
            is FeedAction.Delete -> {
                if (oldState.progress) {
                    sideEffect.tryEmit(FeedSideEffect.Error(Exception("In progress")))
                    oldState
                } else {
                    launch { deleteFeed(action.url) }
                    FeedState(true, oldState.feeds)
                }
            }
            is FeedAction.SelectFeed -> {
                if (action.feed == null || oldState.feeds.contains(action.feed)) {
                    oldState.copy(selectedFeed = action.feed)
                } else {
                    sideEffect.tryEmit(FeedSideEffect.Error(Exception("Unknown feed")))
                    oldState
                }
            }
            is FeedAction.Data -> {
                if (oldState.progress) {
                    val selected = oldState.selectedFeed?.let {
                        if (action.feeds.contains(it)) it else null
                    }
                    FeedState(false, action.feeds, selected)
                } else {
                    sideEffect.tryEmit(FeedSideEffect.Error(Exception("Unexpected action")))
                    oldState
                }
            }
            is FeedAction.Error -> {
                if (oldState.progress) {
                    sideEffect.tryEmit(FeedSideEffect.Error(action.error))
                    FeedState(false, oldState.feeds)
                } else {
                    sideEffect.tryEmit(FeedSideEffect.Error(Exception("Unexpected action")))
                    oldState
                }
            }
        }

        if (newState != oldState) {
            Napier.d(tag = "FeedStore", message = "NewState: $newState")
            state.value = newState
        }
    }

    private suspend fun loadAllFeeds(forceLoad: Boolean) {
        try {
            val allFeeds = rssReader.getAllFeeds(forceLoad)
            dispatch(FeedAction.Data(allFeeds))
        } catch (e: Exception) {
            dispatch(FeedAction.Error(e))
        }
    }

    private suspend fun addFeed(url: String) {
        try {
            rssReader.addFeed(url)
            val allFeeds = rssReader.getAllFeeds(false)
            dispatch(FeedAction.Data(allFeeds))
        } catch (e: Exception) {
            dispatch(FeedAction.Error(e))
        }
    }

    private suspend fun deleteFeed(url: String) {
        try {
            rssReader.deleteFeed(url)
            val allFeeds = rssReader.getAllFeeds(false)
            dispatch(FeedAction.Data(allFeeds))
        } catch (e: Exception) {
            dispatch(FeedAction.Error(e))
        }
    }
}

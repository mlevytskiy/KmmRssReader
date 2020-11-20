package com.github.jetbrains.rssreader.androidApp.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.foundation.lazy.LazyRowFor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.github.jetbrains.app.FeedAction
import com.github.jetbrains.app.FeedStore
import com.github.jetbrains.rssreader.androidApp.R
import com.github.jetbrains.rssreader.androidApp.Screens
import com.github.jetbrains.rssreader.androidApp.ui.base.BaseFragment
import com.github.jetbrains.rssreader.androidApp.ui.util.shortName
import com.github.jetbrains.rssreader.entity.Feed
import com.github.jetbrains.rssreader.entity.Post
import com.github.terrakok.cicerone.Router
import dev.chrisbanes.accompanist.coil.CoilImage
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import dev.chrisbanes.accompanist.insets.navigationBarsHeight
import dev.chrisbanes.accompanist.insets.statusBarsHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class MainFeedFragment : BaseFragment(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    private val store: FeedStore by inject()
    private val router: Router by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (createMode != CreateMode.RESTORED_AFTER_ROTATION) {
            store.dispatch(FeedAction.Refresh(false))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent { MainScreen(store, router) }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        router.exit()
    }
}

private val Orange = Color(0xfff8873c)
private val Purple = Color(0xff6b70fc)

private val LightColors = lightColors(
    primary = Orange,
    primaryVariant = Orange,
    onPrimary = Color.White,
    secondary = Purple,
    onSecondary = Color.White
)

private val DarkColors = darkColors(
    primary = Orange,
    primaryVariant = Orange,
    onPrimary = Color.White,
    secondary = Purple,
    onSecondary = Color.White
)

@Composable
private fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

@Composable
private fun MainScreen(
    store: FeedStore,
    router: Router
) {
    AppTheme {
        ProvideWindowInsets {
            val state = store.observeState().collectAsState()
            val posts = remember(state.value.feeds, state.value.selectedFeed) {
                (state.value.selectedFeed?.posts ?: state.value.feeds.flatMap { it.posts })
                    .sortedByDescending { it.date }
            }
            Column {
                PostList(modifier = Modifier.weight(1f),posts = posts) { post ->
                    post.link?.let { router.navigateTo(Screens.WebView(it)) }
                }
                BottomBar(
                    feeds = state.value.feeds,
                    selectedFeed = state.value.selectedFeed,
                    onFeedClick = { feed -> store.dispatch(FeedAction.SelectFeed(feed)) },
                    onEditClick = { router.navigateTo(Screens.FeedList()) }
                )
                Spacer(
                    Modifier
                        .navigationBarsHeight()
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PostList(
    modifier: Modifier,
    posts: List<Post>,
    onClick: (Post) -> Unit
) {
    Surface(modifier) {
        LazyColumnForIndexed(
            items = posts,
            contentPadding = PaddingValues(16.dp)
        ) { i, post ->
            if (i == 0) {
                Spacer(
                    Modifier
                        .statusBarsHeight()
                        .fillMaxWidth()
                )
            }
            PostItem(post) { onClick(post) }
            if (i != posts.size - 1) {
                Spacer(modifier = Modifier.preferredSize(16.dp))
            }
        }
    }
}

private val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

@Composable
private fun PostItem(
    item: Post,
    onClick: () -> Unit
) {
    val padding = 16.dp
    Box {
        Card(
            elevation = 16.dp,
            shape = RoundedCornerShape(padding)
        ) {
            Column(
                modifier = Modifier.clickable(onClick = onClick)
            ) {
                Spacer(modifier = Modifier.preferredSize(padding))
                Text(
                    modifier = Modifier.padding(start = padding, end = padding),
                    style = MaterialTheme.typography.h6,
                    text = item.title
                )
                item.imageUrl?.let { url ->
                    Spacer(modifier = Modifier.preferredSize(padding))
                    CoilImage(
                        modifier = Modifier.height(180.dp),
                        contentScale = ContentScale.Crop,
                        data = url
                    )
                }
                item.description?.let { description ->
                    Spacer(modifier = Modifier.preferredSize(padding))
                    Text(
                        modifier = Modifier.padding(start = padding, end = padding),
                        style = MaterialTheme.typography.body1,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        text = description
                    )
                }
                Spacer(modifier = Modifier.preferredSize(padding))
                Text(
                    modifier = Modifier.padding(start = padding, end = padding),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    text = dateFormatter.format(Date(item.date))
                )
                Spacer(modifier = Modifier.preferredSize(padding))
            }
        }
    }
}

private sealed class Icons {
    object All : Icons()
    class FeedIcon(val feed: Feed) : Icons()
    object Edit : Icons()
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun BottomBar(
    feeds: List<Feed>,
    selectedFeed: Feed?,
    onFeedClick: (Feed?) -> Unit,
    onEditClick: () -> Unit
) {
    val items = buildList<Icons> {
        add(Icons.All)
        addAll(feeds.map { Icons.FeedIcon(it) })
        add(Icons.Edit)
    }
    Surface(Modifier.fillMaxWidth()) {
        LazyRowFor(
            items = items,
            contentPadding = PaddingValues(16.dp)
        ) {
            when (val item = it) {
                is Icons.All -> FeedIcon(
                    feed = null,
                    isSelected = selectedFeed == null,
                    onClick = { onFeedClick(null) }
                )
                is Icons.FeedIcon -> FeedIcon(
                    feed = item.feed,
                    isSelected = selectedFeed == item.feed,
                    onClick = { onFeedClick(item.feed) }
                )
                is Icons.Edit -> EditIcon(onClick = onEditClick)
            }
            Spacer(modifier = Modifier.preferredSize(16.dp))
        }
    }
}

@Composable
private fun EditIcon(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.preferredSize(48.dp)
            .clip(CircleShape)
            .background(color = MaterialTheme.colors.secondary)
            .clickable(onClick = onClick)
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            asset = vectorResource(R.drawable.ic_edit),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
        )
    }
}

@Composable
private fun FeedIcon(
    feed: Feed?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val txtAll = stringResource(R.string.all)
    val shortName = remember(feed) { feed?.shortName() ?: txtAll }
    Box(
        modifier = Modifier.preferredSize(48.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) MaterialTheme.colors.secondary else Color.Transparent)
    ) {
        Box(
            modifier = Modifier.preferredSize(40.dp)
                .clip(CircleShape)
                .align(Alignment.Center)
                .background(color = MaterialTheme.colors.primary)
                .clickable(onClick = onClick)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colors.onPrimary,
                text = shortName
            )
            feed?.imageUrl?.let { url ->
                CoilImage(url)
            }
        }
    }
}

@Preview
@Composable
private fun PostPreview() {
    AppTheme {
        PostItem(item = post, onClick = {})
    }
}

@Preview
@Composable
private fun FeedIconPreview() {
    AppTheme {
        FeedIcon(feed = feed, false) {}
    }
}

@Preview
@Composable
private fun FeedIconSelectedPreview() {
    AppTheme {
        FeedIcon(feed = feed, true) {}
    }
}

private val post = Post(
    title = "Productive Server-Side Development With Kotlin: Stories From The Industry",
    description = "Kotlin was created as an alternative to Java, meaning that its application area within the JVM ecosystem was meant to be the same as Java’s. Obviously, this includes server-side development. We would love...",
    imageUrl = "https://blog.jetbrains.com/wp-content/uploads/2020/11/server.png",
    link = "https://blog.jetbrains.com/kotlin/2020/11/productive-server-side-development-with-kotlin-stories/",
    date = 42L
)

private val feed = Feed(
    title = "Kotlin Blog",
    link = "blog.jetbrains.com/kotlin/",
    description = "blog.jetbrains.com/kotlin/",
    imageUrl = null,
    posts = listOf(post),
    sourceUrl = "https://blog.jetbrains.com/feed/"
)
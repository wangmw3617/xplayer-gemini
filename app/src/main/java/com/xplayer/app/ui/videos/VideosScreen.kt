package com.xplayer.app.ui.videos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xplayer.app.R
import com.xplayer.app.data.local.entity.HdrType
import com.xplayer.app.data.local.entity.VideoEntity
import com.xplayer.app.ui.home.formatDuration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    onNavigateToPlayer: (String) -> Unit,
    viewModel: VideosViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val videos by viewModel.videos.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedVideoForDetails by remember { mutableStateOf<VideoEntity?>(null) }
    var videoToDelete by remember { mutableStateOf<VideoEntity?>(null) }
    var videoToRename by remember { mutableStateOf<VideoEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_videos),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.scanVideos() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Scan")
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.videos_sort_date)) },
                                onClick = {
                                    viewModel.setSortOrder(VideoSortOrder.DATE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.videos_sort_name)) },
                                onClick = {
                                    viewModel.setSortOrder(VideoSortOrder.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.videos_sort_size)) },
                                onClick = {
                                    viewModel.setSortOrder(VideoSortOrder.SIZE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.videos_sort_duration)) },
                                onClick = {
                                    viewModel.setSortOrder(VideoSortOrder.DURATION_DESC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scanning Indicator
            AnimatedVisibility(visible = isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.videos_search_hint), fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Video Content
            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.videos_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.scanVideos() }) {
                            Text(stringResource(R.string.videos_scan_now))
                        }
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.path }) { video ->
                        VideoGridItem(
                            video = video,
                            onClick = { onNavigateToPlayer(video.path) },
                            onMoreClick = { selectedVideoForDetails = video }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.path }) { video ->
                        VideoListItem(
                            video = video,
                            onClick = { onNavigateToPlayer(video.path) },
                            onDetailsClick = { selectedVideoForDetails = video },
                            onRenameClick = {
                                videoToRename = video
                                renameInputText = video.title
                            },
                            onDeleteClick = { videoToDelete = video }
                        )
                    }
                }
            }
        }
    }

    // Details Dialog
    selectedVideoForDetails?.let { video ->
        AlertDialog(
            onDismissRequest = { selectedVideoForDetails = null },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text(stringResource(R.string.video_details)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "${stringResource(R.string.video_path)}: ${video.path}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${stringResource(R.string.video_resolution)}: ${video.width} x ${video.height}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${stringResource(R.string.video_duration)}: ${formatDuration(video.durationMs)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${stringResource(R.string.video_size)}: ${formatFileSize(video.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${stringResource(R.string.video_codec)}: ${video.videoCodec.ifEmpty { "Unknown" }}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${stringResource(R.string.video_hdr)}: ${video.hdrType.name}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedVideoForDetails = null }) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }

    // Rename Dialog
    videoToRename?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text(stringResource(R.string.video_action_rename)) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameVideo(video, renameInputText) {
                            videoToRename = null
                        }
                    },
                    enabled = renameInputText.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text(stringResource(R.string.video_action_delete)) },
            text = {
                Text(stringResource(R.string.video_delete_confirm, video.title))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video) {
                            videoToDelete = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun VideoListItem(
    video: VideoEntity,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFileSize(video.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Micro Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (video.width >= 3840) {
                        VideoBadge(text = "4K")
                    }
                    when (video.hdrType) {
                        HdrType.DOLBY_VISION -> VideoBadge(text = "DV", isHighlight = true)
                        HdrType.HDR10_PLUS -> VideoBadge(text = "HDR10+")
                        HdrType.HDR10 -> VideoBadge(text = "HDR")
                        HdrType.NONE -> {}
                    }
                    if (video.videoCodec.contains("hevc", ignoreCase = true) || video.videoCodec.contains("h265", ignoreCase = true)) {
                        VideoBadge(text = "HEVC")
                    }
                    if (video.hasAssSubtitle) {
                        VideoBadge(text = "ASS")
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.video_details)) },
                        onClick = {
                            showMenu = false
                            onDetailsClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.video_action_rename)) },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.video_action_delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoGridItem(
    video: VideoEntity,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatDuration(video.durationMs)} • ${formatFileSize(video.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun VideoBadge(text: String, isHighlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            fontSize = 9.sp
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LazyListScrollNavigationControls(
    listState: LazyListState,
    topContentDescription: String,
    bottomContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val canScrollDown by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollUp by remember { derivedStateOf { listState.canScrollBackward } }

    if (!canScrollDown && !canScrollUp) {
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canScrollUp) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.semantics { contentDescription = topContentDescription },
            ) {
                Text("Top")
            }
        }
        if (canScrollDown) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val totalItems = listState.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            listState.animateScrollToItem(totalItems - 1)
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.semantics { contentDescription = bottomContentDescription },
            ) {
                Text("Bottom")
            }
        }
    }
}

@Composable
fun ScrollNavigationControls(
    scrollState: ScrollState,
    topContentDescription: String,
    bottomContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val canScrollDown by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }
    val canScrollUp by remember { derivedStateOf { scrollState.value > 0 } }

    if (!canScrollDown && !canScrollUp) {
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canScrollUp) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.semantics { contentDescription = topContentDescription },
            ) {
                Text("Top")
            }
        }
        if (canScrollDown) {
            Button(
                onClick = {
                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.semantics { contentDescription = bottomContentDescription },
            ) {
                Text("Bottom")
            }
        }
    }
}

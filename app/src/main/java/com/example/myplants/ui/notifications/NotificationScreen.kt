package com.example.myplants.ui.notifications

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myplants.R
import com.example.myplants.data.NotificationEntity
import com.example.myplants.navigation.Route
import com.example.myplants.ui.plantList.FilterRow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val grouped by viewModel.groupedItems.collectAsState()

    val listState = rememberLazyListState()
    val notifications by viewModel.items.collectAsState()

    LaunchedEffect(notifications, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Int } }
            .filter { it.isNotEmpty() } // prevent empty emission
            .distinctUntilChanged()
            .collect { visibleIds ->
                viewModel.markNotificationsAsRead(visibleIds)
            }
    }
    LaunchedEffect(Unit) {
        viewModel.getNotifications()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.bg_plants),
            contentScale = ContentScale.FillWidth,
            contentDescription = "Background plants"
        )

        if (viewModel.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .padding(top = 65.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterStart)
                                .background(Color.White, CircleShape)
                                .clickable {
                                    navController.popBackStack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Go Back",
                                modifier = Modifier.size(30.dp),
                                tint = Color.Black
                            )
                        }

                        Text(
                            text = "Notifications",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            lineHeight = 32.sp
                        )
                    }

                }
                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    grouped.forEach { (sectionTitle, list) ->
                        item {
                            Text(
                                text = sectionTitle,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 24.dp, bottom = 8.dp)
                            )
                        }

                        items(list, key = { it.id }) { notification ->
                            NotificationItem(
                                notification = notification,
                                onGoToPlantClick = { plantId ->
                                    navController.navigate(Route.plantDetailsRoute(plantId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@SuppressLint("RememberReturnType")
@Composable
fun NotificationItem(
    notification: NotificationEntity,
    onGoToPlantClick: (plantId: Int) -> Unit
) {
    val time = remember(notification.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(Date(notification.timestamp))
    }



    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp) // Slightly larger than the image
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ), // Background color and shape
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.plant),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = "Daily plant notification",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.Black
                )


                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }

        Text(
            text = notification.message,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Go to the plant",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { onGoToPlantClick(notification.plantId) }
        )
    }
}
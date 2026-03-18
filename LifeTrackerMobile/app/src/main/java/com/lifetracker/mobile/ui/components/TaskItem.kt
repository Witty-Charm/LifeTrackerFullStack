package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.CloudOff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.theme.CardBackground
import com.lifetracker.mobile.ui.theme.CardBorder
import com.lifetracker.mobile.ui.theme.GoldYellow
import com.lifetracker.mobile.ui.theme.HealthRed
import com.lifetracker.mobile.ui.theme.TextPrimary
import com.lifetracker.mobile.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskUi,
    onCompleteClick: () -> Unit,
    onFailClick: () -> Unit,
    isActionLoading: Boolean,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit,
) {
    val isCompleted = task.isCompleted
    val canAct = !isCompleted && !isActionLoading && !task.isPendingSync
    val cardAlpha = if (isCompleted) 0.5f else 1f

    if (task.isPendingSync) {
        TaskCardContent(
            task = task,
            canAct = true,
            cardAlpha = 0.6f,
            onCompleteClick = onCompleteClick,
            onFailClick = onFailClick,
            modifier = modifier,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && canAct) {
                onDeleteClick()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { DismissBackground() },
        gesturesEnabled = canAct,
        modifier = modifier
    ) {
        TaskCardContent(
            task = task,
            canAct = canAct,
            cardAlpha = cardAlpha,
            onCompleteClick = onCompleteClick,
            onFailClick = onFailClick,
        )
    }
}

@Composable
private fun DismissBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(HealthRed)
            .padding(end = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun TaskCardContent(
    task: TaskUi,
    canAct: Boolean,
    cardAlpha: Float,
    onCompleteClick: () -> Unit,
    onFailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .graphicsLayer { alpha = cardAlpha }
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(
                    color = GoldYellow,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        bottomStart = 12.dp,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .clickable(enabled = canAct, onClick = onCompleteClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.difficultyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color(task.difficultyColor)
            )

            if (task.streakText != null) {
                Text(
                    text = "🔥 ${task.streakText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (task.dueDateText != null) {
                Text(
                    text = task.dueDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isOverdue) HealthRed else TextSecondary
                )
            }

            if (task.isPendingSync) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pending sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .background(
                    color = CardBorder,
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
                .clickable(enabled = canAct, onClick = onFailClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(
                        width = 1.5.dp,
                        color = TextSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

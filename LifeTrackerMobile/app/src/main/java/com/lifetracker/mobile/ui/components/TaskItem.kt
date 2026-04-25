package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.model.TaskPendingAction
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiTaskType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskUi,
    onCompleteClick: () -> Unit,
    onFailClick: () -> Unit,
    isActionLoading: Boolean,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit,
    onRetrySyncClick: () -> Unit,
    onDeleteFailedTaskClick: () -> Unit,
) {
    val isCompleted = task.isCompleted
    val hasPendingAction = task.pendingAction != null
    val isLocalOnly = task.id < 0 || task.isPendingSync || task.syncError != null
    val completeFailEnabled = !isCompleted && !hasPendingAction && !isLocalOnly && !isActionLoading
    val deleteEnabled = !hasPendingAction && !isActionLoading
    val cardAlpha = if (isCompleted) 0.5f else 1f

    if (task.isPendingSync) {
        TaskCardContent(
            task = task,
            completeFailEnabled = false,
            cardAlpha = 0.6f,
            onCompleteClick = onCompleteClick,
            onFailClick = onFailClick,
            onRetrySyncClick = onRetrySyncClick,
            onDeleteFailedTaskClick = onDeleteFailedTaskClick,
            modifier = modifier,
        )
        return
    }

    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart && deleteEnabled) {
                    onDeleteClick()
                }
                false
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { DismissBackground() },
        gesturesEnabled = deleteEnabled,
        modifier = modifier,
    ) {
        TaskCardContent(
            task = task,
            completeFailEnabled = completeFailEnabled,
            cardAlpha = cardAlpha,
            onCompleteClick = onCompleteClick,
            onFailClick = onFailClick,
            onRetrySyncClick = onRetrySyncClick,
            onDeleteFailedTaskClick = onDeleteFailedTaskClick,
        )
    }
}

@Composable
private fun DismissBackground() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.error)
                .padding(end = 16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun TaskCardContent(
    task: TaskUi,
    completeFailEnabled: Boolean,
    cardAlpha: Float,
    onCompleteClick: () -> Unit,
    onFailClick: () -> Unit,
    onRetrySyncClick: () -> Unit,
    onDeleteFailedTaskClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .graphicsLayer { alpha = cardAlpha },
    ) {
        val positiveHighlighted = positiveActionHighlighted(task, completeFailEnabled)
        val negativeHighlighted = negativeActionHighlighted(task, completeFailEnabled)
        val positiveEnabled = positiveActionClickable(task, completeFailEnabled)
        val negativeEnabled = negativeActionClickable(task, completeFailEnabled)
        val positiveContainerColor = if (positiveHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
        val positiveBadgeColor =
            if (positiveHighlighted) {
                MaterialTheme.colorScheme.onSecondary.copy(
                    alpha = 0.2f,
                )
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
            }
        val positiveTextColor = if (positiveHighlighted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        val negativeContainerColor = if (negativeHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
        val negativeBadgeColor =
            if (negativeHighlighted) {
                MaterialTheme.colorScheme.onSecondary.copy(
                    alpha = 0.2f,
                )
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
            }
        val negativeTextColor = if (negativeHighlighted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant

        if (task.showsPositiveAction) {
            Box(
                modifier =
                    Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(
                            color = positiveContainerColor,
                            shape =
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    bottomStart = 12.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp,
                                ),
                        ).clickable(enabled = positiveEnabled, onClick = onCompleteClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                color = positiveBadgeColor,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (task.type == UiTaskType.Daily) {
                        Box(
                            modifier =
                                Modifier
                                    .size(18.dp)
                                    .background(
                                        color = if (task.isCompleted) positiveTextColor else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .border(width = 2.dp, color = positiveTextColor, shape = CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (task.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = positiveContainerColor,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleMedium,
                            color = positiveTextColor,
                        )
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.difficultyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color(task.difficultyColor),
            )

            if (task.streakText != null) {
                Text(
                    text = "🔥 ${task.streakText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (task.isShieldActive) {
                Text(
                    text = "🛡️ Shield active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (task.dueDateText != null) {
                Text(
                    text = task.dueDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            task.pendingAction?.let { pendingAction ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (pendingAction) {
                        TaskPendingAction.Complete -> "Completing…"
                        TaskPendingAction.Fail -> "Failing…"
                        TaskPendingAction.Delete -> "Deleting…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (task.actionError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.actionError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (task.syncError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.syncError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Button(
                        onClick = onRetrySyncClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDeleteFailedTaskClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }

            if (task.isPendingSync) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pending sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (task.showsNegativeAction) {
            Box(
                modifier =
                    Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(
                            color = negativeContainerColor,
                            shape =
                                RoundedCornerShape(
                                    topStart = 0.dp,
                                    bottomStart = 0.dp,
                                    topEnd = 12.dp,
                                    bottomEnd = 12.dp,
                                ),
                        ).clickable(enabled = negativeEnabled, onClick = onFailClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                color = negativeBadgeColor,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleMedium,
                        color = negativeTextColor,
                    )
                }
            }
        }
    }
}

internal fun positiveActionHighlighted(
    task: TaskUi,
    canAct: Boolean,
): Boolean = task.positiveActionEnabled

internal fun negativeActionHighlighted(
    task: TaskUi,
    canAct: Boolean,
): Boolean = task.negativeActionEnabled

internal fun positiveActionClickable(
    task: TaskUi,
    canAct: Boolean,
): Boolean = canAct && task.positiveActionEnabled

internal fun negativeActionClickable(
    task: TaskUi,
    canAct: Boolean,
): Boolean = canAct && task.negativeActionEnabled

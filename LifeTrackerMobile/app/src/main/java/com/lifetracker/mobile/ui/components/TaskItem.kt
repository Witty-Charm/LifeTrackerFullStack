package com.lifetracker.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.model.ChecklistItemUi
import com.lifetracker.mobile.ui.model.TaskPendingAction
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiTaskType
import kotlinx.collections.immutable.ImmutableList

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
    onEditClick: () -> Unit = {},
    onChecklistItemToggle: (itemId: String) -> Unit = {},
) {
    val hasPendingAction = task.pendingAction != null
    val isLocalOnly = task.id < 0 || task.isPendingSync || task.syncError != null
    val completeFailEnabled = !hasPendingAction && !isLocalOnly && !isActionLoading
    val deleteEnabled = !hasPendingAction && !isActionLoading
    val editEnabled = !hasPendingAction && !isLocalOnly && !isActionLoading
    val cardAlpha = taskCardAlpha(task)

    if (task.isPendingSync) {
        TaskCardContent(
            task = task,
            completeFailEnabled = false,
            cardAlpha = 0.6f,
            onCompleteClick = onCompleteClick,
            onFailClick = onFailClick,
            onRetrySyncClick = onRetrySyncClick,
            onDeleteFailedTaskClick = onDeleteFailedTaskClick,
            onEditClick = onEditClick,
            editEnabled = false,
            onChecklistItemToggle = onChecklistItemToggle,
            checklistToggleEnabled = false,
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
            onEditClick = onEditClick,
            editEnabled = editEnabled,
            onChecklistItemToggle = onChecklistItemToggle,
            checklistToggleEnabled = editEnabled,
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
    onEditClick: () -> Unit,
    editEnabled: Boolean,
    onChecklistItemToggle: (itemId: String) -> Unit,
    checklistToggleEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var checklistExpanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val hasChecklist = task.checklistItems.isNotEmpty()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .graphicsLayer { alpha = cardAlpha },
    ) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .height(IntrinsicSize.Min),
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
                        val isCheckedToday = task.isCheckedToday
                        val dailyCircleColor =
                            if (isCheckedToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        val animatedDailyCheckScale by animateFloatAsState(
                            targetValue = dailyCheckmarkScale(isCheckedToday = isCheckedToday),
                            animationSpec = tween(durationMillis = 150),
                            label = "dailyCheckScale",
                        )
                        val animatedDailyCheckAlpha by animateFloatAsState(
                            targetValue = dailyCheckmarkAlpha(isCheckedToday = isCheckedToday),
                            animationSpec = tween(durationMillis = 110),
                            label = "dailyCheckAlpha",
                        )

                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .background(
                                        color = dailyCircleColor,
                                        shape = CircleShape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = dailyCheckmarkTint(MaterialTheme.colorScheme.onPrimary),
                                modifier =
                                    Modifier
                                        .size(13.dp)
                                        .graphicsLayer {
                                            alpha = animatedDailyCheckAlpha
                                            scaleX = animatedDailyCheckScale
                                            scaleY = animatedDailyCheckScale
                                        },
                            )
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
                    .clickable(enabled = editEnabled, onClick = onEditClick)
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

            if (task.nextScheduledHint != null) {
                Text(
                    text = task.nextScheduledHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            task.pendingAction?.let { pendingAction ->
                val suppressLabel = task.type == UiTaskType.Daily && pendingAction == TaskPendingAction.Complete
                if (!suppressLabel) {
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
            val negativeRoundRight = !hasChecklist
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
                                    topEnd = if (negativeRoundRight) 12.dp else 0.dp,
                                    bottomEnd = if (negativeRoundRight) 12.dp else 0.dp,
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

            if (hasChecklist) {
                ChecklistProgressColumn(
                    items = task.checklistItems,
                    expanded = checklistExpanded,
                    onClick = { checklistExpanded = !checklistExpanded },
                )
            }
        }

        AnimatedVisibility(visible = hasChecklist && checklistExpanded) {
            ChecklistExpandedList(
                items = task.checklistItems,
                onToggle = onChecklistItemToggle,
                enabled = checklistToggleEnabled,
            )
        }
    }
}

@Composable
private fun ChecklistProgressColumn(
    items: ImmutableList<ChecklistItemUi>,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val completed = items.count { it.isCompleted }
    val total = items.size
    Box(
        modifier =
            Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape =
                        RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp,
                        ),
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "$completed / $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse checklist" else "Expand checklist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ChecklistExpandedList(
    items: ImmutableList<ChecklistItemUi>,
    onToggle: (String) -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = enabled) { onToggle(item.id) }
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val checkBg =
                    if (item.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(checkBg)
                            .border(
                                width = 1.5.dp,
                                color =
                                    if (item.isCompleted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                shape = RoundedCornerShape(4.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (item.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    textDecoration =
                        if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal fun taskCardAlpha(task: TaskUi): Float =
    when {
        task.type == UiTaskType.Daily && task.isCheckedToday -> 0.72f
        task.isCompleted -> 0.5f
        else -> 1f
    }

internal fun dailyCheckmarkAlpha(isCheckedToday: Boolean): Float = if (isCheckedToday) 1f else 0f

internal fun dailyCheckmarkScale(isCheckedToday: Boolean): Float = if (isCheckedToday) 1f else 0.82f

internal fun dailyCheckmarkTint(onCompletedColor: Color): Color = onCompletedColor

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

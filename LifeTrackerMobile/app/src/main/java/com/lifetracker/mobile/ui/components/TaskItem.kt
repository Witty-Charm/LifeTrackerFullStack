package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.model.TaskUi

@Composable
fun TaskItem(
    task: TaskUi,
    onCompleteClick: () -> Unit,
    onFailClick: () -> Unit,
    isActionLoading: Boolean,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = Color(task.difficultyColor),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = task.difficulty.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.rewardText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = task.penaltyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (task.streakText != null) {
                Text(
                    text = task.streakText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.dueDateText != null) {
                Text(
                    text = task.dueDateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.isOverdue) Color.Red else LocalContentColor.current
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val canAct = !task.isCompleted && !isActionLoading

                OutlinedButton(
                    onClick = onFailClick,
                    enabled = canAct
                ) {
                    Text("Fail")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onCompleteClick,
                    enabled = canAct
                ) {
                    Text("Complete")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDeleteClick,
                    enabled = canAct
                ) {
                    Text("Delete")
                }
            }
        }
    }
}


package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.theme.CardBorder
import com.lifetracker.mobile.ui.theme.ObjectiveCardBg
import com.lifetracker.mobile.ui.theme.GoldYellow
import com.lifetracker.mobile.ui.theme.OnGoldText
import com.lifetracker.mobile.ui.theme.PurpleBorder
import com.lifetracker.mobile.ui.theme.PurpleAccent
import com.lifetracker.mobile.ui.theme.TextPrimary

@Composable
fun DailyObjectiveCard(
    hero: HeroUi,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = PurpleBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .background(ObjectiveCardBg)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = GoldYellow,
                    contentColor = OnGoldText,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = hero.dailyText,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnGoldText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { hero.dailyProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = PurpleAccent,
                trackColor = CardBorder
            )
        }
    }
}


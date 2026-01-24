package com.example.myplants.presentation.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplants.R

@Composable
fun HeaderCard(
    totalPlants: Int,
    healthScore: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Total Plants
            Text(
                text = stringResource(R.string.analytics_your_plants),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$totalPlants",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Health Score
            Text(
                text = stringResource(R.string.analytics_health_score),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${healthScore.toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = getHealthScoreLabel(healthScore),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { healthScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = getHealthScoreColor(healthScore),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun getHealthScoreLabel(score: Float): String {
    return when {
        score >= 90f -> stringResource(R.string.analytics_health_excellent)
        score >= 75f -> stringResource(R.string.analytics_health_great)
        score >= 60f -> stringResource(R.string.analytics_health_good)
        score >= 40f -> stringResource(R.string.analytics_health_fair)
        else -> stringResource(R.string.analytics_health_needs_work)
    }
}

@Composable
private fun getHealthScoreColor(score: Float) = when {
    score >= 75f -> MaterialTheme.colorScheme.primary
    score >= 50f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

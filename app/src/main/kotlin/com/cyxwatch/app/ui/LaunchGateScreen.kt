package com.cyxwatch.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.R

@Composable
fun LaunchGateScreen(
    onStartMonitoringClick: () -> Unit,
    onOpenPrivacyControlsClick: () -> Unit,
    runtimeIntegrityNotice: String? = null,
) {
    val background = Color(0xFF0B1120)
    val brandBlue = Color(0xFF2563EB)
    val brandBlueLight = Color(0xFF3B82F6)
    val logoForeground = Color(0xFF1A2A41)
    val accentCyan = Color(0xFF38BDF8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val logoSize = 136.dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.24f))
                if (!runtimeIntegrityNotice.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFef4444)),
                    ) {
                        Text(
                            runtimeIntegrityNotice,
                            color = Color(0xFFffffff),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(logoSize),
                    shape = CircleShape,
                    color = logoForeground,
                    border = BorderStroke(3.dp, Brush.sweepGradient(listOf(brandBlue, brandBlueLight, brandBlue))),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_cyxwatch_logo),
                            contentDescription = "CyxWatch logo",
                            modifier = Modifier.size(86.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    repeat(5) {
                        BoxDot(color = accentCyan, size = 8.dp)
                    }
                }

                Text(
                    "CyxWatch",
                    color = Color(0xFFF0F6FF),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                        color = Color(0xFFF0F6FF),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 26.dp),
                )
                Text(
                    "Watch what runs behind the screen",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = accentCyan,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.fillMaxHeight(0.18f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Button(
                    onClick = onStartMonitoringClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .semantics { contentDescription = "Start monitoring from launch gate" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandBlue,
                    ),
                    shape = CircleShape,
                ) {
                    Text(
                        "Start Monitoring",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(
                    onClick = onOpenPrivacyControlsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .semantics { contentDescription = "Open privacy controls from launch gate" },
                    border = BorderStroke(1.5.dp, brandBlue),
                    shape = CircleShape,
                ) {
                    Text(
                        "View Privacy Controls",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = brandBlueLight,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxDot(color: Color, size: androidx.compose.ui.unit.Dp) {
    BoxDotShape(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
    )
}

@Composable
private fun BoxDotShape(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

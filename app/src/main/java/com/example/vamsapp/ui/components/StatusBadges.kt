package com.example.vamsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.ui.theme.*

@Composable
fun SeverityBadge(severity: String) {
    val (backgroundColor, textColor) = when (severity.uppercase()) {
        "CRITICAL" -> Pair(SeverityCritical.copy(alpha = 0.15f), SeverityCritical)
        "EMERGENCY" -> Pair(SeverityCritical.copy(alpha = 0.15f), SeverityCritical)
        "HIGH" -> Pair(SeverityHigh.copy(alpha = 0.15f), SeverityHigh)
        "MEDIUM" -> Pair(SeverityMedium.copy(alpha = 0.15f), SeverityMedium)
        "LOW" -> Pair(SeverityLow.copy(alpha = 0.15f), SeverityLow)
        else -> Pair(Color.Gray.copy(alpha = 0.15f), Color.LightGray)
    }

    Text(
        text = severity.uppercase(),
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "OPEN" -> Pair(Warning.copy(alpha = 0.15f), Warning)
        "IN_PROGRESS" -> Pair(PrimaryBlue.copy(alpha = 0.15f), PrimaryBlue)
        "RESOLVED" -> Pair(Success.copy(alpha = 0.15f), Success)
        "REOPENED" -> Pair(SeverityMedium.copy(alpha = 0.15f), SeverityMedium)
        else -> Pair(Color.Gray.copy(alpha = 0.15f), Color.LightGray)
    }

    Text(
        text = status.replace("_", " ").uppercase(),
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

package com.expiryguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expiryguard.app.ui.theme.*
import com.expiryguard.app.util.DateUtils

@Composable
fun ExpiryBadge(
    expiryDateMillis: Long,
    modifier: Modifier = Modifier
) {
    val status = DateUtils.getExpiryStatus(expiryDateMillis)
    val text = DateUtils.formatDaysRemaining(expiryDateMillis)

    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    val backgroundColor = when (status) {
        DateUtils.ExpiryStatus.EXPIRED -> if (isDark) ExpiredContainerDark else ExpiredContainerLight
        DateUtils.ExpiryStatus.CRITICAL -> if (isDark) ExpiredContainerDark else ExpiredContainerLight
        DateUtils.ExpiryStatus.WARNING -> if (isDark) WarningContainerDark else WarningContainerLight
        DateUtils.ExpiryStatus.SAFE -> if (isDark) SafeContainerDark else SafeContainerLight
    }

    val textColor = when (status) {
        DateUtils.ExpiryStatus.EXPIRED -> if (isDark) ExpiredRedDark else ExpiredRedLight
        DateUtils.ExpiryStatus.CRITICAL -> if (isDark) ExpiredRedDark else ExpiredRedLight
        DateUtils.ExpiryStatus.WARNING -> if (isDark) WarningOrangeDark else WarningOrangeLight
        DateUtils.ExpiryStatus.SAFE -> if (isDark) SafeGreenDark else SafeGreenLight
    }

    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

package com.hackathon.voicenavigator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.hackathon.voicenavigator.ui.theme.*

enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String
) {
    API("API", Icons.Outlined.Api, Icons.Filled.Api, "market_research"),
    ESG("ESG", Icons.Outlined.Eco, Icons.Filled.Eco, "esg_dashboard"),
    DMV("DMV", Icons.Outlined.DirectionsCar, Icons.Filled.DirectionsCar, "dmv")
}

@Composable
fun AppBottomNavBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = SurfaceLight,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedItem == item) item.selectedIcon else item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    indicatorColor = PrimaryLightBlue.copy(alpha = 0.2f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

/**
 * Top app bar with logo area and Trade/Country Flag placeholders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    showLogo: Boolean = true,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showLogo) {
                    TextButton(onClick = onLogoClick) {
                        Text("Logo", color = PrimaryBlue)
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
                TextButton(onClick = { }) {
                    Text("🇺🇸", style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceLight
        ),
        modifier = modifier
    )
}

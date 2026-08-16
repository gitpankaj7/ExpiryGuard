package com.expiryguard.app.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expiryguard.app.util.DateUtils
import com.expiryguard.app.util.LossRecoveryReport
import com.expiryguard.app.util.RecoveryAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LossRecoveryScreen(modifier: Modifier = Modifier) {
    val viewModel: LossRecoveryViewModel = viewModel(factory = LossRecoveryViewModel.Factory)
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("AI Loss Recovery", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.report != null) {
            val report = state.report!!
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummarySection(report)
                }

                if (report.highPriorityItems.isNotEmpty()) {
                    item {
                        Text(
                            "🔥 High Priority Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(report.highPriorityItems) { action ->
                        ActionCard(action)
                    }
                }

                if (report.allActions.filter { it.riskLevel == "Medium" }.isNotEmpty()) {
                    item {
                        Text(
                            "👀 Monitor (Medium Risk)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(report.allActions.filter { it.riskLevel == "Medium" }) { action ->
                        ActionCard(action)
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun SummarySection(report: LossRecoveryReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overall Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SummaryItem("At Risk", report.totalAtRisk.toString(), Icons.Rounded.Warning)
                SummaryItem("Critical", report.criticalCount.toString(), Icons.Rounded.PriorityHigh)
                SummaryItem("Expired", report.expiredCount.toString(), Icons.Rounded.Error)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ActionCard(action: RecoveryAction) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val riskColor = when(action.riskLevel) {
                    "Critical" -> Color(0xFFD32F2F)
                    "High" -> Color(0xFFF57C00)
                    "Medium" -> Color(0xFFFBC02D)
                    else -> Color(0xFF388E3C)
                }
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(riskColor))
                Spacer(Modifier.width(8.dp))
                Text(
                    action.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (action.daysLeft < 0) "Expired" else "${action.daysLeft}d left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Best Action: ${action.bestAction}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(Modifier.padding(bottom = 8.dp))
                    Text("Kyu? (Why):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(action.whyThisAction, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Aaj kya karein? (Today):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(action.doToday, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Next Step:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(action.doNext, style = MaterialTheme.typography.bodySmall)
                    
                    if (action.notifySupplier) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Call Supplier / Distributor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

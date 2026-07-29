package com.kachat.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kachat.app.models.PortfolioEntity
import com.kachat.app.services.PortfolioManager
import com.kachat.app.ui.theme.KaspaTeal
import com.kachat.app.ui.theme.LocalAppColors
import com.kachat.app.util.formatFiatAmount
import com.kachat.app.viewmodels.PortfolioCardData
import java.util.Locale

/**
 * Robinhood-style portfolio switcher: a horizontally-scrollable row of always-visible cards
 * (name, total balance, today's % change), one per portfolio. Also owns the add/rename/delete UI
 * for the up-to-5 portfolio list — small enough to not need a separate management screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PortfolioPickerHeader(
    portfolios: List<PortfolioEntity>,
    activePortfolioId: String?,
    cardSummaries: Map<String, PortfolioCardData>,
    currencyCode: String,
    onSelect: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newPortfolioName by remember { mutableStateOf("") }
    var renamingPortfolio by remember { mutableStateOf<PortfolioEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deletingPortfolio by remember { mutableStateOf<PortfolioEntity?>(null) }
    var menuTargetId by remember { mutableStateOf<String?>(null) }

    val canAddMore = portfolios.size < PortfolioManager.MAX_PORTFOLIOS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        portfolios.forEach { portfolio ->
            PortfolioCard(
                portfolio = portfolio,
                isActive = portfolio.id == activePortfolioId,
                cardData = cardSummaries[portfolio.id],
                currencyCode = currencyCode,
                onClick = { onSelect(portfolio.id) },
                onLongClick = { menuTargetId = portfolio.id },
                showMenu = menuTargetId == portfolio.id,
                onDismissMenu = { menuTargetId = null },
                canDelete = portfolios.size > 1,
                onRenameClick = {
                    renameText = portfolio.name
                    renamingPortfolio = portfolio
                    menuTargetId = null
                },
                onDeleteClick = {
                    deletingPortfolio = portfolio
                    menuTargetId = null
                }
            )
        }
        if (canAddMore) {
            AddPortfolioCard(onClick = { newPortfolioName = ""; showAddDialog = true })
        }
    }

    if (showAddDialog) {
        PortfolioNameDialog(
            title = "New Portfolio",
            initialText = "",
            confirmLabel = "Create",
            onConfirm = { onAdd(it); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    renamingPortfolio?.let { portfolio ->
        PortfolioNameDialog(
            title = "Rename Portfolio",
            initialText = renameText,
            confirmLabel = "Save",
            onConfirm = { onRename(portfolio.id, it); renamingPortfolio = null },
            onDismiss = { renamingPortfolio = null }
        )
    }

    deletingPortfolio?.let { portfolio ->
        AlertDialog(
            onDismissRequest = { deletingPortfolio = null },
            containerColor = LocalAppColors.current.surface,
            title = { Text("Delete Portfolio", color = LocalAppColors.current.textPrimary) },
            text = {
                Text(
                    "Delete '${portfolio.name}' and its transactions? This can't be undone.",
                    color = LocalAppColors.current.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(portfolio.id); deletingPortfolio = null }) {
                    Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPortfolio = null }) {
                    Text("Cancel", color = LocalAppColors.current.textSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortfolioCard(
    portfolio: PortfolioEntity,
    isActive: Boolean,
    cardData: PortfolioCardData?,
    currencyCode: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    canDelete: Boolean,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPositive = (cardData?.todayChangeAmount ?: 0.0) >= 0.0
    Box {
        Column(
            modifier = Modifier
                .widthIn(min = 140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LocalAppColors.current.surface)
                .border(
                    width = if (isActive) 1.5.dp else 0.8.dp,
                    color = if (isActive) KaspaTeal else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                portfolio.name,
                color = if (isActive) LocalAppColors.current.textPrimary else LocalAppColors.current.textSecondary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatFiatAmount(cardData?.currentValue ?: 0.0, currencyCode),
                color = LocalAppColors.current.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (cardData?.todayChangePercent != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isPositive) Color(0xFF4CD964) else Color(0xFFFF3B30),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${String.format(Locale.US, "%.2f", kotlin.math.abs(cardData.todayChangePercent))}%",
                        color = if (isPositive) Color(0xFF4CD964) else Color(0xFFFF3B30),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text("—", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = onDismissMenu) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = onRenameClick)
            if (canDelete) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = onDeleteClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddPortfolioCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, LocalAppColors.current.textSecondary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Portfolio", tint = KaspaTeal, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text("Add", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PortfolioNameDialog(
    title: String,
    initialText: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalAppColors.current.surface,
        title = { Text(title, color = LocalAppColors.current.textPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Portfolio Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LocalAppColors.current.textPrimary,
                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                    focusedBorderColor = KaspaTeal,
                    unfocusedBorderColor = LocalAppColors.current.textSecondary,
                    focusedLabelColor = KaspaTeal,
                    unfocusedLabelColor = LocalAppColors.current.textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.trim().isNotEmpty()) {
                Text(confirmLabel, color = KaspaTeal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LocalAppColors.current.textSecondary)
            }
        }
    )
}

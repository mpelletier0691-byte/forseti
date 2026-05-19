package com.forseti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.R
import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity
import com.forseti.deadlines.IcsExporter
import com.forseti.deadlines.Rule6
import com.forseti.deadlines.TimingRule
import com.forseti.deadlines.TimingRules
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors
import com.forseti.util.RequestNotificationsPermissionOnce
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DeadlinesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: DeadlinesViewModel = hiltViewModel()
) {
    RequestNotificationsPermissionOnce()

    val cases by viewModel.cases.collectAsState()
    var selectedCaseId by remember { mutableStateOf<Long?>(null) }
    val activeCase = cases.firstOrNull { it.id == selectedCaseId } ?: cases.firstOrNull()

    LaunchedEffect(cases) { if (selectedCaseId == null && activeCase != null) selectedCaseId = activeCase.id }

    var showCaseDialog by remember { mutableStateOf(false) }
    var showDeadlineDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (cases.isEmpty()) showCaseDialog = true else showDeadlineDialog = true
                },
                containerColor = ForsetiColors.RuneGold,
                contentColor = ForsetiColors.SplashBlack
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (cases.isEmpty()) R.string.deadlines_add_case else R.string.deadlines_add_deadline
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(ForsetiColors.Background)) {
            ForsetiTopBar(
                title = stringResource(R.string.nav_deadlines),
                sidebarExpanded = sidebarExpanded,
                onToggleSidebar = onToggleSidebar,
                actions = {
                    IconButton(onClick = { showCaseDialog = true }) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.deadlines_add_case_cd), tint = ForsetiColors.AshWhite)
                    }
                }
            )
            if (cases.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                CaseSelector(cases = cases, selectedId = activeCase?.id, onSelect = { selectedCaseId = it.id })
                if (activeCase != null) {
                    val pct = (viewModel.completeness(activeCase) * 100).toInt()
                    if (pct < 100) {
                        ProfileNudge(percent = pct)
                    }
                }
                HorizontalDivider(color = ForsetiColors.Stone)
                if (activeCase != null) {
                    DeadlineList(
                        viewModel = viewModel,
                        case = activeCase,
                        onShowFolder = {
                            val path = viewModel.caseFolderPath(activeCase) ?: "Folder unavailable"
                            scope.launch { snackbar.showSnackbar(path) }
                        }
                    )
                }
            }
        }
    }

    if (showCaseDialog) {
        AddCaseDialog(onDismiss = { showCaseDialog = false }, onSave = { c ->
            viewModel.addCase(c)
            showCaseDialog = false
        })
    }
    if (showDeadlineDialog && activeCase != null) {
        AddDeadlineDialog(case = activeCase, onDismiss = { showDeadlineDialog = false }, onSave = { d ->
            viewModel.addDeadline(activeCase, d)
            showDeadlineDialog = false
        })
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                "No cases tracked yet.",
                style = MaterialTheme.typography.headlineMedium,
                color = ForsetiColors.RuneGold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Add a case to start tracking deadlines computed under FRCP 6.",
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshGrey
            )
        }
    }
}

@Composable
private fun CaseSelector(cases: List<CaseEntity>, selectedId: Long?, onSelect: (CaseEntity) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        items(cases, key = { it.id }) { c ->
            val selected = c.id == selectedId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) ForsetiColors.SidebarSelected else ForsetiColors.Surface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(c) }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, color = if (selected) ForsetiColors.RuneGold else ForsetiColors.AshWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = listOfNotNull(
                        c.court.takeIf { it.isNotBlank() },
                        c.caseNumber.takeIf { it.isNotBlank() },
                        c.role.takeIf { it.isNotBlank() }
                    ).joinToString(" \u00B7 ")
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileNudge(percent: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Case profile $percent% complete",
                style = MaterialTheme.typography.titleSmall,
                color = ForsetiColors.MeadAmber
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Add court, case number, role, and filing date in the Case Profile tab so deadline defaults and folder names are accurate.",
                style = MaterialTheme.typography.bodySmall,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { percent / 100f },
                color = ForsetiColors.MeadAmber,
                trackColor = ForsetiColors.Stone,
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
        }
    }
}

@Composable
private fun DeadlineList(
    viewModel: DeadlinesViewModel,
    case: CaseEntity,
    onShowFolder: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deadlines by viewModel.deadlinesFor(case.id).collectAsState(initial = emptyList())
    var detail by remember { mutableStateOf<DeadlineEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${deadlines.count { !it.completed }} active",
                style = MaterialTheme.typography.labelLarge,
                color = ForsetiColors.AshGrey,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onShowFolder) {
                Icon(Icons.Outlined.Folder, null, tint = ForsetiColors.RavenBlue)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.deadlines_folder), color = ForsetiColors.RavenBlue)
            }
            TextButton(onClick = {
                scope.launch {
                    val ics = IcsExporter.export(case, deadlines)
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/calendar"
                        putExtra(android.content.Intent.EXTRA_TEXT, ics)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "${case.title} deadlines")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share calendar"))
                }
            }) {
                Icon(Icons.Outlined.IosShare, null, tint = ForsetiColors.RavenBlue)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.deadlines_export_ics), color = ForsetiColors.RavenBlue)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(deadlines, key = { it.id }) { d ->
                DeadlineRow(
                    deadline = d,
                    onClick = { detail = d },
                    onToggle = { viewModel.toggle(d) },
                    onDelete = { viewModel.delete(d) }
                )
                Spacer(Modifier.height(6.dp))
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    detail?.let { d ->
        DeadlineDetailDialog(
            deadline = d,
            onDismiss = { detail = null },
            onToggleComplete = {
                viewModel.toggle(d)
                detail = null
            },
            onShowFolder = {
                onShowFolder()
                detail = null
            },
            onDelete = {
                viewModel.delete(d)
                detail = null
            }
        )
    }
}

@Composable
private fun DeadlineRow(
    deadline: DeadlineEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = deadline.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ForsetiColors.RuneGold,
                    uncheckedColor = ForsetiColors.Stone,
                    checkmarkColor = ForsetiColors.SplashBlack
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    deadline.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (deadline.completed) ForsetiColors.AshGrey else ForsetiColors.AshWhite
                )
                Row {
                    Text(
                        text = formatDate(deadline.dueAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (deadline.completed) ForsetiColors.AshGrey else ForsetiColors.MeadAmber
                    )
                    Spacer(Modifier.width(8.dp))
                    deadline.ruleCitation?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = ForsetiColors.AshGrey)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete", tint = ForsetiColors.AshGrey)
            }
        }
    }
}

/**
 * Tap on a deadline → full breakdown: rule citation, hint text from the FRCP
 * timing-rule catalog (when the citation matches one of ours), countdown, and
 * shortcut buttons for "Mark complete", "Show case folder", and Delete.
 */
@Composable
private fun DeadlineDetailDialog(
    deadline: DeadlineEntity,
    onDismiss: () -> Unit,
    onToggleComplete: () -> Unit,
    onShowFolder: () -> Unit,
    onDelete: () -> Unit
) {
    val ruleHint = remember(deadline.ruleCitation) {
        TimingRules.all.firstOrNull { it.rule.equals(deadline.ruleCitation, ignoreCase = true) }
    }
    val now = System.currentTimeMillis()
    val msToDue = deadline.dueAt - now
    val daysRemaining = (msToDue / (24L * 60 * 60 * 1000)).toInt()
    val countdown = when {
        deadline.completed -> "Completed"
        msToDue < 0 -> "Overdue by ${-daysRemaining} day${if (-daysRemaining == 1) "" else "s"}"
        daysRemaining == 0 -> "Due today"
        else -> "$daysRemaining day${if (daysRemaining == 1) "" else "s"} remaining"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onToggleComplete) {
                Icon(
                    Icons.Outlined.Check,
                    null,
                    tint = if (deadline.completed) ForsetiColors.MeadAmber else ForsetiColors.RuneGold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (deadline.completed) "Mark active" else "Mark complete",
                    color = if (deadline.completed) ForsetiColors.MeadAmber else ForsetiColors.RuneGold
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onShowFolder) {
                    Icon(Icons.Outlined.Folder, null, tint = ForsetiColors.RavenBlue)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.deadlines_folder), color = ForsetiColors.RavenBlue)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = ForsetiColors.AshGrey)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_delete), color = ForsetiColors.AshGrey)
                }
            }
        },
        title = { Text(deadline.title, color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column {
                Text(
                    "Due ${formatDate(deadline.dueAt)} \u00B7 $countdown",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (deadline.completed) ForsetiColors.AshGrey
                    else if (msToDue < 0) ForsetiColors.MeadAmber
                    else ForsetiColors.RuneGold
                )
                if (!deadline.ruleCitation.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Authority: ${deadline.ruleCitation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForsetiColors.AshWhite
                    )
                }
                if (ruleHint != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Default window: ${ruleHint.days} days. ${ruleHint.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForsetiColors.AshGrey
                    )
                }
                deadline.notifyAt?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Reminder scheduled for ${formatDate(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.RavenBlue
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tap Folder to open the on-device case workspace folder where your filings for this deadline live. Once you've filed, tap Mark complete \u2014 the next deadline (if any) computed from this one will roll forward in your calendar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
            }
        }
    )
}

@Composable
private fun AddCaseDialog(onDismiss: () -> Unit, onSave: (CaseEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var court by remember { mutableStateOf("") }
    var num by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(CaseEntity(
                        title = title.trim(),
                        court = court.trim(),
                        caseNumber = num.trim(),
                        role = role.trim(),
                        complaintFiledAt = null,
                        createdAt = System.currentTimeMillis()
                    ))
                }
            }) { Text(stringResource(R.string.action_save), color = ForsetiColors.RuneGold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ForsetiColors.AshGrey) } },
        title = { Text("Add case", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column {
                LabeledField(label = "Case title", value = title, onChange = { title = it })
                LabeledField(label = "Court", value = court, onChange = { court = it })
                LabeledField(label = "Case number", value = num, onChange = { num = it })
                LabeledField(label = "Your role (e.g. Plaintiff, Defendant)", value = role, onChange = { role = it })
            }
        }
    )
}

@Composable
private fun AddDeadlineDialog(
    case: CaseEntity,
    onDismiss: () -> Unit,
    onSave: (DeadlineEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var citation by remember { mutableStateOf("") }
    var triggerIso by remember { mutableStateOf(LocalDate.fromIso(today())) }
    var days by remember { mutableStateOf("21") }
    var serviceMode by remember { mutableStateOf(Rule6.ServiceMode.Personal) }

    val computed = remember(triggerIso, days, serviceMode) {
        runCatching {
            Rule6.computeDeadline(triggerIso, days.toInt(), serviceMode)
        }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val due = computed ?: return@TextButton
                val dueMs = due.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                onSave(
                    DeadlineEntity(
                        caseId = case.id,
                        title = title.trim().ifBlank { "Deadline" },
                        ruleCitation = citation.trim().ifBlank { null },
                        dueAt = dueMs,
                        notifyAt = dueMs - 24 * 60 * 60 * 1000L,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }) { Text(stringResource(R.string.action_save), color = ForsetiColors.RuneGold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ForsetiColors.AshGrey) } },
        title = { Text("Add deadline", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column {
                LabeledField(label = "Title", value = title, onChange = { title = it })
                LabeledField(label = "Rule citation (optional)", value = citation, onChange = { citation = it })
                Spacer(Modifier.height(8.dp))
                Text("Quick add from FRCP timing rules", color = ForsetiColors.AshGrey, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column {
                        TimingRules.all.take(6).forEach { tr ->
                            QuickAddChip(tr) {
                                title = tr.title
                                citation = tr.rule
                                days = tr.days.toString()
                                serviceMode = tr.defaultServiceMode
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LabeledField(label = "Trigger date (YYYY-MM-DD)", value = triggerIso.toIso(), onChange = { v ->
                    LocalDate.fromIsoOrNull(v)?.let { triggerIso = it }
                })
                LabeledField(label = "Days", value = days, onChange = { days = it.filter { c -> c.isDigit() } })
                Row {
                    Rule6.ServiceMode.values().forEach { m ->
                        AssistChip(
                            onClick = { serviceMode = m },
                            label = { Text(m.label()) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (m == serviceMode) ForsetiColors.SidebarSelected else ForsetiColors.SurfaceVariant,
                                labelColor = if (m == serviceMode) ForsetiColors.RuneGold else ForsetiColors.AshWhite
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Deadline: " + (computed?.toIso() ?: "—"),
                    color = ForsetiColors.RuneGold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    )
}

@Composable
private fun QuickAddChip(rule: TimingRule, onApply: () -> Unit) {
    AssistChip(
        onClick = onApply,
        label = { Text("${rule.title} (${rule.days}d)") },
        leadingIcon = { Icon(Icons.Outlined.Check, null) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = ForsetiColors.SurfaceVariant,
            labelColor = ForsetiColors.AshWhite,
            leadingIconContentColor = ForsetiColors.MeadAmber
        ),
        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ForsetiColors.SurfaceVariant,
            unfocusedContainerColor = ForsetiColors.SurfaceVariant,
            focusedIndicatorColor = ForsetiColors.RuneGold,
            cursorColor = ForsetiColors.RuneGold,
            focusedLabelColor = ForsetiColors.RuneGold
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

private fun Rule6.ServiceMode.label() = when (this) {
    Rule6.ServiceMode.Personal -> "Personal"
    Rule6.ServiceMode.Mail -> "Mail (+3)"
    Rule6.ServiceMode.Electronic -> "Electronic (+3)"
    Rule6.ServiceMode.FilingByCourt -> "Filing"
}

private fun today(): String {
    val d = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return d.toIso()
}

private fun LocalDate.toIso(): String =
    "%04d-%02d-%02d".format(year, monthNumber, dayOfMonth)

private fun LocalDate.Companion.fromIso(iso: String): LocalDate =
    fromIsoOrNull(iso) ?: LocalDate(2026, 1, 1)

private fun LocalDate.Companion.fromIsoOrNull(iso: String): LocalDate? = runCatching {
    val parts = iso.split('-')
    LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}.getOrNull()

private fun formatDate(epochMillis: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d".format(ldt.year, ldt.monthNumber, ldt.dayOfMonth)
}

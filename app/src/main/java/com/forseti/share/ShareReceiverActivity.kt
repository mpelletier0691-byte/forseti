package com.forseti.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.forseti.casefiles.CaseFolderService
import com.forseti.data.entities.CaseEntity
import com.forseti.deadlines.DeadlineRepository
import com.forseti.imports.UploadedRulesService
import com.forseti.ui.theme.ForsetiColors
import com.forseti.ui.theme.ForsetiTheme
import com.forseti.ui.theme.setupForsetiEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Receives PDF (and other) documents shared *into* Forseti from other apps —
 * the system Files app, Drive, scanner apps, an email attachment, etc. The
 * user picks where the file should land:
 *
 *  • Uploaded Rules tab — for court-rule PDFs the app couldn't fetch on its own.
 *  • A specific case workspace — auto-routes via [CaseFolderService.classifyForCase]
 *    using a label hint so it lands in the matching phase folder.
 *
 * Registered in the manifest with intent-filters for ACTION_SEND and ACTION_VIEW
 * on `application/pdf`. We deliberately keep this lightweight Activity outside
 * of MainActivity so the share flow doesn't disturb the running app session.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject lateinit var folders: CaseFolderService
    @Inject lateinit var uploaded: UploadedRulesService
    @Inject lateinit var deadlineRepo: DeadlineRepository

    private val cases = MutableStateFlow<List<CaseEntity>>(emptyList())
    val casesFlow: StateFlow<List<CaseEntity>> = cases.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupForsetiEdgeToEdge()

        val incoming = extractUri(intent)
        if (incoming == null) {
            Toast.makeText(this, "No file attached to share.", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        // Persist read access for the lifetime of this activity (or longer if needed).
        runCatching {
            grantUriPermission(packageName, incoming, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val list = deadlineRepo.allCasesSnapshot()
            withContext(Dispatchers.Main) { cases.value = list }
        }

        setContent {
            ForsetiTheme(forceDark = true) {
                ShareSheet(
                    onUploadAsRule = { title, jurisdiction ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val saved = uploaded.importFromUri(
                                uri = incoming,
                                title = title,
                                jurisdiction = jurisdiction,
                                source = "Shared into Forseti"
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ShareReceiverActivity,
                                    if (saved == null) "Couldn't import the file." else "Saved to Uploaded Rules.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    },
                    onSaveToCase = { case, label ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val target = folders.classifyForCase(case, label)
                                ?: folders.scannerFolder(case)
                            val suggested = label.ifBlank { "shared_${System.currentTimeMillis()}.pdf" }
                                .let { if (it.endsWith(".pdf", true)) it else "$it.pdf" }
                            val result = if (target != null) {
                                folders.importContent(incoming, target, suggested)
                            } else null
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ShareReceiverActivity,
                                    if (result == null) "Couldn't save to that case."
                                    else "Saved to ${target?.parentFile?.name}/${target?.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun extractUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
    }

    @Composable
    private fun ShareSheet(
        onUploadAsRule: (title: String, jurisdiction: String) -> Unit,
        onSaveToCase: (CaseEntity, String) -> Unit,
        onCancel: () -> Unit
    ) {
        val all by casesFlow.collectAsState()
        var step by remember { mutableStateOf(Step.Pick) }
        var ruleTitle by remember { mutableStateOf("") }
        var ruleJurisdiction by remember { mutableStateOf("") }
        var caseLabel by remember { mutableStateOf("") }

        Surface(
            color = ForsetiColors.Background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    "Save shared file to…",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ForsetiColors.RuneGold
                )
                Spacer(Modifier.height(12.dp))

                when (step) {
                    Step.Pick -> {
                        Text(
                            "Where do you want this file to live in Forseti?",
                            color = ForsetiColors.AshGrey,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        DestinationCard(
                            icon = Icons.Outlined.PictureAsPdf,
                            title = "Uploaded Rules",
                            subtitle = "For court rules the app couldn't fetch automatically. Stays in the Imports tab and is never auto-filed into a case.",
                            onClick = { step = Step.RuleMeta }
                        )
                        Spacer(Modifier.height(8.dp))
                        DestinationCard(
                            icon = Icons.Outlined.FolderShared,
                            title = "Case workspace",
                            subtitle = "Pick a case and Forseti routes the file into the matching phase folder by label keywords.",
                            onClick = { step = Step.PickCase }
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForsetiColors.Surface,
                                contentColor = ForsetiColors.AshWhite
                            )
                        ) { Text("Cancel") }
                    }

                    Step.RuleMeta -> {
                        OutlinedTextField(
                            value = ruleTitle,
                            onValueChange = { ruleTitle = it },
                            singleLine = true,
                            label = { Text("Rule title (e.g. MA R. Civ. P. 12)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = goldField()
                        )
                        OutlinedTextField(
                            value = ruleJurisdiction,
                            onValueChange = { ruleJurisdiction = it },
                            singleLine = true,
                            label = { Text("Jurisdiction (state, court, or 'Federal')") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = goldField()
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { step = Step.Pick }) {
                                Text("Back", color = ForsetiColors.AshGrey)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onUploadAsRule(ruleTitle.trim(), ruleJurisdiction.trim()) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForsetiColors.RuneGold,
                                    contentColor = ForsetiColors.SplashBlack
                                )
                            ) { Text("Save to Uploaded Rules") }
                        }
                    }

                    Step.PickCase -> {
                        if (all.isEmpty()) {
                            Text(
                                "No cases yet. Open Forseti, create a case under Case Profile, then re-share this file.",
                                color = ForsetiColors.AshGrey,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onCancel) { Text("Close") }
                        } else {
                            OutlinedTextField(
                                value = caseLabel,
                                onValueChange = { caseLabel = it },
                                singleLine = true,
                                label = { Text("Filename / phase keyword (optional)") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = goldField()
                            )
                            Text(
                                "Tip: include words like \"motion\", \"discovery\", \"answer\", \"order\" so the file lands in the right phase folder.",
                                color = ForsetiColors.AshGrey,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            HorizontalDivider(color = ForsetiColors.Stone)
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(all, key = { it.id }) { case ->
                                    CaseRow(case = case) { onSaveToCase(case, caseLabel.trim()) }
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { step = Step.Pick }) {
                                    Text("Back", color = ForsetiColors.AshGrey)
                                }
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) { /* keep activity alive */ }
    }

    private enum class Step { Pick, RuleMeta, PickCase }

    @Composable
    private fun DestinationCard(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = ForsetiColors.RuneGold)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
                }
            }
        }
    }

    @Composable
    private fun CaseRow(case: CaseEntity, onClick: () -> Unit) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, null, tint = ForsetiColors.MeadAmber)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        case.title.ifBlank { "Case ${case.id}" },
                        style = MaterialTheme.typography.titleMedium,
                        color = ForsetiColors.AshWhite
                    )
                    Text(
                        case.court,
                        style = MaterialTheme.typography.bodySmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        }
    }

    @Composable
    private fun goldField() = TextFieldDefaults.colors(
        focusedContainerColor = ForsetiColors.SurfaceVariant,
        unfocusedContainerColor = ForsetiColors.SurfaceVariant,
        focusedIndicatorColor = ForsetiColors.RuneGold,
        cursorColor = ForsetiColors.RuneGold,
        focusedLabelColor = ForsetiColors.RuneGold
    )
}

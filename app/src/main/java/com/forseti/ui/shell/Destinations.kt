package com.forseti.ui.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.forseti.R

enum class Destination(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    QuickJump("quick_jump", R.string.nav_quick_jump, Icons.Outlined.Explore),
    Drafts("drafts", R.string.nav_drafts, Icons.Outlined.Description),
    Guides("guides", R.string.nav_guides, Icons.AutoMirrored.Outlined.MenuBook),
    States("states", R.string.nav_states, Icons.Outlined.Public),
    Imports("imports", R.string.nav_imports, Icons.Outlined.PictureAsPdf),
    Deadlines("deadlines", R.string.nav_deadlines, Icons.Outlined.CalendarMonth),
    Cases("cases", R.string.nav_cases, Icons.Outlined.FolderShared),
    Scanner("scanner", R.string.nav_scanner, Icons.Outlined.CameraAlt),
    CaseStudies("case_studies", R.string.nav_case_studies, Icons.Outlined.Gavel),
    Backup("backup", R.string.nav_backup, Icons.Outlined.Backup),
    Glossary("glossary", R.string.nav_glossary, Icons.AutoMirrored.Outlined.LibraryBooks),
    Notes("notes", R.string.nav_notes, Icons.Outlined.Bookmarks),
    References("references", R.string.nav_references, Icons.Outlined.Copyright),
    Settings("settings", R.string.nav_settings, Icons.Outlined.Settings);

    companion object {
        val SidebarOrder: List<Destination> = listOf(
            QuickJump, Drafts, Guides, States, Imports, Deadlines, Cases, Scanner,
            CaseStudies, Backup, Glossary, Notes, References, Settings
        )
        val Default: Destination = QuickJump
    }
}

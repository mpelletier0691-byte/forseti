package com.forseti.guides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forseti.ui.theme.ForsetiColors

/**
 * Minimal in-house markdown renderer used by the Guides tab. Built specifically
 * to never crash on the subset of CommonMark syntax we use in
 * the bundled markdown files under `assets/guides/`:
 *
 *  - `#`, `##`, `###` headings (rune-gold)
 *  - blockquotes (`> ...`) rendered as a colored side-bar block
 *  - unordered list items (`-` `+`)
 *  - ordered list items (`1.`)
 *  - inline emphasis: bold, italic, and inline code spans
 *  - fenced code blocks (triple-backtick)
 *  - markdown tables (rendered as bullet lists, not real tables)
 *
 * Anything we can't recognize is rendered as a plain paragraph. The earlier
 * dependency on `multiplatform-markdown-renderer-m3` was crashing on the
 * tables in `discovery_basics.md`; this avoids that whole class of bug.
 *
 * IMPORTANT: do not put a literal slash-star sequence anywhere in this kdoc.
 * Kotlin block comments support nesting, so an inadvertent slash-star in
 * a doc comment opens an unmatched inner comment that breaks the file.
 */
@Composable
fun SafeMarkdown(content: String) {
    val blocks = remember(content) { parseMarkdown(content) }
    Column(modifier = Modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Heading(block)
                is MdBlock.Paragraph -> Paragraph(block.text)
                is MdBlock.Quote -> Quote(block.text)
                is MdBlock.Bullet -> Bullet(block.text)
                is MdBlock.Numbered -> Numbered(block.index, block.text)
                is MdBlock.Code -> CodeBlock(block.text)
                is MdBlock.HorizontalRule -> Hr()
                MdBlock.Gap -> Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Heading(block: MdBlock.Heading) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineMedium
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }
    Spacer(Modifier.height(if (block.level == 1) 4.dp else 12.dp))
    Text(
        text = block.text,
        style = style,
        color = ForsetiColors.RuneGold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = renderInline(text),
        style = MaterialTheme.typography.bodyLarge,
        color = ForsetiColors.AshWhite,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun Quote(text: String) {
    Surface(
        color = ForsetiColors.SurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(ForsetiColors.RuneGold)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = renderInline(text),
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshWhite,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("•", color = ForsetiColors.RuneGold, modifier = Modifier.width(16.dp))
        Text(
            text = renderInline(text),
            style = MaterialTheme.typography.bodyLarge,
            color = ForsetiColors.AshWhite
        )
    }
}

@Composable
private fun Numbered(index: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "$index.",
            color = ForsetiColors.RuneGold,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = renderInline(text),
            style = MaterialTheme.typography.bodyLarge,
            color = ForsetiColors.AshWhite
        )
    }
}

@Composable
private fun CodeBlock(text: String) {
    Surface(
        color = ForsetiColors.Charcoal,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = ForsetiColors.MeadAmber,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun Hr() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 0.dp)
            .background(ForsetiColors.Stone)
    )
    Spacer(Modifier.height(8.dp))
}

// ----- Inline renderer (bold, italic, code spans) -----

/**
 * Greedy left-to-right scanner. Cheap, safe, and handles the few inline
 * markers we use without trying to be a full CommonMark parser.
 */
internal fun renderInline(raw: String): AnnotatedString {
    val out = buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            val rest = raw.substring(i)
            when {
                rest.startsWith("**") -> {
                    val close = raw.indexOf("**", i + 2)
                    if (close > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(raw.substring(i + 2, close))
                        }
                        i = close + 2
                    } else { append('*'); i++ }
                }
                rest.startsWith("*") -> {
                    val close = raw.indexOf("*", i + 1)
                    if (close > 0) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(raw.substring(i + 1, close))
                        }
                        i = close + 1
                    } else { append('*'); i++ }
                }
                rest.startsWith("`") -> {
                    val close = raw.indexOf('`', i + 1)
                    if (close > 0) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33D6A75A),
                                fontSize = 14.sp
                            )
                        ) {
                            append(raw.substring(i + 1, close))
                        }
                        i = close + 1
                    } else { append('`'); i++ }
                }
                else -> { append(raw[i]); i++ }
            }
        }
    }
    return out
}

// ----- Block parser -----

internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Numbered(val index: Int, val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    object HorizontalRule : MdBlock
    object Gap : MdBlock
}

private val numberedRegex = Regex("^(\\d+)\\.\\s+(.*)")
private val tableRowRegex = Regex("^\\s*\\|.*\\|\\s*$")
private val tableSepRegex = Regex("^\\s*\\|?\\s*:?-{2,}.*")

internal fun parseMarkdown(raw: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = raw.replace("\r\n", "\n").split("\n")
    var i = 0
    val paraBuf = StringBuilder()

    fun flushParagraph() {
        if (paraBuf.isNotEmpty()) {
            blocks += MdBlock.Paragraph(paraBuf.toString().trim())
            paraBuf.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> {
                flushParagraph()
                if (blocks.isNotEmpty() && blocks.last() != MdBlock.Gap) {
                    blocks += MdBlock.Gap
                }
                i++
            }
            trimmed.startsWith("```") -> {
                flushParagraph()
                val codeBuf = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeBuf.appendLine(lines[i])
                    i++
                }
                if (i < lines.size) i++ // skip closing fence
                blocks += MdBlock.Code(codeBuf.toString().trimEnd())
            }
            trimmed.startsWith("# ") -> {
                flushParagraph(); blocks += MdBlock.Heading(1, trimmed.removePrefix("# ").trim()); i++
            }
            trimmed.startsWith("## ") -> {
                flushParagraph(); blocks += MdBlock.Heading(2, trimmed.removePrefix("## ").trim()); i++
            }
            trimmed.startsWith("### ") -> {
                flushParagraph(); blocks += MdBlock.Heading(3, trimmed.removePrefix("### ").trim()); i++
            }
            trimmed.startsWith("> ") -> {
                flushParagraph(); blocks += MdBlock.Quote(trimmed.removePrefix("> ").trim()); i++
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                flushParagraph(); blocks += MdBlock.HorizontalRule; i++
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flushParagraph(); blocks += MdBlock.Bullet(trimmed.drop(2).trim()); i++
            }
            numberedRegex.matches(trimmed) -> {
                flushParagraph()
                val match = numberedRegex.find(trimmed)!!
                val idx = match.groupValues[1].toInt()
                blocks += MdBlock.Numbered(idx, match.groupValues[2].trim())
                i++
            }
            tableRowRegex.matches(line) -> {
                // Convert markdown tables into bullet rows so we don't blow up.
                flushParagraph()
                while (i < lines.size && tableRowRegex.matches(lines[i])) {
                    val row = lines[i].trim().trim('|').split('|').map { it.trim() }
                    if (!tableSepRegex.matches(lines[i])) {
                        blocks += MdBlock.Bullet(row.filter { it.isNotEmpty() }.joinToString("  —  "))
                    }
                    i++
                }
            }
            else -> {
                if (paraBuf.isNotEmpty()) paraBuf.append(' ')
                paraBuf.append(trimmed)
                i++
            }
        }
    }
    flushParagraph()
    return blocks
}

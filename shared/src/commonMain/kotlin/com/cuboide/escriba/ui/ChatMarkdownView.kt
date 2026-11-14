package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.core.text.MdBlock
import com.cuboide.escriba.core.text.parseChatMarkdown
import com.cuboide.escriba.core.text.parseInlineMarkdown

/** Resposta da IA com o Markdown aplicado (títulos, listas, citações, código e ênfase). */
@Composable
fun ChatMarkdownView(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseChatMarkdown(text) }
    val palette = LocalPalette.current
    val codeBg = MaterialTheme.colorScheme.surfaceContainer
    val body = MaterialTheme.typography.bodyMedium
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { b ->
            when (b) {
                is MdBlock.Heading -> Text(
                    inline(b.text, codeBg),
                    style = when (b.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 6.dp),
                )
                is MdBlock.Paragraph -> Text(inline(b.text, codeBg), style = body)
                is MdBlock.ListItem -> Row(Modifier.fillMaxWidth().padding(start = (b.depth * 16).dp)) {
                    Text(b.marker, style = body, color = palette.muted, modifier = Modifier.width(if (b.marker == "•") 14.dp else 24.dp))
                    Text(inline(b.text, codeBg), style = body, modifier = Modifier.weight(1f))
                }
                is MdBlock.Quote -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(palette.hairline))
                    Text(inline(b.text, codeBg), style = body.copy(fontStyle = FontStyle.Italic), color = palette.muted, modifier = Modifier.padding(start = 10.dp))
                }
                is MdBlock.Code -> Text(
                    b.text,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = body.fontSize * 0.92f),
                    modifier = Modifier.fillMaxWidth().clip(Radius.control).background(codeBg).padding(10.dp),
                )
                MdBlock.Rule -> Hairline(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

private fun inline(text: String, codeBg: Color): AnnotatedString = buildAnnotatedString {
    parseInlineMarkdown(text).forEach { r ->
        val style = SpanStyle(
            fontWeight = if (r.bold) FontWeight.SemiBold else null,
            fontStyle = if (r.italic) FontStyle.Italic else null,
            fontFamily = if (r.code) FontFamily.Monospace else null,
            background = if (r.code) codeBg else Color.Unspecified,
        )
        withStyle(style) { append(r.text) }
    }
}

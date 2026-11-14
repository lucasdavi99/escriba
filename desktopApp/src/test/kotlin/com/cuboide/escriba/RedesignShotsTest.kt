package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EnredoView
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.app.UniverseTool
import com.cuboide.escriba.core.calendar.CalendarTemplate
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.outline.StructureTemplates
import com.cuboide.escriba.core.storage.ThemeId
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Capturas de todas as telas principais nos quatro temas, para revisão visual do design.
 * Só grava (e só roda de verdade) com `-Pescriba.layoutPreviewDir=<pasta>`.
 */
@OptIn(ExperimentalTestApi::class)
class RedesignShotsTest {
    @Test
    fun capturesEveryScreenInEveryTheme() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it, "redesign").apply { mkdirs() } } ?: return
        val dir = Files.createTempDirectory("escriba-redesign").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-12" },
            )
            lateinit var c: EscribaController
            runDesktopComposeUiTest(width = 1440, height = 900) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember { EscribaController(services, scope) }
                    Box(Modifier.size(1440.dp, 900.dp)) { App(c) }
                }
                waitForIdle()
                fun shot(name: String, dialog: Boolean = false) {
                    waitForIdle()
                    val node = if (dialog) onAllNodes(isRoot()).onLast() else onRoot()
                    ImageIO.write(node.captureToImage().toAwtImage(), "png", java.io.File(out, "$name.png"))
                }
                shot("00-primeiro-uso")

                runOnIdle {
                    c.createBook("A Casa das Marés", "Helena Prado")
                    c.createBook("O Farol de Areia", "Marina Duarte")
                    c.updateBookDetails(
                        "O Farol de Areia", "Marina Duarte",
                        "Numa vila de pescadores, o farol amanhece soterrado e uma menina decide descobrir por quê.", "", 800, 60000,
                    )
                    c.renameChapter(c.currentChapterId!!, "O degrau a mais")
                    c.chapter!!.type(TextFieldValue(TEXT, TextRange(TEXT.length)))
                    c.flushSave()
                    c.addChapter(); c.addChapter()
                    val lia = c.createEntry(EntryKind.Personagem).copy(name = "Lia", summary = "Neta do faroleiro, 14 anos, teimosa e curiosa.")
                    c.saveEntry(lia)
                    c.saveEntry(c.createEntry(EntryKind.Personagem).copy(name = "Seu Bento", summary = "O avô, faroleiro aposentado."))
                    c.saveEntry(c.createEntry(EntryKind.Local).copy(name = "O farol", summary = "Torre branca de cento e doze degraus."))
                    c.applyTemplate(StructureTemplates.all.first())
                    c.createCalendar(CalendarTemplate.Exemplo)
                }
                waitForIdle()

                for (theme in ThemeId.entries) {
                    val t = theme.name.lowercase()
                    runOnIdle { c.updateSettings { it.copy(theme = theme) }; c.goTo(Section.Biblioteca) }
                    shot("$t-01-biblioteca")
                    runOnIdle { c.goTo(Section.Manuscrito); c.selectChapter(c.book!!.manifest.chapters.first().id) }
                    shot("$t-02-manuscrito")
                    runOnIdle { c.universeTool = UniverseTool.Fichas; c.goTo(Section.Universo) }
                    shot("$t-03-universo")
                    runOnIdle { c.showCalendar() }
                    shot("$t-04-calendario")
                    runOnIdle { c.enredoView = EnredoView.Roteiro; c.goTo(Section.Enredo) }
                    shot("$t-05-roteiro")
                    runOnIdle { c.enredoView = EnredoView.Linha }
                    shot("$t-06-linha-do-tempo")
                    runOnIdle { c.enredoView = EnredoView.Mapa }
                    shot("$t-07-mapa")
                    runOnIdle { c.goTo(Section.Metas) }
                    shot("$t-08-metas")
                    runOnIdle { c.goTo(Section.Preferencias) }
                    shot("$t-09-preferencias")
                    if (theme == ThemeId.Papel) {
                        runOnIdle { c.universeTool = UniverseTool.Idiomas; c.goTo(Section.Universo) }
                        shot("$t-10-idiomas")
                        runOnIdle { c.goTo(Section.Versoes) }
                        shot("$t-11-versoes")
                        runOnIdle { c.goTo(Section.Busca) }
                        shot("$t-12-busca")
                        runOnIdle { c.goTo(Section.Exportar) }
                        shot("$t-13-exportar")
                        runOnIdle { c.goTo(Section.Lixeira) }
                        shot("$t-14-lixeira")
                        runOnIdle { c.goTo(Section.Diagramacao) }
                        shot("$t-15-diagramacao")
                        runOnIdle { c.universeTool = UniverseTool.Acervo; c.goTo(Section.Universo) }
                        shot("$t-16-acervo")
                    }
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        const val TEXT = "Naquela manhã o farol amanheceu coberto de areia. Ninguém na vila sabia explicar como o vento tinha subido " +
            "tão alto, nem por que as gaivotas haviam desaparecido.\n" +
            "Lia subiu os degraus contando cada um, como fazia o avô. Seu Bento dizia que eram cento e doze, mas naquele dia " +
            "ela contou cento e catorze.\n" +
            "— Vô, tem um degrau a mais — gritou, a voz batendo nas paredes curvas.\n" +
            "Ninguém respondeu. Lá embaixo, o mar recuava em silêncio."
    }
}

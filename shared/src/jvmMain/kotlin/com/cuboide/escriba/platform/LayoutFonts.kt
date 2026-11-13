package com.cuboide.escriba.platform

import com.cuboide.escriba.core.layout.FontSpec
import com.cuboide.escriba.core.layout.GlyphMeasurer
import java.awt.Font
import java.awt.font.FontRenderContext
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Uma face (arquivo + índice na coleção) de uma família instalada. Só TrueType: é o que o PDF embute. */
internal class FontFace(
    val file: File,
    val family: String,
    val postScriptName: String,
    val bold: Boolean,
    val italic: Boolean,
    /** Fonte AWT em corpo 1 (derive para o tamanho desejado). */
    val awt: Font,
) {
    val isCollection: Boolean get() = file.name.endsWith(".ttc", ignoreCase = true)

    /** Corpo 1000, usado para medir com precisão (derivar a cada medição sairia caro). */
    val awt1000: Font by lazy { awt.deriveFont(1000f) }
}

/** Catálogo das fontes TrueType do sistema, lido uma vez (em segundo plano, na primeira diagramação). */
internal object FontCatalog {
    private val symbolFamilies = setOf(
        "wingdings", "wingdings 2", "wingdings 3", "webdings", "symbol", "marlett", "mt extra",
        "segoe mdl2 assets", "segoe fluent icons", "hololens mdl2 assets", "bookshelf symbol 7",
    )
    private val fallbackFamilies = listOf("Segoe UI Symbol", "Segoe UI", "Arial", "DejaVu Sans", "Noto Sans", "Liberation Sans")

    val faces: Map<String, List<FontFace>> by lazy { scan() }

    fun families(): List<String> =
        faces.values.mapNotNull { list -> list.firstOrNull()?.family }
            .filter { it.lowercase() !in symbolFamilies }
            .sortedBy { it.lowercase() }

    fun has(family: String) = faces.containsKey(family.lowercase())

    /** Face mais próxima do estilo pedido (itálico pesa mais que negrito). */
    fun face(spec: FontSpec): FontFace? {
        val list = faces[spec.family.lowercase()] ?: return null
        return list.firstOrNull { it.bold == spec.bold && it.italic == spec.italic }
            ?: list.firstOrNull { it.italic == spec.italic }
            ?: list.firstOrNull { !it.bold && !it.italic }
            ?: list.first()
    }

    val fallback: FontFace? by lazy { fallbackFamilies.firstNotNullOfOrNull { face(FontSpec(it)) } }

    /** Fonte da escrita inventada do livro aberto (ver `registerScriptFont`). */
    @Volatile
    var script: FontFace? = null

    private fun fontDirs(): List<File> {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> listOfNotNull(
                File(System.getenv("WINDIR") ?: "C:\\Windows", "Fonts"),
                System.getenv("LOCALAPPDATA")?.let { File(it, "Microsoft\\Windows\\Fonts") },
            )
            os.contains("mac") -> listOf(File("/System/Library/Fonts"), File("/Library/Fonts"), File(home, "Library/Fonts"))
            else -> listOf(File("/usr/share/fonts"), File("/usr/local/share/fonts"), File(home, ".fonts"), File(home, ".local/share/fonts"))
        }
    }

    private fun scan(): Map<String, List<FontFace>> {
        val result = HashMap<String, MutableList<FontFace>>()
        for (dir in fontDirs()) {
            dir.walkTopDown().maxDepth(4)
                .filter { it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".ttc", true)) }
                .forEach { file ->
                    val fonts = runCatching { Font.createFonts(file) }.getOrNull() ?: return@forEach
                    for (f in fonts) {
                        val family = f.getFamily(Locale.ENGLISH)
                        val name = f.getFontName(Locale.ENGLISH).lowercase()
                        val style = name.removePrefix(family.lowercase())
                        val bold = Regex("\\b(bold|black|heavy)\\b").containsMatchIn(style)
                        val italic = Regex("\\b(italic|oblique)\\b").containsMatchIn(style)
                        // Pesos intermediários (light, semibold…) ficam fora para não disputar com o regular/negrito.
                        if (Regex("\\b(light|thin|semilight|semibold|demibold|medium|extralight|condensed|narrow)\\b").containsMatchIn(style)) continue
                        result.getOrPut(family.lowercase()) { ArrayList() } += FontFace(file, family, f.psName, bold, italic, f)
                    }
                }
        }
        return result
    }
}

/** Divide o texto em trechos pela cobertura da fonte: o que ela não desenha vai para a fonte reserva. */
internal fun coverageRuns(text: String, face: FontFace): List<Pair<String, FontFace>> {
    val script = FontCatalog.script
    val hasScript = script != null && text.any { it.code in 0xE000..0xF8FF }
    if (!hasScript && face.awt.canDisplayUpTo(text) == -1) return listOf(text to face)
    val fallback = FontCatalog.fallback
    val out = ArrayList<Pair<String, FontFace>>()
    val sb = StringBuilder()
    var current: FontFace? = null
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val chars = String(Character.toChars(cp))
        val target = when {
            // A escrita inventada sempre vence: outras fontes podem ter algo na área de uso privado.
            script != null && cp in 0xE000..0xF8FF && script.awt.canDisplay(cp) -> script
            face.awt.canDisplay(cp) -> face
            fallback != null && fallback.awt.canDisplay(cp) -> fallback
            else -> face
        }
        val shown = if (target.awt.canDisplay(cp)) chars else "?"
        if (current != null && current !== target) { out += sb.toString() to current; sb.clear() }
        current = target
        sb.append(shown)
        i += Character.charCount(cp)
    }
    if (sb.isNotEmpty()) out += sb.toString() to current!!
    return out
}

/** Medição com as métricas dos mesmos arquivos de fonte que vão para o PDF (sem kerning, como o PDF). */
internal object AwtGlyphMeasurer : GlyphMeasurer {
    private val frc = FontRenderContext(null, true, true)
    private val cache = ConcurrentHashMap<Pair<FontSpec, String>, Float>()

    /** A fonte da escrita mudou: medidas guardadas com ela não valem mais. */
    fun clearCache() = cache.clear()
    private val serifDefaults = listOf("Georgia", "Times New Roman", "Cambria", "Liberation Serif", "DejaVu Serif", "Noto Serif")

    fun advance(text: String, face: FontFace, sizePt: Float): Float {
        if (text.isEmpty()) return 0f
        val gv = face.awt1000.createGlyphVector(frc, text)
        return gv.getGlyphPosition(gv.numGlyphs).x.toFloat() * sizePt / 1000f
    }

    override fun width(text: String, font: FontSpec, sizePt: Float, trackingEm: Float): Float {
        if (text.isEmpty()) return 0f
        val at1000 = cache.getOrPut(font to text) {
            val face = FontCatalog.face(font) ?: return@getOrPut text.length * 500f
            coverageRuns(text, face).sumOf { (seg, f) -> advance(seg, f, 1000f).toDouble() }.toFloat()
        }
        return at1000 * sizePt / 1000f + trackingEm * sizePt * text.length
    }

    override fun hasFamily(family: String) = FontCatalog.has(family)

    override fun resolveFamily(candidates: List<String>): String =
        candidates.firstOrNull { hasFamily(it) }
            ?: serifDefaults.firstOrNull { hasFamily(it) }
            ?: FontCatalog.families().firstOrNull()
            ?: "Serif"
}

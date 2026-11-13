package com.cuboide.escriba.core.font

/** Glifo para a fonte: caractere, largura de avanço e contornos (pontos todos sobre a curva, y para cima). */
data class TtfGlyph(val codepoint: Int, val advance: Int, val contours: List<List<Pair<Int, Int>>>)

/**
 * Escreve uma fonte TrueType mínima e válida (sem dicas): tabelas head, hhea, maxp 1.0, OS/2 v4, hmtx, cmap
 * (formato 4), loca (longo), glyf, name e post 3.0, com checksums e o ajuste do head. Glifo 0 é o ".notdef"
 * (um retângulo vazado) e o 1 é o espaço. Os demais vêm de [glyphs], em ordem de caractere.
 */
fun writeTtf(family: String, glyphs: List<TtfGlyph>, unitsPerEm: Int = 1000, ascent: Int = 800, descent: Int = -200, spaceAdvance: Int = 300): ByteArray {
    val sorted = glyphs.sortedBy { it.codepoint }
    val notdef = TtfGlyph(0, 500, listOf(
        listOf(50 to 0, 50 to 700, 450 to 700, 450 to 0),      // externo, horário
        listOf(100 to 50, 400 to 50, 400 to 650, 100 to 650),  // interno, anti-horário (vazado)
    ))
    val space = TtfGlyph(0x20, spaceAdvance, emptyList())
    val all = listOf(notdef, space) + sorted

    // ---------- glyf + loca ----------
    val glyf = Bytes()
    val loca = IntArray(all.size + 1)
    var maxPoints = 0
    var maxContours = 0
    var xMin = Int.MAX_VALUE; var yMin = Int.MAX_VALUE; var xMax = Int.MIN_VALUE; var yMax = Int.MIN_VALUE
    val lsb = IntArray(all.size)
    all.forEachIndexed { i, g ->
        loca[i] = glyf.size
        val pts = g.contours.flatten()
        if (pts.isNotEmpty()) {
            val gx0 = pts.minOf { it.first }; val gy0 = pts.minOf { it.second }
            val gx1 = pts.maxOf { it.first }; val gy1 = pts.maxOf { it.second }
            xMin = minOf(xMin, gx0); yMin = minOf(yMin, gy0); xMax = maxOf(xMax, gx1); yMax = maxOf(yMax, gy1)
            lsb[i] = gx0
            maxPoints = maxOf(maxPoints, pts.size)
            maxContours = maxOf(maxContours, g.contours.size)
            glyf.i16(g.contours.size); glyf.i16(gx0); glyf.i16(gy0); glyf.i16(gx1); glyf.i16(gy1)
            var end = -1
            g.contours.forEach { c -> end += c.size; glyf.u16(end) }
            glyf.u16(0) // sem instruções
            repeat(pts.size) { glyf.u8(0x01) } // todos sobre a curva; coordenadas em 16 bits
            var px = 0; var py = 0
            pts.forEach { (x, _) -> glyf.i16(x - px); px = x }
            pts.forEach { (_, y) -> glyf.i16(y - py); py = y }
            glyf.padTo4()
        }
    }
    loca[all.size] = glyf.size
    if (xMin == Int.MAX_VALUE) { xMin = 0; yMin = 0; xMax = 0; yMax = 0 }

    val locaT = Bytes().apply { loca.forEach { u32(it.toLong()) } }

    // ---------- hmtx ----------
    val hmtx = Bytes().apply { all.forEachIndexed { i, g -> u16(g.advance); i16(lsb[i]) } }

    // ---------- head ----------
    val head = Bytes().apply {
        u32(0x00010000); u32(0x00010000) // versão, revisão
        u32(0) // checkSumAdjustment (preenchido no fim)
        u32(0x5F0F3CF5)
        u16(0x000B) // linha de base em y=0, lsb em x=0, tamanhos inteiros
        u16(unitsPerEm)
        u64(0); u64(0) // criada, modificada
        i16(xMin); i16(yMin); i16(xMax); i16(yMax)
        u16(0) // macStyle
        u16(8) // lowestRecPPEM
        i16(2) // fontDirectionHint
        i16(1) // indexToLocFormat: longo
        i16(0)
    }

    // ---------- hhea ----------
    val hhea = Bytes().apply {
        u32(0x00010000)
        i16(ascent); i16(descent); i16(0)
        u16(all.maxOf { it.advance })
        i16(lsb.minOrNull() ?: 0)
        i16(all.indices.minOfOrNull { all[it].advance - (glyphMaxX(all[it]) ?: 0) } ?: 0)
        i16(xMax)
        i16(1); i16(0); i16(0) // caret
        i16(0); i16(0); i16(0); i16(0)
        i16(0) // metricDataFormat
        u16(all.size)
    }

    // ---------- maxp 1.0 ----------
    val maxp = Bytes().apply {
        u32(0x00010000); u16(all.size)
        u16(maxPoints); u16(maxContours); u16(0); u16(0)
        u16(2); u16(0); u16(0); u16(0); u16(0); u16(0); u16(0); u16(0); u16(0)
    }

    // ---------- OS/2 v4 ----------
    val firstChar = 0x20
    val lastChar = sorted.lastOrNull()?.codepoint?.coerceAtMost(0xFFFF) ?: 0x20
    val os2 = Bytes().apply {
        u16(4)
        i16(all.map { it.advance }.average().toInt())
        u16(400); u16(5); u16(0) // peso, largura, fsType (pode embutir)
        i16(650); i16(600); i16(0); i16(140) // subscrito
        i16(650); i16(600); i16(0); i16(480) // sobrescrito
        i16(50); i16(300) // tachado
        i16(0) // família
        repeat(10) { u8(0) } // PANOSE
        u32(1); u32(1L shl 28); u32(0); u32(0) // Latim básico; área de uso privado (bit 60)
        ascii("ESCR")
        u16(0x40) // regular
        u16(firstChar); u16(lastChar)
        i16(ascent); i16(descent); i16(0)
        u16(ascent + 100); u16(-descent + 50)
        u32(1); u32(0) // página de código Latin 1
        i16(450); i16(650) // altura-x, altura das maiúsculas
        u16(0); u16(0x20); u16(1)
    }

    // ---------- cmap formato 4 ----------
    val cmap = Bytes().apply {
        // Segmentos: espaço, cada faixa contígua dos glifos e o terminal 0xFFFF.
        data class Seg(val start: Int, val end: Int, val delta: Int)
        val segs = mutableListOf(Seg(0x20, 0x20, 1 - 0x20))
        var k = 0
        while (k < sorted.size) {
            var j = k
            while (j + 1 < sorted.size && sorted[j + 1].codepoint == sorted[j].codepoint + 1) j++
            segs += Seg(sorted[k].codepoint, sorted[j].codepoint, (k + 2) - sorted[k].codepoint)
            k = j + 1
        }
        segs += Seg(0xFFFF, 0xFFFF, 1)
        val segX2 = segs.size * 2
        var pow = 1; var log = 0
        while (pow * 2 <= segs.size) { pow *= 2; log++ }
        val sub = Bytes().apply {
            u16(4); u16(16 + segs.size * 8); u16(0)
            u16(segX2); u16(pow * 2); u16(log); u16(segX2 - pow * 2)
            segs.forEach { u16(it.end) }
            u16(0)
            segs.forEach { u16(it.start) }
            segs.forEach { u16(it.delta and 0xFFFF) }
            segs.forEach { u16(0) }
        }
        u16(0); u16(1) // versão, uma subtabela
        u16(3); u16(1); u32(12) // Windows, Unicode BMP
        bytes(sub.toByteArray())
    }

    // ---------- name ----------
    val name = Bytes().apply {
        val records = listOf(1 to family, 2 to "Regular", 3 to "$family Regular", 4 to family, 6 to family.filter { it.isLetterOrDigit() }.ifEmpty { "Escrita" })
        val strings = records.map { (_, s) -> utf16be(s) }
        u16(0); u16(records.size); u16(6 + records.size * 12)
        var offset = 0
        records.forEachIndexed { i, (id, _) ->
            u16(3); u16(1); u16(0x409); u16(id); u16(strings[i].size); u16(offset)
            offset += strings[i].size
        }
        strings.forEach { bytes(it) }
    }

    // ---------- post 3.0 ----------
    val post = Bytes().apply { u32(0x00030000); u32(0); i16(-100); i16(50); u32(0); u32(0); u32(0); u32(0); u32(0) }

    // O diretório pede as tabelas em ordem de etiqueta (ordem de bytes: maiúsculas antes).
    val tables = listOf(
        "OS/2" to os2.toByteArray(), "cmap" to cmap.toByteArray(), "glyf" to glyf.toByteArray(), "head" to head.toByteArray(),
        "hhea" to hhea.toByteArray(), "hmtx" to hmtx.toByteArray(), "loca" to locaT.toByteArray(), "maxp" to maxp.toByteArray(),
        "name" to name.toByteArray(), "post" to post.toByteArray(),
    ).sortedBy { it.first }
    return assemble(tables)
}

private fun glyphMaxX(g: TtfGlyph): Int? = g.contours.flatten().maxOfOrNull { it.first }

private fun assemble(tables: List<Pair<String, ByteArray>>): ByteArray {
    val n = tables.size
    var pow = 1; var log = 0
    while (pow * 2 <= n) { pow *= 2; log++ }
    val out = Bytes()
    out.u32(0x00010000); out.u16(n); out.u16(pow * 16); out.u16(log); out.u16(n * 16 - pow * 16)
    var offset = 12 + n * 16
    val placed = tables.map { (tag, data) ->
        val at = offset
        offset += (data.size + 3) / 4 * 4
        Triple(tag, data, at)
    }
    var headAt = -1
    for ((tag, data, at) in placed) {
        out.ascii(tag); out.u32(checksum(data)); out.u32(at.toLong()); out.u32(data.size.toLong())
        if (tag == "head") headAt = at
    }
    for ((_, data, _) in placed) { out.bytes(data); out.padTo4() }
    val bytes = out.toByteArray()
    // Ajuste do head: 0xB1B0AFBA menos a soma da fonte inteira.
    val adjust = (0xB1B0AFBAL - checksum(bytes)) and 0xFFFFFFFFL
    bytes[headAt + 8] = (adjust ushr 24).toByte(); bytes[headAt + 9] = (adjust ushr 16).toByte()
    bytes[headAt + 10] = (adjust ushr 8).toByte(); bytes[headAt + 11] = adjust.toByte()
    return bytes
}

private fun checksum(data: ByteArray): Long {
    var sum = 0L
    var i = 0
    while (i < data.size) {
        var v = 0L
        for (k in 0 until 4) v = (v shl 8) or ((data.getOrNull(i + k)?.toLong() ?: 0L) and 0xFF)
        sum = (sum + v) and 0xFFFFFFFFL
        i += 4
    }
    return sum
}

private fun utf16be(s: String): ByteArray {
    val b = ByteArray(s.length * 2)
    s.forEachIndexed { i, ch -> b[2 * i] = (ch.code ushr 8).toByte(); b[2 * i + 1] = ch.code.toByte() }
    return b
}

/** Buffer de bytes big-endian. */
private class Bytes {
    private var buf = ByteArray(256)
    var size = 0; private set

    private fun ensure(n: Int) {
        if (size + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, size + n))
    }

    fun u8(v: Int) { ensure(1); buf[size++] = v.toByte() }
    fun u16(v: Int) { u8(v ushr 8); u8(v) }
    fun i16(v: Int) = u16(v and 0xFFFF)
    fun u32(v: Long) { u16((v ushr 16).toInt() and 0xFFFF); u16(v.toInt() and 0xFFFF) }
    fun u32(v: Int) = u32(v.toLong() and 0xFFFFFFFFL)
    fun u64(v: Long) { u32(v ushr 32); u32(v and 0xFFFFFFFFL) }
    fun ascii(s: String) = s.forEach { u8(it.code) }
    fun bytes(b: ByteArray) { ensure(b.size); b.copyInto(buf, size); size += b.size }
    fun padTo4() { while (size % 4 != 0) u8(0) }
    fun toByteArray(): ByteArray = buf.copyOf(size)
}

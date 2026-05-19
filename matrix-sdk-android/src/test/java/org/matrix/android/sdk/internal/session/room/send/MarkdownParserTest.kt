/*
 * Copyright (c) 2026 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.send

import io.mockk.every
import io.mockk.mockk
import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.maths.MathsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.android.sdk.internal.session.room.send.pills.TextPillsUtils

class MarkdownParserTest {

    private val extensions: List<Extension> = listOf(MathsExtension.create(), TablesExtension.create())
    private val textPillsUtils = mockk<TextPillsUtils> {
        every { processSpecialSpansToMarkdown(any()) } returns null
    }
    private val parser = MarkdownParser(
            advancedParser = Parser.builder().extensions(extensions).build(),
            simpleParser = Parser.builder().enabledBlockTypes(setOf(BlockQuote::class.java)).build(),
            htmlRenderer = HtmlRenderer.builder().extensions(extensions).softbreak("<br />").build(),
            textPillsUtils = textPillsUtils
    )

    @Test
    fun `given gfm table markdown when parsing then formatted body contains table html`() {
        val input = """
            | Column A | Column B |
            | --- | --- |
            | 1 | 2 |
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(input, result.text)
        assertNotNull(result.formattedText)
        assertTrue(result.formattedText!!.contains("<table>"))
        assertTrue(result.formattedText!!.contains("<th>Column A</th>"))
        assertTrue(result.formattedText!!.contains("<td>2</td>"))
    }

    @Test
    fun `given inline latex markdown when parsing then formatted body contains matrix math html`() {
        val input = "Euler: ${'$'}e^{i\\pi}+1=0${'$'}"

        val result = parser.parse(input)

        assertEquals(input, result.text)
        assertNotNull(result.formattedText)
        assertTrue(result.formattedText!!.contains("""<span data-mx-maths="e^{i\pi}+1=0">"""))
    }
}

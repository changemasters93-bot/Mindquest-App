package com.android.mindquest.presentation.quiz.visual

import androidx.compose.ui.graphics.Color

/**
 * Represents a resolved visual token for quiz rendering.
 * KMP-compatible — no platform resources needed.
 */
sealed class VisualToken {
    /** A geometric shape drawn via Canvas. */
    data class Shape(val shape: ShapeType, val fillColor: Color = Color.Unspecified) : VisualToken()

    /** A solid color swatch (circle). */
    data class ColorSwatch(val color: Color) : VisualToken()

    /** A shape filled with a specific color. */
    data class Combo(val shape: ShapeType, val color: Color) : VisualToken()

    /** Fallback: render raw text/emoji. */
    data class Emoji(val text: String) : VisualToken()
}

enum class ShapeType {
    TRIANGLE, SQUARE, CIRCLE, PENTAGON, STAR, DIAMOND, HEXAGON, HEART, PLUS, CROSS,
    ARROW_UP, ARROW_RIGHT, SEMICIRCLE, PARALLELOGRAM, TRAPEZOID, OCTAGON, CRESCENT,
}

/**
 * Central token-to-visual registry for quiz rendering.
 *
 * Resolves readable names (or aliases) to [VisualToken]:
 * - "shape:triangle", "triangle", "△"  → Shape(TRIANGLE)
 * - "color:red", "red", "🔴"           → ColorSwatch(red)
 * - "combo:red_circle"                  → Combo(CIRCLE, red)
 *
 * Returns null when input is a plain emoji (let caller render it directly).
 */
object QuizVisualTokenMapper {

    // ── Shape Aliases ──────────────────────────────────────────────────────

    private val shapeAliasMap = buildMap<String, ShapeType> {
        fun add(type: ShapeType, vararg aliases: String) = aliases.forEach { put(it, type) }

        add(ShapeType.TRIANGLE, "triangle", "tri", "△", "▲", "▽", "▼")
        add(ShapeType.SQUARE, "square", "sq", "□", "■", "▪", "◻", "◼")
        add(ShapeType.CIRCLE, "circle", "circ", "dot", "○", "●", "◯")
        add(ShapeType.PENTAGON, "pentagon", "penta")
        add(ShapeType.STAR, "star", "⭐", "☆", "★")
        add(ShapeType.DIAMOND, "diamond", "rhombus", "◆", "◇", "♦")
        add(ShapeType.HEXAGON, "hexagon", "hexa")
        add(ShapeType.HEART, "heart", "❤", "♥", "💜", "💙", "💚")
        add(ShapeType.PLUS, "plus", "+", "➕")
        add(ShapeType.CROSS, "cross", "x", "✕", "✖", "✗", "❌")
        add(ShapeType.ARROW_UP, "arrow_up", "arrow", "↑", "⬆")
        add(ShapeType.ARROW_RIGHT, "arrow_right", "→", "➡")
        add(ShapeType.SEMICIRCLE, "semicircle", "half_circle")
        add(ShapeType.PARALLELOGRAM, "parallelogram", "para")
        add(ShapeType.TRAPEZOID, "trapezoid", "trap")
        add(ShapeType.OCTAGON, "octagon", "octa", "stop")
        add(ShapeType.CRESCENT, "crescent", "moon", "🌙")
    }

    // ── Color Aliases ──────────────────────────────────────────────────────

    private val colorAliasMap = buildMap<String, Color> {
        fun add(color: Color, vararg aliases: String) = aliases.forEach { put(it, color) }

        add(Color(0xFFEF4444), "red", "🔴")
        add(Color(0xFF3B82F6), "blue", "🔵")
        add(Color(0xFF22C55E), "green", "🟢")
        add(Color(0xFFEAB308), "yellow", "🟡")
        add(Color(0xFFF97316), "orange", "🟠")
        add(Color(0xFFA855F7), "purple", "🟣")
        add(Color(0xFFEC4899), "pink", "🩷")
        add(Color(0xFF14B8A6), "teal", "cyan")
        add(Color(0xFF6B7280), "gray", "grey")
        add(Color(0xFF0F172A), "black", "⚫")
        add(Color(0xFFFFFFFF), "white", "⚪")
        add(Color(0xFF92400E), "brown", "🟤")
    }

    // ── Combo Aliases ──────────────────────────────────────────────────────

    private val comboAliasMap: Map<String, Pair<ShapeType, Color>> by lazy {
        buildMap {
            val comboPairs = listOf(
                "red_circle" to Pair(ShapeType.CIRCLE, Color(0xFFEF4444)),
                "blue_square" to Pair(ShapeType.SQUARE, Color(0xFF3B82F6)),
                "red_square" to Pair(ShapeType.SQUARE, Color(0xFFEF4444)),
                "green_circle" to Pair(ShapeType.CIRCLE, Color(0xFF22C55E)),
                "yellow_diamond" to Pair(ShapeType.DIAMOND, Color(0xFFEAB308)),
                "blue_triangle" to Pair(ShapeType.TRIANGLE, Color(0xFF3B82F6)),
                "red_triangle" to Pair(ShapeType.TRIANGLE, Color(0xFFEF4444)),
                "green_square" to Pair(ShapeType.SQUARE, Color(0xFF22C55E)),
                "purple_star" to Pair(ShapeType.STAR, Color(0xFFA855F7)),
                "orange_circle" to Pair(ShapeType.CIRCLE, Color(0xFFF97316)),
                "pink_heart" to Pair(ShapeType.HEART, Color(0xFFEC4899)),
                "blue_diamond" to Pair(ShapeType.DIAMOND, Color(0xFF3B82F6)),
                "red_star" to Pair(ShapeType.STAR, Color(0xFFEF4444)),
                "green_triangle" to Pair(ShapeType.TRIANGLE, Color(0xFF22C55E)),
                "yellow_circle" to Pair(ShapeType.CIRCLE, Color(0xFFEAB308)),
                "purple_hexagon" to Pair(ShapeType.HEXAGON, Color(0xFFA855F7)),
            )
            comboPairs.forEach { (key, value) ->
                put(key, value)
                put("combo:$key", value)
                put(key.replace("_", " "), value)
            }
        }
    }

    /**
     * Resolve raw text to a [VisualToken].
     *
     * Tries combos first, then `shape:` / `color:` prefixed tokens,
     * then bare shape/color names, then dynamic "color_shape" parsing.
     *
     * Returns [VisualToken.Emoji] for unresolvable tokens (emoji passthrough).
     */
    fun resolve(rawText: String): VisualToken {
        val raw = rawText.trim()
        if (raw.isBlank()) return VisualToken.Emoji("\uD83D\uDCCC")

        val normalized = normalize(raw)

        // 1. Explicit combo match
        comboAliasMap[normalized]?.let { (shape, color) ->
            return VisualToken.Combo(shape, color)
        }

        // 2. Prefixed tokens: "shape:triangle", "color:red", "combo:red_circle"
        if (normalized.startsWith("combo:")) {
            comboAliasMap[normalized]?.let { (s, c) -> return VisualToken.Combo(s, c) }
        }
        if (normalized.startsWith("shape:")) {
            val token = normalized.removePrefix("shape:")
            shapeAliasMap[token]?.let { return VisualToken.Shape(it) }
        }
        if (normalized.startsWith("color:")) {
            val token = normalized.removePrefix("color:")
            colorAliasMap[token]?.let { return VisualToken.ColorSwatch(it) }
        }

        // 3. Bare shape name
        shapeAliasMap[normalized]?.let { return VisualToken.Shape(it) }

        // 4. Bare color name
        colorAliasMap[normalized]?.let { return VisualToken.ColorSwatch(it) }

        // 5. Dynamic combo: try to parse "color_shape" or "color shape"
        val dynamicCombo = tryDynamicCombo(normalized)
        if (dynamicCombo != null) return dynamicCombo

        // 6. Multi-character alias search (for embedded tokens in longer strings)
        val shape = findShapeInText(normalized)
        val color = findColorInText(normalized)
        if (shape != null && color != null) return VisualToken.Combo(shape, color)
        if (shape != null) return VisualToken.Shape(shape)
        if (color != null) return VisualToken.ColorSwatch(color)

        // 7. Fallback: treat as emoji/text
        return VisualToken.Emoji(raw)
    }

    /**
     * Quick check: does this text look like a visual token (not plain emoji)?
     */
    fun isToken(rawText: String): Boolean {
        val n = normalize(rawText.trim())
        return n.startsWith("shape:") || n.startsWith("color:") || n.startsWith("combo:") ||
            shapeAliasMap.containsKey(n) || colorAliasMap.containsKey(n) || comboAliasMap.containsKey(n)
    }

    // ── Internals ──────────────────────────────────────────────────────────

    private fun tryDynamicCombo(normalized: String): VisualToken? {
        // "red_circle" → color=red, shape=circle
        val parts = normalized.split("_", " ").filter { it.isNotBlank() }
        if (parts.size == 2) {
            // Try color_shape order
            val color1 = colorAliasMap[parts[0]]
            val shape1 = shapeAliasMap[parts[1]]
            if (color1 != null && shape1 != null) return VisualToken.Combo(shape1, color1)

            // Try shape_color order
            val shape2 = shapeAliasMap[parts[0]]
            val color2 = colorAliasMap[parts[1]]
            if (shape2 != null && color2 != null) return VisualToken.Combo(shape2, color2)
        }
        return null
    }

    private fun findShapeInText(text: String): ShapeType? {
        shapeAliasMap.forEach { (alias, type) ->
            if (alias.length > 1 && text.contains(alias)) return type
        }
        return null
    }

    private fun findColorInText(text: String): Color? {
        colorAliasMap.forEach { (alias, color) ->
            if (alias.length > 1 && text.contains(alias)) return color
        }
        return null
    }

    private fun normalize(raw: String): String {
        return raw.trim().lowercase().replace("-", "_").replace(Regex("\\s+"), " ").trim()
    }
}

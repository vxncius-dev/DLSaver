package com.vxncius.dlsaver

import java.text.Normalizer

private val removableTerms = listOf(
    "clipe oficial",
    "clipe",
    "official video",
    "official music video",
    "official audio",
    "official lyric video",
    "lyric video",
    "lyrics video",
    "audio oficial",
    "vídeo oficial",
    "video oficial",
    "music video",
    "visualizer",
    "vizualizer",
    "official visualizer",
    "performance video",
    "live session",
    "ao vivo",
    "live",
    "remastered",
    "hq",
    "hd",
    "4k"
)

fun sanitizeDownloadTitle(rawTitle: String): String {
    if (rawTitle.isBlank()) return "Download"

    var text = rawTitle
        .replace(Regex("#\\S+"), " ")
        .replace(Regex("""\[[^\]]*]"""), " ")
        .replace(Regex("""\(([^)]*)\)""")) { match ->
            val inner = normalizeComparisonText(match.groupValues[1])
            if (removableTerms.any { it in inner }) " " else match.value
        }

    removableTerms.forEach { term ->
        text = text.replace(Regex("(?i)\\b${Regex.escape(term)}\\b"), " ")
    }

    text = text
        .replace(Regex("""[\\/:*?"<>|]"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
        .trim('-', '_', '.', ' ')

    return if (text.isBlank()) "Download" else text
}

fun fileNameWithoutExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf('.')
    return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
}

fun renamePreservingExtension(originalName: String, typedName: String): String {
    val cleanBase = typedName.trim().trimEnd('.')
    if (cleanBase.isBlank()) return originalName

    val dotIndex = originalName.lastIndexOf('.')
    return if (dotIndex > 0) {
        "$cleanBase.${originalName.substring(dotIndex + 1)}"
    } else {
        cleanBase
    }
}

fun isValidRenamedBaseName(typedName: String): Boolean {
    val cleanBase = typedName.trim().trimEnd('.')
    if (cleanBase.isBlank()) return false
    if (cleanBase.startsWith('.')) return false
    if (Regex("""[\\/:*?"<>|]""").containsMatchIn(cleanBase)) return false
    return true
}

fun normalizedDownloadBaseName(name: String): String {
    return normalizeComparisonText(fileNameWithoutExtension(name))
}

fun nextUniqueFileName(desiredName: String, existingNames: Set<String>): String {
    if (desiredName !in existingNames) return desiredName

    val extension = desiredName.substringAfterLast('.', "")
    val baseName = fileNameWithoutExtension(desiredName)
    var index = 1
    while (true) {
        val candidate = if (extension.isBlank() || desiredName.lastIndexOf('.') <= 0) {
            "$baseName ($index)"
        } else {
            "$baseName ($index).$extension"
        }
        if (candidate !in existingNames) return candidate
        index++
    }
}

private fun normalizeComparisonText(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .lowercase()
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
}

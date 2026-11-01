package org.devlive.metaforge.convert.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * A setting a convert plugin declares for its writer, shown by the export page as a field: a
 * choice among [choices] or free text when there are none. Labels are keyed by language (zh, en).
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "Read-only description of a plugin setting")
data class ConvertOption(
    val name: String,
    val labels: Map<String, String>,
    val defaultValue: String,
    val choices: List<ConvertChoice> = listOf()
)

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "Read-only description of a choice")
data class ConvertChoice(
    val value: String,
    val labels: Map<String, String>
)

fun choice(value: String, zh: String, en: String) = ConvertChoice(value, mapOf("zh" to zh, "en" to en))

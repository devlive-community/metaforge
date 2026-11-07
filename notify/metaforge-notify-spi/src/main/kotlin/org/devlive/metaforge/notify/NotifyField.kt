package org.devlive.metaforge.notify

/**
 * One setting a notification channel needs from each user, such as a robot token. The server keeps
 * SECRET values write-only: users see that a value is set, never the value itself.
 *
 * @param name stable key stored in the user's channel settings
 * @param label English label; the web UI shows `notify.field.<channel>.<name>` when translated
 */
data class NotifyField(
    val name: String,
    val label: String,
    val type: NotifyFieldType = NotifyFieldType.TEXT,
    val required: Boolean = false,
    val description: String? = null,
    val placeholder: String? = null,
)

enum class NotifyFieldType
{
    TEXT,
    SECRET,
    URL
}

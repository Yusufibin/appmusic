package com.company.product

/** A single private Telegram account is authorized locally, before any network access. */
data class BotConfiguration(val token: String, val ownerId: Long, val deviceName: String) {
    companion object {
        fun validToken(value: String): Boolean = Regex("[0-9]{6,16}:[A-Za-z0-9_-]{30,64}").matches(value)
        fun validOwner(value: String): Boolean = value.toLongOrNull()?.let { it > 0 } == true
    }

    fun isValid(): Boolean = validToken(token) && ownerId > 0 && deviceName.isNotBlank() &&
        deviceName.length <= 48 && deviceName.none { it.isISOControl() }
}

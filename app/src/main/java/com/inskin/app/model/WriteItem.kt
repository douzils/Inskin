package com.inskin.app.model

import kotlinx.serialization.Serializable

/**
 * Types of items that can be written to an NFC tag.
 */
@Serializable
enum class WriteItemType {
    TEXT,
    URL,
    WEB_SEARCH,
    SOCIAL,
    VIDEO,
    FILE,
    APP,
    MAIL,
    CONTACT,
    PHONE,
    SMS,
    LOCATION,
    CUSTOM_LOCATION,
    ADDRESS,
    DESTINATION,
    NEARBY_SEARCH,
    STREET_VIEW,
    EMERGENCY,
    CRYPTO,
    BLUETOOTH,
    WIFI,
    CUSTOM_DATA,
    SETTINGS,
    CONDITION
}

/**
 * Sealed hierarchy representing an item to write.
 * Only a small subset is currently used by the UI but the model allows more
 * types to be added later.
 */
@Serializable
sealed class WriteItem {
    abstract val type: WriteItemType

    /** Plain text payload. */
    @Serializable
    data class Text(val text: String) : WriteItem() {
        override val type: WriteItemType = WriteItemType.TEXT
    }

    /** Web or application URL. */
    @Serializable
    data class Url(val url: String) : WriteItem() {
        override val type: WriteItemType = WriteItemType.URL
    }

    /** Phone number. */
    @Serializable
    data class Phone(val number: String) : WriteItem() {
        override val type: WriteItemType = WriteItemType.PHONE
    }

    /** SMS payload with optional body. */
    @Serializable
    data class Sms(val number: String, val body: String? = null) : WriteItem() {
        override val type: WriteItemType = WriteItemType.SMS
    }

    /** Email payload. */
    @Serializable
    data class Mail(val to: String, val subject: String? = null, val body: String? = null) : WriteItem() {
        override val type: WriteItemType = WriteItemType.MAIL
    }

    /** Wi‑Fi network information. */
    @Serializable
    data class Wifi(val ssid: String, val security: String, val password: String? = null) : WriteItem() {
        override val type: WriteItemType = WriteItemType.WIFI
    }

    /** Bluetooth MAC address. */
    @Serializable
    data class Bluetooth(val mac: String) : WriteItem() {
        override val type: WriteItemType = WriteItemType.BLUETOOTH
    }

    /** Minimal contact information. */
    @Serializable
    data class Contact(val name: String, val phone: String? = null, val email: String? = null) : WriteItem() {
        override val type: WriteItemType = WriteItemType.CONTACT
    }

    /** Geographic coordinates. */
    @Serializable
    data class Location(val lat: Double, val lon: Double) : WriteItem() {
        override val type: WriteItemType = WriteItemType.LOCATION
    }

    /** Generic key/value pair serialised as custom data. */
    @Serializable
    data class Data(val key: String, val value: String) : WriteItem() {
        override val type: WriteItemType = WriteItemType.CUSTOM_DATA
    }

    /** Generic URI based item for less common types. */
    @Serializable
    data class UriItem(override val type: WriteItemType, val uri: String) : WriteItem()
}


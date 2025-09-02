// tags/TagDetails.kt
package com.inskin.app

data class ClassicBlockInfo(
    val sector: Int,
    val indexInSector: Int,
    val absBlock: Int,
    val dataHex: String?,
    val isValue: Boolean?,
    val value: Int?,
    val addr: Int?,
    val access: String?
)

data class ClassicTrailer(
    val keyA: String?,
    val accessBitsHex: String?,
    val accessDecoded: String,
    val keyB: String?
)

data class ClassicSectorInfo(
    val sector: Int,
    val usedKeyType: String?,
    val usedKeyHex: String?,
    val blockCount: Int,
    val blocks: List<ClassicBlockInfo>,
    val trailer: ClassicTrailer?
)

data class TagDetails(
    val uidHex: String,
    val techList: List<String>,
    val atqaHex: String? = null,
    val sakHex: String? = null,
    val manufacturer: String? = null,
    val batchInfo: String? = null,
    val chipType: String? = null,
    val totalMemoryBytes: Int? = null,
    val memoryLayout: String? = null,
    val versionHex: String? = null,
    val atsHex: String? = null,
    val historicalBytesHex: String? = null,
    val hiLayerResponseHex: String? = null,
    val lockBitsHex: String? = null,
    val otpBytesHex: String? = null,
    val isNdefWritable: Boolean? = null,
    val canMakeReadOnly: Boolean? = null,
    val ndefCapacity: Int? = null,
    val ndefRecords: List<NdefRecordInfo> = emptyList(),
    val usedBytes: Int? = null,
    val rawReadableBytes: Int? = null,
    val rawDumpFirstBytesHex: String? = null,
    val countersHex: String? = null,
    val tearFlag: Boolean? = null,
    val eccSignatureHex: String? = null,
    val rfConfig: String? = null,
    val applications: List<String>? = null,
    val files: List<String>? = null,
    val classicSectors: List<ClassicSectorInfo>? = null,
    val classicReadableSectors: Int? = null,
    val classicDiscoveredKeys: Int? = null
)
data class NdefRecordInfo(
    val tnf: Int? = null,
    val type: String? = null,
    val payloadHex: String? = null,
    val idHex: String? = null,
    val text: String? = null,
    val lang: String? = null,
    val uri: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Int? = null,
    // compat UI existante
    val value: String? = null
)

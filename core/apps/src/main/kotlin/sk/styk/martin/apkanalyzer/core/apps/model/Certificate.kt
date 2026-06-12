package sk.styk.martin.apkanalyzer.core.apps.model

import java.util.Date

data class Certificate(
    val signAlgorithm: String,
    val certificateHashMd5: String,
    val certificateHashSha1: String,
    val certificateHashSha256: String,
    val publicKeyMd5: String,
    val publicKeySha1: String,
    val publicKeySha256: String,
    val startDate: Date,
    val endDate: Date,
    val serialNumber: Int = 0,
    val issuerName: String? = null,
    val issuerOrganization: String? = null,
    val issuerCountry: String? = null,
    val subjectName: String? = null,
    val subjectOrganization: String? = null,
    val subjectCountry: String? = null,
)

package sk.styk.martin.apkanalyzer.core.entitlements.impl.billing

internal class BillingUnavailableException(responseCode: Int? = null) :
    Exception(responseCode?.let { "Google Play returned response code $it" } ?: "Google Play could not be reached")

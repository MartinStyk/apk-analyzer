package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

enum class UnusedAppsPeriod(val days: Int) {
    ONE_MONTH(30),
    TWO_MONTHS(60),
    THREE_MONTHS(90),
    SIX_MONTHS(180),
    ONE_YEAR(365),
}

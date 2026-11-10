package sk.styk.martin.apkanalyzer.core.common.digest

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat

class FingerprintFormattingTest {

    @Test
    fun `Given lowercase hex When formatting with separators Then pairs are uppercase and colon separated`() {
        val hash = "a1b2c3d4"

        val fingerprint = hash.toFingerprint(FingerprintFormat.ColonSeparated)

        fingerprint shouldBe "A1:B2:C3:D4"
    }

    @Test
    fun `Given lowercase hex When formatting continuously Then separators are omitted`() {
        val hash = "a1b2c3d4"

        val fingerprint = hash.toFingerprint(FingerprintFormat.Continuous)

        fingerprint shouldBe "A1B2C3D4"
    }

    @Test
    fun `Given colon separated hex When formatting continuously Then existing separators are removed`() {
        val hash = "a1:b2:c3:d4"

        val fingerprint = hash.toFingerprint(FingerprintFormat.Continuous)

        fingerprint shouldBe "A1B2C3D4"
    }

    @Test
    fun `Given colon separated hex When formatting with separators Then separators are not duplicated`() {
        val hash = "a1:b2:c3:d4"

        val fingerprint = hash.toFingerprint(FingerprintFormat.ColonSeparated)

        fingerprint shouldBe "A1:B2:C3:D4"
    }
}

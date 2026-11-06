package sk.styk.martin.apkanalyzer.core.appexport.format

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.ApplicationName
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.Debuggable
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.Manifest
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.ProcessName
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.RequestedPermissions
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.SplitApkDetails
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.VersionCode
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Flag
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Items
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Missing
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Number
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Record
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Text
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.TextList
import java.io.StringWriter
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class JsonInventoryWriterTest {

    private val output = StringWriter()
    private val generatedAt = Instant.parse("2026-11-03T14:49:12.345Z")

    @Test
    fun `Given no apps When closing Then document holds the envelope and an empty list`() {
        val writer = JsonInventoryWriter(listOf(ApplicationName), output, generatedAt)

        writer.begin()
        writer.close()

        output.parsed() shouldBe parsed(
            """
            {
              "${'$'}schema": "https://martinstyk.github.io/apk-analyzer/inventory-export/v1/schema.json",
              "schema_version": 1,
              "generated_at": "2026-11-03T14:49:12.345Z",
              "apps": []
            }
            """,
        )
    }

    @Test
    fun `Given a value of every type When writing an app Then each field has its JSON type`() {
        val writer = JsonInventoryWriter(listOf(ApplicationName, VersionCode, Debuggable, RequestedPermissions, ProcessName), output, generatedAt)

        writer.begin()
        writer.writeApp(listOf(Text("Maps"), Number(42), Flag(true), TextList(listOf("CAMERA", "INTERNET")), Missing))
        writer.close()

        output.parsed() shouldBe parsed(
            """
            {
              "${'$'}schema": "https://martinstyk.github.io/apk-analyzer/inventory-export/v1/schema.json",
              "schema_version": 1,
              "generated_at": "2026-11-03T14:49:12.345Z",
              "apps": [
                {
                  "application_name": "Maps",
                  "version_code": 42,
                  "debuggable": true,
                  "requested_permissions": ["CAMERA", "INTERNET"],
                  "process_name": null
                }
              ]
            }
            """,
        )
    }

    @Test
    fun `Given nested records and an empty list When writing an app Then nesting is preserved`() {
        val writer = JsonInventoryWriter(listOf(SplitApkDetails, Manifest, RequestedPermissions), output, generatedAt)

        writer.begin()
        writer.writeApp(
            listOf(
                Items(listOf(Record(mapOf("file_name" to Text("split.apk"), "file_path" to Missing)))),
                Record(mapOf("xml" to Text("<manifest/>"))),
                TextList(emptyList()),
            ),
        )
        writer.close()

        output.parsed() shouldBe parsed(
            """
            {
              "${'$'}schema": "https://martinstyk.github.io/apk-analyzer/inventory-export/v1/schema.json",
              "schema_version": 1,
              "generated_at": "2026-11-03T14:49:12.345Z",
              "apps": [
                {
                  "split_apk_details": [{ "file_name": "split.apk", "file_path": null }],
                  "manifest": { "xml": "<manifest/>" },
                  "requested_permissions": []
                }
              ]
            }
            """,
        )
    }

    @Test
    fun `Given several apps When writing Then each app is its own object in order`() {
        val writer = JsonInventoryWriter(listOf(ApplicationName), output, generatedAt)

        writer.begin()
        writer.writeApp(listOf(Text("Maps")))
        writer.writeApp(listOf(Text("Notes")))
        writer.close()

        output.parsed() shouldBe parsed(
            """
            {
              "${'$'}schema": "https://martinstyk.github.io/apk-analyzer/inventory-export/v1/schema.json",
              "schema_version": 1,
              "generated_at": "2026-11-03T14:49:12.345Z",
              "apps": [{ "application_name": "Maps" }, { "application_name": "Notes" }]
            }
            """,
        )
    }
}

private fun StringWriter.parsed() = parsed(toString())

private fun parsed(json: String) = Json.parseToJsonElement(json)

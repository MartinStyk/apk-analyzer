package sk.styk.martin.apkanalyzer.core.appexport.format

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.ApplicationName
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.Debuggable
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.ProcessName
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.RequestedPermissions
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.SplitApkDetails
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn.VersionCode
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Flag
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Missing
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Number
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Text
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.TextList
import java.io.StringWriter

class CsvInventoryWriterTest {

    private val output = StringWriter()
    private val writer = CsvInventoryWriter(listOf(ApplicationName), output)

    @Test
    fun `Given several columns When beginning Then header row holds their keys`() {
        val writer = CsvInventoryWriter(listOf(ApplicationName, VersionCode), output)

        writer.begin()

        output.toString() shouldBe "application_name,version_code\r\n"
    }

    @Test
    fun `Given a value of every type When writing an app Then each cell is written as published`() {
        val writer = CsvInventoryWriter(listOf(ApplicationName, VersionCode, Debuggable, RequestedPermissions, ProcessName), output)

        writer.writeApp(listOf(Text("Maps"), Number(42), Flag(true), TextList(listOf("CAMERA", "INTERNET")), Missing))

        output.toString() shouldBe "Maps,42,true,CAMERA; INTERNET,\r\n"
    }

    @Test
    fun `Given a structured column When creating the writer Then it is rejected`() {
        val columns = listOf(ApplicationName, SplitApkDetails)

        shouldThrow<IllegalArgumentException> { CsvInventoryWriter(columns, output) }
    }

    @Test
    fun `Given separators or line breaks in a text When writing an app Then cell is quoted`() {
        writer.writeApp(listOf(Text("Fish, Chips")))
        writer.writeApp(listOf(Text("Line\nBreak")))
        writer.writeApp(listOf(Text("Line\rBreak")))

        output.toString() shouldBe "\"Fish, Chips\"\r\n\"Line\nBreak\"\r\n\"Line\rBreak\"\r\n"
    }

    @Test
    fun `Given a quote in a text When writing an app Then quote is doubled`() {
        writer.writeApp(listOf(Text("The \"Best\" App")))

        output.toString() shouldBe "\"The \"\"Best\"\" App\"\r\n"
    }

    @Test
    fun `Given a text starting with a formula character When writing an app Then cell is guarded`() {
        writer.writeApp(listOf(Text("=1+1")))
        writer.writeApp(listOf(Text("+1")))
        writer.writeApp(listOf(Text("-1")))
        writer.writeApp(listOf(Text("@SUM")))
        writer.writeApp(listOf(Text("\tTab")))

        output.toString() shouldBe "'=1+1\r\n'+1\r\n'-1\r\n'@SUM\r\n'\tTab\r\n"
    }

    @Test
    fun `Given a text starting with a line break When writing an app Then cell is guarded and quoted`() {
        writer.writeApp(listOf(Text("\nBreak")))
        writer.writeApp(listOf(Text("\rBreak")))

        output.toString() shouldBe "\"'\nBreak\"\r\n\"'\rBreak\"\r\n"
    }

    @Test
    fun `Given a formula with a comma When writing an app Then guard stays inside the quotes`() {
        writer.writeApp(listOf(Text("=SUM(A1,B1)")))

        output.toString() shouldBe "\"'=SUM(A1,B1)\"\r\n"
    }

    @Test
    fun `Given a list starting with a formula When writing an app Then cell is guarded`() {
        val writer = CsvInventoryWriter(listOf(RequestedPermissions), output)

        writer.writeApp(listOf(TextList(listOf("=1+1", "CAMERA"))))

        output.toString() shouldBe "'=1+1; CAMERA\r\n"
    }

    @Test
    fun `Given a negative number When writing an app Then cell is not guarded`() {
        val writer = CsvInventoryWriter(listOf(VersionCode), output)

        writer.writeApp(listOf(Number(-1)))

        output.toString() shouldBe "-1\r\n"
    }
}

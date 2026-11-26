package sk.styk.martin.apkanalyzer.core.apps.impl.manifest

import android.content.res.Resources

internal interface ManifestXmlRenderer {
    fun render(resources: Resources): String
}

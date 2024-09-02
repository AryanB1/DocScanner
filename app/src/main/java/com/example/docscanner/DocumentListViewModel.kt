package com.example.docscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class DocumentListViewModel : ViewModel() {

    data class ScannedDocument(val name: String,val path: String, val previewImage: Bitmap? = null)

    private val _scannedDocuments = MutableLiveData<List<ScannedDocument>>(emptyList())
    val scannedDocuments: LiveData<List<ScannedDocument>> = _scannedDocuments

    fun loadScannedDocuments(context: Context) {
        val documents = getPdfFilesFromInternalStorage(context)
        _scannedDocuments.value = documents
    }

    private fun getPdfFilesFromInternalStorage(context: Context): List<ScannedDocument> {
        val filesDir = context.filesDir
        val pdfFiles = filesDir.listFiles { file -> file.isFile && file.name.endsWith(".pdf") }
        val previewImage = pdfFiles?.firstOrNull()?.let { generatePdfPreview(it) }
        return pdfFiles?.map { file ->
            ScannedDocument(
                name = file.nameWithoutExtension,
                path = file.absolutePath,
                previewImage = previewImage
            )
        } ?: emptyList()
    }
    private fun generatePdfPreview(pdfFile: File): Bitmap? {
        try {
            val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(parcelFileDescriptor)
            val page = renderer.openPage(0) // Get the first page
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            parcelFileDescriptor.close()
            return bitmap
        } catch (e: Exception) {
            Log.e("DocumentListViewModel", "Error generating PDF preview: ${e.message}")
            return null
        }
    }
}
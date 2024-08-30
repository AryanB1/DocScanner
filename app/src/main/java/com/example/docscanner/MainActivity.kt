package com.example.docscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.docscanner.ui.theme.DocScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Background
@Composable
fun MainScreen() {
	// Sets background colour
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(color = Color(0xFFECEFF1))
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(160.dp)
				.size(14.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text("Press the button below to scan your document!")
		}
	}
}

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val docScanner = createDocumentScanner()
		// figure out if I even want edge to edge display for app
//		enableEdgeToEdge()

		setContent {
			DocScannerTheme {
				MainScreen()
				var showDialog by remember { mutableStateOf(false) }
				var pdfUris by remember { mutableStateOf<Uri?>(null) }

				val DocLauncher = createDocLauncher(
					onPdfSaved = { pdfUri ->
						pdfUris = pdfUri
						showDialog = true
					}
				)
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
					Column(modifier = Modifier.fillMaxSize()) {
//						ScannedImages(imageURIs)
					}

					ScanDocumentButton(docScanner = docScanner, docLauncher = DocLauncher, this@MainActivity)

					if (showDialog) {
						DocumentScanScreen(
							onDismiss = { showDialog = false },
							onSave = { filename ->
								pdfUris?.let { uri ->
									savePdfFile(uri, filename)
								}
								showDialog = false
							}
						)
					}
				}
			}
		}
	}
	// Properties for the document scanner
	private fun createDocumentScanner(): GmsDocumentScanner {
		val docScanOptions = GmsDocumentScannerOptions.Builder()
			.setScannerMode(SCANNER_MODE_FULL)
			.setGalleryImportAllowed(true)
			.setPageLimit(Int.MAX_VALUE)
			.setResultFormats(RESULT_FORMAT_PDF)
			.build()
		return GmsDocumentScanning.getClient(docScanOptions)
	}
	// Saves file after scanning
	private fun savePdfFile(pdfUri: Uri, name: String) {
		try {
			val file = File(filesDir, name)
			val filestream = FileOutputStream(file)
			contentResolver.openInputStream(pdfUri)?.use { inputStream ->
				inputStream.copyTo(filestream)}
			// Success Toast
			Toast.makeText(this@MainActivity, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
			Handler(Looper.getMainLooper()).postDelayed({
				val pdfFileUri = FileProvider.getUriForFile(
					this@MainActivity,
					"${this@MainActivity.packageName}.fileprovider",
					file
				)
				val intent = Intent(Intent.ACTION_VIEW).apply {
					setDataAndType(pdfFileUri, "application/pdf")
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				}
				this@MainActivity.startActivity(intent)
			}, 100)
		} catch (e: IOException) {
			Log.e("SavePDF", "Error saving PDF: ${e.message}")
			// Error Toast
			Toast.makeText(this@MainActivity, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
		}
	}

	@Composable
	private fun createDocLauncher(
		onPdfSaved: (Uri) -> Unit
	): ActivityResultLauncher<IntentSenderRequest> {
		return rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartIntentSenderForResult(),
			onResult = {
				if (it.resultCode == RESULT_OK) {
					val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
					result?.pdf?.let { pdf -> onPdfSaved(pdf.uri) }
				}
			}
		)
	}
}

@Composable
fun ScanDocumentButton(
	docScanner: GmsDocumentScanner,
	docLauncher: ActivityResultLauncher<IntentSenderRequest>,
	mainActivity: MainActivity
) {
	FloatingActionButton(
		onClick = {
			docScanner.getStartScanIntent(mainActivity)
				.addOnSuccessListener {
					IntentSenderRequest.Builder(it).build().let { request ->
						docLauncher.launch(request)
					}
				}
				.addOnFailureListener {
					Toast.makeText(
						mainActivity,
						it.message,
						Toast.LENGTH_LONG
					).show()
				}
		},
		modifier = Modifier
			.padding(top = 600.dp)
			.size(80.dp),
	) {
		Icon(
			Icons.Filled.Add,
			contentDescription = stringResource(R.string.scan_new_document),
			modifier = Modifier.size(40.dp),
		)
	}
}
//
//@Composable
//fun ScannedImages(imageURIs: List<Uri>) {
//	Column(
//		modifier = Modifier
//			.fillMaxWidth()
//			.padding(16.dp),
//		verticalArrangement = Arrangement.Top,
//		horizontalAlignment = Alignment.CenterHorizontally
//	) {
//		imageURIs.forEach { uri ->
//			AsyncImage(
//				model = uri,
//				contentDescription = null,
//				contentScale = ContentScale.FillWidth,
//				modifier = Modifier.fillMaxWidth().padding(8.dp)
//			)
//		}
//	}
//}

@Composable
fun DocumentScanScreen(onDismiss: () -> Unit, onSave: (String) -> Unit) {
	var filename by remember { mutableStateOf("") }

	AlertDialog(
		onDismissRequest = { onDismiss() },
		title = { Text(text = "Save Document") },
		text = {
			Column {
				Text("Enter a filename:")
				TextField(
					value = filename,
					onValueChange = { filename = it },
					placeholder = { Text("Filename") }
				)
			}
		},
		confirmButton = {
			Button(onClick = { onSave(filename) }) {
				Text("Save")
			}
		},
		dismissButton = {
			Button(onClick = { onDismiss() }) {
				Text("Cancel")
			}
		}
	)
}
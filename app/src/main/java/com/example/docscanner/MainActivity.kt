package com.example.docscanner

import android.net.Uri
import android.os.Bundle
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
		enableEdgeToEdge()

		setContent {
			DocScannerTheme {
				MainScreen()
				var showDialog by remember { mutableStateOf(false) }
				var imageURIs by remember { mutableStateOf<List<Uri>>(emptyList()) }

				val DocLauncher = createDocLauncher(
					onScanned = { uris ->
						imageURIs = uris
						showDialog = true // Trigger the dialog after the scan
					},
					onPdfSaved = { pdfUri -> savePdfFile(pdfUri) }
				)
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
					Column(modifier = Modifier.fillMaxSize()) {
						ScannedImages(imageURIs)
					}

					ScanDocumentButton(docScanner = docScanner, docLauncher = DocLauncher, this@MainActivity)

					if (showDialog) {
						DocumentScanScreen(
							onDismiss = { showDialog = false },
							onSave = { filename ->
								// Handle saving the document with the filename
								showDialog = false
							}
						)
					}
				}
			}
		}
	}

	private fun createDocumentScanner(): GmsDocumentScanner {
		val docScanOptions = GmsDocumentScannerOptions.Builder()
			.setScannerMode(SCANNER_MODE_FULL)
			.setGalleryImportAllowed(true)
			.setPageLimit(Int.MAX_VALUE)
			.setResultFormats(RESULT_FORMAT_PDF, RESULT_FORMAT_JPEG)
			.build()
		return GmsDocumentScanning.getClient(docScanOptions)
	}

	private fun savePdfFile(pdfUri: Uri) {
		val filestream = FileOutputStream(File(filesDir, "docScan.pdf"))
		contentResolver.openInputStream(pdfUri)?.use {
			it.copyTo(filestream)
		}
	}

	@Composable
	private fun createDocLauncher(
		onScanned: (List<Uri>) -> Unit,
		onPdfSaved: (Uri) -> Unit
	): ActivityResultLauncher<IntentSenderRequest> {
		return rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartIntentSenderForResult(),
			onResult = {
				if (it.resultCode == RESULT_OK) {
					val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
					val uris = result?.pages?.map { it.imageUri } ?: emptyList()
					onScanned(uris)
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

@Composable
fun ScannedImages(imageURIs: List<Uri>) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp),
		verticalArrangement = Arrangement.Top,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		imageURIs.forEach { uri ->
			AsyncImage(
				model = uri,
				contentDescription = null,
				contentScale = ContentScale.FillWidth,
				modifier = Modifier.fillMaxWidth().padding(8.dp)
			)
		}
	}
}

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
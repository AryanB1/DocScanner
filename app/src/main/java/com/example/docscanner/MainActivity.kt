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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.docscanner.ui.theme.DocScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun BackgroundColour() {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(color = Color(0xFFECEFF1))
	)
}

class MainActivity : ComponentActivity() {
	private val viewModel: DocumentListViewModel by viewModels()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val docScanner = createDocumentScanner()

		setContent {
			DocScannerTheme {
				BackgroundColour()
				var showDialog by remember { mutableStateOf(false) }
				var pdfUris by remember { mutableStateOf<Uri?>(null) }

				val DocLauncher = createDocLauncher(
					onPdfSaved = { pdfUri ->
						pdfUris = pdfUri
						showDialog = true
					}
				)
				val documentsLiveData= viewModel.scannedDocuments
				var documents by remember { mutableStateOf<List<DocumentListViewModel.ScannedDocument>>(emptyList()) }

				documentsLiveData.observe(this@MainActivity) { newDocuments ->
					documents = newDocuments
				}

				Column(modifier = Modifier.fillMaxSize()) {
					DocumentListScreen(documents)
				}
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {

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
		viewModel.loadScannedDocuments(this@MainActivity)
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
	@Composable
	fun DocumentListScreen(documents:List<DocumentListViewModel.ScannedDocument>) {
		if (documents.isEmpty()) {
			Column(
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = "No documents saved yet. \nPress the button to start scanning!",
					textAlign = TextAlign.Center,
					fontSize = 20.sp,
					fontWeight = FontWeight.Medium,
					fontFamily = FontFamily.SansSerif,
					color = Color(0xFF212121)
				)
			}
		}
		else {
			LazyVerticalGrid(
				columns = GridCells.Fixed(2),
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				items(documents) { document ->
					DocumentTile(document) { documentPath ->
						launchPdfViewer(documentPath)
					}
				}
			}
		}
	}

	@Composable
	fun DocumentTile(document: DocumentListViewModel.ScannedDocument, onDocumentClick: (String) -> Unit) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(6.dp)
				.clickable { onDocumentClick(document.path) },
			elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
			shape = RoundedCornerShape(8.dp),
			colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF))
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				document.previewImage?.let {
					Image(
						bitmap = it.asImageBitmap(),
						contentDescription = "Document Preview",
						modifier = Modifier
							.fillMaxWidth()
							.height(150.dp)
							.clip(RoundedCornerShape(8.dp))
					)
					Spacer(modifier = Modifier.height(8.dp))
				}
				Text(
					text = document.name,
					fontWeight = FontWeight.Bold,
					fontFamily = FontFamily.Serif
				)
			}
		}
	}
	private fun launchPdfViewer(documentPath: String) {
		val file = File(documentPath)
		val uri = FileProvider.getUriForFile(this@MainActivity, "${this@MainActivity.packageName}.fileprovider", file)

		val intent = Intent(Intent.ACTION_VIEW).apply {
			setDataAndType(uri, "application/pdf")
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
		this@MainActivity.startActivity(intent)
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
			.padding(top = 500.dp, bottom = 30.dp)
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
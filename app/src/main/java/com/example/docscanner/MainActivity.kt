package com.example.docscanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.docscanner.ui.theme.DocScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
//		Initialize the document scanner
		val docScanOptions = GmsDocumentScannerOptions.Builder()
			.setScannerMode(SCANNER_MODE_FULL)
			.setGalleryImportAllowed(true)
			.setPageLimit(Int.MAX_VALUE)
			.setResultFormats(RESULT_FORMAT_PDF, RESULT_FORMAT_JPEG)
			.build()
		// Create a document scanner client
        val docScanner = GmsDocumentScanning.getClient(docScanOptions)
		enableEdgeToEdge()
		setContent {
			DocScannerTheme {
				MainScreen()
				// Create a list of image URIs to store the scanned images
				var imageURIS by remember { mutableStateOf<List<Uri>>(emptyList()) }
				val DocLauncher = rememberLauncherForActivityResult(
					contract = ActivityResultContracts.StartIntentSenderForResult(),
					onResult = {
						// Handles the result of the document scanning
						if(it.resultCode == RESULT_OK) {
							val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
							imageURIS = result?.pages?.map {it.imageUri} ?: emptyList()
							result?.pdf?.let { pdf ->
								val filestream = FileOutputStream(File(filesDir, "docScan.pdf"))
								contentResolver.openInputStream(pdf.uri)?.use {
									it.copyTo(filestream)
								}
							}

						}
					}
				)
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.BottomCenter

				) {
					Column(
						modifier = Modifier.fillMaxSize(),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						imageURIS.forEach { uri ->
							AsyncImage(
								model = uri,
								contentDescription = null,
								contentScale = ContentScale.FillWidth,
								modifier = Modifier.fillMaxWidth()
							)
						}
						FloatingActionButton(
							onClick = {
								// Starts the document scanning
								docScanner.getStartScanIntent(this@MainActivity)
									.addOnSuccessListener {
										IntentSenderRequest.Builder(it).build()
									}
									.addOnFailureListener {
										Toast.makeText(
											applicationContext,
											it.message,
											Toast.LENGTH_LONG,
										).show()
									}
								println("Logging: button pressed")
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
				}
			}
		}
	}
}

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
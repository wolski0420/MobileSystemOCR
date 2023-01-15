package com.example.ocr

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var monitor: ResourcesMonitor
    private lateinit var csvService: CSVFileService
    private lateinit var collector: ResultDataCollector
    private lateinit var dirChooser: ActivityResultLauncher<Uri>
    private lateinit var initialDir: Uri
    private lateinit var chosenDir: Uri
    private var language = "eng"
    private var mTessOCR: TesseractOCR? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialDir = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        dirChooser = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            if (result != null) {
                chosenDir = result
                initialDir = result

                val dirDocument = DocumentFile.fromTreeUri(this.applicationContext, chosenDir)
                if (dirDocument != null && dirDocument.exists() && dirDocument.isDirectory) {
                    choose_dir_button.setBackgroundColor(Color.GREEN)
                } else {
                    choose_dir_button.setBackgroundColor(Color.RED)
                }
            }
        }

        mTessOCR = TesseractOCR(this, language)
        monitor = ResourcesMonitor(this)
        csvService = CSVFileService(this)
        collector = ResultDataCollector(monitor, csvService)
    }

    fun switchToMonitoring(view: View) {
        val intent = Intent(this, ResourcesActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("Recycle")
    private fun loadFilesAsByteArrays(): List<ByteArray>{
        val dirDocument: DocumentFile? = DocumentFile.fromTreeUri(this.applicationContext, chosenDir)
        val listOfByteArrays = mutableListOf<ByteArray>()

        dirDocument?.listFiles()?.filter { it.exists() && !it.isDirectory &&
                (it.name!!.endsWith(".jpg") || it.name!!.endsWith(".jpeg") || it.name!!.endsWith(".png"))
        }?.forEach { fileDocument ->
            contentResolver.openInputStream(fileDocument.uri)?.readBytes()?.let { listOfByteArrays.add(it) }
        }

        return listOfByteArrays
    }

    fun chooseDirectory(view: View) {
        dirChooser.launch(initialDir)
    }

    fun ocrLocal(view: View) {
        // taking params from inputs
        val iterations = iterations_input.text.toString().toInt()
        val packetSize = packet_size_input.text.toString().toInt()

        Thread {
            // loading files
            val filesAsBA = loadFilesAsByteArrays()

            // processing multiple times
            for (i in 1..iterations) {
                // taking random images
                val randomImages = filesAsBA.shuffled().take(packetSize)

                // preparing for monitoring
                var size = 0
                collector.start()

                // every image from random set
                for (image in randomImages) {
                    // converting into bitmaps and OCRing
                    val bmp = BitmapFactory.decodeByteArray(image, 0, image.size)
                    val resultText = mTessOCR!!.getOCRResult(bmp)

                    if (resultText != "") {
                        Log.d("MainActivity - LocalOCR", resultText)
                    } else {
                        Log.d("MainActivity - LocalOCR", "No text found for $i packet and $image")
                    }

                    // counting processed data size
                    size += image.size
                }

                // end of monitoring
                collector.finish(size)
                collector.save()
            }
        }.start()
    }

    fun ocrCloud(view: View) {
        // taking params from inputs
        val url = url_input.text
        val iterations = iterations_input.text.toString().toInt()
        val packetSize = packet_size_input.text.toString().toInt()

        Thread {
            // loading files
            val filesAsBA = loadFilesAsByteArrays()

            for (i in 1..iterations) {
                // taking random images
                val randomImages = filesAsBA.shuffled().take(packetSize)

                // preparing for monitoring
                var size = 0
                collector.start()

                // every image from random set
                for (image in randomImages) {
                    // sending request
                    val response = OwnHttpClient().sendRequestWithBytes(
                        "http://$url:8000/upload",
                        "pobrane.png", image
                    )

                    // processing response
                    if (response != null) {
                        if (response.isSuccessful) {
                            Log.d("MainActivity - CloudOCR", "Success")
                            Log.d("MainActivity - CloudOCR", response.body!!.string())
                        } else {
                            Log.d("MainActivity - CloudOCR", "Failure")
                            Log.d("MainActivity - CloudOCR", response.body!!.string())
                        }
                    }

                    // counting processed data size
                    size += image.size
                }

                // end of monitoring
                collector.finish(size)
                collector.save()
            }
        }.start()
    }
}
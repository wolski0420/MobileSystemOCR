package com.example.ocr

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


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

        dirDocument?.listFiles()?.forEach { fileDocument ->
            contentResolver.openInputStream(fileDocument.uri)?.readBytes()?.let { listOfByteArrays.add(it) }
        }

        return listOfByteArrays
    }

    fun chooseDirectory(view: View) {
        dirChooser.launch(initialDir)
    }

    fun ocrLocal(view: View) {
        // getting drawables
        val res = resources as Resources
        var size: Int
        val ids = listOf(R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane)
        var resultText: String

        // taking params from inputs
        val iterations = iterations_input.text.toString().toInt()
        val packetSize = packet_size_input.text.toString().toInt()
        val inputStreams = loadFilesAsByteArrays()

        Thread {
            // processing multiple times
            for (i in 1..1) {
                size = 0
                collector.start()

                // every time different number of drawables
                for (j in 1..i) {
                    // converting into bitmaps and OCRing
                    val image = inputStreams[0]
                    val bmp = BitmapFactory.decodeByteArray(image, 0, image.size)
//                    size += bmp.byteCount
                    size += image.size
                    resultText = mTessOCR!!.getOCRResult(bmp)

                    if (resultText != "") {
                        println(resultText)
                        Log.d("MainActivity - LocalOCR", resultText)
                    } else {
                        println("No text found ERROR$i $j")
                        Log.d("MainActivity - LocalOCR", "No text found ERROR$i $j")
                    }
                }

                collector.finish(size)
                collector.save()
            }
        }.start()
    }

    fun ocrCloud(view: View) {
        // getting drawable and converting into byteArray
        val image = resources.getDrawable(R.drawable.pobrane)
        val bitmap = (image as BitmapDrawable).bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        // converting byteArray into File
        val file = File(cacheDir, "image.jpg")
        file.createNewFile()
        val fos = FileOutputStream(file)
        fos.write(byteArray)
        fos.flush()
        fos.close()

        // taking params from inputs
        val url = url_input.text
        val iterations = iterations_input.text.toString().toInt()
        val packetSize = packet_size_input.text.toString().toInt()

        Thread {
            collector.start()

            for (i in 1..10) {
                // sending request once
                OwnHttpClient().sendRequestWithFile(
                    "http://$url:8000/upload",
                    "pobrane.png", file
                )
            }

            collector.finish(iterations * image.bitmap.byteCount)
            collector.save()
        }.start()
    }
}
package com.example.ocr

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var monitor: ResourcesMonitor
    private lateinit var csvService: CSVFileService
    private lateinit var collector: ResultDataCollector
    private var language = "eng"
    private var mTessOCR: TesseractOCR? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTessOCR = TesseractOCR(this, language)
        monitor = ResourcesMonitor(this)
        csvService = CSVFileService(this)
        collector = ResultDataCollector(monitor, csvService)
    }

    fun switchToMonitoring(view: View) {
        val intent = Intent(this, ResourcesActivity::class.java)
        startActivity(intent)
    }

    fun ocrLocal(view: View) {
        // getting drawables
        val res = resources as Resources
        var size: Int
        val ids = listOf(R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane)
        var resultText: String

        Thread {
            // processing multiple times
            for (i in 1..10) {
                size = 0
                collector.start()

                // every time different number of drawables
                for (j in 0..i) {
                    // converting into bitmaps and OCRing
                    val bmp = BitmapFactory.decodeResource(res, ids[j])
                    size += bmp.byteCount
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

        Thread {
            collector.start()

            for (i in 1..10) {
                // sending request once
                OwnHttpClient().sendRequestWithFile(
                    "http://192.168.0.136:8000/upload",
                    "pobrane.png", file
                )
            }

            collector.finish(10 * image.bitmap.byteCount)
            collector.save()
        }.start()
    }
}
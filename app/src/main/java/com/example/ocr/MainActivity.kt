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
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
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
    private var mapOfLevelDescriptions: Map<Int, String> = mapOf(
        1 to "standard sending",
        2 to "sending with checksum",
        3 to "client-server encryption",
        4 to "server-client encryption",
        5 to "client-server-client encryption",
        6 to "client-server-client checksum encryption",
        7 to "client-server-client checksum and result encryption",
        8 to "client-server-client checksum and data encryption"
    )

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

        val securityLevel = "${getString(R.string.security_level)} 1"
        security_level_textview.text = securityLevel

        security_level_slider.addOnChangeListener { slider, value, fromUser ->
            val newIntValue = value.toInt()
            val newText = "${getString(R.string.security_level)} $newIntValue"
            security_level_textview.text = newText

            security_description_textview.text = mapOfLevelDescriptions[newIntValue]
        }
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
            // setting initial status on button
            val buttonText = "${getString(R.string.ocr_local)} (loading)"
            local_ocr_button.text = buttonText
            local_ocr_button.isClickable = false
            local_ocr_button.setBackgroundColor(Color.YELLOW)

            // loading files
            val filesAsBA = loadFilesAsByteArrays()

            // setting progress status on button
            local_ocr_button.setBackgroundColor(Color.BLUE)

            // processing multiple times
            for (i in 1..iterations) {
                // taking random images
                val randomImages = filesAsBA.shuffled().take(packetSize)

                // preparing for monitoring
                var size = 0
                collector.start()

                // every image from random set
                for (j in 1..randomImages.size) {
                    // iteration status
                    val buttonText = "${getString(R.string.ocr_local)} ($i/$iterations)->($j/$packetSize)"
                    local_ocr_button.text = buttonText

                    // taking right image
                    val image = randomImages[j-1]

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

            // setting finish status on button
            local_ocr_button.isClickable = true
            local_ocr_button.text = getString(R.string.ocr_local)
            local_ocr_button.setBackgroundColor(Color.GREEN)
        }.start()
    }

    fun ocrCloud(view: View) {
        // taking params from inputs
        val urlAndPort = url_input.text
        val iterations = iterations_input.text.toString().toInt()
        val packetSize = packet_size_input.text.toString().toInt()

        Thread {
            // setting initial status on button
            val buttonText = "${getString(R.string.ocr_cloud)} (loading)"
            cloud_ocr_button.text = buttonText
            cloud_ocr_button.isClickable= false
            cloud_ocr_button.setBackgroundColor(Color.YELLOW)

            // loading files
            val filesAsBA = loadFilesAsByteArrays()

            // setting progress status on button
            cloud_ocr_button.setBackgroundColor(Color.BLUE)

            // processing multiple times
            for (i in 1..iterations) {
                // taking random images
                val randomImages = filesAsBA.shuffled().take(packetSize)

                // preparing for monitoring
                var size = 0
                collector.start()

                // every image from random set
                for (j in 1..randomImages.size) {
                    // iteration status
                    val buttonText = "${getString(R.string.ocr_cloud)} ($i/$iterations)->($j/$packetSize)"
                    cloud_ocr_button.text = buttonText

                    // taking right image
                    val image = randomImages[j-1]

                    // sending request
                    val response = OwnHttpClient().sendRequestWithBytes(
                        "http://$urlAndPort:8000/upload",
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

            // setting finish status on button
            cloud_ocr_button.isClickable = true
            cloud_ocr_button.text = getString(R.string.ocr_cloud)
            cloud_ocr_button.setBackgroundColor(Color.GREEN)
        }.start()
    }

    private fun decideAboutDelegation(packetSize: Int, securityLevel: Int) : Boolean {
        Log.d("DelegationDecision", "PacketSize=$packetSize, SecurityLevel=$securityLevel")
        // provided args are calculated on button clicked, not anytime like resources usage
        // more metrics can be obtained from ResourcesMonitor, just call "monitor.getX()"
        // @TODO some stuff there about decision
        // false - stay local, true - delegate to cloud
        return true
    }

    fun ocrDecision(view: View) {
        // taking params from inputs
        val urlAndPort = url_input.text
        val iterations = iterations_input.text.toString().toInt()
        val packetSize = packet_size_input.text.toString().toInt()
        val securityLevel = security_level_slider.value.toInt()

        Thread {
            // setting initial status on button
            val buttonText = "${getString(R.string.ocr_decision)} (loading)"
            decision_ocr_button.text = buttonText
            decision_ocr_button.isClickable= false
            decision_ocr_button.setBackgroundColor(Color.YELLOW)

            // loading files
            val filesAsBA = loadFilesAsByteArrays()

            // setting progress status on button
            decision_ocr_button.setBackgroundColor(Color.BLUE)

            // processing multiple times
            for (i in 1..iterations) {
                // taking random images
                val randomImages = filesAsBA.shuffled().take(packetSize)

                // preparing for monitoring
                var size = 0
                collector.start()

                // decision
                val imagesSize = randomImages.stream().mapToInt { it.size }.sum()
                val toDelegate = decideAboutDelegation(imagesSize, securityLevel)

                // every image from random set
                for (j in 1..randomImages.size) {
                    // taking right image
                    val image = randomImages[j-1]

                    if (toDelegate) {
                        // iteration status
                        val buttonText = "${getString(R.string.ocr_decision)} ($i/$iterations)->($j/$packetSize) (cloud)"
                        decision_ocr_button.text = buttonText
                        var secMapper = SecurityMapper(securityLevel)
                        // sending request
                        val result = secMapper.sendWithSecLevel(urlAndPort.toString(), image)
                    } else {
                        // iteration status
                        val buttonText = "${getString(R.string.ocr_decision)} ($i/$iterations)->($j/$packetSize) (local)"
                        decision_ocr_button.text = buttonText

                        // converting into bitmaps and OCRing
                        val bmp = BitmapFactory.decodeByteArray(image, 0, image.size)
                        val resultText = mTessOCR!!.getOCRResult(bmp)

                        if (resultText != "") {
                            Log.d("MainActivity - DecisionOCR (local)", resultText)
                        } else {
                            Log.d("MainActivity - DecisionOCR (local)", "No text found for $i packet and $image")
                        }
                    }


                    // counting processed data size
                    size += image.size
                }

                // end of monitoring
                collector.finish(size)
                collector.save()
            }

            // setting finish status on button
            decision_ocr_button.isClickable = true
            decision_ocr_button.text = getString(R.string.ocr_decision)
            decision_ocr_button.setBackgroundColor(Color.GREEN)
        }.start()
    }
}
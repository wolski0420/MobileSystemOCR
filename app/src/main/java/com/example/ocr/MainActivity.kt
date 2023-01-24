package com.example.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
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
import java.nio.FloatBuffer
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

    private fun decideAboutDelegation(packetSize: Int, securityLevel: Int) : Pair<Boolean, Double> {
        Log.d("DelegationDecision", "PacketSize=$packetSize, SecurityLevel=$securityLevel")

        val inputs = floatArrayOf(
            packetSize.toFloat(), monitor.getNetworkDownloadBandwidth().toFloat(),
            monitor.getNetworkUploadBandwidth().toFloat()
        )

        val ortEnvironment = OrtEnvironment.getEnvironment()
        val ortSessionLocal = createORTSessionlocal(ortEnvironment)
        val outputLocal = runPrediction(inputs, ortSessionLocal, ortEnvironment)
        val ortSessionCloud = createORTSessionCloud(ortEnvironment)
        val outputCloud = runPrediction(inputs, ortSessionCloud, ortEnvironment)

        Log.d("DelegationDecision", "Local-time: " + outputLocal.toString() + "; Cloud-time: " + outputCloud.toString())


        val timeRatio = outputLocal / outputCloud
        var time_ax = 2
        var battery_ax = 2
        var ram_ax = 2
        var safetyLevel_ax = 2

        if (timeRatio < 1) {
            time_ax = 5
        } else if (timeRatio >= 1 && timeRatio < 1.5) {
            time_ax = 4
        } else if (timeRatio >= 1.5 && timeRatio < 2) {
            time_ax = 3
        } else {
            time_ax = 2
        }

        val battery = iterations_input.text.toString().toInt()
        if (battery > 75) {
            battery_ax = 5
        } else if (battery >= 50) {
            battery_ax = 4
        } else if (battery >= 25) {
            battery_ax = 3
        } else {
            battery_ax = 2
        }

        val ram = packet_size_input.text.toString().toInt()
        if (ram > 75) {
            ram_ax = 2
        } else if (ram >= 50) {
            ram_ax = 3
        } else if (ram >= 25) {
            ram_ax = 4
        } else {
            ram_ax = 5
        }


        if (securityLevel == 7 || securityLevel == 8) {
            safetyLevel_ax = 5
        } else if (securityLevel == 6 || securityLevel == 5) {
            safetyLevel_ax = 4
        } else if (securityLevel == 4 || securityLevel == 3) {
            safetyLevel_ax = 3
        } else {
            safetyLevel_ax = 2
        }


        var weighted_prediction =
            0.15 * time_ax + battery_ax * 0.2 + ram_ax * 0.2 + safetyLevel_ax * 0.45
        weighted_prediction *= 20
        Log.d("DelegationDecision", weighted_prediction.toString())
        return Pair(weighted_prediction < 60, weighted_prediction)
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
                val toDelegate = decideAboutDelegation(imagesSize, securityLevel).first

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

    // Create an OrtSession with the given OrtEnvironment
    private fun createORTSessionlocal( ortEnvironment: OrtEnvironment) : OrtSession {
        val modelBytes = resources.openRawResource( R.raw.local_model ).readBytes()
        return ortEnvironment.createSession( modelBytes )
    }


    private fun createORTSessionCloud( ortEnvironment: OrtEnvironment) : OrtSession {
        val modelBytes = resources.openRawResource( R.raw.cloud_model ).readBytes()
        return ortEnvironment.createSession( modelBytes )
    }
    // Make predictions with given inputs
    private fun runPrediction( input : FloatArray , ortSession: OrtSession , ortEnvironment: OrtEnvironment ) : Float {
        // Get the name of the input node
        val inputName = ortSession.inputNames?.iterator()?.next()
        // Make a FloatBuffer of the inputs
        val floatBufferInputs = FloatBuffer.wrap( input)
        // Create input tensor with floatBufferInputs of shape ( 1 , 1 )
        val inputTensor = OnnxTensor.createTensor( ortEnvironment , floatBufferInputs , longArrayOf( 1, 3 ) )
        // Run the model
        val results = ortSession.run( mapOf( inputName to inputTensor ) )
        // Fetch and return the results
        val output = results[0].value as Array<FloatArray>
        return output[0][0]
    }

    fun testDecision(view: View) {
        // taking security level and setting some params
        val secLevels = 8

        Thread {
            // setting initial status on button
            val buttonText = "${getString(R.string.test_decision)} (loading)"
            test_decision_button.text = buttonText
            test_decision_button.isClickable= false
            test_decision_button.setBackgroundColor(Color.YELLOW)

            // loading files
            val filesAsBA = loadFilesAsByteArrays()

            // setting progress status on button
            test_decision_button.setBackgroundColor(Color.BLUE)

            // processing multiple times
            for (level in 1..secLevels) {
                // taking random images
                val randomImages = filesAsBA.shuffled().take(50)

                // decision
                val imagesSize = randomImages.stream().mapToInt { it.size }.sum()
                val toDelegate = decideAboutDelegation(imagesSize, level)

                // saving results
                csvService.saveDecisionsToFile(listOf(toDelegate.first, toDelegate.second))
            }

            // setting finish status on button
            test_decision_button.isClickable = true
            test_decision_button.text = getString(R.string.test_decision)
            test_decision_button.setBackgroundColor(Color.GREEN)
        }.start()
    }
}
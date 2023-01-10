package com.example.ocr

import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    private var ocrScan = OcrScan()
    private var csvWriter = CsvWriter()
    private var fileName = "data.csv"
    private var isUpdating = false
    var language = "eng"
    private var mTessOCR: TesseractOCR? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addUpdatesToRepeater()
        mTessOCR = TesseractOCR(this, language)
    }

    override fun onDestroy() {
        // may be needed when forcing app close
        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(1)
        super.onDestroy()
    }

    private fun getBatteryLevel () : Long {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isBatteryCharging () : Boolean {
        val batteryBroadcast = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) as Intent
        return batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0
    }

    private  fun getRAMInfo () : ActivityManager.MemoryInfo {
        val memoryManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfoResult = ActivityManager.MemoryInfo()
        memoryManager.getMemoryInfo(memoryInfoResult)

        return memoryInfoResult
    }

    private fun getRAMTotal () : Double {
        return getRAMInfo().totalMem / 1000000000.0
    }

    private fun getRAMUsed () : Double {
        val ramInfo = getRAMInfo()
        return (ramInfo.totalMem - ramInfo.availMem) / 1000000000.0
    }

    private fun getNetworkDownloadBandwidth () : Double {
        val networkManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = networkManager.getNetworkCapabilities(networkManager.activeNetwork)
        return networkCapabilities?.linkDownstreamBandwidthKbps?.div(8000.0) ?: 0.0
    }

    private fun getNetworkUploadBandwidth () : Double {
        val networkManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = networkManager.getNetworkCapabilities(networkManager.activeNetwork)
        return networkCapabilities?.linkUpstreamBandwidthKbps?.div(8000.0) ?: 0.0
    }

    private fun getNetworkType () : String {
        val networkManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = networkManager.activeNetworkInfo
        return if (networkInfo != null && networkInfo.isConnected) {
            when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            "NotConnected"
        }
    }

    private fun getCurrentTimeInMillis () : Long {
        return SystemClock.elapsedRealtime()
    }

    private fun addUpdatesToRepeater() {
        // battery info
        UpdatesRepeater.addAtomicUpdate {
            battery_percentage_text_view.text = String.format("%d %%", getBatteryLevel())
            battery_charging_text_view.text = if (isBatteryCharging()) "Yes" else "No"
        }

        // RAM info
        UpdatesRepeater.addAtomicUpdate {
            val divider = 100.0 * getRAMUsed() / getRAMTotal()
            ram_usage_text_view.text = String.format("%.2f GB / %.2f GB (%.2f %%)",
                getRAMUsed(), getRAMTotal(), divider)
        }

        // network info
        UpdatesRepeater.addAtomicUpdate {
            download_text_view.text = String.format("%.2f MB/s", getNetworkDownloadBandwidth())
            upload_text_view.text = String.format("%.2f MB/s", getNetworkUploadBandwidth())
            network_type_text_view.text = getNetworkType()
        }
    }

    fun startOrStopUpdating(view: View) {
        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

        if (isUpdating) {
            // if already updating, button action should stop it
            jobScheduler.cancel(1)

            updateButton.text = resources.getString(R.string.start_scheduler)
            isUpdating = false
        } else {
            // if not updating, button action should start it
            val jobInfo = JobInfo.Builder(1, ComponentName(this, UpdatesRepeater::class.java))
                .setOverrideDeadline(0)
                .setPersisted(true)
                .build()

            if (jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS) {
                Log.i("MainActivity", "Values are being updated, scheduler succeeded!")
            } else {
                Log.i("MainActivity", "Values are not being updated, scheduler failed!")
            }

            updateButton.text = resources.getString(R.string.stop_scheduler)
            isUpdating = true
        }
    }

    private fun saveToFile (line: List<Any>) {
        val completePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName

        if (!File(completePath).exists()) {
            csvWriter.open(completePath, append = true) {
                writeRow(listOf("Time", "Battery", "RAM", "DownloadBandwidth", "UploadBandwidth", "NetworkType"))
            }
        }

        csvWriter.open(completePath, append = true) {
            writeRow(line)
        }
        Log.d("CSV", "Updated $fileName with new deltas, here is the complete path: $completePath")
    }

    fun calculateDeltaAndSave(view: View) {
        val batteryStart = getBatteryLevel()
        val ram = getRAMUsed()
        var timeStart = getCurrentTimeInMillis()

        // here we have OCR process
        SystemClock.sleep(5000)

        val timeEnd = getCurrentTimeInMillis()
        val batteryEnd = getBatteryLevel()

        val batteryDiff = batteryStart - batteryEnd
        val timeDiff = timeEnd - timeStart

        Log.d("CSV", "Delta's have been calculated!")

        saveToFile(listOf(timeDiff, batteryDiff, ram,
            getNetworkDownloadBandwidth(),
            getNetworkUploadBandwidth(),
            getNetworkType())
        )
    }

    private fun sendPost (filePath: String) {
//        val url = URL("http://ec2-54-237-44-168.compute-1.amazonaws.com/upload")
//
//        with(url.openConnection() as HttpURLConnection) {
//            requestMethod = "POST"
//
//            println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")
//
//            inputStream.bufferedReader().use {
//                it.lines().forEach { line ->
//                    println(line)
//                }
//            }
//        }
    }

    fun ocrLocal(view: View) {
        val res = resources as Resources
        var size = 0;
        val ids = listOf(R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane)
        var resultText = ""

        for (i in 1..3) {
            size = 0
            val batteryStart = getBatteryLevel()
            val ram = getRAMUsed()
            val timeStart = getCurrentTimeInMillis()
            for (j in 0..i) {

                var bmp = BitmapFactory.decodeResource(res, ids[j])
                size += bmp.byteCount
                resultText = mTessOCR!!.getOCRResult(bmp)
                if (resultText != null && resultText != "") {
                    println(resultText)
                }else
                    println("No text found ERROR" + i.toString() + " " + j.toString())
                }

            getMeasurmestsAfterAndSave(view, batteryStart, timeStart, ram)

            }

        }






    fun getMeasurmestsAfterAndSave(view: View, batteryStart: Long, timeStart: Long, ram:Double) {
        val timeEnd = getCurrentTimeInMillis()
        val batteryEnd = getBatteryLevel()

        val batteryDiff = batteryStart - batteryEnd
        var timeDiff = timeEnd - timeStart

        Log.d("CSV", "Delta's have been calculated!")

        saveToFile(listOf(timeDiff, batteryDiff, ram,
            getNetworkDownloadBandwidth(),
            getNetworkUploadBandwidth(),
            getNetworkType())
        )
    }


    fun ocrCloud(view: View) {
//        val values = mapOf("name" to "John Doe", "occupation" to "gardener")
//
//        val objectMapper = ObjectMapper()
//        val requestBody: String = objectMapper
//            .writeValueAsString(values)
//
//        val client = HttpClient.newBuilder().build();
//        val request = HttpRequest.newBuilder()
//            .uri(URI.create("https://httpbin.org/post"))
//            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//            .build()
//        val response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        println(response.body())


    }
}
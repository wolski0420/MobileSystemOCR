package com.example.ocr

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var monitor: ResourcesMonitor
    private lateinit var csvService: CSVFileService
    private lateinit var collector: ResultDataCollector
    private var isUpdating = false
    private var language = "eng"
    private var mTessOCR: TesseractOCR? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTessOCR = TesseractOCR(this, language)
        monitor = ResourcesMonitor(this)
        csvService = CSVFileService(this)
        collector = ResultDataCollector(monitor, csvService)
        addUpdatesToRepeater()
    }

    override fun onDestroy() {
        // may be needed when forcing app close
        (getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(1)

        super.onDestroy()
    }

    private fun addUpdatesToRepeater() {
        // battery info
        UpdatesRepeater.addAtomicUpdate {
            battery_percentage_text_view.text = String.format("%d %%", monitor.getBatteryLevel())
            battery_charging_text_view.text = if (monitor.isBatteryCharging()) "Yes" else "No"
        }

        // RAM info
        UpdatesRepeater.addAtomicUpdate {
            val divider = 100.0 * monitor.getRAMUsed() / monitor.getRAMTotal()
            ram_usage_text_view.text = String.format("%.2f GB / %.2f GB (%.2f %%)",
                monitor.getRAMUsed(), monitor.getRAMTotal(), divider)
        }

        // network info
        UpdatesRepeater.addAtomicUpdate {
            download_text_view.text = String.format("%.2f MB/s", monitor.getNetworkDownloadBandwidth())
            upload_text_view.text = String.format("%.2f MB/s", monitor.getNetworkUploadBandwidth())
            network_type_text_view.text = monitor.getNetworkType()
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

    fun calculateDeltaAndSave(view: View) {
        collector.start()

        // here we have OCR process
        SystemClock.sleep(5000)

        collector.finish(-1)
        collector.save()
    }

    fun ocrLocal(view: View) {
        val res = resources as Resources
        var size: Int
        val ids = listOf(R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane, R.drawable.pobrane)
        var resultText: String

        for (i in 1..3) {
            size = 0
            collector.start()

            for (j in 0..i) {
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
    }

    fun ocrCloud(view: View) {
        // @TODO
    }
}
package com.example.ocr

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_resources.*

class ResourcesActivity : AppCompatActivity() {
    private lateinit var monitor: ResourcesMonitor
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resources)

        monitor = ResourcesMonitor(this)
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

    fun goBackToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
package com.example.ocr

import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addUpdatesToRepeater()
    }

    override fun onDestroy() {
        // may be needed when forcing app close
        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(1)
        super.onDestroy()
    }

    private fun getBatteryLevel () : Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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
}
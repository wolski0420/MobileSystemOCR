package com.example.ocr

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.SystemClock

class ResourcesMonitor(private var activity: MainActivity) {

    fun getBatteryLevel () : Long {
        val batteryManager = activity.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isBatteryCharging () : Boolean {
        val batteryBroadcast = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) as Intent
        return batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0
    }

    fun getRAMInfo () : ActivityManager.MemoryInfo {
        val memoryManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfoResult = ActivityManager.MemoryInfo()
        memoryManager.getMemoryInfo(memoryInfoResult)

        return memoryInfoResult
    }

    fun getRAMTotal () : Double {
        return getRAMInfo().totalMem / 1000000000.0
    }

    fun getRAMUsed () : Double {
        val ramInfo = getRAMInfo()
        return (ramInfo.totalMem - ramInfo.availMem) / 1000000000.0
    }

    fun getNetworkDownloadBandwidth () : Double {
        val networkManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = networkManager.getNetworkCapabilities(networkManager.activeNetwork)
        return networkCapabilities?.linkDownstreamBandwidthKbps?.div(8000.0) ?: 0.0
    }

    fun getNetworkUploadBandwidth () : Double {
        val networkManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = networkManager.getNetworkCapabilities(networkManager.activeNetwork)
        return networkCapabilities?.linkUpstreamBandwidthKbps?.div(8000.0) ?: 0.0
    }

    fun getNetworkType () : String {
        val networkManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    fun getCurrentTimeInMillis () : Long {
        return SystemClock.elapsedRealtime()
    }
}
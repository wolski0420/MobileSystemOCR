package com.example.ocr

import android.util.Log

class ResultDataCollector (
    private var monitor: ResourcesMonitor,
    private var csvService: CSVFileService
    ) {
    private var batteryDiff = 0L
    private var ram = 0.0
    private var timeDiff = 0L
    private var imageSize = 0

    fun start() {
        batteryDiff = monitor.getBatteryLevel()
        ram = monitor.getRAMUsed()
        timeDiff = monitor.getCurrentTimeInMillis()

        Log.d("ResultDataCollector", "Took initial values, started calculating differences")
    }

    fun finish(providedSize: Int) {
        batteryDiff -= monitor.getBatteryLevel()
        timeDiff = monitor.getCurrentTimeInMillis() - timeDiff
        imageSize = providedSize

        Log.d("ResultDataCollector", "Took final values, calculated differences")
    }

    fun save() {
        csvService.saveToFile(listOf(
            imageSize, timeDiff, batteryDiff, ram,
            monitor.getNetworkDownloadBandwidth(),
            monitor.getNetworkUploadBandwidth(),
            monitor.getNetworkType()
        ))

        Log.d("ResultDataCollector", "Saved all deltas to csv file")
    }
}
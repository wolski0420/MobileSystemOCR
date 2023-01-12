package com.example.ocr

import android.os.Environment
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import java.io.File

class CSVFileService(private var activity: MainActivity) {
    private var csvWriter = CsvWriter()
    private var fileName = "data.csv"

    fun saveToFile (line: List<Any>) {
        val completePath = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName

        if (!File(completePath).exists()) {
            csvWriter.open(completePath, append = true) {
                writeRow(listOf("Size", "Time", "Battery", "RAM", "DownloadBandwidth", "UploadBandwidth", "NetworkType"))
            }
        }
        csvWriter.open(completePath, append = true) {
            writeRow(line)
        }

        Log.d("CSVFileService", "Updated $fileName with new deltas, here is the complete path: $completePath")
    }
}
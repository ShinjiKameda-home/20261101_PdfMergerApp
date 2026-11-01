package com.example.pdfmerger

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val selectedUriList = mutableListOf<Uri>()
    private lateinit var btnMerge: Button

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.size >= 2) {
            selectedUriList.clear()
            selectedUriList.addAll(uris)

            btnMerge.visibility = View.VISIBLE
            btnMerge.text = "${uris.size}個のファイルを結合する"

            Toast.makeText(this, "${uris.size}個のファイルが選択されました", Toast.LENGTH_SHORT).show()
        } else {
            selectedUriList.clear()
            btnMerge.visibility = View.GONE
            Toast.makeText(this, "結合するには2つ以上のPDFファイルを選択してください", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelect = findViewById<Button>(R.id.btnSelect)
        btnMerge = findViewById<Button>(R.id.btnMerge)

        btnSelect.setOnClickListener {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }

        btnMerge.setOnClickListener {
            if (selectedUriList.size >= 2) {
                mergeSelectedPdfs(selectedUriList)
            }
        }
    }

    private fun mergeSelectedPdfs(uris: List<Uri>) {
        try {
            data class PdfItem(val name: String, val file: File)
            val tempFiles = mutableListOf<PdfItem>()

            for ((index, uri) in uris.withIndex()) {
                val fileName = getFileName(uri) ?: "document_$index.pdf"
                val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}_$index.pdf")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFiles.add(PdfItem(fileName, tempFile))
            }

            // ファイル名で昇順ソート
            val sortedFiles = tempFiles.sortedBy { it.name }.map { it.file }

            // ★ 1. 一度キャッシュ内に一時的な出力ファイルを作成する
            val tempOutputFile = File(cacheDir, "merged_temp.pdf")

            val merger = PDFMergerUtility().apply {
                destinationFileName = tempOutputFile.absolutePath
                sortedFiles.forEach { file ->
                    addSource(file)
                }
            }

            merger.mergeDocuments(null)

            // ★ 2. キャッシュにあるファイルを MediaStore を経由して Download フォルダに保存する
            val saved = savePdfToDownloads(tempOutputFile)

            if (saved) {
                Toast.makeText(
                    this,
                    "結合が完了しました！\n保存先: Download フォルダ (merged_output.pdf)",
                    Toast.LENGTH_LONG
                ).show()
                // 結合ボタンを再び隠す
                btnMerge.visibility = View.GONE
            } else {
                Toast.makeText(this, "Downloadフォルダへの保存に失敗しました", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "結合エラー: ${e.javaClass.simpleName} - ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ★ 3. DownloadフォルダへPDFを保存するヘルパー関数
    private fun savePdfToDownloads(sourceFile: File): Boolean {
        return try {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                // ファイル名（同名がすでにある場合の競合を防ぐため、タイムスタンプを付与するか固定にする）
                put(MediaStore.Downloads.DISPLAY_NAME, "merged_output_${System.currentTimeMillis()}.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}
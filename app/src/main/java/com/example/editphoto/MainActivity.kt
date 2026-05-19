package com.example.editphoto

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import java.io.OutputStream
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions.SubjectResultOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var btnSelectImage: Button
    private lateinit var btnProcess: Button
    private lateinit var btnSave: Button
    private lateinit var stickerContainer: LinearLayout

    private var selectedImageUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var stickerBitmaps = mutableListOf<Bitmap>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            try {
                selectedImageUri?.let { uri ->
                    originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(this.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    }
                    
                    // Chuyển sang chuẩn ARGB_8888 để hỗ trợ xử lý đồ hoạ tốt nhất
                    originalBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                    stickerContainer.removeAllViews()
                    
                    val iv = ImageView(this@MainActivity)
                    iv.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    iv.adjustViewBounds = true
                    iv.setImageBitmap(originalBitmap)
                    stickerContainer.addView(iv)
                    
                    btnProcess.isEnabled = true
                    btnSave.isEnabled = false
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnProcess = findViewById(R.id.btnProcess)
        btnSave = findViewById(R.id.btnSave)
        stickerContainer = findViewById(R.id.stickerContainer)

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        btnProcess.setOnClickListener {
            originalBitmap?.let { bitmap ->
                processImage(bitmap)
            }
        }
        
        btnSave.setOnClickListener {
            if (stickerBitmaps.isNotEmpty()) {
                var successCount = 0
                for (bitmap in stickerBitmaps) {
                    if (saveImageToGallery(bitmap)) successCount++
                }
                Toast.makeText(this, "Đã lưu $successCount sticker vào Thư viện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        btnProcess.isEnabled = false
        Toast.makeText(this, "Đang phân tích các chủ thể...", Toast.LENGTH_SHORT).show()

        val options = SubjectSegmenterOptions.Builder()
            .enableMultipleSubjects(
                SubjectResultOptions.Builder()
                    .enableSubjectBitmap()
                    .build()
            )
            .build()

        val segmenter = SubjectSegmentation.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        segmenter.process(image)
            .addOnSuccessListener { result ->
                stickerBitmaps.clear()
                stickerContainer.removeAllViews()

                val subjects = result.subjects
                if (subjects.isNotEmpty()) {
                    for (subject in subjects) {
                        val subjectBitmap = subject.bitmap
                        if (subjectBitmap != null) {
                            val borderedBitmap = addWhiteBorderToBitmap(subjectBitmap)
                            val sticker = createSticker(borderedBitmap)
                            stickerBitmaps.add(sticker)
                            
                            val iv = ImageView(this).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT
                                ).apply { setMargins(16, 0, 16, 0) }
                                adjustViewBounds = true
                                setImageBitmap(sticker)
                            }
                            stickerContainer.addView(iv)
                        }
                    }
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Hoàn tất! Tìm thấy ${subjects.size} người.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Không tìm thấy chủ thể trong ảnh", Toast.LENGTH_SHORT).show()
                }
                btnProcess.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                btnProcess.isEnabled = true
            }
    }

    private fun addWhiteBorderToBitmap(foreground: Bitmap): Bitmap {
        val maxDim = Math.max(foreground.width, foreground.height)
        val strokeWidth = (maxDim / 40f).coerceAtLeast(10f).coerceAtMost(60f)
        
        val newWidth = foreground.width + (strokeWidth * 2).toInt()
        val newHeight = foreground.height + (strokeWidth * 2).toInt()
        
        val outBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        
        val alphaBitmap = foreground.extractAlpha()
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val cx = strokeWidth
        val cy = strokeWidth
        
        val steps = 36
        for (i in 0 until steps) {
            val angle = i * 2 * Math.PI / steps
            val dx = cx + (Math.cos(angle) * strokeWidth).toFloat()
            val dy = cy + (Math.sin(angle) * strokeWidth).toFloat()
            canvas.drawBitmap(alphaBitmap, dx, dy, paint)
        }
        
        canvas.drawBitmap(foreground, cx, cy, null)
        
        return outBitmap
    }

    private fun createSticker(bitmap: Bitmap): Bitmap {
        val maxSize = 512
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val ratio = width.toFloat() / height.toFloat()
        val finalWidth: Int
        val finalHeight: Int
        
        if (width > height) {
            finalWidth = maxSize
            finalHeight = (maxSize / ratio).toInt()
        } else {
            finalHeight = maxSize
            finalWidth = (maxSize * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun saveImageToGallery(bitmap: Bitmap): Boolean {
        val filename = "Sticker_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        try {
            val resolver = contentResolver
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri)
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}

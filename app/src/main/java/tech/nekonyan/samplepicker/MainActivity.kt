package tech.nekonyan.samplepicker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import tech.nekonyan.samplepicker.Helper.CAMERA_CODE
import tech.nekonyan.samplepicker.Helper.DOCUMENT_CODE
import tech.nekonyan.samplepicker.Helper.GALLERY_CODE
import tech.nekonyan.samplepicker.Helper.checkPermission
import tech.nekonyan.samplepicker.Helper.copyUriToFile
import tech.nekonyan.samplepicker.Helper.getFilesListener
import tech.nekonyan.samplepicker.Helper.openCamera
import tech.nekonyan.samplepicker.Helper.openFilePicker
import tech.nekonyan.samplepicker.Helper.requestPermission

class MainActivity : AppCompatActivity() {
    private var cameraResult: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCamera = findViewById<Button>(R.id.btnCamera)
        val btnGallery = findViewById<Button>(R.id.btnGallery)
        val btnDocument = findViewById<Button>(R.id.btnDocument)

        val cameraListener = getFilesListener(CAMERA_CODE) {
            val file = copyUriToFile(cameraResult, 0, true)
            Log.d("DebugPicker", "Camera result: ${file?.path}")
        }
        val galleryListener = getFilesListener(GALLERY_CODE)
        val documentListener = getFilesListener(DOCUMENT_CODE)

        btnCamera.setOnClickListener {
            cameraResult = openCamera(cameraListener)
        }

        btnGallery.setOnClickListener {
            if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 10)
                    type = "image/jpeg"
                }
                galleryListener.launch(intent)
            } else {
                galleryListener.openFilePicker(GALLERY_CODE)
            }
        }

        btnDocument.setOnClickListener {
            documentListener.openFilePicker(DOCUMENT_CODE)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!checkPermission()) {
            requestPermission()
            return
        }
    }
}
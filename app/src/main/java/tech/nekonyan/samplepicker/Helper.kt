package tech.nekonyan.samplepicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object Helper {
    private const val PERMISSION_ALL = 1

    const val CAMERA_CODE = 1
    const val GALLERY_CODE = 2
    const val DOCUMENT_CODE = 3

    private const val JPG_ONLY = "image/jpeg"
    private const val PDF_ONLY = "application/pdf"
    private const val ALL_IMAGE_ONLY = "image/*"
    private const val ALL_FILE_TYPE = "*/*"

    private val PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    @RequiresApi(33)
    private val PERMISSIONS_13 = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.CAMERA
    )

    fun Context.checkPermission(): Boolean {
        return if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.hasPermissions(*PERMISSIONS_13)
        } else {
            this.hasPermissions(*PERMISSIONS)
        }
    }

    fun Context.hasPermissions(vararg permissions: String): Boolean {
        if (permissions.isNotEmpty()) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    fun Activity.requestPermission() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_13,
                PERMISSION_ALL
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                PERMISSION_ALL
            )
        }
    }

    fun Activity.openCamera(launcher: ActivityResultLauncher<Intent>): Uri? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            createImageFileInAppDir()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            return null
        }

        var cameraResultUri: Uri? = null
        photoFile?.also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            cameraResultUri = photoURI
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        launcher.launch(intent)
        return cameraResultUri
    }

    fun Context.copyUriToFile(uri: Uri?, index: Int = 0, isImage: Boolean = true): File? {
        return try {
            if (uri == null) {
                return null
            }

            val stream = contentResolver.openInputStream(uri)
            val file =
                if (isImage) createImageFileInAppDir(index) else createDocumentFileInAppDir(index)
            FileUtils.copyInputStreamToFile(stream, file)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(33)
    fun ActivityResultLauncher<Intent>.openGalleryPickerAndroid13() {
        val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 10)
            type = JPG_ONLY
        }

        launch(intent)
    }

    fun ActivityResultLauncher<Intent>.openFilePicker(code: Int) {
        val fileType = when (code) {
            GALLERY_CODE -> JPG_ONLY
            DOCUMENT_CODE -> PDF_ONLY
            else -> ALL_FILE_TYPE
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
            type = fileType
        }

        launch(intent)
    }

    fun ComponentActivity.getFilesListener(
        code: Int,
        listener: ((ActivityResult) -> Unit)? = null
    ): ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val prefix = when (code) {
                GALLERY_CODE -> "Gallery"
                DOCUMENT_CODE -> "Document"
                else -> ""
            }
            val data = result?.data
            val clipData = data?.clipData
            listener?.invoke(result)
            if (data != null && result.resultCode == AppCompatActivity.RESULT_OK) {
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        val file = copyUriToFile(uri, i, code != DOCUMENT_CODE)
                        Log.d("DebugPicker", "$prefix$i: Uri - ${file?.path}")
                    }
                } else {
                    val uri = data.data
                    val file = copyUriToFile(uri, 0, code != DOCUMENT_CODE)
                    Log.d("DebugPicker", "$prefix: Uri - ${file?.path}")
                }
            }
        }

    private fun Context.createFileInAppDir(index: Int = 0, code: Int): File = when (code) {
        DOCUMENT_CODE -> createDocumentFileInAppDir(index)
        else -> createImageFileInAppDir(index)
    }

    private fun Context.createDocumentFileInAppDir(index: Int = 0): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imagePath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(imagePath, "Document_${timeStamp}_${index}.pdf")
    }

    private fun Context.createImageFileInAppDir(index: Int = 0): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(imagePath, "Image_${timeStamp}_${index}.jpg")
    }
}
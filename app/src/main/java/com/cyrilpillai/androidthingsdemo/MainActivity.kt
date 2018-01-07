package com.cyrilpillai.androidthingsdemo

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by cyril on 7/1/18.
 */
class MainActivity : Activity() {

    private val cameraHelper by lazy { CameraHelper.instance }

    private val cameraThread by lazy {
        HandlerThread("CameraBackground")
    }
    private val cameraHandler by lazy {
        Handler(cameraThread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No permission")
            return
        }

        cameraThread.start()
        cameraHelper.initializeCamera(context = this, backgroundHandler = cameraHandler,
                imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
                    val image = reader.acquireLatestImage()
                    val imageBuf = image.planes[0].buffer
                    val imageBytes = ByteArray(imageBuf.remaining())
                    imageBuf.get(imageBytes)
                    image.close()
                    onPictureTaken(imageBytes)
                })

        btnCaptureImage.setOnClickListener { cameraHelper.takePicture() }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper.shutDown()
        cameraThread.quitSafely()
    }

    private fun onPictureTaken(imageBytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        runOnUiThread { imCameraImage.setImageBitmap(bitmap) }
    }
}

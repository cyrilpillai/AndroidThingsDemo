package com.cyrilpillai.androidthingsdemo

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.view.Surface

/**
 * Created by cyril on 7/1/18.
 */
class CameraHelper private constructor() {

    private object Holder {
        val INSTANCE = CameraHelper()
    }

    companion object {
        val instance: CameraHelper by lazy { Holder.INSTANCE }
        private val IMAGE_WIDTH = 320
        private val IMAGE_HEIGHT = 240
        private val MAX_IMAGES = 1
        private val TAG = CameraHelper::class.java.simpleName
    }

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    private val imageReader by lazy {
        ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, MAX_IMAGES)
    }

    fun initializeCamera(context: Context, backgroundHandler: Handler,
                         imageAvailableListener: ImageReader.OnImageAvailableListener) {
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        var camIds: Array<out String> = emptyArray()

        try {
            camIds = cameraManager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Cam access exception getting IDs", e)
        }

        if (camIds.isEmpty()) {
            Log.d(TAG, "No cameras found")
            return
        }

        val selectedCameraId = camIds[0]
        Log.d(TAG, "Using camera id " + selectedCameraId)
        imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

        try {
            cameraManager.openCamera(selectedCameraId, stateCallback, backgroundHandler)
        } catch (cae: CameraAccessException) {
            Log.d(TAG, "Camera Access Exception", cae)
        }

    }

    fun takePicture() {
        if (cameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.")
            return
        }

        try {
            cameraDevice?.createCaptureSession(
                    listOf<Surface>(imageReader.getSurface()),
                    sessionCallback,
                    null)
        } catch (cae: CameraAccessException) {
            Log.d(TAG, "Camera Access Exception while preparing pic", cae)
        }

    }

    fun shutDown() {
        cameraDevice?.close()
    }

    private val stateCallback by lazy {
        object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice?) {
                this@CameraHelper.cameraDevice = cameraDevice
            }

            override fun onClosed(cameraDevice: CameraDevice?) {
                this@CameraHelper.cameraDevice = null
            }

            override fun onDisconnected(cameraDevice: CameraDevice?) {
                cameraDevice?.close()
            }

            override fun onError(cameraDevice: CameraDevice?, i: Int) {
                cameraDevice?.close()
            }
        }
    }

    private val sessionCallback by lazy {
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession?) {
                Log.w(TAG, "Failed to configure camera")
            }

            override fun onConfigured(cameraCaptureSession: CameraCaptureSession?) {
                if (cameraDevice == null) return
                this@CameraHelper.cameraCaptureSession = cameraCaptureSession
                triggerImageCapture()
            }
        }
    }

    private fun triggerImageCapture() {
        try {
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(imageReader.surface)
            captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            Log.d(TAG, "Session initialized.")
            cameraCaptureSession?.capture(captureBuilder?.build(), captureCallback, null)

        } catch (cae: CameraAccessException) {
            Log.d(TAG, "Camera Access Exception", cae)
        }
    }

    private val captureCallback by lazy {
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureProgressed(session: CameraCaptureSession?,
                                             request: CaptureRequest?,
                                             partialResult: CaptureResult?) {
                Log.d(TAG, "Partial result")
            }

            override fun onCaptureCompleted(session: CameraCaptureSession?,
                                            request: CaptureRequest?,
                                            result: TotalCaptureResult?) {
                Log.d(TAG, "CaptureSession closed")
                session?.close()
                cameraCaptureSession = null
            }
        }
    }
}
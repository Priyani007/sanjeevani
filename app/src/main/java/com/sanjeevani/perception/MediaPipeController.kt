package com.sanjeevani.perception

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.sanjeevani.model.NormalizedLandmark
import com.sanjeevani.model.PerceptionFrame

private const val TAG = "MediaPipeController"

class MediaPipeController(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null
    private var handLandmarker: HandLandmarker? = null
    private var faceLandmarker: FaceLandmarker? = null

    @Volatile private var latestPoseResult: PoseLandmarkerResult? = null
    @Volatile private var latestHandResult: HandLandmarkerResult? = null
    @Volatile private var latestFaceResult: FaceLandmarkerResult? = null

    fun initialize() {
        initPose()
        initHands()
        initFace()
    }

    private fun buildBaseOptions(modelAsset: String): BaseOptions {
        // Try GPU delegate (Snapdragon Adreno/NPU acceleration) — fall back to CPU if unavailable
        return try {
            BaseOptions.builder()
                .setModelAssetPath(modelAsset)
                .setDelegate(Delegate.GPU)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate unavailable for $modelAsset, falling back to CPU: ${e.message}")
            BaseOptions.builder()
                .setModelAssetPath(modelAsset)
                .build()
        }
    }

    private fun initPose() {
        try {
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(buildBaseOptions("pose_landmarker_heavy.task"))
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(2)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputSegmentationMasks(false)
                .setResultListener { result, _ -> latestPoseResult = result }
                .build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init pose landmarker: ${e.message}")
        }
    }

    private fun initHands() {
        try {
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(buildBaseOptions("hand_landmarker.task"))
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ -> latestHandResult = result }
                .build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init hand landmarker: ${e.message}")
        }
    }

    private fun initFace() {
        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(buildBaseOptions("face_landmarker.task"))
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(2)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputFaceBlendshapes(false)
                .setResultListener { result, _ -> latestFaceResult = result }
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init face landmarker: ${e.message}")
        }
    }

    fun processFrame(bitmap: Bitmap, timestampMs: Long, width: Int, height: Int): PerceptionFrame {
        val mpImage = BitmapImageBuilder(bitmap).build()

        poseLandmarker?.detectAsync(mpImage, timestampMs)
        handLandmarker?.detectAsync(mpImage, timestampMs)
        faceLandmarker?.detectAsync(mpImage, timestampMs)

        // LIVE_STREAM results arrive asynchronously — read the most recent snapshot
        // Never relabel cached inference as a new observation. Ambiguous multi-person
        // scenes require reframing; selecting first() silently can swap patient/rescuer.
        val poseResult = latestPoseResult?.takeIf {
            timestampMs - it.timestampMs() in 0..300 && it.landmarks().size == 1
        }
        val handResult = latestHandResult?.takeIf {
            timestampMs - it.timestampMs() in 0..250
        }
        val faceResult = latestFaceResult?.takeIf { timestampMs - it.timestampMs() in 0..300 }

        // 2D normalized screen landmarks
        val poseLandmarks = poseResult?.landmarks()
            ?.firstOrNull()
            ?.map { lm -> NormalizedLandmark(lm.x(), lm.y(), lm.z(), lm.visibility().orElse(1f)) }

        // 3D world landmarks: metric coordinates (meters), origin at hip midpoint, Y=up, Z=toward camera
        val worldLandmarks = poseResult?.worldLandmarks()
            ?.firstOrNull()
            ?.map { lm -> NormalizedLandmark(lm.x(), lm.y(), lm.z(), lm.visibility().orElse(1f)) }

        // 478 face mesh landmarks (normalized screen coords)
        val faceLandmarks = faceResult?.faceLandmarks()
            ?.firstOrNull()
            ?.map { lm -> NormalizedLandmark(lm.x(), lm.y(), lm.z()) }

        var leftHandLandmarks: List<NormalizedLandmark>? = null
        var rightHandLandmarks: List<NormalizedLandmark>? = null
        handResult?.let { hr ->
            for (i in hr.handednesses().indices) {
                val handedness = hr.handednesses()[i].firstOrNull()?.categoryName() ?: continue
                val landmarks = hr.landmarks()[i].map { lm -> NormalizedLandmark(lm.x(), lm.y(), lm.z()) }
                // MediaPipe reports from camera perspective (mirrored): "Left" = user's right hand
                if (handedness == "Left") rightHandLandmarks = landmarks
                else leftHandLandmarks = landmarks
            }
        }

        return PerceptionFrame(
            timestamp = timestampMs,
            poseLandmarks = poseLandmarks,
            leftHandLandmarks = leftHandLandmarks,
            rightHandLandmarks = rightHandLandmarks,
            imageWidth = width,
            imageHeight = height,
            worldLandmarks = worldLandmarks,
            faceLandmarks = faceLandmarks,
            poseTimestamp = poseResult?.timestampMs() ?: timestampMs,
            handTimestamp = handResult?.timestampMs() ?: timestampMs
        )
    }

    fun close() {
        poseLandmarker?.close()
        handLandmarker?.close()
        faceLandmarker?.close()
    }
}

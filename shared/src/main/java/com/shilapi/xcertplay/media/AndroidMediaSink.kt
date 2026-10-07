package com.shilapi.xcertplay.media

import android.util.Log
import android.view.Surface
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.MediaSink
import com.shilapi.xcertplay.airplay.MicrophoneConfig
import com.shilapi.xcertplay.airplay.VideoCodec

/**
 * Android 4.0.4 / API 15 compatibility media sink.
 *
 * This experimental backend deliberately avoids direct references to MediaCodec,
 * MediaFormat and AudioAttributes because those classes do not exist on API 15.
 * The first API-15 milestone is USB/IAP2/CarPlay session bring-up; media
 * rendering/decoding is intentionally disabled until a separate legacy decoder
 * is added.
 */
class AndroidMediaSink(
    surface: Surface? = null,
    private val videoWidth: Int = 1280,
    private val videoHeight: Int = 720,
    private val preferSoftwareHevcDecoder: Boolean = false,
    private val advancedAudioChannelMapping: Boolean = false,
    private val navigationStreamType: Int = AudioChannelMapper.DEFAULT_NAVIGATION_STREAM_TYPE,
    onScreenStreamActiveChanged: ((Int, Boolean) -> Unit)? = null,
    private val mediaBufferMillis: Int = MediaAudioBuffer.DEFAULT_MILLIS,
    private val onAudioDiagnostic: (String) -> Unit = {},
    private val onMediaAudioChanged: (Boolean) -> Unit = {},
) : MediaSink {

    private var screenListener = onScreenStreamActiveChanged
    private val surfaces = HashMap<Int, Surface>()

    init {
        if (surface != null) surfaces[110] = surface
        Log.i(TAG, "API15 media sink active: video/audio decoding disabled; USB session testing mode")
    }

    override fun setVideoRecoveryHandler(type: Int, handler: () -> Unit) = Unit

    override fun setVideoDiagnosticHandler(type: Int, handler: (String) -> Unit) {
        handler("API15 media backend: decoder unavailable on Android 4.0.4")
    }

    fun setSurface(type: Int, surface: Surface) {
        surfaces[type] = surface
    }

    fun clearSurface(type: Int, surface: Surface) {
        if (surfaces[type] === surface) surfaces.remove(type)
    }

    fun setScreenStreamActiveChangedListener(listener: ((Int, Boolean) -> Unit)?) {
        screenListener = listener
    }

    override fun onVideoCodec(type: Int, codec: VideoCodec) {
        Log.i(TAG, "API15 video codec negotiated type=" + type + " codec=" + codec + "; decoder disabled")
    }

    override fun onVideoConfig(type: Int, codecData: ByteArray) {
        Log.i(TAG, "API15 video config received type=" + type + " bytes=" + codecData.size + "; decoder disabled")
    }

    override fun onVideoFrame(type: Int, naluBytes: ByteArray) = Unit

    override fun onScreenStreamActive(type: Int, active: Boolean) {
        screenListener?.invoke(type, active)
        if (active) {
            Log.i(TAG, "API15 screen stream active type=" + type + " (rendering disabled)")
        } else {
            Log.i(TAG, "API15 screen stream stopped type=" + type)
        }
    }

    override fun onAudioStarted(type: Int, format: AudioFormat, firstSample: Int) {
        Log.i(TAG, "API15 audio stream started type=" + type + " codec=" + format.codec + "; audio output disabled")
        if (format.audioType == "media") onMediaAudioChanged(true)
        onAudioDiagnostic("API15 media backend: audio decoder unavailable; stream accepted for session testing")
    }

    override fun onAudioRtp(type: Int, format: AudioFormat, rtp: ByteArray, sample: Int) = Unit

    override fun onAudioStopped(type: Int) {
        onMediaAudioChanged(false)
        Log.i(TAG, "API15 audio stream stopped type=" + type)
    }

    override fun onMicrophoneStarted(type: Int, config: MicrophoneConfig) {
        Log.i(TAG, "API15 microphone stream requested type=" + type + "; uplink disabled")
    }

    override fun onMicrophoneStopped(type: Int) = Unit

    fun close() {
        surfaces.clear()
        screenListener = null
        onMediaAudioChanged(false)
    }

    private companion object {
        const val TAG = "xcertplay-usb"
    }
}

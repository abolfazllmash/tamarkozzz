package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class GameSoundSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    fun playCollectCoin() {
        scope.launch {
            try {
                val duration1 = 0.08
                val duration2 = 0.15
                val freq1 = 987.77 // B5
                val freq2 = 1318.51 // E6
                
                val buf1 = generateSineWave(freq1, duration1)
                val buf2 = generateSineWave(freq2, duration2, 0.4f)
                val combined = buf1 + buf2
                
                playBuffer(combined)
            } catch (e: Exception) {
                Log.e("AudioSynth", "Coin sound failed", e)
            }
        }
    }

    fun playSuccess() {
        scope.launch {
            try {
                // Chord progression CEG C6
                val freqs = listOf(523.25, 659.25, 783.99, 1046.50)
                val combined = mutableListOf<Short>()
                for (f in freqs) {
                    combined.addAll(generateSineWave(f, 0.1).toList())
                }
                playBuffer(combined.toShortArray())
            } catch (e: Exception) {
                Log.e("AudioSynth", "Success sound failed", e)
            }
        }
    }

    fun playFailure() {
        scope.launch {
            try {
                // Descending pitch rumble
                val duration = 0.5
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // descend from 400Hz to 60Hz
                    val currentFreq = 400.0 - (340.0 * (t / duration))
                    val value = (sin(2.0 * Math.PI * currentFreq * t) * 32767.0 * 0.4).toInt()
                    buffer[i] = value.coerceIn(-32768, 32767).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                Log.e("AudioSynth", "Failure sound failed", e)
            }
        }
    }

    fun playLevelUp() {
        scope.launch {
            try {
                val freqs = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98, 2093.00)
                val combined = mutableListOf<Short>()
                for (f in freqs) {
                    combined.addAll(generateSineWave(f, 0.07).toList())
                }
                playBuffer(combined.toShortArray())
            } catch (e: Exception) {
                Log.e("AudioSynth", "Level up sound failed", e)
            }
        }
    }

    fun playLaserWarning() {
        scope.launch {
            try {
                // Chirp warning sound
                val duration = 0.2
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // ascending swoop frequency
                    val freq = 200.0 + (1800.0 * (t / duration))
                    val value = (sin(2.0 * Math.PI * freq * t) * 32767.0 * 0.25).toInt()
                    buffer[i] = value.coerceIn(-32768, 32767).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                Log.e("AudioSynth", "Laser warning failed", e)
            }
        }
    }

    fun playDash() {
        scope.launch {
            try {
                // White noise sweep (pseudo white-noise / high frequency slide)
                val duration = 0.15
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // high pass noise
                    val noise = (Math.random() * 2.0 - 1.0)
                    val envelope = 1.0 - (t / duration) // fade out
                    val value = (noise * 32767.0 * 0.3 * envelope).toInt()
                    buffer[i] = value.coerceIn(-32768, 32767).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                Log.e("AudioSynth", "Dash sound failed", e)
            }
        }
    }

    fun playTick() {
        scope.launch {
            try {
                // high decay tick
                val buffer = generateSineWave(1200.0, 0.02, 0.15f)
                playBuffer(buffer)
            } catch (e: Exception) {
                Log.e("AudioSynth", "Tick failed", e)
            }
        }
    }

    fun playSavingAbility() {
        scope.launch {
            try {
                // Ascending shimmering crystal scale (E5, A5, B5, E6, G#6) - extremely saving and uplifting feel
                val freqs = listOf(659.25, 880.00, 987.77, 1318.51, 1661.22)
                val combined = mutableListOf<Short>()
                for (i in freqs.indices) {
                    combined.addAll(generateSineWave(freqs[i], 0.08, 0.3f - (i * 0.04f)).toList())
                }
                playBuffer(combined.toShortArray())
            } catch (e: Exception) {
                Log.e("AudioSynth", "Saving sound failed", e)
            }
        }
    }

    fun playDangerRumble() {
        scope.launch {
            try {
                val duration = 0.4
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // A beating frequency between 62Hz and 68Hz to create an ominous acoustic thrum
                    val freq1 = 62.0
                    val freq2 = 68.0
                    val wave1 = sin(2.0 * Math.PI * freq1 * t)
                    val wave2 = sin(2.0 * Math.PI * freq2 * t)
                    
                    val envelope = sin(Math.PI * (t / duration)) // smooth pulse rise and fall
                    val value = ((wave1 + wave2) * 0.5 * 32767.0 * 0.35 * envelope).toInt()
                    buffer[i] = value.coerceIn(-32768, 32767).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                Log.e("AudioSynth", "Danger rumble failed", e)
            }
        }
    }

    private fun generateSineWave(freq: Double, duration: Double, amplitude: Float = 0.35f): ShortArray {
        val totalSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val value = (sin(2.0 * Math.PI * freq * t) * 32767.0 * amplitude).toInt()
            buffer[i] = value.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun playBuffer(buffer: ShortArray) {
        val bufferSize = buffer.size * 2
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        
        // Schedule release after playback finished
        scope.launch {
            val durationMs = (buffer.size.toFloat() / sampleRate * 1000).toLong()
            kotlinx.coroutines.delay(durationMs + 100)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }
}

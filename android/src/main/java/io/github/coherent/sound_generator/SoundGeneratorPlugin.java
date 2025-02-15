package io.github.coherent.sound_generator;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.github.coherent.sound_generator.handlers.getOneCycleDataHandler;
import io.github.coherent.sound_generator.handlers.isPlayingStreamHandler;
import io.github.coherent.sound_generator.handlers.isPlayingStreamNoiseHandler;
import io.github.coherent.sound_generator.models.WaveTypes;
/** SoundGeneratorPlugin */
public class SoundGeneratorPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private SoundGenerator soundGenerator = new SoundGenerator();
  private MethodChannel channel;
  private EventChannel onChangeIsPlaying;
  private EventChannel onOneCycleDataHandler;
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {

    channel = new MethodChannel(binding.getBinaryMessenger(), "sound_generator");
    channel.setMethodCallHandler(new SoundGeneratorPlugin());

    // Initialize the event channels
    onChangeIsPlaying = new EventChannel(binding.getBinaryMessenger(), isPlayingStreamHandler.NATIVE_CHANNEL_EVENT);
    onChangeIsPlaying.setStreamHandler(new isPlayingStreamHandler());

    onOneCycleDataHandler = new EventChannel(binding.getBinaryMessenger(), getOneCycleDataHandler.NATIVE_CHANNEL_EVENT);
    onOneCycleDataHandler.setStreamHandler(new getOneCycleDataHandler());

    final EventChannel onChangeIsPlayingNoise = new EventChannel(binding.getBinaryMessenger(), isPlayingStreamNoiseHandler.NATIVE_CHANNEL_EVENT);
    onChangeIsPlayingNoise.setStreamHandler(new isPlayingStreamNoiseHandler());
  }
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      switch (call.method) {
        case "init": {
          Integer sampleRate = call.argument("sampleRate");
          if (sampleRate == null) {
            result.error("INVALID_ARGUMENT", "sampleRate is required", null);
            return;
          }
          result.success(soundGenerator.init(sampleRate));
          break;
        }
        case "release":
          soundGenerator.release();
          result.success(null);
          break;
        case "play":
          soundGenerator.startPlayback();
          result.success(null);
          break;
        case "play_calibration": {
          Integer frequency = call.argument("frequency");
          Integer sampleRate = call.argument("sampleRate");
          Integer actualVolume = call.argument("actualVolume");
          Integer numSamples = call.argument("numSamples");
          Integer s = call.argument("s");

          if (frequency == null || sampleRate == null || actualVolume == null || numSamples == null || s == null) {
            result.error("INVALID_ARGUMENT", "Missing parameters for play_calibration", null);
            return;
          }
          soundGenerator.startPlayback2(frequency, sampleRate, actualVolume, numSamples, s);
          result.success(null);
          break;
        }
        case "stop":
          soundGenerator.stopPlayback();
          result.success(null);
          break;
        case "play_noise": {
          Integer sampleRate = call.argument("sampleRate");
          Integer actualVolume = call.argument("actualVolume");
          Integer numSamples = call.argument("numSamples");
          Integer s = call.argument("s");

          if (sampleRate == null || actualVolume == null || numSamples == null || s == null) {
            result.error("INVALID_ARGUMENT", "Missing parameters for play_noise", null);
            return;
          }
          soundGenerator.startPlayback3(sampleRate, actualVolume, numSamples, s);
          result.success(null);
          break;
        }
        case "stop_noise":
          soundGenerator.stopPlayback3();
          result.success(null);
          break;
        case "isPlaying":
          result.success(soundGenerator.isPlaying());
          break;
        case "setAutoUpdateOneCycleSample": {
          Boolean autoUpdateOneCycleSample = call.argument("autoUpdateOneCycleSample");
          if (autoUpdateOneCycleSample == null) {
            result.error("INVALID_ARGUMENT", "autoUpdateOneCycleSample is required", null);
            return;
          }
          soundGenerator.setAutoUpdateOneCycleSample(autoUpdateOneCycleSample);
          result.success(null);
          break;
        }
        case "setFrequency": {
          Double frequency = call.argument("frequency");
          if (frequency == null) {
            result.error("INVALID_ARGUMENT", "frequency is required", null);
            return;
          }
          soundGenerator.setFrequency(frequency.floatValue());
          result.success(null);
          break;
        }
        case "setDecibel": {
          Double decibel = call.argument("decibel");
          if (decibel == null) {
            result.error("INVALID_ARGUMENT", "decibel is required", null);
            return;
          }
          soundGenerator.setDecibel(decibel.floatValue());
          result.success(null);
          break;
        }
        case "setNoiseDecibel": {
          Double noiseDecibel = call.argument("noise_decibel");
          if (noiseDecibel == null) {
            result.error("INVALID_ARGUMENT", "noise_decibel is required", null);
            return;
          }
          soundGenerator.setNoiseDecibel(noiseDecibel.floatValue());
          result.success(null);
          break;
        }
        case "setWaveform": {
          String waveType = call.argument("waveType");
          if (waveType == null) {
            result.error("INVALID_ARGUMENT", "waveType is required", null);
            return;
          }
          try {
            soundGenerator.setWaveform(WaveTypes.valueOf(waveType));
          } catch (IllegalArgumentException e) {
            result.error("INVALID_WAVEFORM", "Invalid waveform type: " + waveType, null);
            return;
          }
          result.success(null);
          break;
        }
        case "setBalance": {
          Double balance = call.argument("balance");
          if (balance == null) {
            result.error("INVALID_ARGUMENT", "balance is required", null);
            return;
          }
          soundGenerator.setBalance(balance.floatValue());
          result.success(null);
          break;
        }
        case "setVolume": {
          Double volume = call.argument("volume");
          if (volume == null) {
            result.error("INVALID_ARGUMENT", "volume is required", null);
            return;
          }
          soundGenerator.setVolume(volume.floatValue());
          result.success(null);
          break;
        }
        case "getSampleRate":
          result.success(soundGenerator.getSampleRate());
          break;
        case "refreshOneCycleData":
          soundGenerator.refreshOneCycleData();
          result.success(null);
          break;
        default:
          result.notImplemented();
          break;
      }
    } catch (Exception e) {
      result.error("UNKNOWN_ERROR", e.getMessage(), null);
    }
  }


  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
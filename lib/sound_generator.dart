import 'dart:async';

import 'package:flutter/services.dart';
import 'package:coherent_soundgenerator/waveTypes.dart';

class SoundGenerator {
  static const MethodChannel _channel = const MethodChannel('sound_generator');
  static const EventChannel _onChangeIsPlayingDataHandler = const EventChannel(
      'io.github.coherent.sound_generator/onChangeIsPlaying');
  static const EventChannel _onChangeIsPlayingnoiseDataHandler =
      const EventChannel(
          'io.github.coherent.sound_generator/onChangeIsPlayingnoise');
  static const EventChannel _onOneCycleDataHandler = const EventChannel(
      'io.github.coherent.sound_generator/onOneCycleDataHandler');

  /// is Playing data changed event
  static bool _onIsPlayingChangedInitialized = false;
  static late Stream<bool> _onGetIsPlayingChanged;
  static Stream<bool> get onIsPlayingChanged {
    if (!_onIsPlayingChangedInitialized) {
      _onGetIsPlayingChanged = _onChangeIsPlayingDataHandler
          .receiveBroadcastStream()
          .map<bool>((value) => value);

      _onIsPlayingChangedInitialized = true;
    }

    return _onGetIsPlayingChanged;
  }

  /// is Playing noise data changed event
  static bool _onIsPlayingnoiseChangedInitialized = false;
  static late Stream<bool> _onGetIsPlayingnoiseChanged;
  static Stream<bool> get onIsPlayingnoiseChanged {
    if (!_onIsPlayingnoiseChangedInitialized) {
      _onGetIsPlayingnoiseChanged = _onChangeIsPlayingnoiseDataHandler
          .receiveBroadcastStream()
          .map<bool>((value) => value);

      _onIsPlayingnoiseChangedInitialized = true;
    }

    return _onGetIsPlayingnoiseChanged;
  }

  /// One cycle data changed event
  static bool _onGetOneCycleDataHandlerInitialized = false;
  static late Stream<List<int>> _onGetOneCycleDataHandler;
  static Stream<List<int>> get onOneCycleDataHandler {
    if (!_onGetOneCycleDataHandlerInitialized) {
      _onGetOneCycleDataHandler = _onOneCycleDataHandler
          .receiveBroadcastStream()
          .map<List<int>>((value) => new List<int>.from(value));
      _onGetOneCycleDataHandlerInitialized = true;
    }

    return _onGetOneCycleDataHandler;
  }

  /// init function
  static Future<bool> init(int sampleRate) async {
    final bool init = await _channel
        .invokeMethod("init", <String, dynamic>{"sampleRate": sampleRate});
    return init;
  }

  /// Play sound
  static void play() async {
    await _channel.invokeMethod('play');
  }

  /// Play sound
  // static void play_calibration() async {
  //   await _channel.invokeMethod('play_calibration');
  // }
  static Future<bool?> play_calibration(int frequency, int sampleRate,
      int actualVolume, int numSamples, int s) async {
    // final bool? play_calibration =
    //     await _channel.invokeMethod("play_calibration", <String, dynamic>{
    //   "frequency": frequency,
    //   "sampleRate": sampleRate,
    //   "actualVolume": actualVolume,
    //   "numSamples": numSamples,
    //   "s": s
    // });
    final bool play_calibration =
        (await _channel.invokeMethod("play_calibration", {
              "frequency": frequency.toInt(),
              "sampleRate": sampleRate.toInt(),
              "actualVolume": actualVolume.toInt(),
              "numSamples": numSamples.toInt(),
              "s": s.toInt(),
            })) ??
            false;
    // final bool play_calibration =
    //     (await _channel.invokeMethod("play_calibration", <String, dynamic>{
    //           "frequency": frequency,
    //           "sampleRate": sampleRate,
    //           "actualVolume": actualVolume,
    //           "numSamples": numSamples,
    //           "s": s
    //         })) ??
    //         false;
    return play_calibration;
  }

  static Future<bool> play_noise(int frequency, int sampleRate,
      int actualVolume, int numSamples, int s) async {
    final bool play_noise =
        await _channel.invokeMethod("play_noise", <String, dynamic>{
      "frequency": frequency,
      "sampleRate": sampleRate,
      "actualVolume": actualVolume,
      "numSamples": numSamples,
      "s": s
    });
    return play_noise;
  }

  /// Stop playing sound
  static void stop() async {
    await _channel.invokeMethod('stop');
  }

  /// Stop playing noise
  static void stop_noise() async {
    await _channel.invokeMethod('stop_noise');
  }

  /// Release all data
  static void release() async {
    await _channel.invokeMethod('release');
  }

  /// Refresh One Cycle Data
  static void refreshOneCycleData() async {
    await _channel.invokeMethod('refreshOneCycleData');
  }

  /// Get is Playing data
  static Future<bool> get isPlaying async {
    final bool playing = await _channel.invokeMethod('isPlaying');
    return playing;
  }

  /// Get SampleRate
  static Future<int> get getSampleRate async {
    final int sampleRate = await _channel.invokeMethod('getSampleRate');
    return sampleRate;
  }

  /// Set AutoUpdateOneCycleSample
  static void setAutoUpdateOneCycleSample(bool autoUpdateOneCycleSample) async {
    await _channel.invokeMethod(
        "setAutoUpdateOneCycleSample", <String, dynamic>{
      "autoUpdateOneCycleSample": autoUpdateOneCycleSample
    });
  }

  /// Set Frequency
  static void setFrequency(double frequency) async {
    await _channel.invokeMethod(
        "setFrequency", <String, dynamic>{"frequency": frequency});
  }

  /// Set decibel
  static void setDecibel(double decibel) async {
    await _channel
        .invokeMethod("setDecibel", <String, dynamic>{"decibel": decibel});
  }

  /// Set noisedecibel
  static void setNoiseDecibel(double decibel) async {
    await _channel.invokeMethod(
        "setNoiseDecibel", <String, dynamic>{"noise_decibel": decibel});
  }

  /// Set Balance Range from -1 to 1
  static void setBalance(double balance) async {
    await _channel
        .invokeMethod("setBalance", <String, dynamic>{"balance": balance});
  }

  /// Set WaveType
  static void setWaveType(waveTypes waveType) async {
    await _channel.invokeMethod("setWaveform", <String, dynamic>{
      "waveType": waveType.toString().replaceAll("waveTypes.", "")
    });
  }

  /// Set Volume Range from 0 to 1
  static void setVolume(double volume) async {
    await _channel
        .invokeMethod("setVolume", <String, dynamic>{"volume": volume});
  }
}

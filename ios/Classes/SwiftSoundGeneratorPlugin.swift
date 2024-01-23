import Flutter
import UIKit
import AudioKit

public class SwiftSoundGeneratorPlugin: NSObject, FlutterPlugin {
  var onChangeIsPlaying: BetterEventChannel?;
  var onChangeIsPlayingnoise: BetterEventChannel?;
  var onOneCycleDataHandler: BetterEventChannel?;
  // This is not used yet.
  var sampleRate: Int = 48000;
  var isPlaying: Bool = false;
  var oscillator: AKOscillator = AKOscillator();
  var panner: AKPanner?;
  var mixer: AKMixer?;
    
  var noisePlayer: NoisePlayer?

//  public static func register(with registrar: FlutterPluginRegistrar) {
//    /*let instance =*/ _ = SwiftSoundGeneratorPlugin(registrar: registrar)
//  }
//
//  public init(registrar: FlutterPluginRegistrar) {
//    super.init()
//    self.panner = AKPanner(self.oscillator, pan: 0.0)
//    self.mixer = AKMixer(self.panner!)
//    self.mixer!.volume = 1.0
//    AKSettings.disableAVAudioSessionCategoryManagement = true
//    AKSettings.disableAudioSessionDeactivationOnStop = true
//    AKManager.output = self.mixer!
//    let methodChannel = FlutterMethodChannel(name: "sound_generator", binaryMessenger: registrar.messenger())
//    self.onChangeIsPlaying = BetterEventChannel(name: "io.github.mertguner.sound_generator/onChangeIsPlaying", messenger: registrar.messenger())
//    self.onChangeIsPlayingnoise = BetterEventChannel(name: "io.github.mertguner.sound_generator/onChangeIsPlayingnoise", messenger: registrar.messenger())
//    self.onOneCycleDataHandler = BetterEventChannel(name: "io.github.coherent.sound_generator/onOneCycleDataHandler", messenger: registrar.messenger())
//    registrar.addMethodCallDelegate(self, channel: methodChannel)
//
//    // Set up the NoisePlayer
//    noisePlayer = NoisePlayer()
//
//  }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
           let instance = SwiftSoundGeneratorPlugin()
           instance.onRegister(registrar: registrar)
       }

       private func onRegister(registrar: FlutterPluginRegistrar) {
           self.mixer = AKMixer(self.oscillator)
           self.mixer!.volume = 1.0
           AKSettings.disableAVAudioSessionCategoryManagement = true
           AKSettings.disableAudioSessionDeactivationOnStop = true
           AKManager.output = self.mixer!
           let methodChannel = FlutterMethodChannel(name: "sound_generator", binaryMessenger: registrar.messenger())
           self.onChangeIsPlaying = BetterEventChannel(name: "io.github.coherent.sound_generator/onChangeIsPlaying", messenger: registrar.messenger())
           self.onChangeIsPlayingnoise = BetterEventChannel(name: "io.github.coherent.sound_generator/onChangeIsPlayingnoise", messenger: registrar.messenger())
           self.onOneCycleDataHandler = BetterEventChannel(name: "io.github.coherent.sound_generator/onOneCycleDataHandler", messenger: registrar.messenger())
           registrar.addMethodCallDelegate(self, channel: methodChannel)

           // Set up the NoisePlayer
           noisePlayer = NoisePlayer(mixer: self.mixer!)
       }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
      case "init":
        //let args = call.arguments as! [String: Any]
        //let sampleRate = args["sampleRate"] as Int
        self.oscillator.frequency = 400
        do {
            try AKManager.start()
            result(true);
        } catch {
            result(FlutterError(
                code: "init_error",
                message: "Unable to start AKManager",
                details: ""))
        }
        break
      case "release":
        result(nil);
        break;
      case "play":
        self.oscillator.start()
        onChangeIsPlaying!.sendEvent(event: true)
        result(nil);
        break;
      case "stop":
        self.oscillator.stop();
        onChangeIsPlaying!.sendEvent(event: false)
        result(nil);
        break;
    case "play_noise":
        noisePlayer?.startNoise()
        onChangeIsPlayingnoise!.sendEvent(event: true)
      result(nil);
      break;
    case "stop_noise":
        noisePlayer?.stopNoise()
        onChangeIsPlayingnoise!.sendEvent(event: false)
      result(nil);
      break;
      case "isPlaying":
        result(self.isPlaying);
        break;
      case "setAutoUpdateOneCycleSample":
        result(nil);
        break;
      case "setFrequency":
        let args = call.arguments as! [String: Any]
        self.oscillator.frequency = args["frequency"] as! Double
        result(nil);
        break;
      case "setWaveform":
        result(nil);
        break;
      case "setBalance":
        let args = call.arguments as! [String: Any]
        self.panner!.pan = args["balance"] as! Double
        break;
      case "setVolume":
        let args = call.arguments as! [String: Any]
        self.mixer!.volume = args["volume"] as! Double
        result(nil);
        break;
      case "setDecibel":
//        let args = call.arguments as! [String: Any]
//        self.mixer!.volume = args["volume"] as! Double
        
        
        if let args = call.arguments as? [String: Any],
        let decibel = args["decibel"] as? Double {
        // Set decibel level for both oscillator and noise
            setOscillatorDecibel(decibel)
            noisePlayer?.setNoiseDecibel(decibel)
        }
        result(nil);
        break;
      case "getSampleRate":
        result(self.sampleRate);
        break;
      case "refreshOneCycleData":
        result(nil);
        break;
      default:
        result(FlutterMethodNotImplemented);
    }
  }
    

    
    // Helper function to set oscillator amplitude using decibel
    func setOscillatorDecibel(_ decibel: Double) {
        self.oscillator.amplitude = decibelToAmplitude(decibel)
    }
}

// Helper function to convert decibel to amplitude
func decibelToAmplitude(_ decibel: Double) -> Double {
    return pow(10.0, decibel / 20.0)
}


class NoisePlayer {
    var whiteNoise: AKWhiteNoise?
    var silence: AKBooster?
    var mixer: AKMixer?

    var isNoisePlaying: Bool = false

    init(mixer: AKMixer) {
        self.mixer = mixer
        do {
            try AKSettings.setSession(category: .playAndRecord, with: .mixWithOthers)
            whiteNoise = AKWhiteNoise(amplitude: 0.5)
            silence = AKBooster(whiteNoise!)
            mixer.connect(input: silence!)
            try AKManager.start()
        } catch {
            AKLog("AudioKit did not start!")
        }
    }
    
    func setNoiseDecibel(_ decibel: Double) {
            whiteNoise?.amplitude = decibelToAmplitude(decibel)
        }

    func startNoise() {
        if !isNoisePlaying {
            whiteNoise?.start()
            isNoisePlaying = true
        }
    }

    func stopNoise() {
        if isNoisePlaying {
            whiteNoise?.stop()
            isNoisePlaying = false
        }
    }
}

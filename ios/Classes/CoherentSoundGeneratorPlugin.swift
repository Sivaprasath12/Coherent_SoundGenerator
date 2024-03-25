import Flutter
import UIKit
import AudioKit
import AVFoundation


public class CoherentSoundGeneratorPlugin: NSObject, FlutterPlugin {
    var audioEngine: AVAudioEngine?
    var audioPlayerNode: AVAudioPlayerNode?
    var audioFormat: AVAudioFormat?
    
    var audioEngine_noise: AVAudioEngine?
    var audioPlayerNode_noise: AVAudioPlayerNode?
    var audioFormat_noise: AVAudioFormat?
    
    
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
             let instance = CoherentSoundGeneratorPlugin()
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
//             noisePlayer = NoisePlayer(mixer: self.mixer!)
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
          self.stopPlayback();
          result(nil);
          break;
//      case "play_noise":
////          noisePlayer?.startNoise()
////          onChangeIsPlayingnoise!.sendEvent(event: true)
//        result(nil);
//        break;
      case "stop_noise":
//          noisePlayer?.stopNoise()
//          onChangeIsPlayingnoise!.sendEvent(event: false)
        self.stopPlayback_noise();
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
        case "play_calibration":
          guard let args = call.arguments as? [String: Any],
                            let frequency = args["frequency"] as? Int,
                            let sampleRate = args["sampleRate"] as? Int,
                            let actualVolume = args["actualVolume"] as? Int,
                            let numSamples = args["numSamples"] as? Int,
                            let s = args["s"] as? Int else {
                          result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments received", details: nil))
                          return
                      }

            startPlayback(frequency: frequency, sampleRate: sampleRate, actualVolume: actualVolume, numSamples: numSamples, s: s)
            result(nil)
          break;
        case "play_noise":
        guard let args = call.arguments as? [String: Any],
                        let sampleRate = args["sampleRate"] as? Int,
                        let actualVolume = args["actualVolume"] as? Int,
                        let numSamples = args["numSamples"] as? Int,
                        let s = args["s"] as? Int else {
                        result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments received for playing noise", details: nil))
                        return
                    }

        startPlaybackNoise(sampleRate: sampleRate, actualVolume: actualVolume, numSamples: numSamples, s: s)
        result(nil)
       
        break;
        default:
          result(FlutterMethodNotImplemented);
      }
    }



    // Helper function to set oscillator amplitude using decibel
    func setOscillatorDecibel(_ decibel: Double) {
        self.oscillator.amplitude = decibelToAmplitude(decibel)
     }

    // Helper function to convert decibel to amplitude
    func decibelToAmplitude(_ decibel: Double) -> Double {
        return pow(10.0, decibel / 20.0)
    }
    
    func startPlaybackNoise(sampleRate: Int, actualVolume: Int, numSamples: Int, s: Int) {
          let generatedNoise = genNoise(volume: actualVolume, numSamples: numSamples)
          playSound_noise(generatedSnd: generatedNoise, ear: s, sampleRate: sampleRate)
      }
    
    
//    func startPlayback(frequency: Int, sampleRate: Int, actualVolume: Int, numSamples: Int, s: Int) {
//            let increment = (2.0 * Double.pi * Double(frequency)) / Double(sampleRate)
//            let generatedTone = genTone(increment: increment, volume: actualVolume, numSamples: numSamples)
//
//            playSound(generatedSnd: generatedTone, ear: s, sampleRate: sampleRate)
//        }
    func startPlayback(frequency: Int, sampleRate: Int, actualVolume: Int, numSamples: Int, s: Int) {
        let increment = (2.0 * Float.pi * Float(frequency)) / Float(sampleRate)
//        let generatedTone = genTone(increment: increment, volume: actualVolume, numSamples: numSamples)
        let generatedTone = genPulseTone(increment: increment, volume: actualVolume, numSamples: numSamples)

        playSound(generatedSnd: generatedTone, ear: s, sampleRate: sampleRate)
    }

    
    func genNoise(volume: Int, numSamples: Int) -> [Float] {
          var generatedSnd = [Float](repeating: 0.0, count: numSamples)
          for i in 0..<numSamples {
              generatedSnd[i] = (Float.random(in: -1...1) * Float(volume) / 32768)
          }
          return generatedSnd
      }

//    func genTone(increment: Double, volume: Int, numSamples: Int) -> [Float] {
//        var angle: Double = 0
//        var generatedSnd = [Float](repeating: 0.0, count: numSamples)
//
//        for i in 0..<numSamples {
//            generatedSnd[i] = Float(sin(angle) * Double(volume) / 32768.0)
//            angle += increment
//        }
//
//        return generatedSnd
//    }
    
    func genTone(increment: Float, volume: Int, numSamples: Int) -> [Float] {
        var angle: Float = 0
        var generatedSound = [Float](repeating: 0.0, count: numSamples)
        for i in 0..<numSamples {
            generatedSound[i] = sin(angle) * Float(volume) / 32768.0
            angle += increment
        }
        return generatedSound
    }
    
    func genPulseTone(increment: Float, volume: Int, numSamples: Int) -> [Float] {
        var angle: Float = 0
        var generatedSnd = [Float](repeating: 0, count: numSamples)
        let pulsePeriod = 44100 / 10 // Adjust for faster or slower pulse

        for i in 0..<numSamples {
            let isPulseOn = (i / pulsePeriod) % 2 == 0
            if isPulseOn {
                generatedSnd[i] = sin(angle) * Float(volume) / 32768.0
            } else {
                generatedSnd[i] = 0
            }
            angle += increment
        }
        
        return generatedSnd
    }


//    func playSound(generatedSnd: [Float], ear: Int, sampleRate: Int) {
//        stopPlayback() // Ensure any existing playback is stopped
//
//        audioEngine = AVAudioEngine()
//        audioPlayerNode = AVAudioPlayerNode()
//        audioFormat = AVAudioFormat(standardFormatWithSampleRate: Double(sampleRate), channels: 1)
//
//        guard let audioEngine = audioEngine, let audioPlayerNode = audioPlayerNode, let audioFormat = audioFormat else { return }
//
//        audioEngine.attach(audioPlayerNode)
//        audioEngine.connect(audioPlayerNode, to: audioEngine.mainMixerNode, format: audioFormat)
//
//        do {
//            let buffer = AVAudioPCMBuffer(pcmFormat: audioFormat, frameCapacity: AVAudioFrameCount(generatedSnd.count))
//            buffer?.frameLength = buffer!.frameCapacity
//            let channelData = buffer?.floatChannelData![0]
//            memcpy(channelData, generatedSnd, generatedSnd.count * MemoryLayout<Float>.size)
//
//            try audioEngine.start()
//            audioPlayerNode.scheduleBuffer(buffer!, at: nil, options: [], completionHandler: nil)
//
//            // Adjust pan based on the 'ear' parameter
//            switch ear {
//            case 0: // Left ear
//                audioPlayerNode.pan = -1.0
//            case 1: // Right ear
//                audioPlayerNode.pan = 1.0
//            default: // Both ears
//                audioPlayerNode.pan = 0.0
//            }
//
//            audioPlayerNode.play()
//
//
//        } catch {
//            print("Error starting audio engine: \(error)")
//        }
//    }
    
    func playSound(generatedSnd: [Float], ear: Int, sampleRate: Int) {
        stopPlayback() // Ensure any existing playback is stopped
        print("starrrrrrr start playback now.")
        audioEngine = AVAudioEngine()
        audioPlayerNode = AVAudioPlayerNode()
        audioFormat = AVAudioFormat(standardFormatWithSampleRate: Double(sampleRate), channels: 1)
        
        guard let audioEngine = audioEngine, let audioPlayerNode = audioPlayerNode, let audioFormat = audioFormat else { return }
        
        audioEngine.attach(audioPlayerNode)
        audioEngine.connect(audioPlayerNode, to: audioEngine.mainMixerNode, format: audioFormat)
        
        do {
            let buffer = AVAudioPCMBuffer(pcmFormat: audioFormat, frameCapacity: AVAudioFrameCount(generatedSnd.count))
            buffer?.frameLength = buffer!.frameCapacity
            let channelData = buffer?.floatChannelData![0]
            memcpy(channelData, generatedSnd, generatedSnd.count * MemoryLayout<Float>.size)
            
            try audioEngine.start()
            audioPlayerNode.scheduleBuffer(buffer!, at: nil, options: [], completionHandler: nil)
            
            // Adjust pan based on the 'ear' parameter
            switch ear {
            case 0: // Left ear
                audioPlayerNode.pan = -1.0
            case 1: // Right ear
                audioPlayerNode.pan = 1.0
            default: // Both ears
                audioPlayerNode.pan = 0.0
            }
            
            audioPlayerNode.play()
            
//            // Stop the sound after 1 second
//            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
//                print("starrrrrrr stop playback now.")
//                audioPlayerNode.stop()
//
//                // Optional: If you also want to stop and reset the audio engine, uncomment the following lines
//                 audioEngine.stop()
//                 audioEngine.reset()
//            }
            
        } catch {
            print("Error starting audio engine: \(error)")
        }
    }

    
    func playSound_noise(generatedSnd: [Float], ear: Int, sampleRate: Int) {
        stopPlayback_noise() // Ensure any existing playback is stopped

        audioEngine_noise = AVAudioEngine()
        audioPlayerNode_noise = AVAudioPlayerNode()
        audioFormat_noise = AVAudioFormat(standardFormatWithSampleRate: Double(sampleRate), channels: 1)

        guard let audioEngine = audioEngine_noise, let audioPlayerNode = audioPlayerNode_noise, let audioFormat = audioFormat_noise else { return }

        audioEngine.attach(audioPlayerNode)
        audioEngine.connect(audioPlayerNode, to: audioEngine.mainMixerNode, format: audioFormat)

        do {
            let buffer = AVAudioPCMBuffer(pcmFormat: audioFormat, frameCapacity: AVAudioFrameCount(generatedSnd.count))
            buffer?.frameLength = buffer!.frameCapacity
            let channelData = buffer?.floatChannelData![0]
            memcpy(channelData, generatedSnd, generatedSnd.count * MemoryLayout<Float>.size)

            try audioEngine.start()
            audioPlayerNode.scheduleBuffer(buffer!, at: nil, options: [], completionHandler: nil)

            // Adjust pan based on the 'ear' parameter
            switch ear {
            case 0: // Left ear
                audioPlayerNode.pan = -1.0
            case 1: // Right ear
                audioPlayerNode.pan = 1.0
            default: // Both ears
                audioPlayerNode.pan = 0.0
            }

            audioPlayerNode.play()

        } catch {
            print("Error starting audio engine: \(error)")
        }
    }


    func stopPlayback() {
        audioPlayerNode?.stop()
        audioEngine?.stop()
        audioEngine = nil
        audioPlayerNode = nil
    }
    
    
    func stopPlayback_noise() {
        audioPlayerNode_noise?.stop()
        audioEngine_noise?.stop()
        audioEngine_noise = nil
        audioPlayerNode_noise = nil
    }

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
//            whiteNoise?.amplitude = decibelToAmplitude(decibel)
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



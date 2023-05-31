import Flutter
import UIKit
import CoreBluetooth
import os.log

public class SwiftBluetoothEnablePlugin: NSObject, FlutterPlugin, CBCentralManagerDelegate {
    var centralManager: CBCentralManager!
    var lastKnownState: CBManagerState!
    var isAvailablePendingResult: FlutterResult?
    var isEnabledPendingResult: FlutterResult?
    var enablePendingResult: FlutterResult?
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        lastKnownState = central.state;
        os_log("central.state is: %@", log: .default, type: .debug, _getStateString(state: lastKnownState));
        
        if let finalPendingResult = enablePendingResult {
            if (lastKnownState == .poweredOn){
                finalPendingResult(true)
            } else {
                finalPendingResult(false)
            }
        }
        enablePendingResult = nil
        
        if let finalPendingResult = isAvailablePendingResult {
            if (lastKnownState == .unsupported){
                finalPendingResult(false)
            } else {
                finalPendingResult(true)
            }
        }
        isAvailablePendingResult = nil
        
        if let finalPendingResult = isEnabledPendingResult {
            if (lastKnownState == .poweredOn){
                finalPendingResult(true)
            } else {
                finalPendingResult(false)
            }
        }
        isEnabledPendingResult = nil
    }
    
    private func _getStateString(state: CBManagerState) -> String {
        switch (state) {
        case .unknown:
            return ".unknown";
        case .resetting:
            return ".resetting";
        case .unsupported:
            return ".unsupported";
        case .unauthorized:
            return ".unauthorized";
        case .poweredOff:
            return ".poweredOff";
        case .poweredOn:
            return ".poweredOn";
        @unknown default:
            return "";
        }
    }
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "bluetooth_enable", binaryMessenger: registrar.messenger())
    let instance = SwiftBluetoothEnablePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      switch (call.method) {
      case "isAvailable":
          if (lastKnownState == .unsupported) {
              result(false)
          } else {
              centralManager = CBCentralManager(delegate: self, queue: nil, options: [CBCentralManagerOptionShowPowerAlertKey: false])
              isAvailablePendingResult = result;
          }
          break;
      case "isEnabled":
          if (lastKnownState == .poweredOn) {
              result(true)
          } else {
              centralManager = CBCentralManager(delegate: self, queue: nil, options: [CBCentralManagerOptionShowPowerAlertKey: false])
              isEnabledPendingResult = result;
          }
          break;
      case "enableBluetooth":
          if (lastKnownState == .poweredOn) {
              result(true)
          } else {
              centralManager = CBCentralManager(delegate: self, queue: nil)
              enablePendingResult = result;
          }
          break;
      default:
          os_log("Unsupported method : %@", log: .default, type: .debug, call.method);
          break;
      }
  }
}

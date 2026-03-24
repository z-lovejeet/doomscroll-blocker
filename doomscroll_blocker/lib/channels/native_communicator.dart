import 'package:flutter/services.dart';

/// Wraps the native MethodChannel and EventChannel for clean access from the UI.
class NativeCommunicator {
  static const _methodChannel = MethodChannel('com.example.doomscroll/enforcer');
  static const _eventChannel = EventChannel('com.example.doomscroll/timer_events');

  /// Check if the AccessibilityService is currently running.
  static Future<bool> getServiceStatus() async {
    final result = await _methodChannel.invokeMethod<bool>('getServiceStatus');
    return result ?? false;
  }

  /// Check if the overlay (draw over apps) permission is granted.
  static Future<bool> getOverlayPermissionStatus() async {
    final result =
        await _methodChannel.invokeMethod<bool>('getOverlayPermissionStatus');
    return result ?? false;
  }

  /// Get the current stats from the native StorageManager.
  static Future<Map<String, dynamic>> getStats() async {
    final result = await _methodChannel.invokeMethod<Map>('getStats');
    if (result == null) return {};
    return Map<String, dynamic>.from(result);
  }

  /// Attempt to unlock a penalty with the typed text.
  /// Returns true if the text matched and penalty was unlocked.
  static Future<bool> unlockPenalty(String text) async {
    final result = await _methodChannel
        .invokeMethod<bool>('unlockPenalty', {'text': text});
    return result ?? false;
  }

  /// Open the Android Accessibility Settings screen.
  static Future<void> openAccessibilitySettings() async {
    await _methodChannel.invokeMethod('openAccessibilitySettings');
  }

  /// Open the Android Overlay Permission Settings screen.
  static Future<void> openOverlaySettings() async {
    await _methodChannel.invokeMethod('openOverlaySettings');
  }

  /// Stream of live timer ticks (seconds) from the native service.
  static Stream<int> get timerStream {
    return _eventChannel.receiveBroadcastStream().map((event) => event as int);
  }
}

# Doubao Speech Engine SDK
-keep class com.bytedance.speech.speechengine.SpeechEngine {*;}
-keep class com.bytedance.speech.speechengine.SpeechEngine$SpeechListener {*;}
-keep class com.bytedance.speech.speechengine.SpeechEngineDefines {*;}
-keep class com.bytedance.speech.speechengine.SpeechEngineGenerator {*;}
-keep class com.bytedance.speech.speechengine.SpeechEngineLoader {*;}
-keep class com.bytedance.speech.speechengine.SpeechEngineLoader$PluginAdapter {*;}
-keep class com.bytedance.speech.speechengine.SpeechResourceManager {*;}
-keep class com.bytedance.speech.speechengine.SpeechResourceManager$FetchResourceListener {*;}
-keep class com.bytedance.speech.speechengine.SpeechResourceManager$CheckResouceUpdateListener {*;}
-keep class com.bytedance.speech.speechengine.SpeechResourceManagerGenerator {*;}
-keep class com.bytedance.speech.speechengine.bridge.SpeechBridge {*;}
-keep class com.bytedance.speech.speechengine.bridge.SpeechBridgeCallback {*;}
-keepclassmembernames class com.bytedance.speech.speechengine.SpeechEngineImpl {
    public void onSpeechMessage(int, byte[], int);
    public void onNetCommand(int, byte[]);
}
-keep public enum com.bytedance.speech.speechengine.SpeechEngineLoader$** {
    **[] $VALUES;
    public *;
}
-keep class com.bytedance.speech.speechengine.net.ws.IWsClient { *; }
-keep class com.bytedance.speech.speechengine.net.ws.IWsListener { *; }
-keep class com.bytedance.speech.speechengine.net.ws.WsConnectionConfig { *; }

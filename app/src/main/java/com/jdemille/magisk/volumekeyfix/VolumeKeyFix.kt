package com.jdemille.magisk.volumekeyfix

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class VolumeKeyFix : IXposedHookLoadPackage {
    companion object {
        var mAudioManager: AudioManager? = null
        var audioManager: AudioManager
            get() = mAudioManager ?: context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            set(value) {
                mAudioManager = value
            }

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    fun log(t: Throwable) = XposedBridge.log(t)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            if (lpparam.packageName == "android" && lpparam.processName == "android") {
                val classAudioService: Class<*> = XposedHelpers.findClass(
                    "com.android.server.audio.AudioService",
                    lpparam.classLoader
                );
                XposedBridge.hookAllConstructors(classAudioService, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context;
                    }
                })
                XposedHelpers.findAndHookMethod(
                    classAudioService,
                    "getActiveStreamType",
                    Int::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val activeStreamType = param.result as Int;
                            if (activeStreamType == AudioManager.STREAM_MUSIC && !isMusicActive()) {
                                param.result = AudioManager.STREAM_RING;
                            }
                        }
                    })
            }
        } catch (t: Throwable) {
            log(t)
        }
    }

    fun isMusicActive(): Boolean {
        try {
            if (audioManager.isMusicActive)
                return true
            if (XposedHelpers.callMethod(audioManager, "isMusicActiveRemotely") as Boolean)
                return true
        } catch (t: Throwable) {
            log(t)
        }
        return false
    }
}
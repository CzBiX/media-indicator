package com.czbix.xposed.mediaindicator;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaSyncEvent;
import android.os.Handler;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("unused")
public class Hook implements IXposedHookLoadPackage {
    private static final Class<?> ACTIVITY_THREAD_CLS = XposedHelpers.findClass("android.app.ActivityThread", null);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        hookCameraApi(param.classLoader);
        hookCamera2Api(param.classLoader);
        hookMediaRecorder(param.classLoader);
    }

    @SuppressWarnings("deprecation")
    private void hookCameraApi(ClassLoader classLoader) {
        XposedHelpers.findAndHookConstructor(Camera.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.hasThrowable()) {
                    return;
                }

                final int sourceId = (int) param.args[0];
                final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(sourceId, cameraInfo);

                String facingStr;
                switch (cameraInfo.facing) {
                    case Camera.CameraInfo.CAMERA_FACING_FRONT:
                        facingStr = "front";
                        break;
                    case Camera.CameraInfo.CAMERA_FACING_BACK:
                        facingStr = "back";
                        break;
                    default:
                        facingStr = "unknown";
                        break;
                }

                sendBroadcast(getCurrentApplication(), String.format("using %s camera(legacy)", facingStr));
            }
        });
    }

    private void hookMediaRecorder(ClassLoader classLoader) {
        final XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Context context = getCurrentApplication();
                sendBroadcast(context, "using MIC");
            }
        };

        XposedHelpers.findAndHookMethod(MediaRecorder.class, "setAudioSource", int.class,
                hook);
        XposedHelpers.findAndHookMethod(AudioRecord.class, "startRecording", hook);
        XposedHelpers.findAndHookMethod(AudioRecord.class, "startRecording", MediaSyncEvent.class, hook);
    }

    private void hookCamera2Api(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(CameraManager.class, "openCamera",
                String.class, CameraDevice.StateCallback.class, Handler.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.hasThrowable()) {
                            return;
                        }

                        final CameraManager thiz = (CameraManager) param.thisObject;
                        final CameraCharacteristics characteristics = thiz.getCameraCharacteristics((String) param.args[0]);
                        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing == null) {
                            return;
                        }

                        String facingStr;
                        switch (facing) {
                            case CameraCharacteristics.LENS_FACING_FRONT:
                                facingStr = "front";
                                break;
                            case CameraCharacteristics.LENS_FACING_BACK:
                                facingStr = "back";
                                break;
                            case CameraCharacteristics.LENS_FACING_EXTERNAL:
                                facingStr = "external";
                                break;
                            default:
                                facingStr = "unknown";
                                break;
                        }

                        sendBroadcast(getCurrentApplication(),
                                String.format("using %s camera(legacy)", facingStr));
                    }
                });
    }

    private static Application getCurrentApplication() {
        return (Application) XposedHelpers.callStaticMethod(ACTIVITY_THREAD_CLS, "currentApplication");
    }

    private static void sendBroadcast(Context context, String msg) {
        final Intent intent = new Intent(ToastReceiver.INTENT_ACTION);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra(ToastReceiver.KEY_PKG_NAME, context.getPackageName());
        intent.putExtra(ToastReceiver.KEY_MSG, msg);

        context.sendBroadcast(intent);
    }
}

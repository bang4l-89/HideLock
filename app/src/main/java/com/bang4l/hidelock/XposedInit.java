package com.bang4l.hidelock;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.content.Context;

public class XposedInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("Keyguard Disabler: Loading HandleLoadPackage");
        java.util.ArrayList<String> targetPackages = new java.util.ArrayList<>();
        targetPackages.add("android");
        targetPackages.add("com.android.systemui");
        targetPackages.add("com.google.android.gms");


        XposedBridge.log("Keyguard Disabler: Hooking package: " + lpparam.packageName);
        if (lpparam.packageName.equals("android")) {
            XposedBridge.log("Keyguard Disabler: Loading HookSystem");
            MainHook.hookSystem(lpparam.classLoader);
        } else if (lpparam.packageName.equals("com.android.settings")) {
            XposedBridge.log("Keyguard Disabler: Loading Settings");

            MainHook.hookSettings(lpparam.classLoader);
            XC_MethodHook isSecureMethodHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    param.setResult(false);

                    XposedBridge.log("Keyguard Disabler: isSecure called by: " + lpparam.packageName);
                }
            };

        } else if (lpparam.packageName.equals("com.google.android.gms")) {
            XposedBridge.log("Keyguard Disabler: Loading HookGMS");
            MainHook.hookGMS(lpparam.classLoader);
        }
        else if (targetPackages.contains(lpparam.packageName)) {

            try {
                // Hook KeyguardManager methods
                Class<?> keyguardManagerClass = XposedHelpers.findClass("android.app.KeyguardManager",
                        lpparam.classLoader);

                // Hook isDeviceSecure() - returns true if device has secure lock screen
       /*     XposedHelpers.findAndHookMethod(
                    keyguardManagerClass,
                    "isDeviceSecure",
                    object : XC_MethodHook() {
                override fun afterHookedMethod(param:XC_MethodHook.MethodHookParam) {
                    param.result = false;
                }
            }

         // Hook isDeviceSecure(int userId) - for multi-user devices
        XposedHelpers.findAndHookMethod(
                    keyguardManagerClass,
                    "isDeviceSecure",
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            }
            )
            // Hook isKeyguardSecure() - returns true if keyguard requires password
            XposedHelpers.findAndHookMethod(
                    keyguardManagerClass,
                    "isKeyguardSecure",
                    object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            }
            )
            );*/
                //aaaaaaaaaaaaaaaaaaaaa
                XposedHelpers.findAndHookMethod("android.app.KeyguardManager", lpparam.classLoader, "isDeviceSecure", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardManagerClass, "isDeviceSecure", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardManagerClass, "isKeyguardSecure", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
                //bbbbbbbbbbbbbbbbbbbbb


                // Hook LockPatternUtils methods if available
                try {
                    Class<?> lockPatternUtilsClass = XposedHelpers.findClass(
                            "com.android.internal.widget.LockPatternUtils",
                            lpparam.classLoader
                    );

                    XposedBridge.hookAllMethods(lockPatternUtilsClass, "getKeyguardStoredPasswordQuality", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(false);
                        }
                    });
                    XposedHelpers.findAndHookMethod(lockPatternUtilsClass, "getActivePasswordQuality", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(false);
                        }
                    });
/*
                // Hook getKeyguardStoredPasswordQuality()
                XposedHelpers.findAndHookMethod(
                        lockPatternUtilsClass,
                        "getKeyguardStoredPasswordQuality",
                        object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Return 0 for no security (swipe)
                        param.result = 0
                    }
                }
                )

                // Hook getActivePasswordQuality()
                XposedHelpers.findAndHookMethod(
                        lockPatternUtilsClass,
                        "getActivePasswordQuality",
                        object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Return 0 for no security (swipe)
                        param.result = 0
                    }
                }
                )
*/
                } catch (Throwable t) {
                    XposedBridge.log("FakeLockScreen: LockPatternUtils not available: ${e.message}" + t.getMessage());
                }

            } catch (Throwable t) {
                XposedBridge.log("FakeLockScreen: Error hooking methods: ${e.message}" + t.getMessage());
            }

        }
    }
}
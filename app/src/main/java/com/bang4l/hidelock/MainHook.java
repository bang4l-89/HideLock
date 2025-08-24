package com.bang4l.hidelock;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook {
    private static final int PASSWORD_QUALITY_NUMERIC = 0x20000;

    private static final String LOCK_UTILS_CLASS = "com.android.internal.widget.LockPatternUtils";
    private static final String DPM_CLASS = "android.app.admin.DevicePolicyManager";
    private static final String KEYGUARD_UPDATE_CLASS = "com.android.systemui.keyguard.KeyguardViewMediator";
    // Realme-specific class names
    private static final String REALME_LOCK_UTILS = "com.oplus.lockscreen.OplusLockPatternUtils";
    private static final String REALME_DP_MANAGER = "com.oplus.devicepolicy.OplusDevicePolicyManager";
    private static final String ENGINEER_MODE = "com.oplus.engineermode.EngineerModeManager";


    public static void hookSystem(ClassLoader classLoader) {
        try {
            // Hook Realme's custom lock utils
            hookRealmeLockUtils(classLoader);

            // Hook standard Android classes as fallback
            hookAospClasses(classLoader);

            // Bypass Realme's engineer mode checks
            hookEngineerMode(classLoader);

        } catch (Throwable t) {
            XposedBridge.log("[FakeLock] Error: " + t.getMessage());

        }
    }

    private static void hookRealmeLockUtils(ClassLoader classLoader) {
        try {
            // Realme's isSecure method
          /*  XposedBridge.log("Keyguard Disabler: Realme's isSecure method");
            XposedHelpers.findAndHookMethod(REALME_LOCK_UTILS, classLoader,
                    "isSecure", int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });
*/
            // Realme's password quality method
            XposedBridge.log("Keyguard Disabler: Realme's password quality method");
            XposedHelpers.findAndHookMethod(REALME_LOCK_UTILS, classLoader,
                    "getKeyguardStoredPasswordQuality", Boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(PASSWORD_QUALITY_NUMERIC);
                        }
                    });

        } catch (Throwable t) {
            XposedBridge.log("[FakeLock] Realme classes not found " + t.getMessage());
        }
    }

    private static void hookAospClasses(ClassLoader classLoader) {
        try {
            // Standard Android hooks (fallback)
            XposedBridge.log("Keyguard Disabler: Standard Android hooks");
            XposedHelpers.findAndHookMethod("com.android.internal.widget.LockPatternUtils",
                    classLoader, "isSecure", int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });

            XposedHelpers.findAndHookMethod("android.app.admin.DevicePolicyManager",
                    classLoader, "getPasswordQuality",
                    android.content.ComponentName.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(PASSWORD_QUALITY_NUMERIC);
                        }
                    });

        } catch (Throwable t) {
            XposedBridge.log("[FakeLock] AOSP classes not found");
        }
    }
    public static void hookSettings(ClassLoader classLoader) {
        hookSystem(classLoader); // Reuse system hooks
    }

    public static void hookGMS(ClassLoader classLoader) {
        hookSystem(classLoader); // Reuse system hooks
    }

    private static class SecureHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            param.setResult(true);
        }
    }

    private static class QualityHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            param.setResult(PASSWORD_QUALITY_NUMERIC);
        }
    }
    private static void hookEngineerMode(ClassLoader classLoader) {
        try {
            // Bypass Realme's security validation
            XposedHelpers.findAndHookMethod(ENGINEER_MODE, classLoader,
                    "isSecureLockEnabled", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });

            XposedHelpers.findAndHookMethod(ENGINEER_MODE, classLoader,
                    "validateSecurity", String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(0); // Success code
                        }
                    });

        } catch (Throwable t) {
            XposedBridge.log("[FakeLock] EngineerMode not found");
        }
    }
}

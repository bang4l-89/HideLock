package com.bang4l.hidelock;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Lockscreen implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    XSharedPreferences prefs;
    private long lastLockTime = 0;
    private int lockScreenTimeoutToEnforce;
    private String lockScreenType = LOCK_SCREEN_TYPE_SLIDE;
    private boolean LOG = false;
    private boolean resecuredStatusBar = false;
    private boolean isLocked = true;
    private boolean lockonBootup = false;


    final static String LOCK_SCREEN_TYPE_NONE = "none";
    final static String LOCK_SCREEN_TYPE_SLIDE = "slide";
    final static String LOCK_SCREEN_TYPE_SMART = "smart";
    final static String LOCK_SCREEN_TYPE_DEVICE = "device";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences("com.bang4l.hidelock");
        prefs.makeWorldReadable();
        lockScreenTimeoutToEnforce = Integer.parseInt(prefs.getString("lockscreentimeout", "1")) * 60 * 1000;
        LOG = prefs.getBoolean("logtofile", false);
        lockScreenType = prefs.getString("lockscreentype", LOCK_SCREEN_TYPE_SLIDE);
        lockonBootup = prefs.getBoolean("lockscreenonboot", false);
        // Make sure we start any bootup locked...
    }

    /**
     * Reload our preferences from file, and update
     * any class variables that reflect preference values.
     */
    private void reloadPrefs() {
        prefs.reload();
        lockScreenTimeoutToEnforce = Integer.parseInt(prefs.getString("lockscreentimeout", "1")) * 60 * 1000;
        LOG = prefs.getBoolean("logtofile", false);
        lockScreenType = prefs.getString("lockscreentype", LOCK_SCREEN_TYPE_SLIDE);
        lockonBootup = prefs.getBoolean("lockscreenonboot", false);
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // Android 5.0
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && (
                lpparam.packageName.contains("android.keyguard") || lpparam.packageName.contains("com.android.systemui"))) {
            if (LOG) XposedBridge.log("Keyguard Disabler: Loading Lollipop specific code");
            hookAllNeededMethods(lpparam,
                    "com.android.keyguard.KeyguardSecurityModel$SecurityMode",
                    "com.android.keyguard.KeyguardSecurityModel",
                    "com.android.systemui.keyguard.KeyguardViewMediator",
                    "Android 5.0");
        }
        // Android 4.4
        else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) && lpparam.packageName.contains("android.keyguard")) {
            if (LOG) XposedBridge.log("Keyguard Disabler: Loading Kitkat specific code");
            hookAllNeededMethods(lpparam,
                    "com.android.keyguard.KeyguardSecurityModel$SecurityMode",
                    "com.android.keyguard.KeyguardSecurityModel",
                    "com.android.keyguard.KeyguardViewMediator",
                    "Android 4.4");

        } else if (lpparam.packageName.contains("cyngn.keyguard")) {
            if (LOG) XposedBridge.log("Keyguard Disabler: Loading OnePlus One CM11S specific code");
            hookAllNeededMethods(lpparam,
                    "com.cyngn.keyguard.KeyguardSecurityModel$SecurityMode",
                    "com.cyngn.keyguard.KeyguardSecurityModel",
                    "com.cyngn.keyguard.KeyguardViewMediator",
                    "OnePlus One CM11S");

        }

        // Android > 4.2
        else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) && lpparam.packageName.contains("android.internal")) {
            if (LOG) XposedBridge.log("Keyguard Disabler: Loading Jellybean specific code");
            hookAllNeededMethods(lpparam,
                    "com.android.internal.policy.impl.keyguard.KeyguardSecurityModel$SecurityMode",
                    "com.android.internal.policy.impl.keyguard.KeyguardSecurityModel",
                    "com.android.internal.policy.impl.keyguard.KeyguardViewMediator",
                    "Android > 4.2");
        }

        // HTC

        // Android < 4.2
        else if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) && lpparam.packageName.equals("android")) {
            // This only works for completely disabling screen - not for slide.
            if (LOG) XposedBridge.log("Keyguard Disabler: Loading < Jellybean MR1 specific code");
            hookAllNeededMethods(lpparam,
                    "NOT_USED",
                    "NOT_USED",
                    "com.android.internal.policy.impl.KeyguardViewMediator",
                    "Android < 4.2");
        }
        // Check to hook the settings app
        else if (lpparam.packageName.equals("com.android.settings")) {
            // This allows the settings app to show that 'Swipe' is the default, and therefore gives the ability to customize
            // your swipe settings
            XC_MethodHook isSecureMethodHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    reloadPrefs();
                    if (lockScreenType.equals(LOCK_SCREEN_TYPE_SLIDE)) {
                        param.setResult(false);
                    }
                    if (LOG) XposedBridge.log("Keyguard Disabler: isSecure called by: " + lpparam.packageName);
                }
            };
            // Check whether LockPatternUtil or LockPatternUtils exists (depends on ROM).
            if (LOG) XposedBridge.log("Keyguard Disabler: Detecting LockPatternUtil(s)");
            Class lpu;
            try {
                // XposedHelpers.findClassIfExists may not be present, so use a try-catch block.
                lpu = XposedHelpers.findClass("com.android.internal.widget.LockPatternUtil", lpparam.classLoader);
            } catch (ClassNotFoundError cnfe) {
                lpu = XposedHelpers.findClass("com.android.internal.widget.LockPatternUtils", lpparam.classLoader);
            }
            if (LOG) XposedBridge.log("Keyguard Disabler:    found as " + lpu.getName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                XposedHelpers.findAndHookMethod(lpu, "isSecure", int.class, isSecureMethodHook);
            } else {
                XposedHelpers.findAndHookMethod(lpu, "isSecure", isSecureMethodHook);
            }
        }

        // Else, we didn't find the package.
        else {
            // Add a log of what packages we are NOT hooking for debug purposes.  This helps us identify
            // if there are weird OEM keyguard packages (such as HTC) that we can try and handle.  This
            // Will pollute the log, but should prove helpful (and is off by default).
            // Commenting this out as it's too verbose - if needed one can supply a special build
            //if (LOG) XposedBridge.log("Keyguard Disabler: Avoid Hooking package: " + lpparam.packageName);
        }
    }


    /**
     * Helper method to handle various incarnations of getSecurityMode.  Generally, it will
     * return a security mode of None if we want the keyguard disabled, otherwise it returns
     * the normal value.
     *
     * @param lpparam
     * @param nestedClassName
     * @param outerClassName
     * @param mediatorClassName
     * @param variant
     * @throws ClassNotFoundError
     */
    private void hookAllNeededMethods(final LoadPackageParam lpparam,
                                      final String nestedClassName, final String outerClassName,
                                      final String mediatorClassName, final String variant) throws ClassNotFoundError {

        // We will hook the showLocked method just to completely disable things.  If the 'none' feature
        // is enabled it means they don't ever want to see the lockscreen.  If they've not set 'none'
        // as the value, then we'll just pass right through.
        XC_MethodHook showLockedMethodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                reloadPrefs();
                if (lockScreenType.equals(LOCK_SCREEN_TYPE_NONE)) {
                    if ((lastLockTime != 0) && hideLockBasedOnTimer()) {
                        param.setResult(null);
                    }
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            XposedHelpers.findAndHookMethod(mediatorClassName, lpparam.classLoader, "showLocked", Bundle.class, showLockedMethodHook);
        } else {
            XposedHelpers.findAndHookMethod(mediatorClassName, lpparam.classLoader, "showLocked", showLockedMethodHook);
        }


        // We will know when the lockscreen is enabled or disabled, and update our lastLockTime
        // appropriately.  In short, whenever the lock screen locks, our timer starts.  The one
        // time we care about the lockscreen going away is on initial bootup.  We always want
        // the normal lock to show on initial bootup to ensure somebody can't just reboot the
        // phone to get access...
        XC_MethodHook handleHideMethodHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (LOG) XposedBridge.log("Keyguard Disabler: About to unlock...");
                resecuredStatusBar = false;
                isLocked = false;
                if (lastLockTime == 0) {
                    lastLockTime = SystemClock.elapsedRealtime();
                }
            }
        };
        // handleHide has different method signatures depending on OS flavor.
        if (LOG) XposedBridge.log("Keyguard Disabler: Hooking handleHide...");
        Member handleHide = null;
        Class kvm = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator",
                lpparam.classLoader);
        Method[] ms = kvm.getDeclaredMethods();
        for (Method m : ms) {
            if (m.getName() == "handleHide") {
                handleHide = m;
            }
        }
        if (handleHide != null) {
            XposedBridge.hookMethod(handleHide, handleHideMethodHook);
            if (LOG) XposedBridge.log("Keyguard Disabler: ...handleHide hook installed.");
        } else {
            if (LOG) XposedBridge.log("Keyguard Disabler: ...handleHide hook failed!");
        }

        // handleShow has 2 different method signatures depending on SDK version
        XC_MethodHook handleShowMethodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (lastLockTime != 0) {
                    // If not zero, we'll set it to now. If zero, we'll set it when we first unlock
                    // This ensures at initial bootup, we're always locked.
                    lastLockTime = SystemClock.elapsedRealtime();
                }
                isLocked = true;
                if (LOG)
                    XposedBridge.log("Keyguard Disabler: We are locking...time to enforcement: " + ((lockScreenTimeoutToEnforce - (SystemClock.elapsedRealtime() - lastLockTime)) / 1000));
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            XposedHelpers.findAndHookMethod(mediatorClassName, lpparam.classLoader, "handleShow", Bundle.class, handleShowMethodHook);
        } else {
            XposedHelpers.findAndHookMethod(mediatorClassName, lpparam.classLoader, "handleShow", handleShowMethodHook);
        }


        // Honoring 'slide' only works on jellybean MR1 and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // We'll hook isSecure() directly on the mediator class in order to be able to fixup the status
            // bar after being insecure.
            XposedHelpers.findAndHookMethod(mediatorClassName, lpparam.classLoader, "isSecure", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    boolean setResult = false;
                    if (lockScreenType.equals(LOCK_SCREEN_TYPE_SLIDE)) {
                        if (hideLockBasedOnTimer()) {
                            param.setResult(false);
                        } else if (!resecuredStatusBar && isLocked) {
                            resecuredStatusBar = true;
                            XposedHelpers.callMethod(param.thisObject, "adjustStatusBarLocked");
                            if (LOG) XposedBridge.log("Keyguard Disabler: isSecure called when locked - making sure status bar is secure...");
                        }
                    }
                }
            });


            final Class<?> securityModeEnum = XposedHelpers.findClass(nestedClassName, lpparam.classLoader);
            final Object securityModePassword = XposedHelpers.getStaticObjectField(securityModeEnum, "Password");
            final Object securityModeNone = XposedHelpers.getStaticObjectField(securityModeEnum, "None");
            final Object securityModePattern = XposedHelpers.getStaticObjectField(securityModeEnum, "Pattern");
            final Object securityModePin = XposedHelpers.getStaticObjectField(securityModeEnum, "PIN");
            final Object securityModeBiometric;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            {
                securityModeBiometric = XposedHelpers.getStaticObjectField(securityModeEnum, "Biometric");
            } else
            {
                securityModeBiometric = "bad_field - no biometric in marshmallow";
            }
            // LG KnockOn - likely doesn't work for other knock on implementations...
            Object tmpField;
            try {
                tmpField = XposedHelpers.getStaticObjectField(securityModeEnum, "KnockOn");
            } catch (NoSuchFieldError nsfe) {
                tmpField = "bad_field " + nsfe.getMessage();
            }
            final Object securityModeKnockOn = tmpField;

            if (LOG) XposedBridge.log("Keyguard Disabler: SecurityEnum (" + variant + ") found and parsed");


            // We use the afterHook here to let the normal method handle all the logic of what the
            // security mode should be.  If the normal method returns that it is a password, pin
            // etc., and we want to override it (based on time), then we'll return None instead.
            XposedHelpers.findAndHookMethod(outerClassName, lpparam.classLoader, "getSecurityMode", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Don't reload preferences here..it's too expensive for the frequency
                    // that this method gets called.  We'll reload on lock...

                    // If they requested 'device' - this means they want the mod basically disabled,
                    // so we'll honor the device settings.  If this is SMART, we'll also stick with
                    // the system settings.
                    if (lockScreenType.equals(LOCK_SCREEN_TYPE_DEVICE) || lockScreenType.equals(LOCK_SCREEN_TYPE_SMART)) {
                        return;
                    }

                    if (hideLockBasedOnTimer()) {
                        // Only hide things if we're in password, pattern, pin, biometric, or knock on.
                        // If we're in various SIM modes, we want to show things as normal.
                        Object secModeResult = param.getResult();
                        if (LOG) XposedBridge.log("Keyguard Disabler: secMode is: " + secModeResult);
                        if (securityModePassword.equals(secModeResult) ||
                                securityModePattern.equals(secModeResult) ||
                                securityModePin.equals(secModeResult) ||
                                securityModeBiometric.equals(secModeResult) ||
                                securityModeKnockOn.equals(secModeResult)) {
                            param.setResult(securityModeNone);
                        }
                    }
                }
            });
        }
    }

    private boolean hideLockBasedOnTimer() {
        if (lastLockTime == 0 && lockonBootup) {
            if (LOG) XposedBridge.log("Keyguard Disabler: First boot - lock as normal");
            return false;
        }
        final long currTime = SystemClock.elapsedRealtime();
        if ((lockScreenTimeoutToEnforce > 0) && ((currTime - lastLockTime) > lockScreenTimeoutToEnforce)) {
            if (LOG)
                XposedBridge.log("Keyguard Disabler: Lock as normal...time expired: " + ((currTime - lastLockTime) / 1000 / 60) + " >= " + (lockScreenTimeoutToEnforce / 1000 / 60));
            return false;
        } else {
            if (LOG)
                XposedBridge.log("Keyguard Disabler: Hiding the lock...time not expired: " + ((currTime - lastLockTime) / 1000 / 60) + " <= " + (lockScreenTimeoutToEnforce / 1000 / 60));
            return true;
        }
    }
}

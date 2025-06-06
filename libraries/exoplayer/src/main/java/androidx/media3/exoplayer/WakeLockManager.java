/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.exoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.annotation.Nullable;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.Log;

/**
 * Handles a {@link WakeLock}.
 *
 * <p>The handling of wake locks requires the {@link android.Manifest.permission#WAKE_LOCK}
 * permission.
 */
/* package */ final class WakeLockManager {

  private static final String TAG = "WakeLockManager";
  private static final String WAKE_LOCK_TAG = "ExoPlayer:WakeLockManager";

  private final WakeLockManagerInternal wakeLockManagerInternal;
  private final HandlerWrapper wakeLockHandler;

  private boolean enabled;
  private boolean stayAwake;

  /**
   * Creates the wake lock manager.
   *
   * @param context A {@link Context}
   * @param wakeLockLooper The {@link Looper} to call wake lock system calls on.
   * @param clock The {@link Clock} to schedule handler messages.
   */
  public WakeLockManager(Context context, Looper wakeLockLooper, Clock clock) {
    wakeLockManagerInternal = new WakeLockManagerInternal(context.getApplicationContext());
    wakeLockHandler = clock.createHandler(wakeLockLooper, /* callback= */ null);
  }

  /**
   * Sets whether to enable the acquiring and releasing of the {@link WakeLock}.
   *
   * <p>By default, wake lock handling is not enabled. Enabling this will acquire the wake lock if
   * necessary. Disabling this will release the wake lock if it is held.
   *
   * <p>Enabling {@link WakeLock} requires the {@link android.Manifest.permission#WAKE_LOCK}.
   *
   * @param enabled True if the player should handle a {@link WakeLock}, false otherwise.
   */
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;
    boolean stayAwakeCurrent = stayAwake;
    wakeLockHandler.post(() -> wakeLockManagerInternal.updateWakeLock(enabled, stayAwakeCurrent));
  }

  /**
   * Sets whether to acquire or release the {@link WakeLock}.
   *
   * <p>Please note this method requires wake lock handling to be enabled through setEnabled(boolean
   * enable) to actually have an impact on the {@link WakeLock}.
   *
   * @param stayAwake True if the player should acquire the {@link WakeLock}. False if the player
   *     should release.
   */
  public void setStayAwake(boolean stayAwake) {
    if (this.stayAwake == stayAwake) {
      return;
    }
    this.stayAwake = stayAwake;
    if (enabled) {
      wakeLockHandler.post(
          () -> wakeLockManagerInternal.updateWakeLock(/* enabled= */ true, stayAwake));
    }
  }

  /** Internal methods called on the wifi lock Looper. */
  private static final class WakeLockManagerInternal {

    private final Context applicationContext;

    @Nullable private WakeLock wakeLock;

    public WakeLockManagerInternal(Context applicationContext) {
      this.applicationContext = applicationContext;
    }

    // WakelockTimeout suppressed because the time the wake lock is needed for is unknown (could be
    // listening to radio with screen off for multiple hours), therefore we can not determine a
    // reasonable timeout that would not affect the user.
    @SuppressLint("WakelockTimeout")
    public void updateWakeLock(boolean enabled, boolean stayAwake) {
      if (enabled && wakeLock == null) {
        PowerManager powerManager =
            (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
          Log.w(TAG, "PowerManager is null, therefore not creating the WakeLock.");
          return;
        }
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.setReferenceCounted(false);
      }

      if (wakeLock == null) {
        return;
      }

      if (enabled && stayAwake) {
        wakeLock.acquire();
      } else {
        wakeLock.release();
      }
    }
  }
}

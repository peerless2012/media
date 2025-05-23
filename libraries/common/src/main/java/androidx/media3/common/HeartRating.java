/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.media3.common;

import static androidx.media3.common.util.Assertions.checkArgument;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import java.util.Objects;

/**
 * A rating expressed as "heart" or "no heart". It can be used to indicate whether the content is a
 * favorite.
 */
public final class HeartRating extends Rating {

  private final boolean rated;
  private final boolean isHeart;

  /** Creates a unrated instance. */
  public HeartRating() {
    rated = false;
    isHeart = false;
  }

  /**
   * Creates a rated instance.
   *
   * @param isHeart {@code true} for "heart", {@code false} for "no heart".
   */
  public HeartRating(boolean isHeart) {
    rated = true;
    this.isHeart = isHeart;
  }

  @Override
  public boolean isRated() {
    return rated;
  }

  /** Returns whether the rating is "heart". */
  public boolean isHeart() {
    return isHeart;
  }

  @Override
  public int hashCode() {
    return Objects.hash(rated, isHeart);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof HeartRating)) {
      return false;
    }
    HeartRating other = (HeartRating) obj;
    return isHeart == other.isHeart && rated == other.rated;
  }

  private static final @RatingType int TYPE = RATING_TYPE_HEART;

  private static final String FIELD_RATED = Util.intToStringMaxRadix(1);
  private static final String FIELD_IS_HEART = Util.intToStringMaxRadix(2);

  @UnstableApi
  @Override
  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    bundle.putInt(FIELD_RATING_TYPE, TYPE);
    bundle.putBoolean(FIELD_RATED, rated);
    bundle.putBoolean(FIELD_IS_HEART, isHeart);
    return bundle;
  }

  /** Restores a {@code HeartRating} from a {@link Bundle}. */
  @UnstableApi
  public static HeartRating fromBundle(Bundle bundle) {
    checkArgument(bundle.getInt(FIELD_RATING_TYPE, /* defaultValue= */ RATING_TYPE_UNSET) == TYPE);
    boolean isRated = bundle.getBoolean(FIELD_RATED, /* defaultValue= */ false);
    return isRated
        ? new HeartRating(bundle.getBoolean(FIELD_IS_HEART, /* defaultValue= */ false))
        : new HeartRating();
  }
}

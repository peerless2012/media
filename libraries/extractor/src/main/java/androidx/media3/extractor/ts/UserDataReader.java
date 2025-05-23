/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.media3.extractor.ts;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.CeaUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.List;

/** Consumes user data, outputting contained CEA-608/708 messages to a {@link TrackOutput}. */
/* package */ final class UserDataReader {

  private static final int USER_DATA_START_CODE = 0x0001B2;

  private final List<Format> closedCaptionFormats;
  private final String containerMimeType;
  private final TrackOutput[] outputs;

  public UserDataReader(List<Format> closedCaptionFormats, String containerMimeType) {
    this.closedCaptionFormats = closedCaptionFormats;
    this.containerMimeType = containerMimeType;
    outputs = new TrackOutput[closedCaptionFormats.size()];
  }

  public void createTracks(
      ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
    for (int i = 0; i < outputs.length; i++) {
      idGenerator.generateNewId();
      TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_TEXT);
      Format channelFormat = closedCaptionFormats.get(i);
      @Nullable String channelMimeType = channelFormat.sampleMimeType;
      Assertions.checkArgument(
          MimeTypes.APPLICATION_CEA608.equals(channelMimeType)
              || MimeTypes.APPLICATION_CEA708.equals(channelMimeType),
          "Invalid closed caption MIME type provided: " + channelMimeType);
      output.format(
          new Format.Builder()
              .setId(idGenerator.getFormatId())
              .setContainerMimeType(containerMimeType)
              .setSampleMimeType(channelMimeType)
              .setSelectionFlags(channelFormat.selectionFlags)
              .setLanguage(channelFormat.language)
              .setAccessibilityChannel(channelFormat.accessibilityChannel)
              .setInitializationData(channelFormat.initializationData)
              .build());
      outputs[i] = output;
    }
  }

  public void consume(long pesTimeUs, ParsableByteArray userDataPayload) {
    if (userDataPayload.bytesLeft() < 9) {
      return;
    }
    int userDataStartCode = userDataPayload.readInt();
    int userDataIdentifier = userDataPayload.readInt();
    int userDataTypeCode = userDataPayload.readUnsignedByte();
    if (userDataStartCode == USER_DATA_START_CODE
        && userDataIdentifier == CeaUtil.USER_DATA_IDENTIFIER_GA94
        && userDataTypeCode == CeaUtil.USER_DATA_TYPE_CODE_MPEG_CC) {
      CeaUtil.consumeCcData(pesTimeUs, userDataPayload, outputs);
    }
  }
}

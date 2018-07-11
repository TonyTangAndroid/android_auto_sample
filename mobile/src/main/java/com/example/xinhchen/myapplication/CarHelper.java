/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.example.xinhchen.myapplication;

import android.os.Bundle;

public class CarHelper {
    private static final String AUTO_APP_PACKAGE_NAME = "com.google.android.projection.gearhead";

    /**
     * Action for an intent broadcast by Android Auto when a media app is connected or
     * disconnected. A "connected" media app is the one currently attached to the "media" facet
     * on Android Auto. So, this intent is sent by AA on:
     *
     * - connection: when the phone is projecting and at the moment the app is selected from the
     *       list of media apps
     * - disconnection: when another media app is selected from the list of media apps or when
     *       the phone stops projecting (when the user unplugs it, for example)
     *
     * The actual event (connected or disconnected) will come as an Intent extra,
     * with the key MEDIA_CONNECTION_STATUS (see below).
     */
    public static final String ACTION_MEDIA_STATUS = "com.google.android.gms.car.media.STATUS";

    /**
     * Key in Intent extras that contains the media connection event type (connected or disconnected)
     */
    public static final String MEDIA_CONNECTION_STATUS = "media_connection_status";

    /**
     * Value of the key MEDIA_CONNECTION_STATUS in Intent extras used when the current media app
     * is connected.
     */
    public static final String MEDIA_CONNECTED = "media_connected";


    public static boolean isValidCarPackage(String packageName) {
        return AUTO_APP_PACKAGE_NAME.equals(packageName);
    }

}

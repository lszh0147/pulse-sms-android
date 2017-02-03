/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.MmsSettings;

/**
 * Utility for helping to send messages.
 */
public class SendUtils {

    private Integer subscriptionId;

    public SendUtils() {
        this(null);
    }

    public SendUtils(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void send(Context context, String text, String address) {
        send(context, text, address.split(", "), null, null, subscriptionId);
    }

    public void send(Context context, String text, String[] addresses) {
        send(context, text, addresses, null, null, subscriptionId);
    }

    public Uri send(Context context, String text, String addresses, Uri data,
                           String mimeType) {
        return send(context, text, addresses.split(", "), data, mimeType, subscriptionId);
    }

    public Uri send(Context context, String text, String[] addresses, Uri data,
                           String mimeType, Integer subscriptionId) {
        xyz.klinker.messenger.shared.data.Settings appSettings = xyz.klinker.messenger.shared.data.Settings.get(context);
        if (!appSettings.signature.isEmpty()) {
            text += "\n" + appSettings.signature;
        }

        MmsSettings mmsSettings = MmsSettings.get(context);
        Settings settings = new Settings();
        settings.setDeliveryReports(xyz.klinker.messenger.shared.data.Settings.get(context)
                .deliveryReports);
        settings.setSendLongAsMms(mmsSettings.convertLongMessagesToMMS);
        settings.setGroup(mmsSettings.groupMMS);
        settings.setStripUnicode(xyz.klinker.messenger.shared.data.Settings.get(context)
                .stripUnicode);
        settings.setPreText(xyz.klinker.messenger.shared.data.Settings.get(context)
                .giffgaffDeliveryReports ? "*0#" : "");

        if (mmsSettings.overrideSystemAPN) {
            settings.setUseSystemSending(false);

            settings.setMmsc(mmsSettings.mmscUrl);
            settings.setProxy(mmsSettings.mmsProxy);
            settings.setPort(mmsSettings.mmsPort);
            settings.setAgent(mmsSettings.userAgent);
            settings.setUserProfileUrl(mmsSettings.userAgentProfileUrl);
            settings.setUaProfTagName(mmsSettings.userAgentProfileTagName);
        }

        if (subscriptionId != null && subscriptionId != 0 && subscriptionId != -1) {
            settings.setSubscriptionId(subscriptionId);
        }

        Transaction transaction = new Transaction(context, settings);
        Message message = new Message(text, addresses);

        if (data != null) {
            try {
                if (MimeType.isStaticImage(mimeType)) {
                    data = ImageUtils.scaleToSend(context, data);
                    mimeType = MimeType.IMAGE_JPEG;
                }

                byte[] bytes = getBytes(context, data);
                Log.v("Sending MMS", "size: " + bytes.length + " bytes, mime type: " + mimeType);
                message.addMedia(bytes, mimeType);
            } catch (NullPointerException e) {
                Log.e("Sending Exception", "Could not attach media: " + data, e);
            } catch (IOException e) {
                Log.e("Sending Exception", "Could not attach media: " + data, e);
            }
        }

        if (Account.get(context).primary) {
            try {
                transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
            } catch (IllegalArgumentException e) {
                
            }
        }

        return data;
    }

    public  static byte[] getBytes(Context context, Uri data) throws IOException, NullPointerException {
        InputStream stream = context.getContentResolver().openInputStream(data);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = stream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        stream.close();

        return byteBuffer.toByteArray();
    }

}
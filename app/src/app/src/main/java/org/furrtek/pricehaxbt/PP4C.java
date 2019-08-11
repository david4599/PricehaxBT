package org.furrtek.pricehaxbt;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;

import java.io.IOException;

public class PP4C {
    static AudioManager mAudioManager;
    static int origVolume;
    static double[] sample = new double[48000];

    private static void sendData(byte[] tmp) {
        Log.d("BT SEND", "SENDING DATA...");
        try {
            byte[] buffer = new byte[128];
            MainActivity.outStream.write(tmp);
            do {
            } while (MainActivity.inStream.available() <= 0);

            int res = MainActivity.inStream.read(buffer);
            Log.d("DATA", "Received:" + res);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendPP4C(Context context, byte[] hcode, int length, int donglever, int rpt, AudioTrack audioTrack, int nbRepeatFrame) {
        double[] pcode = new double[256];
        byte[] generatedSnd = new byte[96000];
        if (donglever == 3) {
            int cp;
            byte[] btdata = new byte[58];
            btdata[0] = (byte) -86;
            btdata[1] = (byte) (nbRepeatFrame >> 8);
            btdata[2] = (byte) (nbRepeatFrame & MotionEventCompat.ACTION_MASK);
            btdata[3] = (byte) length;
            for (cp = 0; cp < length; cp++) {
                btdata[cp + 4] = hcode[cp];
            }
            String plHexString = "";
            for (cp = 0; cp < btdata.length; cp++) {
                plHexString = plHexString + String.format("%02X", new Object[]{Byte.valueOf(btdata[cp])});
            }
            sendData(btdata);
            Log.d("DATA", "SENT " + plHexString);
        }
        if (donglever < 3) {
            int i;
            if (donglever == 2) {
                pcode[0] = (double) (rpt & 3);
                pcode[1] = (double) ((rpt >> 2) & 3);
                pcode[2] = (double) ((rpt >> 4) & 3);
                pcode[3] = (double) ((rpt >> 6) & 3);
                length++;
                for (i = 1; i < length; i++) {
                    pcode[(i * 4) + 0] = (double) (hcode[i - 1] & 3);
                    pcode[(i * 4) + 1] = (double) ((hcode[i - 1] >> 2) & 3);
                    pcode[(i * 4) + 2] = (double) ((hcode[i - 1] >> 4) & 3);
                    pcode[(i * 4) + 3] = (double) ((hcode[i - 1] >> 6) & 3);
                }
            } else {
                for (i = 0; i < length; i++) {
                    pcode[(i * 4) + 0] = (double) (hcode[i] & 3);
                    pcode[(i * 4) + 1] = (double) ((hcode[i] >> 2) & 3);
                    pcode[(i * 4) + 2] = (double) ((hcode[i] >> 4) & 3);
                    pcode[(i * 4) + 3] = (double) ((hcode[i] >> 6) & 3);
                }
            }
            mAudioManager = (AudioManager) context.getSystemService("audio");
            origVolume = mAudioManager.getStreamVolume(3);
            mAudioManager.setStreamVolume(3, (int) (((double) mAudioManager.getStreamMaxVolume(3)) * (((double) (((float) MainActivity.transmitVolume) / 100.0f)) + 0.6d)), 0);
            for (int r = 0; r < 48000; r++) {
                sample[r] = 0.0d;
            }
            double pw = 0.0d;
            i = 0;
            while (i < 15) {
                int a = 0;
                double pw2 = pw;
                while (a < 40) {
                    pw = pw2 + 1.0d;
                    sample[(int) pw2] = 0.2d;
                    a++;
                    pw2 = pw;
                }
                a = 0;
                while (a < 40) {
                    pw = pw2 + 1.0d;
                    sample[(int) pw2] = -0.2d;
                    a++;
                    pw2 = pw;
                }
                i++;
                pw = pw2;
            }
            sample[(int) pw] = 1.0d;
            sample[((int) pw) + 1] = 1.0d;
            sample[((int) pw) + 2] = -1.0d;
            sample[((int) pw) + 3] = -1.0d;
            for (i = 0; i < length * 4; i++) {
                if (pcode[i] == 0.0d) {
                    pw += 12.0d;
                }
                if (pcode[i] == 1.0d) {
                    pw += 12.0d;
                }
                if (pcode[i] == 2.0d) {
                    pw += 6.0d;
                }
                if (pcode[i] == 3.0d) {
                    pw += 6.0d;
                }
                sample[(int) pw] = 1.0d;
                sample[((int) pw) + 1] = 1.0d;
                sample[((int) pw) + 2] = -1.0d;
                sample[((int) pw) + 3] = -1.0d;
                if (pcode[i] == 0.0d) {
                    pw += 12.0d;
                }
                if (pcode[i] == 1.0d) {
                    pw += 6.0d;
                }
                if (pcode[i] == 2.0d) {
                    pw += 12.0d;
                }
                if (pcode[i] == 3.0d) {
                    pw += 6.0d;
                }
                sample[(int) pw] = 1.0d;
                sample[((int) pw) + 1] = 1.0d;
                sample[((int) pw) + 2] = -1.0d;
                sample[((int) pw) + 3] = -1.0d;
            }
            int idx = 0;
            for (double dVal : sample) {
                short val = (short) ((int) (32767.0d * dVal));
                int i2 = idx + 1;
                generatedSnd[idx] = (byte) (val & MotionEventCompat.ACTION_MASK);
                idx = i2 + 1;
                generatedSnd[i2] = (byte) ((MotionEventCompat.ACTION_POINTER_INDEX_MASK & val) >>> 8);
            }
            audioTrack.write(generatedSnd, 0, 48000);
            audioTrack.setNotificationMarkerPosition(48000);
            try {
                audioTrack.play();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            audioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
                public void onMarkerReached(AudioTrack track) {
                    try {
                        track.stop();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    PP4C.mAudioManager.setStreamVolume(3, PP4C.origVolume, 0);
                }

                public void onPeriodicNotification(AudioTrack track) {
                }
            });
        }
    }
}

package org.furrtek.pricehaxbt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.TransportMediator;
import android.support.v4.view.MotionEventCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;

public class MainActivity extends Activity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int SELECT_PHOTO = 100;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    public static InputStream inStream = null;
    public static OutputStream outStream = null;
    static AudioTrack audioTrack;
    static BluetoothAdapter btAdapter = null;
    static BluetoothSocket btSocket = null;
    static boolean btok = false;
    static int transmitVolume;

    static {
        System.loadLibrary("iconv");
    }

    final int REQUEST_ENABLE_BT = 1;
    Integer PLType;
    Activity at = this;
    int donglever;
    int rawmode;
    Handler handler = new Handler();
    int hi;
    InputStream imageStream = null;
    ImageView imgbmp;
    TextView label_plhex;
    ProgressBar pgb;
    int plBitDef;
    long plID;
    int ESLType = 0;
    boolean ESLTypeColor = false;
    boolean threadRunning = true;
    int compression_type = 0;
    int datalen = 0;
    int padded_datalen = 0;
    FrameLayout preview;
    byte[] rawbitstream;
    Bitmap scaledimage;
    Bitmap scaledimagepart1;
    Bitmap scaledimagepart2;
    List<Integer> compressed;
    Button scanButton;
    Button btnsendimg;
    Button btnmanualbarcode;
    Button btnsetbarcode;
    TextView scaneibarcode;
    TextView scaneiserial;
    TextView scaneitype;
    ImageScanner scanner;
    SharedPreferences settings;
    Spinner spinner;
    TabHost tabHost;
    int tabPos;
    Timer timer;
    Timer timerdm;
    TextView txtworkh;
    int wi;
    int x;
    int y;
    int idx;
    int ymax;
    int nbRepeatFrame;
    String plHexString;
    private Handler autoFocusHandler;
    private boolean barcodeScanned = false;
    private Camera mCamera;
    private CameraPreview mPreview;
    private boolean previewing = true;
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (MainActivity.this.previewing && MainActivity.this.tabHost.getCurrentTab() == 1 && MainActivity.this.mCamera != null) {
                MainActivity.this.mCamera.autoFocus(MainActivity.this.autoFocusCB);
            }
        }
    };
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            MainActivity.this.autoFocusHandler.postDelayed(MainActivity.this.doAutoFocus, 1000);
        }
    };
    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Size size = camera.getParameters().getPreviewSize();
            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);
            if (MainActivity.this.scanner.scanImage(barcode) != 0) {
                MainActivity.this.previewing = false;
                MainActivity.this.mCamera.setPreviewCallback(null);
                MainActivity.this.mCamera.stopPreview();
                Iterator it = MainActivity.this.scanner.getResults().iterator();
                while (it.hasNext()) {
                    String PLBarcode = ((Symbol) it.next()).getData();
                    setESLBarcode(PLBarcode);
                }
            }
        }
    };

    private void setESLBarcode(String PLBarcode) {
        MainActivity.this.plID = (long) ((Integer.parseInt(PLBarcode.substring(2, 7)) << 16) + Integer.parseInt(PLBarcode.substring(7, 12)));
        String PLSerial = Long.toHexString(MainActivity.this.plID);
        MainActivity.this.PLType = Integer.valueOf(Integer.parseInt(PLBarcode.substring(12, 16)));
        MainActivity.this.scaneibarcode.setText("Barcode: " + PLBarcode);
        MainActivity.this.scaneiserial.setText("ID: " + PLSerial.toUpperCase());
        switch (MainActivity.this.PLType.intValue()) {
            case 1206:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E2 HCS)");
                MainActivity.this.ESLType = 1;
                break;
            case 1207:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E2 HCN)");
                MainActivity.this.plBitDef = 4;
                MainActivity.this.ESLType = 1;
                break;


            case 1240:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E4 HCS)");
                MainActivity.this.plBitDef = 3;
                MainActivity.this.ESLType = 1;
                break;
            case 1241:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E4 HCN)");
                MainActivity.this.plBitDef = 0;
                MainActivity.this.ESLType = 1;
                break;
            case 1242:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E4 HCN FZ)");
                MainActivity.this.plBitDef = 0;
                MainActivity.this.ESLType = 1;
                break;


            case 1217:
            case 1265:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E5 HCS)");
                MainActivity.this.plBitDef = 2;
                MainActivity.this.ESLType = 1;
                break;
            case 1219:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (Continuum E5 HCN)");
                MainActivity.this.plBitDef = 1;
                MainActivity.this.ESLType = 1;
                break;


            case 1291:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (FVL Promoline 3-16 (18619-00) segments bitmap not done !)");
                MainActivity.this.ESLType = 1;
                break;


            case 1510:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag E5 M)");
                MainActivity.this.plBitDef = 1;
                MainActivity.this.ESLType = 1;
                break;


            case 1300:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (DotMatrix DM3370 172x72)");
                MainActivity.this.wi = 172;
                MainActivity.this.hi = 72;
                MainActivity.this.ESLType = 2;
                break;
            case 1276:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (DotMatrix DM90 320x140) EXPERIMENTAL");
                MainActivity.this.wi = 320;
                MainActivity.this.hi = 140;
                MainActivity.this.ESLType = 2;
                break;
            case 1275:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (DotMatrix DM110 320x192 (13400-00)) EXPERIMENTAL");
                MainActivity.this.wi = 320;
                MainActivity.this.hi = 192;
                MainActivity.this.ESLType = 2;
                break;


            case 1317:
            case 1322:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD S 152x152)");
                MainActivity.this.wi = 152;
                MainActivity.this.hi = 152;
                MainActivity.this.ESLType = 2;
                break;
            case 1339:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD S Red 152x152) EXPERIMENTAL");
                MainActivity.this.wi = 152;
                MainActivity.this.hi = 152;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;


            case 1318:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD M 208x112)");
                MainActivity.this.wi = 208;
                MainActivity.this.hi = 112;
                MainActivity.this.ESLType = 2;
                break;
            case 1327:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD M Red 208x112)");
                MainActivity.this.wi = 208;
                MainActivity.this.hi = 112;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;
            case 1324:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD M FZ 208x112) EXPERIMENTAL");
                MainActivity.this.wi = 208;
                MainActivity.this.hi = 112;
                MainActivity.this.ESLType = 2;
                break;


            case 1315:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD L 296x128) EXPERIMENTAL");
                MainActivity.this.wi = 296;
                MainActivity.this.hi = 128;
                MainActivity.this.ESLType = 2;
                break;

            case 1370: // 2021 revision of the SmartTag HD L Red 296x128 - black housing
            case 1328:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD L Red 296x128)");
                MainActivity.this.wi = 296;
                MainActivity.this.hi = 128;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;
            case 1344:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD L Yellow 296x128) EXPERIMENTAL");
                MainActivity.this.wi = 296;
                MainActivity.this.hi = 128;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;


            case 1348:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD T Red 264x176) EXPERIMENTAL");
                MainActivity.this.wi = 264;
                MainActivity.this.hi = 176;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;
            case 1349:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD T Yellow 264x176) EXPERIMENTAL");
                MainActivity.this.wi = 264;
                MainActivity.this.hi = 176;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;


            case 1314:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD110 400x300) EXPERIMENTAL");
                MainActivity.this.wi = 400;
                MainActivity.this.hi = 300;
                MainActivity.this.ESLType = 2;
                break;
            case 1336:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD110 Red 400x300) EXPERIMENTAL");
                MainActivity.this.wi = 400;
                MainActivity.this.hi = 300;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;


            case 1351:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD150 648x480) EXPERIMENTAL");
                MainActivity.this.wi = 648;
                MainActivity.this.hi = 480;
                MainActivity.this.ESLType = 2;
                break;
            case 1371: // 2021 revision of the SmartTag HD150 Red 648x480 - black housing
            case 1353:
            case 1354:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD150 Red 648x480)");
                MainActivity.this.wi = 648;
                MainActivity.this.hi = 480;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;


            case 1319:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD200 800x480) EXPERIMENTAL");
                MainActivity.this.wi = 800;
                MainActivity.this.hi = 480;
                MainActivity.this.ESLType = 2;
                break;
            case 1340:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD200 Red 800x480) EXPERIMENTAL");
                MainActivity.this.wi = 800;
                MainActivity.this.hi = 480;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;
            case 1346:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (SmartTag HD200 Yellow 800x480) EXPERIMENTAL");
                MainActivity.this.wi = 800;
                MainActivity.this.hi = 480;
                MainActivity.this.ESLType = 2;
                MainActivity.this.ESLTypeColor = true;
                break;

            default:
                MainActivity.this.scaneitype.setText("Type: " + MainActivity.this.PLType + " (incompatible)");
                MainActivity.this.ESLType = -1;
                break;
        }
        MainActivity.this.barcodeScanned = true;
    }

    private boolean repeatMode = false;
    private boolean repeatModeDM = false;

    private static void checkBTState() {
        BluetoothDevice device = null;


        if (btAdapter == null) {
            Log.d("BT", "NO BT ADAPTER");
        } else if (btAdapter.isEnabled()) {
            Log.d("BT", "BT ENABLED");
            for (BluetoothDevice bt : btAdapter.getBondedDevices()) {
                Log.d("BT", "LIST: " + bt.getName());
                if (bt.getName().equals("PRICEHAX TX V3")) {
                    device = btAdapter.getRemoteDevice(bt.getAddress());
                    Log.d("BT", "FOUND BT PRICEHAX TX V3 :)");
                    break;
                }
            }
            if (device != null) {

                if (btok == true) {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (Exception e) {
                            Log.d("BT", "EXCEPTION IN CLOSING INSTREAM");
                        }
                        inStream = null;
                    }

                    if (outStream != null) {
                        try {
                            outStream.close();
                        } catch (Exception e) {
                            Log.d("BT", "EXCEPTION IN CLOSING OUTSTREAM");
                        }
                        outStream = null;
                    }

                    if (btSocket != null) {
                        try {
                            btSocket.close();
                        } catch (Exception e) {
                            Log.d("BT", "EXCEPTION IN CLOSING SOCKET");
                        }
                        btSocket = null;
                    }
                    btok = false;
                    Log.d("BT", "DEVICE DISCONNECTED");

                } else {
                    try {
                        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    } catch (IOException e) {
                        Log.d("BT", "EXCEPTION IN RF COMM SOCKET CREATION");
                    }
                    btAdapter.cancelDiscovery();
                    Log.d("BT", "CONNECTING...");
                    try {
                        btSocket.connect();
                        btok = true;
                        Log.d("BT", "CONNECTION OK");

                    } catch (IOException e2) {
                        try {
                            btSocket.close();
                        } catch (IOException e3) {
                            Log.d("BT", "EXCEPTION IN CONNECT");
                        }
                    }
                    try {
                        outStream = btSocket.getOutputStream();
                        inStream = btSocket.getInputStream();
                    } catch (IOException e4) {
                        Log.d("BT", "EXCEPTION IN STREAM CREATION");
                    }
                }
            }
        }
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(0);
        } catch (Exception e) {
        }
        return c;
    }

    public void launchBarDialog(View view) {
        Intent photoPickerIntent = new Intent("android.intent.action.PICK");
        photoPickerIntent.setType("image/*");
        photoPickerIntent.setAction("android.intent.action.GET_CONTENT");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case SELECT_PHOTO /*100*/:
                if (resultCode == -1 && imageReturnedIntent.getData() != null) {
                    try {
                        this.imageStream = getContentResolver().openInputStream(imageReturnedIntent.getData());
                        this.imgbmp = (ImageView) findViewById(R.id.imgvbmp);
                        this.scaledimage = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(this.imageStream), this.wi, this.hi, true);
                        this.imgbmp.setImageBitmap(this.scaledimage);
                        return;
                    } catch (Exception e) {
                        Log.d("PHX", e.getLocalizedMessage());
                        return;
                    }
                }
                return;
            default:
                return;
        }
    }




    private void convertMonochrome(Bitmap image, boolean color) {
        MainActivity mainActivity;
        MainActivity.this.y = 0;
        int w = image.getWidth();
        int h = image.getHeight();
        final int width = w;
        while (MainActivity.this.y < h) {
            MainActivity.this.x = 0;
            while (MainActivity.this.x < w) {
                int pixel = image.getPixel(MainActivity.this.x, MainActivity.this.y);
                if (!color) {
                    if (((int) (((0.299d * ((double) Color.red(pixel))) + (0.587d * ((double) Color.green(pixel)))) + (0.114d * ((double) Color.blue(pixel))))) < 128) {
                        MainActivity.this.rawbitstream[MainActivity.this.idx] = (byte) 0;
                    } else {
                        MainActivity.this.rawbitstream[MainActivity.this.idx] = (byte) 1;
                    }
                }
                else {
                    if (((int) ((double) Color.red(pixel))) >= 80 && ((int) ((double) Color.green(pixel))) < 80 && ((int) ((double) Color.blue(pixel))) < 80) {
                        MainActivity.this.rawbitstream[MainActivity.this.idx] = (byte) 0;
                    } else {
                        MainActivity.this.rawbitstream[MainActivity.this.idx] = (byte) 1;
                    }
                }

                mainActivity = MainActivity.this;
                mainActivity.x++;
                MainActivity.this.idx++;
            }

            MainActivity.this.handler.post(new Runnable() {
                public void run() {
                    MainActivity.this.pgb.setProgress((MainActivity.this.y * 36) / width);
                }
            });
            if (!color) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.this.txtworkh.setText("Converting to monochrome: line " + MainActivity.this.y);
                    }
                });
            }
            else {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.this.txtworkh.setText("Converting to monochrome color: line " + MainActivity.this.y);
                    }
                });
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mainActivity = MainActivity.this;
            mainActivity.y++;

            if (!threadRunning) {
                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.pgb.setProgress(100);
                        MainActivity.this.txtworkh.setText("Stopped successfully !");
                    }
                });

                return;
            }
        }
    }




    private void RLECompress() {
        MainActivity mainActivity;
        int j = MainActivity.this.idx - 1;
        int cnt = 1;
        byte p = MainActivity.this.rawbitstream[0];
        MainActivity.this.y = 0;

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.txtworkh.setText("RLE compress...");
            }
        });
        int m = 1;
        while (m <= j) {
            byte n = MainActivity.this.rawbitstream[m];
            if (n == p) {
                cnt++;

                if (m == j - 1) {
                    MainActivity.this.compressed.add(Integer.valueOf(cnt));
                }
            } else {
                MainActivity.this.compressed.add(Integer.valueOf(cnt));
                cnt = 1;
                if (m == j - 1) {
                    MainActivity.this.compressed.add(Integer.valueOf(1));
                }
            }
            p = n;
            if ((m & 31) == 0) {
                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.pgb.setProgress((MainActivity.this.y / 1238) + 36);
                    }
                });
            }
            m++;
            mainActivity = MainActivity.this;
            mainActivity.y++;

            if (!threadRunning) return;
        }
    }


    private void Hexadecimalifying(StringBuilder bstr_raw, StringBuilder bstr_compressed) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.txtworkh.setText("Hexadecimalifying...");
            }
        });

        // "rawbitstream" to StringBuilder
        for (int countbit = 0; countbit < MainActivity.this.rawbitstream.length; countbit++) {
            Integer intValue = (int) MainActivity.this.rawbitstream[countbit];
            String bs = Integer.toBinaryString(intValue.intValue());
            bstr_raw.append(bs);
        }


        // "compressed" to StringBuilder
        bstr_compressed.append(MainActivity.this.rawbitstream[0]);
        for (Integer intValue : MainActivity.this.compressed) {
            int bsc;
            String bs = Integer.toBinaryString(intValue.intValue());
            StringBuffer stringBuffer = new StringBuffer(bs.length());
            for (bsc = 0; bsc < bs.length() - 1; bsc++) {
                stringBuffer.append("0");
            }
            bstr_compressed.append(stringBuffer.toString());
            bstr_compressed.append(bs);
        }
    }





    private List<Byte> createCompressedHexList(StringBuilder bstr_compressed) {
        compression_type = 2;

        List<Byte> hexlist;
        int klp = bstr_compressed.toString().length() % 320;
        if (klp > 0) {
            klp = 320 - klp;
        }
        for (int bsc = 0; bsc < klp; bsc++) {
            bstr_compressed.append("0");
        }

        String bstr_compressed_string = bstr_compressed.toString();
        hexlist = new ArrayList<Byte>();
        for (int bsc = 0; bsc < bstr_compressed_string.length(); bsc += 8) {
            hexlist.add(Byte.valueOf((byte) Integer.parseInt(bstr_compressed_string.substring(bsc, bsc + 8), 2)));
        }

        datalen = hexlist.size();
        padded_datalen = hexlist.size();

        return hexlist;
    }




    private List<Byte> createRawHexList(StringBuilder bstr_raw) {
        compression_type = 0;

        List<Byte> hexlist;
        String bstr_raw_string = bstr_raw.toString();
        hexlist = new ArrayList<Byte>();
        for (int bsc = 0; bsc < bstr_raw_string.length(); bsc += 8) {
            hexlist.add(Byte.valueOf((byte) Integer.parseInt(bstr_raw_string.substring(bsc, bsc + 8), 2)));
        }

        datalen = hexlist.size();


        int klp = bstr_raw.toString().length() % 320;
        if (klp > 0) {
            klp = 320 - klp;
        }
        for (int bsc = 0; bsc < klp; bsc++) {
            bstr_raw.append("0");
        }

        bstr_raw_string = bstr_raw.toString();
        hexlist = new ArrayList<Byte>();
        for (int bsc = 0; bsc < bstr_raw_string.length(); bsc += 8) {
            hexlist.add(Byte.valueOf((byte) Integer.parseInt(bstr_raw_string.substring(bsc, bsc + 8), 2)));
        }

        padded_datalen = hexlist.size();

        return hexlist;
    }








    private void sendPingCode() {
        MainActivity.audioTrack = new AudioTrack(3, 48000, 4, 2, 48000, 1);
        byte[] pingcode = new byte[]{(byte) -123, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -105, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1};
        pingcode[1] = (byte) ((int) (MainActivity.this.plID & 255));
        pingcode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
        pingcode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
        pingcode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
        byte[] FrameCRC = CRCCalc.GetCRC(pingcode, 30);
        pingcode[30] = FrameCRC[0];
        pingcode[31] = FrameCRC[1];

        if (!threadRunning) return;
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.txtworkh.setText("Waking up ESL...");
            }
        });
        PP4C.sendPP4C(MainActivity.this.at.getApplicationContext(), pingcode, 32, MainActivity.this.donglever, 50, MainActivity.audioTrack, 250);
        if (MainActivity.this.donglever == 2) {
            SystemClock.sleep(2500);
        } else if (MainActivity.this.donglever == 1) {
            SystemClock.sleep(1800);
            PP4C.sendPP4C(MainActivity.this.at.getApplicationContext(), pingcode, 32, MainActivity.this.donglever, 35, MainActivity.audioTrack, 0);
            SystemClock.sleep(1800);
            PP4C.sendPP4C(MainActivity.this.at.getApplicationContext(), pingcode, 32, MainActivity.this.donglever, 35, MainActivity.audioTrack, 0);
            SystemClock.sleep(1800);
        }
    }




    private void sendStartCode(int part) {
        byte[] startcode = new byte[54];
        startcode[0] = (byte) -123;
        startcode[1] = (byte) ((int) (MainActivity.this.plID & 255));
        startcode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
        startcode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
        startcode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
        startcode[5] = (byte) 52;
        startcode[6] = (byte) 0;
        startcode[7] = (byte) 0;
        startcode[8] = (byte) 0;
        startcode[9] = (byte) 5;
        startcode[10] = (byte) (datalen >> 8);
        startcode[11] = (byte) (datalen & MotionEventCompat.ACTION_MASK);
        byte[] bArr = new byte[20];
        if (part == 0) {
            bArr = new byte[]{(byte) 0, (byte) compression_type, (byte) 2, (byte) (MainActivity.this.wi >> 8), (byte) (MainActivity.this.wi & 255), (byte) (MainActivity.this.hi >> 8), (byte) (MainActivity.this.hi & 255), (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -120, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        }
        else if (part == 1) {
            bArr = new byte[]{(byte) 0, (byte) compression_type, (byte) 2, (byte) (MainActivity.this.wi >> 8), (byte) (MainActivity.this.wi & 255), (byte) ((MainActivity.this.hi/2) >> 8), (byte) ((MainActivity.this.hi/2) & 255), (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -120, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        }
        else if (part == 2) {
            bArr = new byte[]{(byte) 0, (byte) compression_type, (byte) 2, (byte) (MainActivity.this.wi >> 8), (byte) (MainActivity.this.wi & 255), (byte) ((MainActivity.this.hi/2) >> 8), (byte) ((MainActivity.this.hi/2) & 255), (byte) 0, (byte) 0, (byte) ((MainActivity.this.hi/2) >> 8), (byte) ((MainActivity.this.hi/2) & 255), (byte) 0, (byte) 0, (byte) -120, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        }

        for (int cp = 0; cp < 20; cp++) {
            startcode[cp + 12] = bArr[cp];
        }

        byte[] FrameCRC = CRCCalc.GetCRC(startcode, 32);
        startcode[32] = FrameCRC[0];
        startcode[33] = FrameCRC[1];

        if (!threadRunning) return;
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.txtworkh.setText("Start frame...");
            }
        });
        PP4C.sendPP4C(MainActivity.this.at.getApplicationContext(), startcode, 34, MainActivity.this.donglever, 6, MainActivity.audioTrack, 10);
        if (MainActivity.this.donglever == 2) {
            SystemClock.sleep(1000);
        }
        if (MainActivity.this.donglever == 1) {
            SystemClock.sleep(1600);
        }
    }





    private void sendFrame(List<Byte> hexlist, int numframe) {
        byte[] startcode = new byte[54];
        startcode[0] = (byte) -123;
        startcode[1] = (byte) ((int) (MainActivity.this.plID & 255));
        startcode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
        startcode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
        startcode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
        startcode[5] = (byte) 52;
        startcode[6] = (byte) 0;
        startcode[7] = (byte) 0;
        startcode[8] = (byte) 0;
        startcode[9] = (byte) 32;
        startcode[10] = (byte) (numframe >> 8);
        startcode[11] = (byte) (numframe & MotionEventCompat.ACTION_MASK);
        for (int cp = 0; cp < 40; cp++) {
            startcode[cp + 12] = ((Byte) hexlist.get((numframe * 40) + cp)).byteValue();
        }
        byte[] FrameCRC = CRCCalc.GetCRC(startcode, 52);
        startcode[52] = FrameCRC[0];
        startcode[53] = FrameCRC[1];

        if (!threadRunning) return;
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.txtworkh.setText("Data frame " + MainActivity.this.y + "/" + MainActivity.this.ymax);
            }
        });
        PP4C.sendPP4C(MainActivity.this.at.getApplicationContext(), startcode, 54, MainActivity.this.donglever, 1, MainActivity.audioTrack, MainActivity.this.nbRepeatFrame);
        if (MainActivity.this.donglever == 2) {
            SystemClock.sleep(550);
        }
        if (MainActivity.this.donglever == 1) {
            SystemClock.sleep(1800);
        }
    }






    private void sendVerifCode() {
        byte[] vercode = new byte[]{(byte) -123, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 52, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        vercode[1] = (byte) ((int) (MainActivity.this.plID & 255));
        vercode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
        vercode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
        vercode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
        byte[] FrameCRC = CRCCalc.GetCRC(vercode, 28);
        vercode[28] = FrameCRC[0];
        vercode[29] = FrameCRC[1];

        if (!threadRunning) return;
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.this.txtworkh.setText("Verify frame...");
            }
        });
        PP4C.sendPP4C(MainActivity.this.at.getApplicationContext(), vercode, 30, MainActivity.this.donglever, 10, MainActivity.audioTrack, 50);
        if (MainActivity.this.donglever != 3) {
            SystemClock.sleep(2000);
        }
    }




    private void sendImage(Bitmap image, int size_raw, int imagePart) {
        MainActivity mainActivity;
        int size_compressed;
        List<Byte> hexlist;

        MainActivity.this.idx = 0;
        MainActivity.this.compressed = new ArrayList<Integer>();

        StringBuilder bstr_raw = new StringBuilder();
        StringBuilder bstr_compressed = new StringBuilder();


        convertMonochrome(image, false);
        if (!threadRunning) return;

        if (MainActivity.this.ESLTypeColor) {
            convertMonochrome(image, true);
            if (!threadRunning) return;
        }


        RLECompress();
        if (!threadRunning) return;


        Hexadecimalifying(bstr_raw, bstr_compressed);

        size_compressed = bstr_compressed.toString().length();
        if (size_compressed < size_raw && rawmode == 0) { // Compressed data mode
            hexlist = createCompressedHexList(bstr_compressed);
        }
        else { // raw data mode
            hexlist = createRawHexList(bstr_raw);
        }


        try {
            Thread.sleep(100);
        } catch (InterruptedException e22) {
            e22.printStackTrace();
        }
        MainActivity.this.handler.post(new Runnable() {
            public void run() {
                MainActivity.this.pgb.setProgress(MainActivity.SELECT_PHOTO);
            }
        });



        sendPingCode();
        if (!threadRunning) return;

        sendStartCode(imagePart);
        if (!threadRunning) return;

        MainActivity.this.ymax = padded_datalen / 40;
        MainActivity.this.y = 0;
        while (MainActivity.this.y < MainActivity.this.ymax) {
            sendFrame(hexlist, MainActivity.this.y);
            if (!threadRunning) return;

            mainActivity = MainActivity.this;
            mainActivity.y++;
        }

        sendVerifCode();
        if (!threadRunning) return;
    }







    public void convertImage() {
        new Thread(new Runnable() {
            public void run() {
                MainActivity mainActivity;

                MainActivity.this.txtworkh = (TextView) MainActivity.this.findViewById(R.id.txtwork);
                MainActivity.this.pgb = (ProgressBar) MainActivity.this.findViewById(R.id.pgb1);
                MainActivity.this.imgbmp = (ImageView) MainActivity.this.findViewById(R.id.imgvbmp);
                MainActivity.this.scaledimage = ((BitmapDrawable) MainActivity.this.imgbmp.getDrawable()).getBitmap();

                int w = MainActivity.this.wi;
                int h = MainActivity.this.hi;

                int size_raw = w * h;

                if (MainActivity.this.ESLTypeColor) {
                    size_raw *= 2;
                }


                MainActivity.this.rawbitstream = new byte[size_raw];

                MainActivity.this.scaledimage = Bitmap.createScaledBitmap(MainActivity.this.scaledimage, w, h, true);


                MainActivity.this.idx = 0;
                int size_compressed;
                MainActivity.this.compressed = new ArrayList<Integer>();
                List<Byte> hexlist;

                StringBuilder bstr_raw = new StringBuilder();
                StringBuilder bstr_compressed = new StringBuilder();



                convertMonochrome(scaledimage, false);
                if (!threadRunning) return;

                if (MainActivity.this.ESLTypeColor) {
                    convertMonochrome(scaledimage, true);
                    if (!threadRunning) return;
                }


                RLECompress();
                if (!threadRunning) return;


                Hexadecimalifying(bstr_raw, bstr_compressed);


                size_compressed = bstr_compressed.toString().length();
                if ((size_compressed/8) <= 65535 && rawmode == 0 || ((size_raw/8) <= 65535 && rawmode == 1)) {
                    if (size_compressed < size_raw && rawmode == 0) {
                        hexlist = createCompressedHexList(bstr_compressed);
                    }
                    else {
                        hexlist = createRawHexList(bstr_raw);
                    }


                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e22) {
                        e22.printStackTrace();
                    }
                    MainActivity.this.handler.post(new Runnable() {
                        public void run() {
                            MainActivity.this.pgb.setProgress(MainActivity.SELECT_PHOTO);
                        }
                    });



                    sendPingCode();
                    if (!threadRunning) return;

                    sendStartCode(0);
                    if (!threadRunning) return;

                    MainActivity.this.ymax = padded_datalen / 40;
                    MainActivity.this.y = 0;
                    while (MainActivity.this.y < MainActivity.this.ymax) {
                        sendFrame(hexlist, MainActivity.this.y);
                        if (!threadRunning) return;

                        mainActivity = MainActivity.this;
                        mainActivity.y++;
                    }

                    sendVerifCode();
                    if (!threadRunning) return;
                }
                else {
                    MainActivity.this.handler.post(new Runnable() {
                        public void run() {
                            MainActivity.this.pgb.setProgress(0);
                            MainActivity.this.txtworkh.setText("Split image and transmit part 1/2...");
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e22) {
                        e22.printStackTrace();
                    }

                    MainActivity.this.scaledimagepart1=Bitmap.createBitmap(MainActivity.this.scaledimage, 0,0, w, h/2);
                    MainActivity.this.scaledimagepart2=Bitmap.createBitmap(MainActivity.this.scaledimage, 0,h/2, w, h/2);


                    MainActivity.this.rawbitstream = new byte[size_raw/2];
                    sendImage(scaledimagepart1, (size_raw/2), 1);
                    if (!threadRunning) return;

                    for (int i = 45; i > 0; i--) {
                        final int sec = i;
                        MainActivity.this.handler.post(new Runnable() {
                            public void run() {
                                MainActivity.this.txtworkh.setText("Waiting " + sec + "s before transmitting part 2/2...");
                            }
                        });
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    MainActivity.this.rawbitstream = new byte[size_raw/2];
                    sendImage(scaledimagepart2, size_raw/2, 2);
                    if (!threadRunning) return;
                }



                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MainActivity.this.txtworkh.setText("Done ! ;-)");
                        MainActivity.this.btnsendimg.setText("Send image");
                    }
                });
            }
        }).start();
    }

    public void setScanPreview() {
        this.autoFocusHandler = new Handler();
        this.mCamera = getCameraInstance();
        Parameters params = this.mCamera.getParameters();
        params.setPreviewSize(640, 480);
        this.mCamera.setParameters(params);
        this.mPreview = new CameraPreview(this, this.mCamera, this.previewCb, this.autoFocusCB);
        this.preview = (FrameLayout) findViewById(R.id.cameraPreview);
        this.preview.addView(this.mPreview);
        this.scanner = new ImageScanner();
        this.scanner.setConfig(0, 256, 3);
        this.scanner.setConfig(0, Config.Y_DENSITY, 3);
        this.scanner.setConfig(0, 0, 0);
        this.scanner.setConfig(128, 0, 1);
        this.scaneibarcode = (TextView) findViewById(R.id.eslinfo_barcode);
        this.scaneiserial = (TextView) findViewById(R.id.eslinfo_serial);
        this.scaneitype = (TextView) findViewById(R.id.eslinfo_type);
        this.scanButton = (Button) findViewById(R.id.scan_button);
        this.scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (MainActivity.this.mCamera == null) {
                    MainActivity.this.previewing = false;

                    MainActivity.this.mCamera = MainActivity.getCameraInstance();
                    MainActivity.this.mCamera.setPreviewCallback(MainActivity.this.previewCb);
                    MainActivity.this.mCamera.startPreview();
                    MainActivity.this.previewing = true;
                    MainActivity.this.mCamera.autoFocus(MainActivity.this.autoFocusCB);
                    ((FrameLayout) MainActivity.this.findViewById(R.id.cameraPreview)).addView(MainActivity.this.mPreview);
                }
                if (MainActivity.this.barcodeScanned) {
                    MainActivity.this.barcodeScanned = false;
                    MainActivity.this.scaneibarcode.setText("Scan ESL barcode");
                    MainActivity.this.mCamera.setPreviewCallback(MainActivity.this.previewCb);
                    MainActivity.this.mCamera.startPreview();
                    MainActivity.this.previewing = true;
                    MainActivity.this.mCamera.autoFocus(MainActivity.this.autoFocusCB);
                    MainActivity.this.ESLType = 0;
                }
            }
        });
    }

    public void onCreate(Bundle savedInstanceState) {
        int i;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.settings = getSharedPreferences("Pricehax", 0);
        audioTrack = new AudioTrack(3, 48000, 4, 2, 48000, 1);
        if (savedInstanceState != null) {
            this.tabPos = savedInstanceState.getInt("tabPos");
        } else {
            this.tabPos = 0;
        }
        getActionBar().hide();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
            }
            else {
                setScanPreview();
            }
        }
        else {
            setScanPreview();
        }


        pairBT();

        SeekBar vControl = (SeekBar) findViewById(R.id.sb);
        vControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MainActivity.transmitVolume = progress;
                ((TextView) MainActivity.this.findViewById(R.id.tvvolume)).setText("Transmit volume: " + (MainActivity.transmitVolume + 60) + "%");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Editor editor = MainActivity.this.getSharedPreferences("Pricehax", 0).edit();
                editor.putInt("tvolume", MainActivity.transmitVolume);
                editor.commit();
            }
        });
        SeekBar nbRepeatControl = (SeekBar) findViewById(R.id.sb_nb_repeat);
        nbRepeatControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MainActivity.this.nbRepeatFrame = progress + 1;
                String p = "";
                if (MainActivity.this.nbRepeatFrame != 1) {
                    p = "s";
                }
                ((TextView) MainActivity.this.findViewById(R.id.tv_nb_repeat)).setText("Frames repeated " + (MainActivity.this.nbRepeatFrame) + " time" + (p) + " (Lower is faster but less reliable !)");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Editor editor = MainActivity.this.getSharedPreferences("Pricehax", 0).edit();
                editor.putInt("nbrepeat", MainActivity.this.nbRepeatFrame);
                editor.commit();
            }
        });
        this.label_plhex = (TextView) findViewById(R.id.label_dbgbs);
        this.donglever = this.settings.getInt("donglever", 0);
        if (this.donglever == 0) {
            this.donglever = 3;
        }
        if (this.donglever > 3) {
            this.donglever = 3;
        }
        transmitVolume = this.settings.getInt("tvolume", 20);
        vControl.setProgress(transmitVolume);
        nbRepeatFrame = this.settings.getInt("nbrepeat", 3);
        if (nbRepeatFrame > 5)
            nbRepeatFrame = 5;
        String p = "";
        if (nbRepeatFrame != 1) {
            p = "s";
        }
        ((TextView) MainActivity.this.findViewById(R.id.tv_nb_repeat)).setText("Frames repeated " + (nbRepeatFrame) + " time" + (p) + " (Lower is faster but less reliable !)");
        nbRepeatControl.setProgress(nbRepeatFrame - 1);

        this.rawmode = this.settings.getInt("rawmode", 0);
        if (this.rawmode == 1) {
            ((CheckBox) findViewById(R.id.force_raw_sending_mode)).setChecked(true);
        }

        if (this.donglever == 2) {
            ((CheckBox) findViewById(R.id.chk_donglever)).setChecked(true);
        } else {
            ((CheckBox) findViewById(R.id.chk_donglever)).setChecked(false);
            if (this.donglever == 3) {
                ((CheckBox) findViewById(R.id.chk_donglebt)).setChecked(true);
            } else {
                ((CheckBox) findViewById(R.id.chk_donglebt)).setChecked(false);
            }
        }
        this.tabHost = (TabHost) findViewById(R.id.tabHost);
        this.tabHost.setup();
        View tabIndicator1 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, this.tabHost.getTabWidget(), false);
        ((TextView) tabIndicator1.findViewById(R.id.title)).setText("CHANGE PAGE");
        TabSpec spec1 = this.tabHost.newTabSpec("Tab 1");
        spec1.setIndicator(tabIndicator1);
        spec1.setContent(R.id.tab1);
        View tabIndicator2 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, this.tabHost.getTabWidget(), false);
        ((TextView) tabIndicator2.findViewById(R.id.title)).setText("PLID SCAN");
        TabSpec spec2 = this.tabHost.newTabSpec("Tab 2");
        spec2.setIndicator(tabIndicator2);
        spec2.setContent(R.id.tab2);
        View tabIndicator3 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, this.tabHost.getTabWidget(), false);
        ((TextView) tabIndicator3.findViewById(R.id.title)).setText("CHANGE SEGMENTS");
        TabSpec spec3 = this.tabHost.newTabSpec("Tab 3");
        spec3.setIndicator(tabIndicator3);
        spec3.setContent(R.id.tab3);
        View tabIndicator4 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, this.tabHost.getTabWidget(), false);
        ((TextView) tabIndicator4.findViewById(R.id.title)).setText("CONFIG");
        TabSpec spec4 = this.tabHost.newTabSpec("Tab 4");
        spec4.setIndicator(tabIndicator4);
        spec4.setContent(R.id.tab4);
        View tabIndicator5 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, this.tabHost.getTabWidget(), false);
        ((TextView) tabIndicator5.findViewById(R.id.title)).setText("LEGAL INFO");
        TabSpec spec5 = this.tabHost.newTabSpec("Tab 5");
        spec5.setIndicator(tabIndicator5);
        spec5.setContent(R.id.tab5);
        View tabIndicator6 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, this.tabHost.getTabWidget(), false);
        ((TextView) tabIndicator6.findViewById(R.id.title)).setText("CHANGE IMAGE");
        TabSpec spec6 = this.tabHost.newTabSpec("Tab 6");
        spec6.setIndicator(tabIndicator6);
        spec6.setContent(R.id.tab6);
        this.tabHost.addTab(spec1);
        this.tabHost.addTab(spec2);
        this.tabHost.addTab(spec3);
        this.tabHost.addTab(spec6);
        this.tabHost.addTab(spec4);
        this.tabHost.addTab(spec5);
        this.tabHost.setCurrentTab(this.tabPos);
        for (i = 0; i < this.tabHost.getTabWidget().getChildCount(); i++) {
            this.tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 128;
        }
        for (i = 0; i < this.tabHost.getTabWidget().getTabCount(); i++) {
            ((TextView) ((ViewGroup) this.tabHost.getTabWidget().getChildAt(i)).getChildAt(0)).setTextColor(Color.parseColor("#E03030"));
        }
        this.spinner = (Spinner) findViewById(R.id.spn);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.segChoices, 17367048);
        adapter.setDropDownViewResource(17367049);
        this.spinner.setAdapter(adapter);
        this.spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long arg) {
                int[][] segPredefs = new int[][]{new int[]{1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0}, new int[]{1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0}, new int[]{0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0}, new int[]{0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0}};
                for (int i = 0; i < 21; i++) {
                    ToggleButton tg1 = (ToggleButton) MainActivity.this.findViewById(MainActivity.this.getResources().getIdentifier("seg" + i, "id", MainActivity.this.getPackageName()));
                    if (segPredefs[position][i] == 1) {
                        tg1.setChecked(true);
                    } else {
                        tg1.setChecked(false);
                    }
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        this.spinner = (Spinner) findViewById(R.id.spnimg);
        adapter = ArrayAdapter.createFromResource(this, R.array.imgChoices, 17367048);
        adapter.setDropDownViewResource(17367049);
        this.spinner.setAdapter(adapter);
        this.spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long arg) {
                ((ImageView) MainActivity.this.findViewById(R.id.imgvbmp)).setImageResource(new int[]{R.drawable.burd, R.drawable.dolan, R.drawable.gratuit, R.drawable.holyshit, R.drawable.link, R.drawable.mahboi, R.drawable.sanic, R.drawable.troll}[position]);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        this.btnsendimg = (Button) findViewById(R.id.btnsendimg);
        this.btnsendimg.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Button btnsend = (Button) v;
                String buttonText = btnsend.getText().toString();
                if (buttonText.equals("Send image")) {
                    if (!isDonglePaired()) {
                        return;
                    }
                    if (MainActivity.this.ESLType != 2) {
                        if (MainActivity.this.ESLType == 0) {
                            Toast.makeText(MainActivity.this, "ESL barcode not scanned !", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Incompatible ESL type !", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    MainActivity.this.threadRunning = true;
                    btnsend.setText("Stop send");
                    convertImage();
                }
                else if (buttonText.equals("Stop send")) {
                    MainActivity.this.threadRunning = false;
                    //SystemClock.sleep(200);
                    btnsend.setText("Send image");
                    MainActivity.this.txtworkh.setText("Stopped successfully !");
                    MainActivity.this.pgb.setProgress(100);
                }
            }
        });

        this.btnmanualbarcode = (Button) findViewById(R.id.manual_scan_button);
        this.btnmanualbarcode.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Button btn = (Button) v;

                FrameLayout camerapreview = (FrameLayout) findViewById(R.id.cameraPreview);
                Button scanbutton = (Button) findViewById(R.id.scan_button);

                TextView manualbarcodetitle = (TextView) findViewById(R.id.manual_barcode_title);
                EditText barcodeedittext = (EditText) findViewById(R.id.barcode_edittext);
                Button setbarcodebutton = (Button) findViewById(R.id.set_barcode_button);

                if (btn.getText().toString().equals("Enter barcode manually")) {
                    btn.setText("Scan barcode using camera");

                    camerapreview.setVisibility(View.GONE);
                    scanbutton.setVisibility(View.GONE);

                    manualbarcodetitle.setVisibility(View.VISIBLE);
                    barcodeedittext.setVisibility(View.VISIBLE);
                    setbarcodebutton.setVisibility(View.VISIBLE);

                    barcodeedittext.requestFocus();
                }
                else {
                    btn.setText("Enter barcode manually");

                    camerapreview.setVisibility(View.VISIBLE);
                    scanbutton.setVisibility(View.VISIBLE);

                    manualbarcodetitle.setVisibility(View.GONE);
                    barcodeedittext.setVisibility(View.GONE);
                    setbarcodebutton.setVisibility(View.GONE);
                }
            }
        });

        this.btnsetbarcode = (Button) findViewById(R.id.set_barcode_button);
        this.btnsetbarcode.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditText barcodeedittext = (EditText) findViewById(R.id.barcode_edittext);

                String barcode = barcodeedittext.getText().toString().toUpperCase();

                if (barcode.length() == 17) {
                    if (Character.isLetter(barcode.charAt(0)) && TextUtils.isDigitsOnly(barcode.substring(1, 17))) {
                        setESLBarcode(barcode);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "The barcode entered is invalid!", Toast.LENGTH_SHORT).show();
                    }

                }
                else {
                    Toast.makeText(MainActivity.this, "The barcode entered is not the right length!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted !", Toast.LENGTH_LONG).show();
                setScanPreview();
                this.scanButton.setText("Scan ESL barcode");
            } else {
                Toast.makeText(this, "Camera permission denied !", Toast.LENGTH_LONG).show();
                this.scanButton = (Button) findViewById(R.id.scan_button);
                this.scanButton.setText("Request permission");
                this.scanButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                        }
                    }
                });
            }
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("tabPos", this.tabHost.getCurrentTab());
    }

    public void onPause() {
        super.onPause();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                releaseCamera();
                this.preview.removeView(this.mPreview);
            }
        }
        else {
            releaseCamera();
            this.preview.removeView(this.mPreview);
        }
        audioTrack.release();
    }

    public void onResume() {
        super.onResume();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (this.mCamera == null) {
                    try {
                        this.autoFocusHandler = new Handler();
                        this.mCamera = getCameraInstance();
                        Parameters params = this.mCamera.getParameters();
                        params.setPreviewSize(640, 480);
                        this.mCamera.setParameters(params);
                        this.mPreview = new CameraPreview(this, this.mCamera, this.previewCb, this.autoFocusCB);
                        this.preview = (FrameLayout) findViewById(R.id.cameraPreview);
                        this.preview.addView(this.mPreview);
                        this.mCamera.autoFocus(MainActivity.this.autoFocusCB);
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        else {
            if (this.mCamera == null) {
                this.autoFocusHandler = new Handler();
                this.mCamera = getCameraInstance();
                Parameters params = this.mCamera.getParameters();
                params.setPreviewSize(640, 480);
                this.mCamera.setParameters(params);
                this.mPreview = new CameraPreview(this, this.mCamera, this.previewCb, this.autoFocusCB);
                this.preview = (FrameLayout) findViewById(R.id.cameraPreview);
                this.preview.addView(this.mPreview);
            }
        }

    }

    private void releaseCamera() {
        if (this.mCamera != null) {
            this.previewing = false;
            this.mCamera.setPreviewCallback(null);
            this.mCamera.release();
            this.mCamera = null;
        }
    }

    public void doAutoFocusOnTouch(View view) {
        if (this.previewing == true) {
            this.mCamera.autoFocus(MainActivity.this.autoFocusCB);
        }
    }

    public void setdonglever(View view) {
        if (((CheckBox) findViewById(R.id.chk_donglever)).isChecked()) {
            this.donglever = 2;
        } else {
            this.donglever = 1;
        }
        if (((CheckBox) findViewById(R.id.chk_donglebt)).isChecked()) {
            this.donglever = 3;
        }
        Editor editor = getSharedPreferences("Pricehax", 0).edit();
        editor.putInt("donglever", this.donglever);
        editor.commit();
    }

    public void setrawmode(View view) {
        if (((CheckBox) findViewById(getResources().getIdentifier("force_raw_sending_mode", "id", getPackageName()))).isChecked()) {
            this.rawmode = 1;
        } else {
            this.rawmode = 0;
        }

        Editor editor = MainActivity.this.getSharedPreferences("Pricehax", 0).edit();
        editor.putInt("rawmode", this.rawmode);
        editor.commit();
    }

    public void pairBTTransmitter(View view) {
        pairBT();
    }

    public void pairBT() {
        new Thread(new Runnable() {
            public void run() {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                checkBTState();
                if (btok) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            ((TextView) findViewById(R.id.tvbtt)).setText("Connected to BT transmitter !");
                            ((Button) findViewById(R.id.btnPairBT)).setText("Unpair BT Transmitter");
                        }
                    });
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            ((TextView) findViewById(R.id.tvbtt)).setText("BT transmitter not found");
                            ((Button) findViewById(R.id.btnPairBT)).setText("Pair BT Transmitter");
                        }
                    });
                }
            }
        }).start();
    }

    public boolean isDonglePaired() {
        if (!btok) {
            Toast.makeText(MainActivity.this, "BT dongle not paired !", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public void sendSegs(View view) {
        new Thread(new Runnable() {
            public void run() {
                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.sendSegUpdate();
                    }
                });
            }
        }).start();
    }

    public void sendPage(View view) {
        new Thread(new Runnable() {
            public void run() {
                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.sendPageUpdate();
                    }
                });
            }
        }).start();
    }

    public void repeatSendPage(View view) {
        if (!isDonglePaired()) {
            return;
        }
        this.repeatMode = ((CheckBox) findViewById(R.id.chkrepeat)).isChecked();
        if (this.repeatMode) {
            if (this.timerdm != null) {
                ((CheckBox) findViewById(R.id.chkrepeatdm)).setChecked(false);
                this.timerdm.cancel();
                this.timerdm.purge();
            }
            this.timer = new Timer();
            final Handler handler = new Handler();
            this.timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            MainActivity.this.sendPageUpdate();
                        }
                    });
                }
            }, 0, 1000);
            return;
        }
        this.timer.cancel();
        this.timer.purge();
    }

    public void repeatSendPageDM(View view) {
        if (!isDonglePaired()) {
            return;
        }
        this.repeatModeDM = ((CheckBox) findViewById(R.id.chkrepeatdm)).isChecked();
        int timerPeriod = 4500;
        if (((RadioButton) findViewById(R.id.raddur10)).isChecked() || ((RadioButton) findViewById(R.id.raddur11)).isChecked()) {
            timerPeriod = 2000;
        }
        if (this.repeatModeDM) {
            if (this.timer != null) {
                ((CheckBox) findViewById(R.id.chkrepeat)).setChecked(false);
                this.timer.cancel();
                this.timer.purge();
            }
            this.timerdm = new Timer();
            final Handler handler = new Handler();
            this.timerdm.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            MainActivity.this.sendDMPageUpdate();
                        }
                    });
                }
            }, 0, timerPeriod);
            return;
        }
        this.timerdm.cancel();
        this.timerdm.purge();
    }

    public void sendDMPage(View view) {
        new Thread(new Runnable() {
            public void run() {
                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.sendDMPageUpdate();
                    }
                });
            }
        }).start();
    }

    public void sendDMKC(View view) {
        new Thread(new Runnable() {
            public void run() {
                MainActivity.this.handler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.sendDMKeyChange();
                    }
                });
            }
        }).start();
    }

    void sendDMPageUpdate() {
        if (!isDonglePaired()) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                byte[] hcode = new byte[18];
                int dispDuration = 0;
                int dispDMDuration = 0;
                int nbrepeat = 400;
                EditText metxtPage = (EditText) findViewById(R.id.etxtPage);
                if (metxtPage.getText() == null) {
                    metxtPage.setText("0");
                }
                byte[] dispDMPage = metxtPage.getText().toString().getBytes();
                for (int i = 1; i <= 11; i++) {
                    if (((RadioButton) findViewById(getResources().getIdentifier("raddur" + i, "id", getPackageName()))).isChecked()) {
                        dispDuration = i;
                        break;
                    }
                }
                if (dispDuration == 1) {
                    dispDMDuration = 2;
                }
                if (dispDuration == 2) {
                    dispDMDuration = 4;
                }
                if (dispDuration == 3) {
                    dispDMDuration = 15;
                }
                if (dispDuration == 4) {
                    dispDMDuration = 240;
                }
                if (dispDuration == 5) {
                    dispDMDuration = 900;
                }
                if (dispDuration == 6) {
                    dispDMDuration = 1800;
                }
                if (dispDuration == 7) {
                    dispDMDuration = 2700;
                }
                hcode[0] = (byte) -123;
                hcode[1] = (byte) 0;
                hcode[2] = (byte) 0;
                hcode[3] = (byte) 0;
                hcode[4] = (byte) 0;
                hcode[5] = (byte) 6;
                if (dispDuration == 8) {
                    hcode[6] = (byte) 241;
                    dispDMDuration = 10;
                } else if (dispDuration == 9) {
                    hcode[6] = (byte) 243;
                    dispDMDuration = (dispDMPage[0] & 15);
                } else if (dispDuration == 10) {
                    hcode[1] = (byte) ((int) (MainActivity.this.plID & 255));
                    hcode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
                    hcode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
                    hcode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
                    hcode[6] = (byte) 73;
                    dispDMDuration = (dispDMPage[0] & 15);
                    nbrepeat = 150;
                } else if (dispDuration == 11) {
                    hcode[1] = (byte) ((int) (MainActivity.this.plID & 255));
                    hcode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
                    hcode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
                    hcode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
                    hcode[6] = (byte) 201;
                    dispDMDuration = (dispDMPage[0] & 15);
                    nbrepeat = 150;
                } else {
                    hcode[6] = (byte) (((dispDMPage[0] & 15) << 3) | 1);
                }
                hcode[7] = (byte) 0;
                hcode[8] = (byte) 0;
                hcode[9] = (byte) (dispDMDuration >> 8);
                hcode[10] = (byte) (dispDMDuration & MotionEventCompat.ACTION_MASK);
                byte[] FrameCRC = CRCCalc.GetCRC(hcode, 11);
                hcode[11] = FrameCRC[0];
                hcode[12] = FrameCRC[1];
                PP4C.sendPP4C(getApplicationContext(), hcode, 13, MainActivity.this.donglever, 60, audioTrack, nbrepeat);
            }
        }).start();
    }

    void sendDMKeyChange() {
        if (!isDonglePaired()) {
            return;
        }
        byte[] hcode = new byte[]{(byte) -123, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 52, (byte) 0, (byte) 0, (byte) 0, (byte) 3, (byte) 19, (byte) 55, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        hcode[1] = (byte) ((int) (this.plID & 255));
        hcode[2] = (byte) ((int) (this.plID >> 8));
        hcode[3] = (byte) ((int) (this.plID >> 16));
        hcode[4] = (byte) ((int) (this.plID >> 24));
        byte[] FrameCRC = CRCCalc.GetCRC(hcode, 28);
        hcode[28] = FrameCRC[0];
        hcode[29] = FrameCRC[1];
        PP4C.sendPP4C(getApplicationContext(), hcode, 30, this.donglever, 60, audioTrack, 0);
        plHexString = "";
        for (int cp = 0; cp < 30; cp++) {
            plHexString = plHexString + String.format("%02X", new Object[]{Byte.valueOf(hcode[cp])});
        }
        ((TextView) findViewById(R.id.label_dbgbs)).setText("DM change MK: " + plHexString);
    }

    void sendPageUpdate() {
        if (!isDonglePaired()) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                byte[] hcode = new byte[11];
                byte dispDuration = (byte) 1;
                EditText metxtPage = (EditText) findViewById(R.id.etxtPage);
                if (!metxtPage.getText().toString().matches("")) {
                    byte[] dispPage = metxtPage.getText().toString().getBytes();
                    for (int i = 1; i < 9; i++) {
                        if (((RadioButton) findViewById(getResources().getIdentifier("raddur" + i, "id", getPackageName()))).isChecked()) {
                            dispDuration = (byte) i;
                            break;
                        }
                    }
                    if (dispDuration == (byte) 8) {
                        dispDuration = Byte.MIN_VALUE;
                    }
                    hcode[0] = (byte) -124;
                    hcode[1] = (byte) 0;
                    hcode[2] = (byte) 0;
                    hcode[3] = (byte) 0;
                    hcode[4] = (byte) 0;
                    hcode[5] = (byte) -85;
                    hcode[6] = (byte) (((dispPage[0] & 7) << 3) | dispDuration);
                    hcode[7] = (byte) 0;
                    hcode[8] = (byte) 0;
                    byte[] FrameCRC = CRCCalc.GetCRC(hcode, 9);
                    hcode[9] = FrameCRC[0];
                    hcode[10] = FrameCRC[1];
                    PP4C.sendPP4C(getApplicationContext(), hcode, 11, MainActivity.this.donglever, 60, audioTrack, 100);
                }
            }
        }).start();
    }

    void sendSegUpdate() {
        if (!isDonglePaired()) {
            return;
        }
        if (MainActivity.this.ESLType != 1) {
            if (MainActivity.this.ESLType == 0) {
                Toast.makeText(MainActivity.this, "ESL barcode not scanned !", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(MainActivity.this, "Incompatible ESL type !", Toast.LENGTH_LONG).show();
            }
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                int cp;
                int[][] segBitPos = new int[][]{new int[]{174, 162, 70, 24, 82, 128, 116, 150, 139, 47, 1, 58, 104, 93, 175, 163, 71, 25, 83, 129, 117}, new int[]{139, 104, 12, 1, 47, 93, 58, 162, 128, 36, 24, 70, 116, 82, 138, 103, 11, 0, 46, 92, 57}, new int[]{145, 110, 18, 7, 53, 99, 64, 168, 134, 42, 30, 76, 122, 88, 144, 109, 17, 6, 52, 98, 63}, new int[]{146, SELECT_PHOTO, 54, 8, 65, 157, 111, 170, 124, 78, 32, 90, 182, 136, 147, 101, 55, 9, 66, 158, 112}, new int[]{172, 160, 68, 34, 80, TransportMediator.KEYCODE_MEDIA_PLAY, 114, 173, 161, 69, 35, 81, TransportMediator.KEYCODE_MEDIA_PAUSE, 115, 149, 138, 46, 11, 57, 103, 92}};
                byte[] segData = new byte[23];
                for (int i = 0; i < 21; i++) {
                    if (((ToggleButton) findViewById(getResources().getIdentifier("seg" + i, "id", getPackageName()))).isChecked()) {
                        int bitPos = segBitPos[MainActivity.this.plBitDef][i];
                        int i2 = bitPos / 8;
                        segData[i2] = (byte) (segData[i2] | ((byte) (1 << ((byte) (bitPos % 8)))));
                    }
                }
                byte[] ecode = new byte[]{(byte) 0, (byte) 0, (byte) 9, (byte) 0, (byte) 16, (byte) 0, (byte) 49};
                byte[] hcode = new byte[43];
                hcode[0] = (byte) -124;
                hcode[1] = (byte) ((int) (MainActivity.this.plID & 255));
                hcode[2] = (byte) ((int) (MainActivity.this.plID >> 8));
                hcode[3] = (byte) ((int) (MainActivity.this.plID >> 16));
                hcode[4] = (byte) ((int) (MainActivity.this.plID >> 24));
                hcode[5] = (byte) -70;
                hcode[6] = (byte) 0;
                hcode[7] = (byte) 0;
                hcode[8] = (byte) 0;
                for (cp = 0; cp < 23; cp++) {
                    hcode[cp + 9] = segData[cp];
                }
                byte[] DataCRC = CRCCalc.GetCRC(segData, 23);
                hcode[32] = DataCRC[0];
                hcode[33] = DataCRC[1];
                for (cp = 0; cp < 7; cp++) {
                    hcode[cp + 34] = ecode[cp];
                }
                byte[] FrameCRC = CRCCalc.GetCRC(hcode, 41);
                hcode[41] = FrameCRC[0];
                hcode[42] = FrameCRC[1];
                plHexString = "";
                for (cp = 0; cp < hcode.length; cp++) {
                    plHexString = plHexString + String.format("%02X", new Object[]{Byte.valueOf(hcode[cp])});
                }

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView) findViewById(R.id.label_dbgbs)).setText("Segment bitstream: " + plHexString);
                    }
                });
                PP4C.sendPP4C(getApplicationContext(), hcode, 43, MainActivity.this.donglever, 15, audioTrack, 100);
            }
        }).start();
    }

    void sendFlashUpdate() {
        if (!isDonglePaired()) {
            return;
        }
        int cp;
        int[][] segBitPos = new int[][]{new int[]{174, 162, 70, 24, 82, 128, 116, 150, 139, 47, 1, 58, 104, 93, 175, 163, 71, 25, 83, 129, 117}, new int[]{139, 104, 12, 1, 47, 93, 58, 162, 128, 36, 24, 70, 116, 82, 138, 103, 11, 0, 46, 92, 57}, new int[]{145, 110, 18, 7, 53, 99, 64, 168, 134, 42, 30, 76, 122, 88, 144, 109, 17, 6, 52, 98, 63}, new int[]{146, SELECT_PHOTO, 54, 8, 65, 157, 111, 170, 124, 78, 32, 90, 182, 136, 147, 101, 55, 9, 66, 158, 112}};
        byte[] segData = new byte[23];
        for (int i = 0; i < 21; i++) {
            if (((ToggleButton) findViewById(getResources().getIdentifier("seg" + i, "id", getPackageName()))).isChecked()) {
                int bitPos = segBitPos[this.plBitDef][i];
                int i2 = bitPos / 8;
                segData[i2] = (byte) (segData[i2] | ((byte) (1 << ((byte) (bitPos % 8)))));
            }
        }
        byte[] ecode = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 17, (byte) 0, (byte) 96};
        byte[] hcode = new byte[43];
        hcode[0] = (byte) -124;
        hcode[1] = (byte) ((int) (this.plID & 255));
        hcode[2] = (byte) ((int) (this.plID >> 8));
        hcode[3] = (byte) ((int) (this.plID >> 16));
        hcode[4] = (byte) ((int) (this.plID >> 24));
        hcode[5] = (byte) -70;
        hcode[6] = (byte) 29;
        hcode[7] = (byte) 0;
        hcode[8] = (byte) 0;
        for (cp = 0; cp < 23; cp++) {
            hcode[cp + 9] = segData[cp];
        }
        byte[] DataCRC = CRCCalc.GetCRC(segData, 23);
        hcode[32] = DataCRC[0];
        hcode[33] = DataCRC[1];
        for (cp = 0; cp < 7; cp++) {
            hcode[cp + 34] = ecode[cp];
        }
        byte[] FrameCRC = CRCCalc.GetCRC(hcode, 41);
        hcode[41] = FrameCRC[0];
        hcode[42] = FrameCRC[1];
        plHexString = "";
        for (cp = 0; cp < hcode.length; cp++) {
            plHexString = plHexString + String.format("%02X", new Object[]{Byte.valueOf(hcode[cp])});
        }
        ((TextView) findViewById(R.id.label_dbgbs)).setText("Data update: " + plHexString);
        PP4C.sendPP4C(getApplicationContext(), hcode, 43, this.donglever, 15, audioTrack, 0);
    }
}

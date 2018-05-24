package com.sourcey.materiallogindemo;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

@SuppressWarnings("ConstantConditions")
public class PresenceActivity extends AppCompatActivity {

    TextView textStudent;
    Button butWyloguj;
    Button butObecnosc;
    EditText textInputDialog;

    InputMethodManager imm;
    Button butRestore;
    Button butStolowka;
    AlertDialog dialog;
    Hashtable<String, String> values = new Hashtable<>();

    FirebaseAuth firebaseAuth;
    DatabaseReference database;
    LinkedList<CodesStore> codes = new LinkedList<>();
    CodesStore current;

    //NFC
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    boolean noNFCMode;
    Tag myTag;
    String mode = "";
    boolean restoreMode  = false;
    boolean semaphoreGGI = true;
    boolean semaphoreGG  = true;
    boolean semaphoreGL  = true;
    boolean semaphoreGC  = true;
    boolean semaphoreSP  = true;
    boolean semaphoreSOS = true;
    boolean semaphoreCP  = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        textStudent = findViewById(R.id.text_student);
        butWyloguj = findViewById(R.id.but_wyloguj2);
        butObecnosc = findViewById(R.id.but_obecnosc);
        butRestore = findViewById(R.id.button_offline);
        butRestore.setText(R.string.check);
        butStolowka = findViewById(R.id.but_stolowka);
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null)
        {
            TextView tv = findViewById(R.id.text_noNFCInfo);
            tv.setText(getString(R.string.no_nfc_message_p1));
            tv.append("\n"+getString(R.string.no_nfc_message_p2));
            noNFCMode = true;
        }

        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };

        butWyloguj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });

        butRestore.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                getGroupID(true);
            }
        });

        butObecnosc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                textStudent.setText(values.get("location"));
                if(!isInternetConnection())
                    goToOfflineActivity();
                else
                {
                    mode = "presence";
                    if (noNFCMode)
                        inputDialog(getString(R.string.presence_no_nfc_message));
                    else
                        infoDialog(getString(R.string.presence_nfc_message), false);
                }
            }
        });

        butStolowka.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if(!isInternetConnection())
                    goToOfflineActivity();
                else
                {
                    mode = "cafeteria";
                    if (noNFCMode)
                        inputDialog(getString(R.string.cafeteria_no_nfc_message));
                    else
                        infoDialog(getString(R.string.cafeteria_nfc_message), false);
                }
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        loadStudentData();
        if(!isInternetConnection())
            goToOfflineActivity();
        else if(!nfcAdapter.isEnabled())
        {
            //TODO uncomment before presentation
            /*
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
            //*/
            //TODO remove before presentation
            TextView tv = findViewById(R.id.text_noNFCInfo);
            tv.setText(getString(R.string.no_nfc_message_p1));
            tv.append("\n"+getString(R.string.no_nfc_message_p2));
            noNFCMode = true;

        }
        restoreCodes();
    }

    private void restoreCodes()
    {
        String[] warehouse = noNFCMode?loadCodes("codeManual"):loadCodes("codeNFC");
        if(warehouse.length>0)
        {
            for(String s : warehouse)
                codes.addFirst(new CodesStore(s.split("[;]")));
            butRestore.setVisibility(View.VISIBLE);
        }
    }

    public String[] loadCodes(String mode)
    {
        ArrayList<String> result = new ArrayList<>();
        try {
            FileInputStream fis = openFileInput(mode);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            while(dis.available()>0)
                result.add(dis.readUTF());
            dis.close();
        } catch (Exception e) { return new String[0]; }
        return result.size()>0?result.toArray(new String[result.size()]):new String[0];
    }

    private void getStudent()
    {
        final String emailStudent = firebaseAuth.getCurrentUser().getEmail();
        textStudent.setText(R.string.loading_data);
        database.child("Studenci").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(emailStudent.equals(item.child("email").getValue().toString()))
                    {
                        values.put("studentName", item.child("imie").getValue().toString() +" "+
                                item.child("nazwisko").getValue().toString());
                        values.put("indexNumber", item.getKey());
                        String s = "Witaj "+values.get("studentName")
                                +"!\nNr albumu: "+values.get("indexNumber")+".";
                        saveStudentData();
                        textStudent.setText(s);
                        nFound = false;
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    //odczyt NFC
    private void readFromIntent(Intent intent)
    {
        String m = mode;
        mode = "";
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs, m);
        }
    }

    private void buildTagViews(NdefMessage[] msgs, String mode)
    {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063;

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }
        if(mode.equals("presence"))
        {
            current = new CodesStore(
                    text,
                    values.get("indexNumber"),
                    new SimpleDateFormat("yy-MM-dd;HH:mm").format(Calendar.getInstance().getTime()),
                    getDeviceID(),
                    "X;X");
            getGroupID(false);
        }
        else if(mode.equals("cafeteria"))
        {
            //TODO verification cafeterias coupons
            values.put("codeNFC", text);
            getCoupon();
        }
    }

    private void getGroupID(boolean restore)
    {
        if(semaphoreGGI)
        {
            semaphoreGGI = false;
            restoreMode = restore;
            database.child("Kody").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                        boolean nFound = true;
                        CodesStore cs = restoreMode?codes.getFirst():current;
                        while (iterator.hasNext() && nFound)
                        {
                            DataSnapshot item = iterator.next();
                            Iterator<DataSnapshot> iterator2 = item.getChildren().iterator();
                            while (iterator2.hasNext() && nFound)
                            {
                                DataSnapshot item2 = iterator2.next();
                                if(cs.code.equals(item2.getKey()))
                                {
                                    cs.groupID        = item.getKey();
                                    cs.setClasses(item2.getValue().toString().split("[;]"));
                                    nFound = false;
                                    semaphoreGGI = true;
                                    getGroup();
                                }
                            }
                        }
                        if(nFound)
                        {
                            refreshWarehouse();
                            semaphoreGGI = true;
                            infoDialog(getString(R.string.no_code_message), true);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
        }
    }

    private void getGroup()
    {
        if(semaphoreGG)
        {
            semaphoreGG = false;
            database.child("Grupy").addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                    boolean nFound = true;
                    CodesStore cs = restoreMode?codes.getFirst():current;
                    while (iterator.hasNext() && nFound)
                    {
                        DataSnapshot item = iterator.next();
                        if(cs.groupID.equals( item.getKey() ))
                        {
                            cs.lecturerID = item.child("idProw").getValue().toString();
                            cs.courseID = item.child("kodKursu").getValue().toString();
                            String tp = item.child("tydzienParz").getValue().toString();
                            cs.evenWeek = (tp.equals("true"))?"tydzień parzysty":(tp.equals("false"))?"tydzień nieparzysty":"";
                            cs.classesWeekday = item.child("dzienTyg").getValue().toString();
                            cs.classesPlace = item.child("budynek").getValue().toString();
                            cs.classesRoom = item.child("sala").getValue().toString();
                            cs.classesStartTime = item.child("godzRoz").getValue().toString();
                            cs.classesEndTime = item.child("godzZak").getValue().toString();
                            nFound = false;
                            semaphoreGG = true;
                            getLecturer();
                            getCourse();
                        }
                    }
                    if(nFound)
                    {
                        semaphoreGG = true;
                        infoDialog(getString(R.string.data_base_error), true);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    private void getLecturer()
    {
        if(semaphoreGL)
        {
            semaphoreGL = false;
            database.child("Prowadzacy").addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                    boolean nFound = true;
                    CodesStore cs = restoreMode?codes.getFirst():current;
                    while (iterator.hasNext() && nFound)
                    {
                        DataSnapshot item = iterator.next();
                        if(cs.lecturerID.equals( item.getKey() ))
                        {
                            cs.lecturer = item.child("tytul").getValue().toString()
                                    +" "+item.child("imie").getValue().toString()
                                    +" "+item.child("nazwisko").getValue().toString();
                            nFound = false;
                            semaphoreGL = true;
                        }
                    }
                    if(nFound)
                    {
                        semaphoreGL = true;
                        infoDialog(getString(R.string.data_base_error), true);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    private void getCourse()
    {
        if(semaphoreGC)
        {
            semaphoreGC = false;
            database.child("Kursy").addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                    boolean nFound = true;
                    CodesStore cs = restoreMode?codes.getFirst():current;
                    while (iterator.hasNext() && nFound)
                    {
                        DataSnapshot item = iterator.next();
                        if(cs.courseID.equals( item.getKey() ))
                        {
                            cs.courseName = item.child("nazwa").getValue().toString();
                            nFound = false;
                            semaphoreGC = true;
                            checkPresence();
                            //presenceDialog();
                        }
                    }
                    if(nFound)
                    {
                        semaphoreGC = true;
                        infoDialog(getString(R.string.data_base_error), true);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    private  void checkPresence()
    {
        if(semaphoreCP)
        {
            semaphoreCP = false;
            CodesStore cs = restoreMode?codes.getFirst():current;
            database.child("Obecnosci").child(cs.groupID).child(cs.indexNumber).child(cs.thisClassesNumber)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.getValue() == null)
                    {
                        semaphoreCP = true;
                        presenceDialog();
                    }
                    else
                    {
                        String[] a = dataSnapshot.getValue().toString().split("[;]");
                        CodesStore cs = restoreMode?codes.getFirst():current;
                        cs.date = a[1];
                        cs.time = a[2];
                        semaphoreCP = true;
                        infoDialog("Obecność została już odnotowana:\n"+cs.toString(), true);
                        refreshWarehouse();
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    private void presenceDialog()
    {
        if(semaphoreSP)
        {
            semaphoreSP = false;
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_note_presence, null);
            Button presence = dialogView.findViewById(R.id.buttonNPPresence);
            Button cancel   = dialogView.findViewById(R.id.buttonNPCancel);
            CodesStore cs = restoreMode?codes.getFirst():current;
            TextView tv = dialogView.findViewById(R.id.textNP);
            tv.setText(cs.toString());
            if(cs.late==4)
                presence.setEnabled(false);
            adb.setView(dialogView);
            final AlertDialog dialog = adb.create();

            presence.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    dialog.cancel();
                    zapiszObecnosc();
                }
            });

            cancel.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    dialog.cancel();
                }
            });

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    semaphoreSP = true;
                    refreshWarehouse();
                }
            });
            dialog.show();
        }
    }

    //zapis obecności na podstawie NFC
    private void zapiszObecnosc()
    {
        if(semaphoreSOS)
        {
            semaphoreSOS = false;
            CodesStore cs = restoreMode?codes.getFirst():current;
            String p = cs.code+";"+cs.date+";"+cs.time+";"+cs.late+";"+cs.deviceID+";"+cs.distance;
            values.put("message", cs.toString());
            //TODO add geo localization, localization verify;
            database.child("Obecnosci").child(cs.groupID).child(cs.indexNumber).child(cs.thisClassesNumber).setValue(p)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            infoDialog("Twoja obecność została odnotowana na zjęciach:\n" + values.get("message"), true);
                            refreshWarehouse();
                            semaphoreSOS = true;
                        }
                        else
                        {
                            semaphoreSOS = true;
                            infoDialog("Wystąpił błąd!", true);
                        }
                    }
                });
        }
    }

    public void refreshWarehouse()
    {
        if(restoreMode)
        {
            try {
                String mode = noNFCMode?"codeManual":"codeNFC";
                String[] warehouse = loadCodes(mode);
                if(warehouse.length>0)
                {
                    if (warehouse.length > 1)
                    {
                        FileOutputStream os = openFileOutput(mode, Context.MODE_PRIVATE);
                        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
                        for (int i = 1; i < warehouse.length; i++)
                            dos.writeUTF(warehouse[i]);
                        dos.close();
                    }
                    else
                    {
                        if(new File(getFilesDir().getAbsolutePath()+"/"+mode).delete())
                            butRestore.setVisibility(View.INVISIBLE);
                    }
                    this.codes.removeFirst();
                }
                restoreMode = false;
            }catch (Exception e) { textStudent.setText(e.getMessage()); }
        }
        else
            current = null;
    }

    private void infoDialog(String message, boolean ok)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_info, null);
        Button close = dialogView.findViewById(R.id.buttonInfoClose);
        if(!ok)
            close.setText(R.string.cancel);
        TextView tv = dialogView.findViewById(R.id.textInfo);
        tv.setText(message);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();
        this.dialog = dialog;

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                mode = "";
            }
        });
        dialog.show();
    }

    private void inputDialog(String message)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        Button cancel = dialogView.findViewById(R.id.buttonCancelInputDialog);
        Button submit = dialogView.findViewById(R.id.buttonSubmitInputDialog);
        TextView tv = dialogView.findViewById(R.id.textDialog);
        tv.setText(message);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String mi = textInputDialog.getText().toString();
                if(verifyCode(mi))
                {
                    if(mode.equals("presence"))
                    {
                        current = new CodesStore(
                                mi,
                                values.get("indexNumber"),
                                new SimpleDateFormat("yy-MM-dd;HH:mm").format(Calendar.getInstance().getTime()),
                                getDeviceID(),
                                "X;X");
                        getGroupID(false);
                    }
                    else if(mode.equals("cafeteria"))
                        getCoupon();
                }
                else
                    infoDialog(getString(R.string.wrong_code), true);
                dialog.cancel();
                mode = "";
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                mode = "";
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                imm = null;
            }
        });

        dialog.show();
        textInputDialog = dialogView.findViewById(R.id.textInputDialog);
        textInputDialog.setInputType(InputType.TYPE_CLASS_NUMBER);
        //TODO remove before presentation
        textInputDialog.setText("776482");
        textInputDialog.requestFocus();
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private boolean verifyCode(String code) { return code.length()==6 && code.matches("^[0-9]*$"); }

    private void getCoupon()
    {
        values.put("date", new SimpleDateFormat("yy-MM-dd").format(Calendar.getInstance().getTime()));
        database.child("Kupony").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                String v = values.get("date");
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(v.equals( item.getKey() ))
                    {
                        String id = values.get("indexNumber");
                        v = item.getValue().toString();
                        v = v.replaceAll("[{},]", "");
                        v = v.replaceAll("=[0-9][0-9]:[0-9][0-9]", "");
                        String[] s = v.split("[ ]");
                        if (!found(s, id))
                            generateCoupon();
                        else
                            infoDialog(getString(R.string.cafeterias_coupon_used)
                                    +" "+item.child(id).getValue().toString()+".", true);

                        nFound = false;
                    }
                }
                if(nFound)
                    generateCoupon();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    protected void generateCoupon()
    {
        values.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
        database.child("Kupony").child(values.get("date")).child(values.get("indexNumber")).setValue(values.get("time"))
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                        infoDialog("Kupon został przyznany.\nCzas przyznania: "
                                +(values.get("time"))+".\nProszę pokazać komunikat ekspedjentce przed zamknięciem.", true);
                    else
                        infoDialog("Wystąpił błąd!", true);
                }
            });
    }

    protected boolean found(String[] IDs, String searchedID)
    {
        boolean nFound = true;
        int i =0;
        while(i<IDs.length && nFound)
        {
            if(IDs[i++].equals(searchedID))
                nFound = false;
        }
        return !nFound;
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if(!mode.equals(""))
        {
            dialog.cancel();
            setIntent(intent);
            readFromIntent(intent);
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
        if(imm!=null)
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(!isInternetConnection())
            goToOfflineActivity();
        WriteModeOn();
    }

    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    public void saveStudentData()
    {
        //TODO data encryption
        try {
            FileOutputStream os = openFileOutput("studentData", Context.MODE_PRIVATE);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            dos.writeUTF(values.get("indexNumber"));
            dos.writeUTF(values.get("studentName"));
            dos.close();
        } catch (Exception e) { textStudent.setText(e.getMessage()); }
    }

    public void loadStudentData()
    {
        try {
            FileInputStream fis = openFileInput("studentData");
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            values.put("indexNumber", dis.readUTF());
            values.put("studentName", dis.readUTF());
            dis.close();
            String s = "Witaj "+values.get("studentName")
                    +"!\nNr albumu: "+values.get("indexNumber")+".";
            textStudent.setText(s);
        } catch (IOException e) { getStudent(); }
    }

    public boolean isInternetConnection()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try
        {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (NullPointerException e) {return false;}
    }

    private void goToOfflineActivity()
    {
        Intent intent = new Intent(getApplicationContext(), OfflineActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0,0);
        finish();
    }

    public String getDeviceID()
    {
        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm)
        {
            try
            {
                deviceUniqueIdentifier = tm.getDeviceId();
            }
            catch (SecurityException e) {}
        }
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
            deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceUniqueIdentifier;
    }
}
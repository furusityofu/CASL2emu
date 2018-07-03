package jp.ac.fukuoka_u.tl.casl2emu.android;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.primitives.Chars;

import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import icepick.Icepick;
import jp.ac.fukuoka_u.tl.casl2emu.Casl2Register;
import jp.ac.fukuoka_u.tl.casl2emu.LogWriter;
import jp.ac.fukuoka_u.tl.casl2emu.R;
import jp.ac.fukuoka_u.tl.casl2emu.databinding.ActivityContextDisplayScreenBinding;

import static android.R.layout.simple_list_item_1;

public class ContextDisplayScreen extends BaseActivity implements LoaderCallbacks,Runnable{

    private static final int REQUEST_WRITE_STORAGE = 112;
    Casl2Exercise exercise =null;
    LogWriter logWriter;



    private void showTextDialog(String text, final int rownum) {
        final Casl2EditText editView = new Casl2EditText(ContextDisplayScreen.this,1);
        final InputMethodManager inputMethodManager =  (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        editView.setText(text);
        editView.setMaxLines(1);
        editView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                    //キーボードを閉じる
                    inputMethodManager.hideSoftInputFromWindow(editView.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);

                    return true;
                }
                return false;
            }
        });
        new AlertDialog.Builder(ContextDisplayScreen.this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(R.layout.input_text_dialog)
                .setTitle("メモリを編集: "+String.format(Locale.US,"0x%04X",rownum & 0xFFFF))
                //setViewにてビューを設定します。
                .setView(editView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //入力した文字をトースト出力する
                        String upperedString =editView.getText().toString().toUpperCase();
                        Pattern pattern = Pattern.compile(getString(R.string.memory_row_pattern_1));
                        Matcher matcher = pattern.matcher(upperedString);
                        if (matcher.matches()) {
                            char[] chars = Casl2EditText.getHexChars(upperedString," ");
                            emulator.setMemoryArray(chars, rownum);
                            refreshMemoryPane(rownum,0);

                        }else {
                            Toast.makeText(ContextDisplayScreen.this, "適切な文字列を入力してください", Toast.LENGTH_LONG).show();
                        }

                    }
                })
                .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    private void refreshMemoryPane(int rownum,int refreshMode) {
        switch(refreshMode){
            case 0://通常の更新
                stringArrayList.remove(rownum);
                arrayAdapter.insert(String.format(Locale.US ,getString(R.string.HEX_REGEX_1), emulator.getMemory(rownum) & 0xFFFF),rownum);
                break;
            case 1://挿入の更新
                arrayAdapter.insert(String.format(Locale.US ,getString(R.string.HEX_REGEX_4),
                        emulator.getMemory(4*rownum) & 0xFFFF, emulator.getMemory(4*rownum+1) & 0xFFFF,
                        emulator.getMemory(4*rownum+2) & 0xFFFF, emulator.getMemory(4*rownum+3) & 0xFFFF),rownum);
                stringArrayList.remove(arrayAdapter.getCount()-1);
                break;
            case 2://削除の更新
                stringArrayList.remove(rownum);
                arrayAdapter.insert(String.format(Locale.US ,getString(R.string.HEX_REGEX_4),
                        emulator.getMemory(0xFFFC) & 0xFFFF, emulator.getMemory(0xFFFD) & 0xFFFF,
                        emulator.getMemory(0xFFFE) & 0xFFFF, emulator.getMemory(0xFFFF) & 0xFFFF),arrayAdapter.getCount()-1);
                break;
        }

        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    protected void refreshMemory(char[] data, char position) {
        super.refreshMemory(data, position);
        refreshMemoryPane(position,0);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_memory,menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        ClipboardManager clipboardManager = (ClipboardManager) getApplicationContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData;
        ClipData.Item memorystring = new ClipData.Item(
                String.valueOf(memoryView.getItemAtPosition(info.position)));
        char[] zero = {0x0000,0x0000,0x0000,0x0000};
        switch(item.getItemId()) {
            //TODO:削除機能を追加
            case R.id.action_pop:
                emulator.deleteMemoryArray(zero, info.position*4);
                refreshMemoryPane(info.position,2);
                break;
            case R.id.action_insert:
                emulator.insertMemoryArray(zero, info.position*4);
                refreshMemoryPane(info.position,1);
                break;
            //TODO:複数行選択機能を追加
            case R.id.action_copy:
                clipData = new ClipData(new ClipDescription("text_data",
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}),memorystring);
                clipboardManager.setPrimaryClip(clipData);

                break;
            case R.id.action_paste:
                clipData = clipboardManager.getPrimaryClip();
                String text = (String) clipData.getItemAt(0).getText();
                Pattern pattern = Pattern.compile(getString(R.string.memory_row_pattern_4));
                Matcher matcher = pattern.matcher(text);
                if (matcher.matches()) {
                    char[] chars = Casl2EditText.getHexChars(text," ");
                    emulator.setMemoryArray(chars, info.position*4);
                    refreshMemoryPane(info.position,0);

                }else {
                    Toast.makeText(ContextDisplayScreen.this, "貼り付けに失敗しました", Toast.LENGTH_LONG).show();
                }
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context_display_screen);
//        Icepick.restoreInstanceState(this,savedInstanceState);

        logWriter=new LogWriter(getApplicationContext());
        register = Casl2Register.getInstance();
        boolean hasPermission = (ContextCompat.checkSelfPermission(ContextDisplayScreen.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(ContextDisplayScreen.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }

        final ActivityContextDisplayScreenBinding binding = DataBindingUtil.setContentView(this,R.layout.activity_context_display_screen);
        char[] initialState;
        String initialString;
        //OUTデモ
        //initialState = new char[]{0,0,0,0,0,0,0x000F,2};
        //initialString = "F000 FF00 4675 6B75 6461 690A 2837 3930 213F 2900"+" "+getString(R.string.short_zerofill);

        //図形描画デモ
        //initialState = new char[]{0,0,0,0,0x001D,0x0016,0x0010,0x000b};
        //initialString = "F000 FF02 1476 F000 FF02 1475 F000 FF02 1474 F000 FF02 0001 0064 0064 0064 0001 0002 00C8 00C8 0190 0190 0000 0003 0190 0190 01F4 01F4 0002 000a 0004 0100 0100 0003 0010"+" "+getString(R.string.short_zerofill);
        //音楽再生デモ
        //initialState = new char[]{0,0,0,0,0x009,0x0008,0x0001,0x0007};
        //initialString = "F000 FF04 1475 F000 FF04 1474 F000 FF04 0001 0002 0003 F000 FF02 0001 0064 0064 0064 0001 0002 00C8 00C8 0190 0190 0000 0003 0190 0190 01F4 01F4 0002 000a 0004 0100 0100 0003 0010"+" "+getString(R.string.short_zerofill);
        //String initialString = "F000 FF06 0314 1592 0000 8100 0000 0003 0001 0001 0020 00C8 00C8 0190 0190 0000"+" "+getString(R.string.short_zerofill);
        //String initialString = "F000 FF06 0314 1592 0000 8100 0000 0003 0001 0001 0020 00C8 00C8 0190 0190 0000"+" "+getString(R.string.short_zerofill);
        //音楽再生デモ
        //initialState = new char[]{0,0,9,8,78,7,1,2};
        //initialString = "F000 FF04 0001 1592 0000 8100 0000 0003 0001 0001 0020 00C8 00C8 0190 0190 0000"+" "+getString(R.string.short_zerofill);
        //STデモ
        //initialState = new char[]{0,0,0,0,0,0,0x000F,2};
        //initialString = "1100 0002 4675 6B75 6461 690A 2837 3930 213F 2900"+" "+getString(R.string.short_zerofill);
        //INPUTデモ
        //initialState = new char[]{0,0,0,0,0,0,0x000F,2};
        //initialString = "F000 FF0E 4675 6B75 6461 690A 2837 3930 213F 2900"+" "+getString(R.string.short_zerofill);
        //ASYNCINPUTデモ
        //initialState = new char[]{0,0,0,0,0,1,0x0001,2};
        //initialString = "F000 FF10 4675 6B75 6461 690A 2837 3930 213F 2900"+" "+getString(R.string.short_zerofill);

        initialState = new char[]{0,0,0,0,0,0,0,0};
        initialString = "0000 0000 0000 0000";

        register.setGr(initialState);
        binding.setCasl2Register(register);

        //String initialString = "8314 1592 F000 FF01 0001 0064 0064 0064 0001 0002 00C8 00C8 0190 0190 0000"+" "+getString(R.string.short_zerofill);
        emulator.clearMemory();


        memoryView = binding.memory.memoryGrid;
        localSetMemoryAdapter(emulator.getMemory(),0);
        memoryView.setOnItemClickListener(showTextEditDialog);

        registerForContextMenu(memoryView);

        binding.runbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emulator.run(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getString(R.string.intervalkey),1000));
                startListTask(new char[0],0);
            }
        });
        binding.stepbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emulator.stepOver();
                startListTask(new char[0],0);
            }
        });
        binding.waitbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emulator.waitEmu();
            }
        });
        binding.waitbutton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                emulator.waitEmu();
                register.setPc((char) 0x0000);
                Toast.makeText(ContextDisplayScreen.this,"PRを0x0000にしました。",Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        binding.outputscreenbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(),OutputScreen.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshMemoryPane(intent.getCharExtra(getString(R.string.BUTTON_INPUT_ADDRESS),(char)0)/4,0);
            }
        };
        IntentFilter filter = new IntentFilter(getString(R.string.action_memory_refresh));
        registerReceiver(refreshReceiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(refreshReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        if(data!=null){
            if(data.getData().getPathSegments().get(1).contains(sharedPreferences.getString("userid","null"))) {
                if(requestCode==1114&&resultCode==RESULT_OK) {
                        startDataSendTask(data, sharedPreferences);
                }
                else if(requestCode==5657&&resultCode==RESULT_OK){

                    List<String> openfilename=data.getData().getPathSegments();
                    byte[] loaddata = new byte[131098];
                    FileInputStream fileInputStream = null;
                    String loadfilename = openfilename.get(1).split(":")[1];
                    try {
                        fileInputStream=new FileInputStream(
                                new File(Environment.getExternalStorageDirectory().getPath(),
                                        loadfilename));
                        fileInputStream.read(loaddata);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    register.setDatafromBinary(loaddata);
                    emulator.setDatafromBinary(loaddata);
                    localSetMemoryAdapter(emulator.getMemory(),0);
                    logWriter.recordLogData("load,"+loadfilename);

                }

            }else {
                Toast.makeText(this,"["+sharedPreferences.getString("userid","USERID")+"]フォルダ内のファイルを指定してください。",Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startDataSendTask(Intent data, SharedPreferences sharedPreferences) {
        Intent intent = new Intent(getApplicationContext(),DataSendTask.class);
        List<String> openfilename=data.getData().getPathSegments();
        ArrayList<String> localfile= new ArrayList<String >();
        localfile.add(getString(R.string.server_address));
        localfile.add("21");
        localfile.add(openfilename.get(1).split(":")[1]);
        localfile.add(sharedPreferences.getString("userid","null"));
        localfile.add(sharedPreferences.getString("password","null"));
        localfile.add("true");
        localfile.add(Environment.getExternalStorageDirectory().getPath() + "/" + openfilename.get(1).split(":")[1]);
        intent.putExtra("data", localfile);
        if (exercise != null) {
            intent.putExtra("kadaifilename", exercise.getFileName());
            intent.putExtra("kadainum",exercise.getNumber());
        }
        startService(intent);
        exercise =null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String filename=getString(R.string.default_file_name);
        final EditText editView = new EditText(getApplicationContext());
        byte[] bytes = new byte[0];
        Intent intent;
        final SharedPreferences preferences;
        switch(item.getItemId()){
            case R.id.execution_interval:
                final Casl2EditText text = new Casl2EditText(ContextDisplayScreen.this,3);
                text.setInputType(InputType.TYPE_CLASS_NUMBER);
                preferences = PreferenceManager.getDefaultSharedPreferences(this);
                text.setText(Integer.toString(preferences.getInt(getString(R.string.intervalkey),1000)));
                new AlertDialog.Builder(this)
                        .setTitle("実行間隔を入力してください。[ms]")
                        .setView(R.layout.input_text_dialog)
                        .setView(text)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //入力した文字をトースト出力する
                                String data = text.getText().toString();
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt(getString(R.string.intervalkey),Integer.parseInt(data));
                                editor.commit();

                            }
                        })
                        .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            case R.id.output_initialize:
                OutputBuffer.getInstance().setData("");
                OutputBuffer.getInstance().clearDrawObjectArray();
                break;
            case R.id.register_initialize:
                register.initializeRegister();
                break;
            case R.id.action_jump:
                final Casl2EditText memory_position = new Casl2EditText(ContextDisplayScreen.this,1);
                memory_position.setTextColor(Color.BLACK);
                new AlertDialog.Builder(ContextDisplayScreen.this)

                        .setView(R.layout.input_text_dialog)
                        .setTitle(getString(R.string.jump_dialog_text))
                        //setViewにてビューを設定します。
                        .setView(memory_position)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //入力した文字をトースト出力する
                                String position = memory_position.getText().toString();
                                if(position.matches("\\w\\w\\w\\w")) {
                                    memoryView.setSelection(Integer.parseInt(position, 16) / 4);
                                }else{
                                    Toast.makeText(ContextDisplayScreen.this,"適切な文字列を入力してください",Toast.LENGTH_SHORT).show();
                                }


                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
                break;
            case R.id.action_load:
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                startActivityForResult(intent,5657);
                break;
            case R.id.action_submit:
                // リスト表示用のアラートダイアログ
                final CharSequence[] items = {"ex01", "ex02", "ex03", "ex04", "ex05", "ex06", "ex07"};
                preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String userId = preferences.getString("userid","null");
                String[] itemsText = new String[items.length];
                for (int i =0;i<items.length;i++){
                       itemsText[i] = items[i]+" 提出日時: "+preferences.getString(userId+"-"+i,"この端末からは提出していません。");
                }
                AlertDialog.Builder listDlg = new AlertDialog.Builder(this);
                listDlg.setTitle("課題番号を選択");
                listDlg.setItems(
                        itemsText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // リスト選択時の処理
                                // which は、選択されたアイテムのインデックス
                                if(exercise==null){
                                    exercise=new Casl2Exercise(items[which]+".bin",which);
                                }
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.setType("*/*");
                                startActivityForResult(intent, 1114);
                            }
                        });

                // 表示
                listDlg.create().show();
                break;
            case R.id.action_save:
                preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if(preferences.contains("LastSavedFileName")){
                    editView.setText(preferences.getString("LastSavedFileName",""));
                }else{
                    editView.setText(filename);
                }
                editView.setTextColor(Color.BLACK);
                new AlertDialog.Builder(ContextDisplayScreen.this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(R.layout.input_text_dialog)
                        .setTitle("ファイル名入力: ")
                        //setViewにてビューを設定します。
                        .setView(editView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //入力した文字をトースト出力する
                                String save_filename = editView.getText().toString();
                                char[] casl2data;
                                casl2data = Chars.concat(ArrayUtils.add(ArrayUtils.add(register.getGr(),register.getPc()),register.getSp()),register.getFr(),emulator.getMemory());
                                byte[]savedata = toBytes(casl2data);
                                FileOutputStream fileOutputStream;

                                try {
                                    SharedPreferences sharedPreferences = PreferenceManager.
                                            getDefaultSharedPreferences(getApplicationContext());
                                    String dirname =Environment.getExternalStorageDirectory().getPath()+
                                            getString(R.string.app_directory_name)+
                                            "/"+ sharedPreferences.getString("userid","null");
                                    File dir = new File(dirname);
                                    boolean hasPermission = (ContextCompat.checkSelfPermission(ContextDisplayScreen.this,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                                    if (!hasPermission) {
                                        ActivityCompat.requestPermissions(ContextDisplayScreen.this,
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_WRITE_STORAGE);
                                    }
                                    if(!dir.exists()){
                                        dir.mkdirs();
                                    }
                                    File file = new File(dirname,save_filename);

                                    fileOutputStream = new FileOutputStream(file);
                                    fileOutputStream.write(savedata);
                                    logWriter.recordLogData("save,"+save_filename);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("LastSavedFileName",save_filename);
                                    editor.commit();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        })
                        .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private byte[] toBytes(char[] chars) {

        byte[] bytes =  new byte[chars.length*2];
        for(int i=0;i<chars.length;i++){
           bytes[i*2]= (byte) (chars[i] >> 8);
           bytes[i*2+1]= (byte) (chars[i]&0x00FF);
        }
        return bytes;
    }

    private void localSetMemoryAdapter(char[] chars, int positon) {
        startListTask(chars,positon);
    }


    public AdapterView.OnItemClickListener getShowTextEditDialog(){
        return showTextEditDialog;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        Icepick.saveInstanceState(this,outState);
    }

}


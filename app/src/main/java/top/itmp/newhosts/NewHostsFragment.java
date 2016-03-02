package top.itmp.newhosts;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by hz on 2016/2/23.
 */
public class NewHostsFragment extends Fragment {
    private static Activity main;
    private static TextView version;
    private static TextView output;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_new_hosts, container, false);
        main = getActivity();
        Button reset = (Button)rootView.findViewById(R.id.reset);
        Button update = (Button)rootView.findViewById(R.id.update);
        version = (TextView)rootView.findViewById(R.id.versionInfo);
        output = (TextView)rootView.findViewById(R.id.output);
        version.setTextColor(Color.parseColor("#FF3F51B5"));
        version.setTextSize(16.0f);
        output.setTextColor(Color.parseColor("#FFFF4081"));
        output.setText(getString(R.string.helpMessage));
        output.setMovementMethod(ScrollingMovementMethod.getInstance());

        checkHostsVersionInfo();

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetHosts();
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*UpdateCheck updateCheck = new UpdateCheck(getActivity());
                try {
                    updateCheck.execute(new URL("https://api.github.com/repos/itmplog/hosts/commits?path=hosts.txt&page=1&per_page=1"));
                }catch (MalformedURLException e){
                    e.printStackTrace();
                }*/
                UpdateCheck.check(getActivity(), "https://api.github.com/repos/itmplog/hosts/commits?path=hosts&page=1&per_page=1");
                checkHostsVersionInfo();
            }
        });

        return rootView;
    }

    public static void checkHostsVersionInfo(){
        File file = new File("/system/etc/hosts");
        if(!file.exists()){
            version.setText("Last Modified Time:\n\t" + "The hosts files doesn't exists.");
        } else {
            version.setText("Last Modified Time:\n\t" + new Date(file.lastModified()));
        }
        String checkedUpdate = getSharedPreference().getString("update", null);
        //PreferenceManager.getDefaultSharedPreferences(main).getString("update", null);
        if(checkedUpdate != null){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
            try {
                Date checked = simpleDateFormat.parse(checkedUpdate);
                version.append("\nServer Update Time:\n\t" + checked);
            }catch (ParseException e){
                e.printStackTrace();
            }
        }
    }

    public static void resetHosts(){
        new Thread(new Runnable() {
            public void run() {
                File original = new File(main.getFilesDir().toString() + File.separator + "original");

                if (original.getParentFile().exists()) {
                    original.getParentFile().mkdir();
                }
                String originalMd5 = getSharedPreference().getString("original", null);
                // PreferenceManager.getDefaultSharedPreferences(main).getString("original", null);
                if (!original.exists() || original.exists() && !MD5.checkMD5(originalMd5, original)) {
                    //original.delete();
                    Log.v("vvv", "hehe");
                    OutputStream out = null;

                    try {
                        out = new FileOutputStream(original);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    try {
                        String tmp = "127.0.0.1 localhost\n" +
                                "::1 localhost";
                        out.write(tmp.getBytes());
                        out.close();
                        getSpEditor().putString("original", MD5.calculateMD5(original)).commit();
                        //PreferenceManager.getDefaultSharedPreferences(main).edit().putString("original", MD5.calculateMD5(original)).commit();
                        //debug
                        /*if(!original.exists()){
                            main.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(main.getApplicationContext(), "No no no", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }*/
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                File hosts = new File("/system/etc/hosts");
                //final String md5 = MD5.calculateMD5(original);
                final String md5 = getSharedPreference().getString("original", null);

                if (hosts.exists() && MD5.checkMD5(md5, hosts)) {
                    main.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(main.getApplicationContext(), main.getString(R.string.already_reset), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    try {
                        Process process = Runtime.getRuntime().exec("/system/xbin/su");
                        DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());

                        outputStream.writeBytes("/system/bin/mount -o rw,remount /system && /system/bin/cp " + main.getFilesDir().toString() + File.separator + "original" + " /system/etc/hosts"
                                + "&& /system/bin/chmod 644 /system/etc/hosts && chown root:root /system/etc/hosts\n");
                        outputStream.writeBytes("sync");

                        outputStream.writeBytes("exit\n");
                        outputStream.flush();
                        //process.waitFor();
                        outputStream.close();
                        Thread.sleep(1000);
                        // checkMD5


                        if (!hosts.exists()) {
                            main.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(main.getApplicationContext(), main.getString(R.string.reset_host_failed), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (MD5.checkMD5(md5, hosts)) {
                            main.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(main.getApplicationContext(), main.getString(R.string.reset_done) + "\nmd5: " + md5, Toast.LENGTH_SHORT).show();
                                    checkHostsVersionInfo();
                                }
                            });

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static SharedPreferences getSharedPreference(){
        return PreferenceManager.getDefaultSharedPreferences(main);
    }
    public static SharedPreferences.Editor getSpEditor(){
        return PreferenceManager.getDefaultSharedPreferences(main).edit();
    }
    public static void runOnUiThread(final String toast){
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(main.getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

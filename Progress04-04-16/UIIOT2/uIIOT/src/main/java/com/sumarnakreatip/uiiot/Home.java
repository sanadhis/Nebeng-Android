package com.sumarnakreatip.uiiot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class Home extends Activity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    HttpPost httppost;
    StringBuffer buffer;
    HttpResponse response;
    HttpClient httpclient;
    List<NameValuePair> nameValuePairs;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    CustomDrawerAdapter adapter;

    public String user,npm, id_tebengan, asal, tujuan, kapasitas, w_b, k, regid;
    int maps = 0;
    private String kuota = "";
    boolean map = false, segarkan = false;

    private NotificationManager mNotificationManager;
    private int notificationID = 100;
    private int numMessages = 0;

    List<DrawerItem> dataList;

    private MFPPush push; // Push client
    private MFPPushNotificationListener notificationListener; // Notification listener to handle a push sent to the phone

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        user    = SaveSharedPreference.getUserName(Home.this);
        regid   = SaveSharedPreference.getUserID(Home.this);
        npm     = SaveSharedPreference.getNPM(Home.this);

        try {
            // initialize SDK with IBM Bluemix application ID and route
            // You can find your backendRoute and backendGUID in the Mobile Options section on top of your Bluemix application dashboard
            // TODO: Please replace <APPLICATION_ROUTE> with a valid ApplicationRoute and <APPLICATION_ID> with a valid ApplicationId
            BMSClient.getInstance().initialize(this, "http://nebeng-app.mybluemix.net", "ff11fc50-a005-4c05-838b-74df51da0768");
        } catch (MalformedURLException mue) {
            Log.i("Initialize", "Fails");
        }

        // Initialize Push client
        MFPPush.getInstance().initialize(this);

        // Create notification listener and enable pop up notification when a message is received
        notificationListener = new MFPPushNotificationListener() {
            @Override
            public void onReceive(final MFPSimplePushNotification message) {
                Log.i("ReceivePush", "Received a Push Notification: " + message.toString());
                runOnUiThread(new Runnable() {
                    public void run() {
                        new AlertDialog.Builder(Home.this)
                                .setTitle("Received a Push Notification")
                                .setMessage(message.getAlert())
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                                .show();
                    }
                });
            }
        };

        // Grabs push client sdk instance
        push = MFPPush.getInstance();
        push.listen(notificationListener);

        // Initializing
        kuota = getIntent().getExtras().getString("Kuota");
        maps = getIntent().getExtras().getInt("Map");
        segarkan = getIntent().getExtras().getBoolean("segar");

        dataList = new ArrayList<DrawerItem>();
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);

        // Add Drawer Item to dataList
        dataList.add(new DrawerItem("Room", R.drawable.ic_action_cloud));
        dataList.add(new DrawerItem("Profil", R.drawable.ic_action_group));
        dataList.add(new DrawerItem("Beri Tebengan", R.drawable.ic_action_good));
        dataList.add(new DrawerItem("Tentang", R.drawable.ic_action_about));
        dataList.add(new DrawerItem("Log Out", R.drawable.ic_action_import_export));

        adapter = new CustomDrawerAdapter(this, R.layout.custom_drawer_item,
                dataList);

        mDrawerList.setAdapter(adapter);

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (maps == 0) {
            SelectItem(0);
        } else if (maps == 1) {
            SelectItem(1);
        } else if (maps == 2) {
            if (!segarkan) {
                map = true;
                asal = getIntent().getExtras().getString("mulai");
                tujuan = getIntent().getExtras().getString("akhir");
                id_tebengan = getIntent().getExtras().getString("id_tebengan");
                kapasitas = getIntent().getExtras().getString("kapasitas");
                w_b = getIntent().getExtras().getString("waktu_berangkat");
                k = getIntent().getExtras().getString("keterangan");
            } else {
                segarkan = false;
            }

            SelectItem(2);
        } else if (savedInstanceState == null) {
            SelectItem(0);
        }


    }

    // If the device has been registered previously, hold push notifications when the app is paused
    @Override
    protected void onPause() {
        super.onPause();

        if (push != null) {
            push.hold();
        }
    }

    // If the device has been registered previously, ensure the client sdk is still using the notification listener from onCreate when app is resumed
    @Override
    protected void onResume() {
        super.onResume();
        if (push != null) {
            push.listen(notificationListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.layout.menu_option, menu);
        return true;
    }

    public void SelectItem(int possition) {

        Fragment fragment = null;
        Bundle args = new Bundle();
        switch (possition) {
            case 0:
                fragment = new Room(Home.this, user, regid);
                args.putString(Room.ITEM_NAME, dataList.get(possition)
                        .getItemName());
                args.putInt(Room.IMAGE_RESOURCE_ID, dataList.get(possition)
                        .getImgResID());
                break;
            case 1:
                fragment = new Profil(Home.this, user);
                args.putString(Profil.ITEM_NAME, dataList.get(possition)
                        .getItemName());
                args.putInt(Profil.IMAGE_RESOURCE_ID, dataList.get(possition)
                        .getImgResID());
                break;
            case 2:
                fragment = new BeriTebengan(Home.this, user, asal, tujuan, kapasitas, w_b, k, map, regid);
                args.putString(BeriTebengan.ITEM_NAME, dataList.get(possition)
                        .getItemName());
                args.putInt(BeriTebengan.IMAGE_RESOURCE_ID, dataList.get(possition)
                        .getImgResID());
                map = false;
                break;
            case 3:
                fragment = new About();
                break;

            case 4:
                fragment = new LogOut();
                SaveSharedPreference.clearUserData(Home.this);
                Intent intent = new Intent(this, LoginPage.class);
                intent.putExtra("regid", regid);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }

        fragment.setArguments(args);
        FragmentManager frgManager = getFragmentManager();
        frgManager.beginTransaction().replace(R.id.content_frame, fragment)
                .commit();

        mDrawerList.setItemChecked(possition, true);
        setTitle(dataList.get(possition).getItemName());
        mDrawerLayout.closeDrawer(mDrawerList);

    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.muatulang:
                //SelectItem(mDrawerList.getCheckedItemPosition());
                if (mDrawerList.getCheckedItemPosition() == 0) {
                    int value = 0;
                    Intent intent = new Intent(this, Home.class);
                    intent.putExtra("username", user);
                    intent.putExtra("Map", value);
                    intent.putExtra("regid", regid);
                    intent.putExtra("Kuota", kuota);
                    startActivity(intent);
                    finish();
                } else if (mDrawerList.getCheckedItemPosition() == 1) {
                    int value = 1;
                    Intent intent = new Intent(this, Home.class);
                    intent.putExtra("username", user);
                    intent.putExtra("Map", value);
                    intent.putExtra("regid", regid);
                    startActivity(intent);
                    finish();
                } else if (mDrawerList.getCheckedItemPosition() == 2) {
                    int value = 2;
                    Intent intent = new Intent(this, Home.class);
                    intent.putExtra("username", user);
                    intent.putExtra("regid", regid);
                    intent.putExtra("id_tebengan", id_tebengan);
                    intent.putExtra("Map", value);
                    intent.putExtra("segar", true);
                    intent.putExtra("mulai", "");
                    intent.putExtra("akhir", "");
                    intent.putExtra("kapasitas", "");
                    intent.putExtra("waktu_berangkat", "");
                    intent.putExtra("keterangan", "");
                    startActivity(intent);
                    finish();
                } else {

                }
                return true;
            default:
                //return false;
                return super.onOptionsItemSelected(item);
        }
    }

    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            SelectItem(position);

        }
    }

    public void onDestroy() {
        super.onDestroy();
    }
}
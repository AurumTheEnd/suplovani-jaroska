package end.the.supl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements KeyWordDialog.KeyWordDialogListener/*, DatePickerDialog.OnDateSetListener*/ {
    private static final int JOB_ID = 123;
    private static final int ALARM_ID = 321;

    private static final String REQUEST_TAG_POPULATE = "PopulateRequests";
    private static final String REQUEST_TAG_REFRESH_PULL = "RefreshRequests";
    private static final String REQUEST_TAG_SEARCH = "SearchRequests";

    private static final String  SEARCH_INFO_DIALOG_TAG = "SearchRequests";
    private static final int MAX_ITEMS_SIZE = 20;
    private final String LOG_TAG = "MainActivityLog";

    private Context mContext;
    private Activity mActivity;

    private RequestQueue queue;

    private SwipeRefreshLayout swipeRefresh;
    private ListView listView;
    private TextView tv;
    private TextView tv2;
    private Button button;
    private Button button2;

    private ArrayList<ListItem> items;
    private ListViewAdapter adapter;

    private boolean isSendingNotifs;

    private Toast errorToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        mActivity = MainActivity.this;

        Hawk.init(mContext).build();
        isSendingNotifs = Hawk.get("isSendingNotifs", false);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        swipeRefresh = findViewById(R.id.swipeRefreshListView);
        listView = findViewById(R.id.listView);
        tv = findViewById(R.id.textView);
        tv2 = findViewById(R.id.textView2);
        button = findViewById(R.id.button1);
        button2 = findViewById(R.id.buttonCancel);

        queue = Volley.newRequestQueue(mContext);

        items = Hawk.get("items", new ArrayList<>());
        adapter = new ListViewAdapter(mContext, R.layout.adapter_view_layout, items);
        listView.setAdapter(adapter);

        final int topID, bottomID;
        if(items.isEmpty()){
            topID = 1860;
            bottomID = 1450;
        } else {
            Date lastDateChecked = Hawk.get("lastDateChecked");
            Date today = Calendar.getInstance().getTime();

            long differenceMillis = Math.abs(today.getTime() - lastDateChecked.getTime());
            long differenceDates = differenceMillis / (24 * 60 * 60 * 1000);

            bottomID = findLatestID() + 1;
            topID = bottomID + 1 + (int)differenceDates;

            tv2.setText(String.valueOf(findLatestID())); //TODO remove, only debug
        }
        populateListView(topID, bottomID);

        removeDuplicateItems();

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(items.isEmpty()) {
                    int bottomID = 1450, topID  = 1860;
                    populateListView(topID, bottomID);

                    swipeRefresh.setRefreshing(false);
                } else {
                    Date lastDateChecked = Hawk.get("lastDateChecked", items.get(0).getDate());
                    Date today = Calendar.getInstance().getTime();

                    long differenceMillis = Math.abs(today.getTime() - lastDateChecked.getTime());
                    long differenceDates = differenceMillis / (24 * 60 * 60 * 1000);

                    int startingID = findLatestID() + 1;
                    //int startingID = items.get(0).getId() + 1;
                    int maxID = startingID + (int) differenceDates + 1;

                    errorToast = null;
                    refreshListView(startingID, maxID);
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem item = (ListItem) parent.getItemAtPosition(position);
                Intent i = new Intent(mActivity, PageActivity.class);

                i.putExtra("url", item.getUrl());
                i.putExtra("date", item.getDate());

                startActivity(i);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem item = (ListItem) parent.getItemAtPosition(position);

                //TextView textView = view.findViewById(R.id.textViewDateTextList);

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("date string", item.getUrl());
                clipboard.setPrimaryClip(clip);

                //Toast.makeText(mContext, item.toString(), Toast.LENGTH_LONG).show();
                Toast.makeText(mContext, R.string.toast_copied, Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        //TODO remove, only debug:
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(!items.isEmpty()){
                    try{
                        tv.setText(listView.getCount() + ", " + items.size());
                    } catch (NullPointerException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!items.isEmpty()){
                    adapter.remove(items.get(0));
                    adapter.notifyDataSetChanged();
                }

                /*
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new NotificationCompat.Builder(mContext, CHANNEL_NEW_PAGE_ID)
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent))
                        .setContentTitle(getString(R.string.notification_new_substitution_found_title) + " - středa 27. 2. 2019")
                        .setContentText(getString(R.string.notification_new_substitution_found_message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .build();
                notificationManager.notify(Integer.valueOf(new SimpleDateFormat("HHmmssSSS").format(new Date())), notification);
                */
                //removeDuplicateItems();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                /*
                boolean isServiceRunning;
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    isServiceRunning = isAlarmTicking();
                } else {
                    JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                    isServiceRunning = jobScheduler.getAllPendingJobs().size() > 0;
                }
                Toast.makeText(mActivity, "Boolean: " + isServiceRunning, Toast.LENGTH_LONG).show();
                */
                adapter.add(adapter.getItem(1));
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void refreshListView(int id, int idMax){
        final String urlCopycat = "https://is.jaroska.cz/suplovani.php?id=" + id + "&skola=0";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlCopycat,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.length() > 0){
                            ListItem item = getItemFromStringPage(response, id, urlCopycat);
                            adapter.add(item);
                            adapter.notifyDataSetChanged();

                            if(items.size() > MAX_ITEMS_SIZE){
                                items.remove(items.size()-1);
                                adapter.notifyDataSetChanged();
                            }

                            tv2.setText(String.valueOf(id));

                            refreshListView(id+1, idMax);
                        } else {
                            if(id < idMax){
                                refreshListView(id+1, idMax);
                            }
                            swipeRefresh.setRefreshing(false);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showErrorToastOnce();

                if(id <= idMax){
                    refreshListView(id+1, idMax);
                }
                if(id == idMax){
                    swipeRefresh.setRefreshing(false);
                }
            }
        }
        );
        stringRequest.setTag(REQUEST_TAG_REFRESH_PULL);
        queue.add(stringRequest);
        removeDuplicateItems();
    }

    private void populateListView(int highestID, int lowestID) {
        final int[] numberOfSuccesses = {0};
        final int[] numberOfBuffers = {0};

        errorToast = null;
        for(int i = highestID; i >= lowestID; i--){
            final String urlCopycat = "https://is.jaroska.cz/suplovani.php?id=" + String.valueOf(i) + "&skola=0";
            final int iCopycat = i;

            StringRequest stringRequest = new StringRequest(Request.Method.GET, urlCopycat,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.length() > 0){
                            ListItem item = getItemFromStringPage(response, iCopycat, urlCopycat);
                            numberOfSuccesses[0]++;

                            if(numberOfSuccesses[0] <= MAX_ITEMS_SIZE){
                                if(!MainActivity.this.items.contains(item)){ //doesnt contain
                                    adapter.add(item); //adds to items array list lol
                                    adapter.notifyDataSetChanged();
                                    if(items.size() > MAX_ITEMS_SIZE){
                                        items.remove(items.size()-1);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            } else {
                                numberOfBuffers[0]++;
                                if(numberOfBuffers[0] <= 5){
                                    for(ListItem itemIterator : items){
                                        if(item.getId() > itemIterator.getId()){
                                            adapter.add(item); //adds to items array list lol
                                            adapter.notifyDataSetChanged();

                                            adapter.remove(items.get(items.size()-1));
                                            adapter.notifyDataSetChanged();
                                            break;
                                        }
                                    }
                                } else {
                                    queue.cancelAll(REQUEST_TAG_POPULATE);

                                    //swipeRefresh.setRefreshing(false); ??
                                }
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showErrorToastOnce();
                    }
                }
            );
            stringRequest.setTag(REQUEST_TAG_POPULATE);
            queue.add(stringRequest);
        }
    }

    private void removeDuplicateItems(){
        Set<ListItem> listWithoutDuplicates = new LinkedHashSet<>(items);

        items.clear();
        adapter.notifyDataSetChanged();

        items.addAll(listWithoutDuplicates);
        adapter.notifyDataSetChanged();
    }

    private int findLatestID(){
        int id = -1;
        for(ListItem item : items){
            if(item.getId() > id){
                id = item.getId();
            }
        }
        return id;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(queue != null){
            queue.cancelAll(REQUEST_TAG_POPULATE);
            queue.cancelAll(REQUEST_TAG_REFRESH_PULL);
            queue.cancelAll(REQUEST_TAG_SEARCH);
        }

        if(!items.isEmpty()){
            Hawk.put("items", items);
            Hawk.put("lastDateChecked", items.get(0).getDate());
        }

        Hawk.put("isSendingNotifs", isSendingNotifs);
    }

    @Override
    protected void onResume() {
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            isSendingNotifs = isAlarmTicking();
        } else {
            isSendingNotifs = isJobServiceRunning();
        }

        if(isSendingNotifs){
            menu.findItem(R.id.notificationsToggleButton).setIcon(R.drawable.ic_notifications_active_black_24dp);
            menu.findItem(R.id.notificationsToggleButton).getIcon().setAlpha(255);
        } else {
            menu.findItem(R.id.notificationsToggleButton).setIcon(R.drawable.ic_notifications_off_black_24dp);
            menu.findItem(R.id.notificationsToggleButton).getIcon().setAlpha(69);
        }

        Hawk.put("isSendingNotifs", isSendingNotifs);
        return true;//return super.onPrepareOptionsMenu(menu);
    }

    private boolean isAlarmTicking(){
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        return (PendingIntent.getBroadcast(getApplicationContext(), ALARM_ID, intent, PendingIntent.FLAG_NO_CREATE) != null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean isJobServiceRunning(){
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE) ;
        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.searchButton:
                createDatePickerDialog();

                return true;
            case R.id.notificationsToggleButton:
                if(items.isEmpty()){
                    Toast.makeText(mActivity, R.string.error_empty_listView, Toast.LENGTH_LONG).show();
                    return true;
                }

                isSendingNotifs = !isSendingNotifs;
                Hawk.put("isSendingNotifs", isSendingNotifs);
                invalidateOptionsMenu();

                toggleNotifications();

                return true;
            case R.id.highlightButton:
                createKeyWordDialog();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleNotifications() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);

            int idToCheck = findLatestID()+1;
            intent.putExtra("idToCheck", idToCheck);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    ALARM_ID, // id, optional
                    intent, // intent to launch
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if(isSendingNotifs){
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 15 * 60 * 1000, pendingIntent);

                Toast.makeText(getApplicationContext(), R.string.notifications_turned_on, Toast.LENGTH_SHORT).show();
            } else {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();

                Toast.makeText(getApplicationContext(), R.string.notifications_turned_off, Toast.LENGTH_SHORT).show();
            }
            /*
            Intent i = new Intent(mContext, ScheduledService.class);
            i.putExtra("idToCheck", items.get(2).getId());
            if(isSendingNotifs){
                mContext.startService(i);
            } else {
                mContext.stopService(i);
            }
            */
            /*
            Intent intent = new Intent(mContext, ScheduledAlarmService.class);
            intent.putExtra("idToCheck", items.get(2).getId());

            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), SERVICE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
            if(isSendingNotifs){
                am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 60000, pendingIntent);
            } else {
                am.cancel(pendingIntent);
                pendingIntent.cancel();
            }*/

        } else {
            if(isSendingNotifs){
                PersistableBundle bundle = new PersistableBundle();
                bundle.putInt("idToCheck", findLatestID()+1);
                //bundle.putInt("idToCheck", items.get(0).getId()-1);

                ComponentName componentName = new ComponentName(this, ScheduledJob.class);
                JobInfo jobInfo = new JobInfo.Builder(123, componentName)
                        //.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(15 * 60 * 1000)
                        .setPersisted(true)
                        .setExtras(bundle)
                        .setBackoffCriteria(15 * 60 * 1000, JobInfo.BACKOFF_POLICY_LINEAR)
                        .build();

                JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                int resultCode = jobScheduler.schedule(jobInfo);

                Toast.makeText(getApplicationContext(), R.string.notifications_turned_on, Toast.LENGTH_SHORT).show();
            } else {
                JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                jobScheduler.cancel(JOB_ID);
                jobScheduler.cancelAll();

                Toast.makeText(getApplicationContext(), R.string.notifications_turned_off, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createKeyWordDialog(){
        KeyWordDialog dialog = new KeyWordDialog();

        Bundle bundle = new Bundle();
        bundle.putString("keyWord", Hawk.get("keyWord", ""));
        bundle.putInt("highlightColor", Hawk.get("highlightColor", Color.parseColor("#CCFF00")));

        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "keyWordDialog");
    }

    @Override
    public void applyKeyWordAndColor(String keyWord, int highlightColor) {
        Hawk.put("keyWord", keyWord);
        Hawk.put("highlightColor", highlightColor);

        adapter.notifyDataSetChanged();
    }

    private void createDatePickerDialog() {
        Calendar now = Calendar.getInstance();

        Calendar min = Calendar.getInstance();
        min.set(2017, 8, 4);

        DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(mActivity,
                null/*MainActivity.this*/,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(min.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(now.getTimeInMillis());

        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE,
            getString(R.string.date_picker_search),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDateSet(datePickerDialog.getDatePicker().getYear(),
                            datePickerDialog.getDatePicker().getMonth(),
                            datePickerDialog.getDatePicker().getDayOfMonth());
                }
        });
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
            getString(R.string.date_picker_cancel),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    datePickerDialog.dismiss();
                }
        });
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            datePickerDialog.setTitle(null);
            datePickerDialog.setCustomTitle(null);
        }
        datePickerDialog.show();
    }

    //@Override
    public void onDateSet(int year, int month, int dayOfMonth) {
        month += 1;
        String dateSelectedString = dayOfMonth + ". " + month + ". " + year;
        SimpleDateFormat format = new SimpleDateFormat("dd. MM. yyyy");
        Date dateSelected = null;
        try {
            dateSelected = format.parse(dateSelectedString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        final Date finalDateSelected = dateSelected;

        int startingID;
        if(items.isEmpty()){
            startingID = 1860;
        } else {
            startingID = findLatestID();
        }

        LayoutInflater inflater = getLayoutInflater();
        View loadingView = inflater.inflate(R.layout.loading_dialog_layout, null);
        final AlertDialog loadingDialog = new AlertDialog.Builder(mActivity/*, R.style.WrapEverythingDialog*/)
                .setCancelable(false)
                .setView(loadingView)
                .create();
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.show();

        errorToast = null;
        for(int i = startingID; i >= 1450; i--){
            final String urlFinal = "https://is.jaroska.cz/suplovani.php?id=" + String.valueOf(i) + "&skola=0";
            final int finalI = i;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, urlFinal, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(response.length() > 0){
                        ListItem item = getItemFromStringPage(response, finalI, urlFinal);

                        Date dateFetched = item.getDate();

                        if(dateFetched.compareTo(finalDateSelected) == 0){
                            queue.cancelAll(REQUEST_TAG_SEARCH);

                            SearchDialog searchDialog = new SearchDialog();

                            Bundle bundle = new Bundle();
                            bundle.putString("url", item.getUrl());
                            bundle.putSerializable("date", dateFetched);

                            String keyWord = Hawk.get("keyWord", "");
                            if(!keyWord.isEmpty()){
                                //Pattern patternKeyWord = Pattern.compile("<TD.*>" + keyWord + "</TD>");
                                Pattern patternKeyWord = Pattern.compile("<TR>\\s+(<TD.*>\\S+</TD>\\s+)*<TD.*>.*" + keyWord); //{0,5} if only absences
                                Matcher matcherKeyWord = patternKeyWord.matcher(item.getPage());

                                if (matcherKeyWord.find()) {
                                    bundle.putBoolean("shouldBeHighlighted", true);
                                } else {
                                    bundle.putBoolean("shouldBeHighlighted", false);
                                }
                            } else {
                                bundle.putBoolean("shouldBeHighlighted", false);
                            }

                            searchDialog.setArguments(bundle);
                            searchDialog.show(getSupportFragmentManager(), SEARCH_INFO_DIALOG_TAG);
                            loadingDialog.dismiss();
                        } else {
                            long differenceMillis = dateFetched.getTime() - finalDateSelected.getTime();
                            long differenceDates = differenceMillis / (24 * 60 * 60 * 1000);

                            if(finalI == 1450 || differenceDates == 0 || differenceDates < -5){
                                queue.cancelAll(REQUEST_TAG_SEARCH);

                                SearchDialog searchDialog = new SearchDialog();
                                searchDialog.show(getSupportFragmentManager(), SEARCH_INFO_DIALOG_TAG);
                                loadingDialog.dismiss();
                            }
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    showErrorToastOnce();
                    if(finalI == 1450){
                        loadingDialog.dismiss();
                    }
                }
            });
            stringRequest.setTag(REQUEST_TAG_SEARCH);
            queue.add(stringRequest);
        }
    }

    private void handleSearch(){} //TODO maybe download all supls with a service

    private void showErrorToastOnce(){
        if(errorToast == null){
            errorToast = Toast.makeText(mActivity, R.string.error_no_internet, Toast.LENGTH_LONG);
            errorToast.show();
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static ListItem getItemFromStringPage(String response, int i, String url){
        String dateString;
        SimpleDateFormat format = new SimpleDateFormat("dd. MM. yyyy");
        SimpleDateFormat formatNoSpace = new SimpleDateFormat("dd.MM.yyyy");
        Date date = null;

        Pattern patternSpace = Pattern.compile("(0?[1-9]|[12][0-9]|3[01])\\. (0?[1-9]|1[012])\\. (20\\d\\d)");
        Matcher matcher = patternSpace.matcher(response);

        if(matcher.find()){
            dateString = matcher.group(0);
            try {
                date = format.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Pattern patternNoSpace = Pattern.compile("(0?[1-9]|[12][0-9]|3[01])\\.(0?[1-9]|1[012])\\.(20\\d\\d)");
            matcher = patternNoSpace.matcher(response);

            if(matcher.find()) {
                dateString = matcher.group(0);
                dateString = dateString.replaceAll("\\.", ". ");
                try {
                    date = formatNoSpace.parse(dateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                Pattern patternNoYear = Pattern.compile("(0?[1-9]|[12][0-9]|3[01])\\.(0?[1-9]|1[012])\\.");
                matcher = patternNoYear.matcher(response);

                if(matcher.find()) {
                    dateString = matcher.group(0);
                    String year = "";

                    if(i >= 1450 && i <= 1525) { year = "2017"; }
                    else { if(i >= 1526 && i <= 1737) { year = "2018"; }
                        else { if(i >= 1738 && i <= 1920) { year = "2019"; }
                            else { year = "2020"; }
                        }
                    }

                    dateString += year;
                    dateString = dateString.replaceAll("\\.", ". ");
                    try {
                        date = formatNoSpace.parse(dateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    Pattern patternNoMonth = Pattern.compile("(0?[1-9]|[12][0-9]|3[01])\\.\\s\\S+\\s+20\\d\\d");
                    matcher = patternNoMonth.matcher(response);

                    if(matcher.find()) {
                        dateString = matcher.group(0);

                        String [] dateParts = dateString.split("\\s");

                        dateString = dateParts[0] + " " + getMonth(dateParts[1]) + ". " + dateParts[dateParts.length-1];
                        try {
                            date = format.parse(dateString);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return new ListItem(i, url, date, response);
    }

    public static int getMonth(@NotNull String value){
        int month = 0;
        if(value.contains("led")) { month = 1; }
        if(value.contains("únor")) { month = 2; }
        if(value.contains("břez")) { month = 3; }
        if(value.contains("dub")) { month = 4; }
        if(value.contains("květ")) { month = 5; }
        if(value.equals("červen") || value.equals("června")) { month = 6; }
        if(value.equals("červenec") || value.equals("července")) { month = 7; }
        if(value.contains("srp")) { month = 8; }
        if(value.contains("září")) { month = 9; }
        if(value.contains("říj")) { month = 10; }
        if(value.contains("listopad")) { month = 11; }
        if(value.contains("prosin")) { month = 12; }
        return month;
    }
}
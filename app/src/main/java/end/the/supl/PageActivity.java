package end.the.supl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.orhanobut.hawk.Hawk;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PageActivity extends AppCompatActivity implements KeyWordDialog.KeyWordDialogListener{
    private static final String LOG_TAG = "PageActivityLog";

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;

    private String keyWord;
    private String highlightColorHex;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        Hawk.init(this).build();

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefreshWebView);

        webView.getSettings().setUseWideViewPort(true);
        webView.setInitialScale(1);
        //webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.getSettings().setJavaScriptEnabled(true);

        Date date = (Date) getIntent().getSerializableExtra("date");
        if(date != null){
            getSupportActionBar().setTitle(new SimpleDateFormat("d. M. yyyy").format(date));
        }

        keyWord = Hawk.get("keyWord", "");
        int highlightColor = Hawk.get("highlightColor", Color.parseColor("#CCFF00"));
        highlightColorHex = String.format("%06X", (0xFFFFFF & highlightColor));

        webView.loadUrl(getIntent().getStringExtra("url"));

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
                swipeRefresh.setRefreshing(false);
            }
        });

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                if(!keyWord.isEmpty()){
                    webView.loadUrl(getJavaScript(keyWord, highlightColorHex));
                }
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.contains(".pdf")){
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    //Log.d(LOG_TAG, intent.resolveActivity(getPackageManager()).toString());
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
                //return super.shouldOverrideUrlLoading(view, url);
            }
        });
    }

    private String getJavaScript(String keyWord, String highlightColor){
        return "javascript:function a(){"+
            "var tables = document.getElementsByTagName('table');"+
            "for(var t = 0; t < tables.length; t++){"+
                "var table = tables[t];"+
                "for (var i = 0, row; row = table.rows[i]; i++) {"+
                    "for (var j = 0, col; col = row.cells[j]; j++) {"+
                        "var innerHTML = col.innerHTML;"+
                        "var index = innerHTML.indexOf('" + keyWord + "');"+
                        "if (index >= 0) {"+
                            "for (var k = 0, cell; cell = row.cells[k]; k++) {"+
                                "cell.style.backgroundColor = '#" + highlightColor + "';"+
                            "}"+
                            "if(col.rowSpan > 1){"+
                                "var remainingRows = col.rowSpan;"+
                                "for(var m = 1; m < remainingRows; m++){"+
                                    "for (var n = 0, smallCell; smallCell = table.rows[i+m].cells[n]; n++) {"+
                                        "smallCell.style.backgroundColor = '#" + highlightColor + "';"+
                                    "}"+
                                "}"+
                            "}"+
                            "if(row.cells.length == 10 || row.cells.length == 7){"+
                                "for(var l = 1; (i-l) >= 1; l++){"+
                                    "if(table.rows[i - l].cells[0].rowSpan > 1 ){"+
                                        "table.rows[i - l].cells[0].style.backgroundColor = '#" + highlightColor + "';"+
                                        "break;"+
                                    "}"+
                                "}"+
                            "}"+
                            "break;"+
                        "}"+
                    "}"+
                "}"+
            "}"+
        "}; a()";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.page_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //TODO add refresh and move with open to 3dot menu
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.highlightButton:
                KeyWordDialog dialog = new KeyWordDialog();

                Bundle bundle = new Bundle();
                bundle.putString("keyWord", Hawk.get("keyWord", ""));
                bundle.putInt("highlightColor", Hawk.get("highlightColor", Color.parseColor("#CCFF00")));

                dialog.setArguments(bundle);
                dialog.show(getSupportFragmentManager(), "keyWordDialog");
                return true;
            case R.id.browserButton:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void applyKeyWordAndColor(String keyWord, int highlightColor) {
        Hawk.put("keyWord", keyWord);
        Hawk.put("highlightColor", highlightColor);

        this.keyWord = keyWord;
        this.highlightColorHex = String.format("%06X", (0xFFFFFF & highlightColor));

        webView.reload();
    }
}

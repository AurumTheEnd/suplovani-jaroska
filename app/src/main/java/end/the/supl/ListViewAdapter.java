package end.the.supl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ListViewAdapter extends ArrayAdapter<ListItem> {
    private Context mContext;
    private int mResource;

    public ListViewAdapter(Context context, int resource, ArrayList<ListItem> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.mResource = resource;

        Hawk.init(context).build();
    }

    @Override
    public void add(@Nullable ListItem object) {
        super.add(object);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {
        int id = getItem(position).getId();
        String url = getItem(position).getUrl();
        Date date = getItem(position).getDate();
        String page = getItem(position).getPage();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        TextView textView = convertView.findViewById(R.id.textViewDateTextList);
        String dateText;
        if(date != null){
            dateText = new SimpleDateFormat("EEEE d. M. yyyy").format(date);
        } else {
            if(page.contains("<title>")){
                dateText = page.substring(page.indexOf("<title>")+7, page.indexOf("</title>"));
            } else {
                dateText = "Error: date for id " + id + " not found.";
            }
        }
        textView.setText(dateText);

        AppCompatImageView highlightIcon = convertView.findViewById(R.id.highlightIcon);
        String keyWord = Hawk.get("keyWord", "");

        if(!keyWord.isEmpty() && !page.isEmpty()){
            //Pattern patternKeyWord = Pattern.compile("<TD.*>" + keyWord + "</TD>");
            Pattern patternKeyWord = Pattern.compile("<TR>\\s+(<TD.*>\\S+</TD>\\s+)*<TD.*>.*" + keyWord); //{0,5} if only absences
            Matcher matcherKeyWord = patternKeyWord.matcher(page);

            if (matcherKeyWord.find()) {
                highlightIcon.setVisibility(View.VISIBLE);
                //convertView.setBackgroundColor(Hawk.get("highlightColor", Color.parseColor("#CCFF00")));
            } else {
                highlightIcon.setVisibility(View.GONE);
            }
        } else {
            highlightIcon.setVisibility(View.GONE);
        }

        View listViewDivider = convertView.findViewById(R.id.listViewDivider);
        if(position + 1 < getCount()/* && position != 0*/) { //TODO maybe use weekOfYear?
            Date dateBottom = getItem(position + 1).getDate();

            long differenceMillis = Math.abs(date.getTime() - dateBottom.getTime());
            float differenceDates = (float)differenceMillis / (24 * 60 * 60 * 1000);

            if (differenceDates > 2) {
                listViewDivider.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorListViewDividerDark));
            }
            /*
            if(((getItem(position + 1).getId() + 1) != id) || ((getItem(position - 1).getId() - 1) != id)){
                textView.append("!!!");
            }*/
        } else {
            listViewDivider.setVisibility(View.GONE);
        }
        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        setNotifyOnChange(false);
        sort(new Comparator<ListItem>() {
            @Override
            public int compare(ListItem o1, ListItem o2) {
                if(o1.getDate() != null && o2.getDate() != null){
                    return o2.getDate().compareTo(o1.getDate());
                } else {
                    return o2.getId() - o1.getId();
                }
            }
        });
        super.notifyDataSetChanged();
    }
}

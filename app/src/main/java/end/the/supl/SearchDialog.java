package end.the.supl;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatImageView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;


import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchDialog extends AppCompatDialogFragment {

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_results_dialog_title);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.search_results_dialog_layout, (ViewGroup) null);
        builder.setView(view);

        TextView searchResult = view.findViewById(R.id.textViewResult);
        AppCompatImageView highlightIcon = view.findViewById(R.id.highlightIconResult);

        Bundle bundle = getArguments();
        if(bundle == null){
            highlightIcon.setVisibility(View.GONE);

            searchResult.setText(R.string.search_results_dialog_no_results);
        } else {
            Date date = (Date) bundle.getSerializable("date");
            if(date != null){
                searchResult.setText(new SimpleDateFormat("EEEE d. M. yyyy").format(date));
            } else {
                searchResult.setText(R.string.search_results_dialog_result_no_date);
            }

            if(bundle.getBoolean("shouldBeHighlighted")){
                highlightIcon.setVisibility(View.VISIBLE);
            } else {
                highlightIcon.setVisibility(View.GONE);
            }

            builder.setPositiveButton(R.string.search_results_dialog_button_open, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(getActivity(), PageActivity.class);
                    intent.putExtra("url", bundle.getString("url"));
                    intent.putExtra("date", date);
                    startActivity(intent);
                }
            });
        }

        builder.setNeutralButton(R.string.search_results_dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

}

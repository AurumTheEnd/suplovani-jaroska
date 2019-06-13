package end.the.supl;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;


public class KeyWordDialog extends AppCompatDialogFragment implements SimpleDialog.OnDialogResultListener {
    private EditText editTextKeyWord;
    private Button colorButton;

    private KeyWordDialogListener listener;

    private String LOG_TAG = "KeyWordDialog";
    private String COLOR_DIALOG = "prd";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String keyWord = bundle.getString("keyWord");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.highlight_dialog_layout, null);

        builder.setView(view)
                .setTitle(getString(R.string.highlight_dialog_title))
                .setMessage(getString(R.string.highlight_dialog_message))
                .setNegativeButton(getString(R.string.highlight_dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton(getString(R.string.highlight_dialog_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String keyWord = editTextKeyWord.getText().toString();
                        listener.applyKeyWordAndColor(keyWord, ViewCompat.getBackgroundTintList(colorButton).getDefaultColor());
                    }
                });
        editTextKeyWord = view.findViewById(R.id.keyWordEditText);
        editTextKeyWord.append(keyWord);

        colorButton = view.findViewById(R.id.colorButton);

        int storedColor = bundle.getInt("highlightColor", Color.parseColor("#CCFF00"));
        ViewCompat.setBackgroundTintList(colorButton, ColorStateList.valueOf(storedColor));
        colorButton.setTextColor(getContrastingTextColor(storedColor));

        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleColorDialog.build()
                        .title(getString(R.string.highlight_dialog_palette_title))
                        .colors(getActivity(), R.array.highlightColors)
                        .allowCustom(true)
                        .colorPreset(ViewCompat.getBackgroundTintList(colorButton).getDefaultColor())
                        .pos(R.string.highlight_dialog_palette_confirm)
                        .neg(R.string.highlight_dialog_palette_close)
                        .show(KeyWordDialog.this, COLOR_DIALOG);
            }
        });

        return builder.create();
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equals(COLOR_DIALOG) && which == BUTTON_POSITIVE){
            int color = (int)extras.get("SimpleColorDialog.color");
            ViewCompat.setBackgroundTintList(colorButton, ColorStateList.valueOf(color));
            colorButton.setTextColor(getContrastingTextColor(color));
        }
        return true;
    }

    @ColorInt
    private static int getContrastingTextColor(@ColorInt int color) {
        double relativeLuminance = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;

        int colorValue;
        if (relativeLuminance < 0.5) {
            colorValue = 0; //bright > black text color
        } else {
            colorValue = 255; //dark > white text color
        }

        return Color.rgb(colorValue, colorValue, colorValue);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (KeyWordDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement KeyWordDialogListener");
        }
    }

    public interface KeyWordDialogListener{
        void applyKeyWordAndColor(String keyWord, int highlightColor);
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        EditText editText = dialog.findViewById(R.id.keyWordEditText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 1 || s.toString().matches("\\s+") || s.toString().contains(" ")){
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }
}
package com.sportytomato.app.util;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sportytomato.app.R;

public class DialogHelper {
    public static androidx.appcompat.app.AlertDialog showStyledDialog(Context context, String title, String message, String positiveText, DialogInterface.OnClickListener positiveListener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.Theme_SportyTomato_Dialog);
        builder.setTitle(title).setMessage(message).setPositiveButton(positiveText, positiveListener);
        androidx.appcompat.app.AlertDialog dialog = builder.create(); dialog.show(); return dialog;
    }
    public static androidx.appcompat.app.AlertDialog showStyledConfirmDialog(Context context, String title, String message, String positiveText, DialogInterface.OnClickListener positiveListener, String negativeText, DialogInterface.OnClickListener negativeListener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.Theme_SportyTomato_Dialog);
        builder.setTitle(title).setMessage(message).setPositiveButton(positiveText, positiveListener).setNegativeButton(negativeText, negativeListener);
        androidx.appcompat.app.AlertDialog dialog = builder.create(); dialog.show(); return dialog;
    }
    public static androidx.appcompat.app.AlertDialog showStyledViewDialog(Context context, String title, View contentView, String positiveText, DialogInterface.OnClickListener positiveListener, String negativeText, DialogInterface.OnClickListener negativeListener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.Theme_SportyTomato_Dialog);
        builder.setTitle(title).setView(contentView).setPositiveButton(positiveText, positiveListener).setNegativeButton(negativeText, negativeListener);
        androidx.appcompat.app.AlertDialog dialog = builder.create(); dialog.show(); return dialog;
    }
    public static androidx.appcompat.app.AlertDialog showStyledViewOnlyDialog(Context context, View contentView) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.Theme_SportyTomato_Dialog);
        builder.setView(contentView);
        androidx.appcompat.app.AlertDialog dialog = builder.create(); dialog.show(); return dialog;
    }
}
package org.smartregister.giz.viewholder;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.smartregister.giz.R;
import org.smartregister.giz.domain.EligibleChild;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.view.ListContract;
import org.smartregister.view.viewholder.ListableViewHolder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EligibleChildrenViewHolder extends ListableViewHolder<EligibleChild> {

    private View currentView;
    private TextView tvName;
    private TextView tvAge;
    private TextView tvFamily;
    private TextView tvDue;
    private Context context;

    public EligibleChildrenViewHolder(@NonNull View itemView, Context context) {
        super(itemView);
        this.currentView = itemView;
        tvName = itemView.findViewById(R.id.tvName);
        tvAge = itemView.findViewById(R.id.tvAge);
        tvFamily = itemView.findViewById(R.id.tvFamily);
        tvDue = itemView.findViewById(R.id.tvDue);
        this.context = context;
    }

    @Override
    public void bindView(EligibleChild eligibleChild, ListContract.View<EligibleChild> view) {
        List<String> stringList = new ArrayList<>();
        tvName.setText(eligibleChild.getFullName());
        String dob = GizUtils.getDuration(
                new SimpleDateFormat(GizConstants.DateTimeFormat.yyyy_MM_dd, Locale.US).format(eligibleChild.getDateOfBirth())
        );
        String age = currentView.getContext().getString(R.string.age);
        tvAge.setText(age + " " + dob);
        tvFamily.setText(eligibleChild.getFamilyName());
        String due = currentView.getContext().getString(R.string.due);
        String[] dueVaccine = eligibleChild.getDueVaccines();
        for (String vaccine : dueVaccine) {
            String val;
            if (vaccine.startsWith(" ")) {
                val = vaccine.toLowerCase().substring(1).replace(" ", "_").trim();
            } else {
                val = vaccine.toLowerCase().replace(" ", "_").trim();
            }
            String value = GizUtils.getStringResourceByName(val, context).trim();
            stringList.add(value);
        }
        tvDue.setText(due + ": " + GizUtils.toCSV(stringList));
        currentView.setOnClickListener(v -> view.onListItemClicked(eligibleChild, v.getId()));
    }


    @Override
    public void resetView() {
        tvName.setText("");
        tvAge.setText("");
        tvFamily.setText("");
        tvDue.setText("");
    }
}
package org.smartregister.giz.viewholder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.smartregister.giz.R;
import org.smartregister.giz.domain.VillageDose;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.view.ListContract;
import org.smartregister.view.viewholder.ListableViewHolder;

import java.util.Map;

public class VillageDoseViewHolder extends ListableViewHolder<VillageDose> {

    private View currentView;
    private TextView tvName;
    private LinearLayout linearLayout;
    private Context context;

    public VillageDoseViewHolder(@NonNull View itemView, Context context) {
        super(itemView);
        this.currentView = itemView;
        tvName = itemView.findViewById(R.id.tvName);
        linearLayout = itemView.findViewById(R.id.linearLayout);
        this.context = context;
    }

    @Override
    public void bindView(VillageDose villageDose, ListContract.View<VillageDose> view) {
        tvName.setText(villageDose.getVillageName());
        if (villageDose.getRecurringServices() != null) {
            for (Map.Entry<String, Integer> entry : villageDose.getRecurringServices().entrySet()) {
                View viewVillage = LayoutInflater.from(currentView.getContext()).inflate(R.layout.village_dose, null, false);

                TextView tvName = viewVillage.findViewById(R.id.tvName);
                String val = entry.getKey() != null ? entry.getKey().toLowerCase().replace(" ", "_").trim() : "";
                String value = GizUtils.getStringResourceByName(val, context).trim();

                tvName.setText(value);

                TextView tvAmount = viewVillage.findViewById(R.id.tvAmount);
                tvAmount.setText(entry.getValue() != null ? entry.getValue().toString() : "");

                linearLayout.addView(viewVillage);
            }
        }
        currentView.setOnClickListener(v -> view.onListItemClicked(villageDose, v.getId()));
    }

    @Override
    public void resetView() {
        if (tvName != null)
            tvName.setText("");

        linearLayout.removeAllViews();
    }
}

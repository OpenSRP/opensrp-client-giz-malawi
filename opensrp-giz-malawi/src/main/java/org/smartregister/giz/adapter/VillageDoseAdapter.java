package org.smartregister.giz.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.smartregister.giz.R;
import org.smartregister.giz.domain.VillageDose;
import org.smartregister.giz.viewholder.VillageDoseViewHolder;
import org.smartregister.view.ListContract;
import org.smartregister.view.adapter.ListableAdapter;
import org.smartregister.view.viewholder.ListableViewHolder;

import java.util.List;

public class VillageDoseAdapter extends ListableAdapter<VillageDose, ListableViewHolder<VillageDose>> {
    private Context context;

    public VillageDoseAdapter(List<VillageDose> items, ListContract.View<VillageDose> view, Context context) {
        super(items, view);
        this.context = context;
    }

    @NonNull
    @Override
    public ListableViewHolder<VillageDose> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.village_dose_report_item, parent, false);
        return new VillageDoseViewHolder(view, context);
    }
}

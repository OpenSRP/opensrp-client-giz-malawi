package org.smartregister.giz.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.smartregister.giz.R;
import org.smartregister.giz.domain.EligibleChild;
import org.smartregister.giz.viewholder.EligibleChildrenViewHolder;
import org.smartregister.view.ListContract;
import org.smartregister.view.adapter.ListableAdapter;
import org.smartregister.view.viewholder.ListableViewHolder;

import java.util.List;

public class EligibleChildrenAdapter extends ListableAdapter<EligibleChild, ListableViewHolder<EligibleChild>> {
    private Context context;

    public EligibleChildrenAdapter(List<EligibleChild> items, ListContract.View<EligibleChild> view, Context context) {
        super(items, view);
        this.context = context;
    }

    @NonNull
    @Override
    public ListableViewHolder<EligibleChild> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.eligible_children_report_item, parent, false);
        return new EligibleChildrenViewHolder(view, context);
    }
}
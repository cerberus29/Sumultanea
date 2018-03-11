package com.sombright.simultanea;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

class PlayersViewAdapter extends ArrayAdapter<Player> implements View.OnClickListener {

    private final LayoutInflater mInflater;

    private final Context mContext;
    private final OnClickPlayerListener mListener;
    private boolean mClickable = false;

    static class ViewHolder {
        TextView textView;
        ImageButton imageButton;
        ProgressBar progressBar;
        int position;
    }

    PlayersViewAdapter(@NonNull Context context, int resource, OnClickPlayerListener listener) {
        super(context, resource);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mListener = listener;
    }

    @Override
    public @NonNull
    View getView(int position, @Nullable View convertView,
                 @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.players_example_item, parent, false);
            holder = new ViewHolder();
            holder.imageButton = convertView.findViewById(R.id.imageButtonPlayer);
            holder.imageButton.setScaleType(ImageView.ScaleType.FIT_START);
            holder.imageButton.setAdjustViewBounds(true);
            holder.imageButton.setOnClickListener(this);
            holder.textView = convertView.findViewById(R.id.textViewPlayerName);
            holder.progressBar = convertView.findViewById(R.id.progressBarPlayerHealth);
            holder.progressBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Player player = getItem(position);

        holder.imageButton.setTag(R.id.id_player, player);
        AnimationDrawable animationDrawable = player.getAnimation();
        holder.imageButton.setImageDrawable(animationDrawable);
        animationDrawable.start();
        holder.imageButton.setEnabled(mClickable && player.getHealth() != 0);

        holder.textView.setText(player.getName());
        holder.progressBar.setProgress(player.getHealth());
        holder.position = position;

        return convertView;
    }

    public void setAnswered(boolean answered) {
        for (int i = 0; i < getCount(); i++) {
            final Player player = getItem(i);
            if (player == null)
                continue;
            player.setAnswered(false);
        }
    }


    @Override
    public void onClick(View v) {
        if (mListener != null) {
            ViewHolder holder = (ViewHolder) v.getTag();
            final Player player = getItem(holder.position);
            mListener.onClickPlayer(player);
        }
    }

    public interface OnClickPlayerListener {
        void onClickPlayer(Player player);
    }

    public void setClickable(boolean clickable) {
        if (mClickable != clickable) {
            mClickable = clickable;
            // Rebuild the views
            notifyDataSetChanged();
        }
    }

    Player getPlayer(String endpointId) {
        for (int i = 0; i < getCount(); i++) {
            final Player player = getItem(i);
            if (player == null)
                continue;
            if (player.getEndpoint().getId().equals(endpointId))
                return player;
        }
        return null;
    }
}

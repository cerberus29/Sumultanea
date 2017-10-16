package com.example.cj.sumultanea;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

class CharacterSelectionAdapter extends ArrayAdapter<Character> {
    private final Context mContext;
    private final int mResource;
    private LayoutInflater mLayoutInflater = null;

    CharacterSelectionAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Character[] objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = null; // = convertView;
        if (view == null) {
            if (mLayoutInflater == null) {
                mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            view = mLayoutInflater.inflate(mResource, parent, false);
        }
        Character character = getItem(position);
        assert character != null;
        ImageView imageView = view.findViewById(R.id.imageViewCharacter);
        imageView.setImageResource(character.getImageResource());
        AnimationDrawable anim = (AnimationDrawable) imageView.getDrawable();
        anim.start();
        TextView textView = view.findViewById(R.id.textViewCharacterName);
        textView.setText(character.getName());
        view.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, 500));
        return view;
    }
}

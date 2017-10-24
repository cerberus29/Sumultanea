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

/* This class provides character tiles to the GridView */
class CharacterSelectionAdapter extends ArrayAdapter<Character> {
    private final Context mContext;
    private final int mResource;
    private LayoutInflater mLayoutInflater = null;

    // We initialize from the list of characters
    CharacterSelectionAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Character[] objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    // This is called by the GridView to request the image/name for a
    // specific character (position) in our list.
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Make sure the character at "position" exists in our list
        Character character = getItem(position);
        assert character != null;

        // Create a small view using our template layout "character_selection_example_item"
        View view = null; // = convertView; TODO: figure-out why animations break when recycling convertView
        if (view == null) {
            // Convert the XML template into a view object using the "layout inflater"
            if (mLayoutInflater == null) {
                mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            view = mLayoutInflater.inflate(mResource, parent, false);
        }

        // Replace the image and name from the template by this specific character info
        ImageView imageView = view.findViewById(R.id.imageViewCharacter);
        imageView.setImageResource(character.getImageResource());
        AnimationDrawable anim = (AnimationDrawable) imageView.getDrawable();
        anim.start();
        TextView textView = view.findViewById(R.id.textViewCharacterName);
        textView.setText(character.getStringResourceName());
        view.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, 500));
        return view;
    }
}

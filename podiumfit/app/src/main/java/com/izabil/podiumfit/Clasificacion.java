package com.izabil.podiumfit;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Clasificacion extends AppCompatActivity {

    private String podium[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clasificacion);
        podium = getIntent().getStringArrayExtra("podium");
        final int posicionPodium = getIntent().getIntExtra("posicionPodium",0);

        ListView simpleList;
        simpleList = (ListView)findViewById(R.id.lista);
        // listViewMother.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListMother) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_list_item, R.id.textView, podium) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                TextView name = (TextView) row.findViewById(R.id.textView);

                if(position== posicionPodium-1)
                {
                    Log.d("A", ""+position);
                    // do something change color
                    row.setBackgroundColor (Color.GREEN); // some color
                }

                return row;
            }

        };

        simpleList.setAdapter(arrayAdapter);
    }


}

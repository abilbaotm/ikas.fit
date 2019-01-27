package com.izabil.podiumfit;

import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectorGrupo extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Grupo actualGrupo;
    private Grupo[] grupos;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector_grupo);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        db.collection("grupo").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    grupos = new Grupo[task.getResult().size()];
                    int i = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        grupos[i] = (new Grupo(document.getId(), document.get("nombre").toString()));
                        i++;
                    }
                    Log.d("GRUPO", "STOP");
                    actualizarListaGrupos( grupos);

                } else {
                    Log.d("GRUPO", "Error getting documents: ", task.getException());
                }
            }
        });


        db.collection("user").document(mAuth.getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                actualGrupo = new Grupo(document.getId(), document.get("nombre").toString());
                            } else {
                                Log.d("TAG", "No such document");
                            }
                        } else {
                            Log.d("TAG", "get failed with ", task.getException());
                        }
                    }
                });


        //Log.e("GRUPOS", grupos.toString());

        //ArrayAdapter adapter = new ArrayAdapter<String>(this,
        //        android.R.layout.simple_spinner_dropdown_item, grupos);



        //spinner.setOnItemSelectedListener(this.onItemSelected);

    }

    public void actualizarListaGrupos(Grupo[] grupos){


        final Spinner spinner = (Spinner) findViewById(R.id.spinnerGrupos);


        ArrayAdapter<Grupo> adapter =
                new ArrayAdapter<Grupo>(getApplicationContext(),  android.R.layout.simple_spinner_dropdown_item, grupos);
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);



        //spinner.setOnItemSelectedListener(this);
    }


    public void guardarGrupo(View view){
        final Spinner spinner = (Spinner) findViewById(R.id.spinnerGrupos);
        Grupo nuevoGrupo = (Grupo) spinner.getSelectedItem();
        db.collection("users").document(mAuth.getUid()).update("grupo", db.collection("grupo").document(nuevoGrupo.getUid()));
        finish();
    }
    private String m_Text = "";

    public void crearGrupo(View view){
        //https://stackoverflow.com/a/10904665
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crear nuevo grupo");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, Object> nuevoGrupo = new HashMap<>();
                nuevoGrupo.put("nombre", input.getText().toString());


                // Add a new document with a generated ID
                db.collection("grupo")
                        .add(nuevoGrupo)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                //grupo creado, ahora guardar
                                db.collection("users").document(mAuth.getUid())
                                        .update("grupo", db.collection("grupo").document(documentReference.getId()));
                                finish();
                            }

                        });

            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

}

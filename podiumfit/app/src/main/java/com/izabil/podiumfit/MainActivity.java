package com.izabil.podiumfit;

import android.content.Intent;
import android.icu.util.Calendar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LOGIN";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleApiClient mGoogleApiClient;


    private int pasosSemana=0;
    private ArrayList<Object> historicoPasos = new ArrayList<>();
    private String podium[];
    private int posicionPodium = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // iniciar FirebaseAuth y FirebaseFirestore
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        signInAnonymously();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();




    }

    private int showDataSet(DataSet dataSet) {
        // https://github.com/navigatorv/Android_Health_Tracking_App/blob/master/app/src/main/java/com/example/kzha6954/mysteps/GoogleFit/GoogleFitService.java#L218-L260
        // obtenido el dataset de Google Fit, calcular todos los pasos de la semana
        int cuentaPasos = 0;
        Log.e("History", "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.e("History", "Data point:");
            Log.e("History", "\tType: " + dp.getDataType().getName());
            Log.e("History", "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.e("History", "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                Log.e("History", "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
                if (field.getName().equals("steps")){
                    //sumar los pasos
                    cuentaPasos += dp.getValue(field).asInt();

                    //generar el historico
                    Map<String, Object> historicoActual = new HashMap<>();


                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(dp.getEndTime(TimeUnit.MILLISECONDS));

                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);

                    historicoActual.put("date", calendar.getTime());
                    historicoActual.put("pasos",dp.getValue(field).asInt());
                    //añadir a la lista el historico de este día
                    historicoPasos.add(historicoActual);

                }
            }
        }

        return (cuentaPasos);
    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    private void signInAnonymously() {
        // [START signin_anonymously]
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            //check usuario
                            db.collection("users").document(mAuth.getUid()).get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document.exists()) {
                                                    Log.d("TAG", "such document");
                                                } else {
                                                    //generar estructura para este usuario nuevo
                                                    Map<String, Object> data = new HashMap<>();
                                                    data.put("pasos", 0);
                                                    data.put("author_id", mAuth.getUid());

                                                    data.put("historico", new HashMap<>());

                                                    // bydefault se añade al grup default
                                                    data.put("grupo", db.collection("grupo").document("default") );

                                                    //subir la info anterior a la db
                                                    db.collection("users").document(mAuth.getUid()).set(data);
                                                    Log.d("TAG", "No such document");
                                                }
                                            } else {
                                                Log.d("TAG", "get failed with ", task.getException());
                                            }
                                        }
                                    });



                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }


                    }
                });
        // [END signin_anonymously]
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("HistoryAPI", "onConnectionFailed");
    }

    public void onConnected(@Nullable Bundle bundle) {
        Log.e("HistoryAPI", "onConnected");

        //solicitar la actualización de datos
        new ViewWeekStepCountTask().execute();
        podium();
    }

    public void podium(){
        //procesar los datos para el podium

        DocumentReference docRef = db.collection("users").document(mAuth.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        db.collection("users")
                                .whereEqualTo("grupo", document.get("grupo"))
                                .orderBy("pasos", Query.Direction.DESCENDING)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            int cantidad = task.getResult().size();
                                            podium = new String[cantidad];
                                            int posicion = 1;
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                Log.d(TAG, document.getId() + " => " + document.getData());
                                                Log.d(TAG, posicion + ". - Pasos: " + document.getData().get("pasos"));
                                                podium[posicion-1] = posicion + ".- " + document.getData().get("pasos");
                                                try {
                                                    if (document.getData().get("author_id").equals(mAuth.getUid())) {
                                                        Log.d(TAG, "  ^^^YO");
                                                        posicionPodium = posicion;

                                                        //guardar grupo actual
                                                        DocumentReference obj = (DocumentReference) document.getData().get("grupo");
                                                        obtenerGrupo(obj);

                                                    }
                                                } catch (NullPointerException e) {
                                                    Log.d(TAG, "");
                                                }
                                                posicion += 1;

                                            }

                                            actualizarUi(posicionPodium, cantidad);

                                            Log.d(TAG, "TOTAL: " + cantidad);
                                        } else {
                                            Log.d(TAG, "Error getting documents: ", task.getException());
                                        }
                                    }
                                });
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });


    }

    public void obtenerGrupo(DocumentReference documentReference){
        //https://firebase.google.com/docs/firestore/query-data/get-data
        Log.d(TAG, documentReference.getPath());
        db.document(documentReference.getPath())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {

                                TextView grupoActual = (TextView) findViewById(R.id.grupoActual);
                                Log.e("STOP", document.getData().get("nombre").toString());
                                grupoActual.setText(document.getData().get("nombre").toString());


                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });



    }

    public void actualizarUi(final int posicionPodium, int cantidad){
        TextView podiumTxt = (TextView) findViewById(R.id.position);
        podiumTxt.setText(posicionPodium + "/" + cantidad);


        TextView pasos = findViewById(R.id.pasosText);
        pasos.setText("" + pasosSemana);


    }

    public void verClasificacion(View view)
    {
        Intent intent = new Intent(MainActivity.this, Clasificacion.class);
        //https://www.dev2qa.com/passing-data-between-activities-android-tutorial/
        intent.putExtra("podium", podium);
        intent.putExtra("posicionPodium", posicionPodium);
        startActivity(intent);
    }

    public void verAjustes(View view){
        Intent intent = new Intent(MainActivity.this, SelectorGrupo.class);
        startActivity(intent);

    }

    private class ViewWeekStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            historicoPasos =  new ArrayList<>();

            Calendar cal = Calendar.getInstance();
            Date now = new Date();


            //hack: decir que la hora actual es 23:59:59 para obtener buckets de días enteros
            cal.setTime(now);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);


            cal.getTime();

            long endTime = cal.getTimeInMillis();
            //restar una semana
            cal.add(Calendar.WEEK_OF_YEAR, -1);
            long startTime = cal.getTimeInMillis();

            DateFormat dateFormat = DateFormat.getDateInstance();
            Log.e("History", "Range Start: " + dateFormat.format(startTime));
            Log.e("History", "Range End: " + dateFormat.format(endTime));

            //solicitar los pasos
            final DataSource ds = new DataSource.Builder()
                    .setAppPackageName("com.google.android.gms")
                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                    .setType(DataSource.TYPE_DERIVED)
                    .setStreamName("estimated_steps")
                    .build();



            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(ds, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build();


            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mGoogleApiClient, readRequest).await(1, TimeUnit.MINUTES);

            int cuentaPasos = 0;
            //procesar los buckets de los pasos
            //Used for aggregated data
            if (dataReadResult.getBuckets().size() > 0) {
                Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        cuentaPasos += showDataSet(dataSet);
                    }
                }
            }
            //Used for non-aggregated data
            else if (dataReadResult.getDataSets().size() > 0) {
                Log.e("History", "Number of returned DataSets: " + dataReadResult.getDataSets().size());
                for (DataSet dataSet : dataReadResult.getDataSets()) {
                    cuentaPasos += showDataSet(dataSet);
                }
            }
            //actualizar los pasos de la semana
            setPasosSemana(cuentaPasos);

            //solicitar la actualización de los pasos
            podium();


            return null;
            //https://github.com/tutsplus/Android-GoogleFit-HistoryAPI/blob/master/app/src/main/java/com/tutsplus/googlefit/MainActivity.java
        }

    }

    private void setPasosSemana(int nuevos) {
        Log.e("HistoryAPI", "Pasos en semana: " + nuevos);

        pasosSemana = nuevos;


        // Actualizar colección con los nuevos pasos e historico
        db.collection("users").document(mAuth.getUid()).update("pasos",pasosSemana);
        db.collection("users").document(mAuth.getUid()).update("historico",historicoPasos);


    }


}

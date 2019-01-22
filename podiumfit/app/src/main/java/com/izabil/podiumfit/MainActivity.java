package com.izabil.podiumfit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.icu.util.Calendar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.Element;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firestore.v1beta1.WriteResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "LOGIN";
    private GoogleApiClient mGoogleApiClient;


    private int pasosSemana=0;
    private ArrayList<Object> historicoPasos = new ArrayList<>();
    private void setPasosSemana(int nuevos) {
        Log.e("HistoryAPI", "Pasos en semana: " + nuevos);

        pasosSemana = nuevos;


// Add a new document with a generated ID
        db.collection("users").document(mAuth.getUid()).update("pasos",pasosSemana);
        db.collection("users").document(mAuth.getUid()).update("historico",historicoPasos);




    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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


//        displayStepDataForToday();


    }

    private class ViewWeekStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {


            Calendar cal = Calendar.getInstance();
            Date now = new Date();



            cal.setTime(now);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);


            cal.getTime();

            long endTime = cal.getTimeInMillis();
            cal.add(Calendar.WEEK_OF_YEAR, -1);
            long startTime = cal.getTimeInMillis();

            DateFormat dateFormat = DateFormat.getDateInstance();
            Log.e("History", "Range Start: " + dateFormat.format(startTime));
            Log.e("History", "Range End: " + dateFormat.format(endTime));

//Check how many steps were walked and recorded in the last 7 days
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
            setPasosSemana(cuentaPasos);

            podium();


            return null;
            //https://github.com/tutsplus/Android-GoogleFit-HistoryAPI/blob/master/app/src/main/java/com/tutsplus/googlefit/MainActivity.java
        }

    }

    private int showDataSet(DataSet dataSet) {
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
                    cuentaPasos += dp.getValue(field).asInt();

                    Map<String, Object> historicoActual = new HashMap<>();


                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(dp.getEndTime(TimeUnit.MILLISECONDS));

                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);

                    historicoActual.put("date", calendar.getTime());
                    historicoActual.put("pasos",dp.getValue(field).asInt());
                    historicoPasos.add(historicoActual);

                }
            }
        }

        return (cuentaPasos);
    }

    private void displayStepDataForToday() {
        DailyTotalResult result = Fitness.HistoryApi.readDailyTotal( mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA ).await(1, TimeUnit.MINUTES);
        showDataSet(result.getTotal());
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

                            Map<String, Object> data = new HashMap<>();
                            data.put("pasos", 0);
                            data.put("author_id", mAuth.getUid());

                            data.put("historico", new HashMap<>());
                            data.put("grupo", db.collection("grupo").document("default") );


                            db.collection("users").document(user.getUid()).set(data);

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }


                    }
                });
        // [END signin_anonymously]
    }
    private class ViewTodaysStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            displayStepDataForToday();
            return null;
        }
    }

    public static Date removeTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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

        new ViewWeekStepCountTask().execute();
    }

    public void podium(){
        db.collection("users")
                .whereEqualTo("grupo", db.collection("grupo").document("default"))
                .orderBy("pasos", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int cantidad = task.getResult().size();
                            int posicion = 1;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Log.d(TAG, posicion + ". - Pasos: " + document.getData().get("pasos"));
                                try {
                                    if (document.getData().get("author_id").equals(mAuth.getUid())) {
                                        Log.d(TAG, "  ^^^YO");
                                    }
                                } catch (NullPointerException e) {
                                    Log.d(TAG, "");
                                }
                                posicion += 1;
                            }
                            Log.d(TAG, "TOTAL: " + cantidad);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

}

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
//const firebaseHelper = require('firebase-functions-helper');

import * as express from 'express';
import * as bodyParser from "body-parser";
admin.initializeApp(functions.config().firebase);
const db = admin.firestore();
const app = express();
const main = express();
//const contactsCollection = 'users';
main.use('/api/v1', app);
main.use(bodyParser.json());
main.use(bodyParser.urlencoded({ extended: false }));





// webApi is your functions name, and you will pass main as
// a parameter
export const comprobarPasos = functions.https.onRequest(main);
app.get('/recuento/', (req, res) => {
    const now: Date = new Date();
    now.setDate(now.getDate() - 6);

    const citiesRef = db.collection('users');
    citiesRef.get()
        .then(snapshot => {
            snapshot.forEach(doc => {
                const datos = doc.data();
                const historicoPasos = datos.historico;
                console.log(historicoPasos.length);

                let nuevaSuma = 0;
                let nuevoHistorial = [];
                for (let i in historicoPasos) {

                    if (now.getTime() < historicoPasos[i].date.getTime()) {
                        nuevaSuma += historicoPasos[i].pasos;
                        nuevoHistorial = nuevoHistorial.concat(historicoPasos[i])
                    }
                }
                console.log(nuevoHistorial);
                console.log(nuevaSuma);

                const setDoc = db.collection('users').doc(doc.id).set({'historico': nuevoHistorial}, { merge: true });
                const setDoc2 = db.collection('users').doc(doc.id).set({'pasos': nuevaSuma}, { merge: true });

                setDoc;
                setDoc2;



            });




        })
        .catch(err => {
            console.log('Error getting documents', err);
        });

    res.send("OK");


});


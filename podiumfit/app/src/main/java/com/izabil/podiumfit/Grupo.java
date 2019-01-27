package com.izabil.podiumfit;

public class Grupo {
    private String uid;
    private String nombre;

    public Grupo(String uid, String nombre) {
        this.uid = uid;
        this.nombre = nombre;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}

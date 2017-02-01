package com.example.versus.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by ilm on 20/12/2014.
 */
public class ConnectThread extends Thread {
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-111111111111");
    private final static String NOMBRE_SERVICIO="miAppBluetooth";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private BluetoothAdapter mbtAdapter;
    private ActividadBluetooth actividad;
    private Handler mHandler;

    public ConnectThread(BluetoothDevice device,BluetoothAdapter btAdapter,Handler h,ActividadBluetooth  act) {

        mbtAdapter=btAdapter;
        mHandler=h;
        actividad=act;
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Obtener un BluetoothSocket para conectar con el BluetoothDevice
        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            //tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    public void run() {
        // Se cancela la búsqueda de dispositivos para no ralentizar la conexión
        mbtAdapter.cancelDiscovery();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        synchronized (this) {
            EnviarCambioEstado(Constantes.ESTADO_CONECTADO, mmSocket.getRemoteDevice());
            actividad.Conectar(mmSocket, mmSocket.getRemoteDevice());
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    public void EnviarCambioEstado(int i,BluetoothDevice device){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO,i,-1);
        //Si hay dispositivo a enviar, se envia como Bundle
        if(device!=null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.NOMBRE_DISPOSITIVO, device.getName());
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }
}
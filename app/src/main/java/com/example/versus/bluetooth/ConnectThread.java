package com.example.versus.bluetooth;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Handler;

/**
 * Created by EmilioCB on 01/02/2017.
 */

public class ConnectThread extends Thread{
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-1lde-8a39-111111111111");
    private final static String NOMBRE_SERVICIO = "miAppBluetooth";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private BluetoothAdapter mbtAdapter;
    private ActividadBluetooth actividad;
    private Handler mHandler;

    public ConnectThread(BluetoothDevice device, BluetoothAdapter btAdapter, Handler h, ActividadBluetooth act){
        mbtAdapter = btAdapter;
        mHandler = h;
        actividad = act;
        BluetoothSocket tmp = null;
        mmDevice = device;

        //Obtener un BluetoothSocket para conectar con el BluetoothDevice
        try{
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException ex){}
        mmSocket = tmp;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void run(){
        //Se cancela la busqueda de dispositivos para no ralentizar la conexion
        mbtAdapter.cancelDiscovery();

        try{
            mmSocket.connect();
        } catch (IOException connectException){
            try{
                mmSocket.close();
            } catch (IOException closeException){}
            return;
        }
        synchronized (this){
            EnviarCambioEstado(Constantes.ESTADO_CONECTADO, mmSocket.getRemoteDevice());
            actividad.Conectar(mmSocket, mmSocket.getRemoteDevice());
        }
    }

    public void cancel(){
        try{
            mmSocket.close();
        } catch (IOException e){}
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void EnviarCambioEstado(int i, BluetoothDevice device){
        Notification.MessagingStyle.Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO, i, -1);
        //Si hay dispositivo a enviar, se envia como un Bundle
        if(device != null){
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.NOMBRE_DISPOSITIVO, device.getName());
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }
}

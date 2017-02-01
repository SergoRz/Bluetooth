package com.example.versus.bluetooth;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import java.io.IOException;
import java.util.UUID;
import android.os.Handler;

/**
 * Created by Versus on 01/02/2017.
 */

public class AcceptThread extends Thread {
    private static final UUID MY_UUIDD = UUID.fromString("fa87c0d0-afac-11de-8a39-111111111111");
    private final static String NOMBRE_SERVICIO = "miAppBluetooth";
    private BluetoothServerSocket mmServerSocket = null;
    private Handler mHandler;
    private ActividadBluetooth actividad;

    public AcceptThread(BluetoothAdapter btAdapter, Handler h, ActividadBluetooth act) {
        mHandler = h;
        actividad = act;
        BluetoothServerSocket tmp = null;

        try{
            tmp = btAdapter.listenUsingRfcommWithServiceRecord(NOMBRE_SERVICIO, MY_UUIDD);
        } catch (IOException e) {}

        mmServerSocket = tmp;
    }

    public  void run(){
        BluetoothSocket socket = null;
        //seguir escuchando hasta que ocurra una excepción
        //o se acepte un socket
        while(true){
            try{
                EnviarCambioEstado(Constantes.ESTADO_CONECTADO, null);
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            //Si la conexion fue aceptada
            if(socket != null){
                synchronized (this){
                    EnviarCambioEstado(Constantes.ESTADO_CONECTADO, socket.getRemoteDevice());
                    actividad.Conectar(socket, socket.getRemoteDevice());
                }
                break;
            }
        }
    }

    public void cancel(){
        try {
            mmServerSocket.close();
        } catch (IOException e) {}
    }

    public void EnviarCambioEstado(int i, BluetoothDevice device){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO, i, -1);
        //Si hay un dispositivoa enviar, se envia como Bundle
        if(device != null){
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.NOMBRE_DISPOSITIVO, device.getName());
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }

}

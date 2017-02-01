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
 * Clase que se utiliza para iniciar la conexion a un dispositivo bluetooth
 * que este escuchando conexiones.
 */
public class ConnectThread extends Thread {
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-111111111111");
    private final static String NOMBRE_SERVICIO="miAppBluetooth";
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private BluetoothAdapter mbtAdapter;
    private ActividadBluetooth actividad;
    private Handler mHandler;

    /**
     * Constructor de la clase ConnectThread
     * @param device Dispositivo conectado
     * @param btAdapter Adaptador utilizado para conectar con el bluetooth
     * @param h Cola de mensajes
     * @param act Clase principal
     */
    public ConnectThread(BluetoothDevice device, BluetoothAdapter btAdapter, Handler h, ActividadBluetooth act) {
        mbtAdapter=btAdapter;
        mHandler=h;
        actividad=act;
        mmDevice = device;

        // Obtener un BluetoothSocket para conectar con el BluetoothDevice
        try {
            mmSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) { }
    }

    /**
     * Metodo que se ejecuta al iniciar el hilo
     * Se encarga de establecer una conexion del cliente con el servidor
     */
    public void run() {
        // Se cancela la búsqueda de dispositivos para no ralentizar la conexión
        mbtAdapter.cancelDiscovery();

        try {
            mmSocket.connect(); //Se conecta con el servidor
        } catch (IOException connectException) {
            try {
                mmSocket.close(); //Se cierra la conextion
            } catch (IOException closeException) { }
            return;
        }

        synchronized (this) {
            //Se notifica del cambio de estado al Handler
            EnviarCambioEstado(Constantes.ESTADO_CONECTADO, mmSocket.getRemoteDevice());
            actividad.Conectar(mmSocket, mmSocket.getRemoteDevice());
        }
    }

    /**
     * Metodo que se encarga de mandar un mensaje que avisa que se ha producido un cambio
     * de estado.
     * El mensaje se envia a traves de un Bundle
     * @param i Estado de la aplicacion
     * @param device Dispositivo conectado
     */
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
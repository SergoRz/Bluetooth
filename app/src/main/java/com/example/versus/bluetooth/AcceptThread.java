package com.example.versus.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;


/**
 * Clase que se utiliza para iniciar el dispositivo como servidor.
 * Pone un socket Bluetooth a la escucha de conexiones.
 */
public class AcceptThread extends Thread {
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-111111111111");
    private final static String NOMBRE_SERVICIO="miAppBluetooth";

    private BluetoothServerSocket mmServerSocket;

    private Handler mHandler;
    private ActividadBluetooth actividad;

    /**
     * Constructor de la clase AcceptThread
     * @param btAdapter Adaptador utilizado para conectar con el bluetooth
     * @param h Cola de mensajes
     * @param act Clase principal
     */
    public AcceptThread(BluetoothAdapter btAdapter, Handler h, ActividadBluetooth act) {
        mHandler=h;
        actividad=act;
        try {
            //Crea el listener para el socket
            mmServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(NOMBRE_SERVICIO, MY_UUID);
        } catch (IOException e) { }
    }

    /**
     * Metodo que se ejecuta al iniciar el hilo
     * Se encarga de escuchar peticiones de clientes
     */
    public void run() {
        BluetoothSocket socket = null;
        // seguir escuchando hasta que ocurra una excepción o se acepte un socket
        while (true) {
            try {
                //Se notifica del cambio de estado al Handler
                EnviarCambioEstado(Constantes.ESTADO_CONECTANDO, null);
                socket = mmServerSocket.accept(); //Se acepta conexion del cliente si es solicitada
            } catch (IOException e) {
                break;
            }
            // Si la conexión es aceptada
            if (socket != null) {
                synchronized (this) {
                    EnviarCambioEstado(Constantes.ESTADO_CONECTADO, socket.getRemoteDevice());
                    //Se establece la conexion
                    actividad.Conectar(socket, socket.getRemoteDevice());
                }
                break;
            }
        }
    }

    /**
     * Metodo que se encarga de mandar un mensaje que avisa que se ha producido un cambio
     * de estado.
     * El mensaje se envia a traves de un Bundle
     * @param i Estado de la aplicacion
     * @param device Dispositivo conectado
     */
    public void EnviarCambioEstado(int i, BluetoothDevice device){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO, i, -1);
        //Si hay dispositivo a enviar, se envia como Bundle
        if(device!=null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.NOMBRE_DISPOSITIVO, device.getName());
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }
}

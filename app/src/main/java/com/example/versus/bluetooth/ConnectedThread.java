package com.example.versus.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Clase ConnectedThread
 * Clase que extiende de Thread, este Thread lo arrancarrán tanto AcceptThread como ConnectThread
 * cuando hayan obtenido un socket válido, habiendose producido la conexión.
 * Este Thread será el encargador de recibir y enviar datos mediante el BluetoothSocket.
 */
public class ConnectedThread extends Thread {
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private Handler mHandler;

    /**
     * Constructor de la clase ConnectedThread
     * @param socket conexion mediante la que envia y recibe datos
     * @param handler cola de mensajes que generan los Threads
     */
    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        mHandler = handler;

        //Se obtienen del BluetoothSocket los streams de entrada y salida
        try {
            mmInStream = socket.getInputStream();
            mmOutStream = socket.getOutputStream();
        } catch (IOException e) {}
    }

    /**
     * Método run del Thread, se ejecuta cuando se use el método start() del Thread.
     * Lee del InputStream del BluetoothSocket y obtiene en mensaje
     */
    public void run() {
        //Se crea un array de bytes
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                //Se leen los datos del InputStream
                bytes = mmInStream.read(buffer);

                //Envia un mensaje a la cola de mensajes indicando que ha recibido un mensaje
                mHandler.obtainMessage(Constantes.MENSAJE_RECIBIDO, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                EnviarCambioEstado(Constantes.CONEXION_PERDIDA);
                break;
            }
        }
    }

    /**
     * Método que envia a la cola de mensajes del Thread el mensaje de que se ha cambiado el estado.
     * @param i Entero que indica el estado de la aplicacion
     */
    public void EnviarCambioEstado(int i){
        //Obtiene el mensaje de la cola de mensajes
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO,i,-1);
        //Envia el mensaje al final de la cola de mensaje
        mHandler.sendMessage(msg);
    }

    /**
     * Método que permite escribir en el OutputSteram del BluetoothSocket los datos que
     * se le pasen por parametro.
     * @param buffer Datos que va a escribir en el OutputStream del BluetoothSocket
     */
    public void write(byte[] buffer) {
        try {
            //Se escriben los datos en el OutputStream del BluetoothSocket
            mmOutStream.write(buffer);

            //Envia un mensaje a la cola de mensajes indicando que ha enviado un mensaje
            mHandler.obtainMessage(Constantes.MENSAJE_ENVIADO, -1, -1, buffer).sendToTarget();
        } catch (IOException e) {}
    }
}

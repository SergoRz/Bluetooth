package com.example.versus.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Handler;

public class ConnectedThread extends Thread{
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler mHandler;

    public ConnectedThread(BluetoothSocket socket, Handler handler){
        mmSocket = socket;
        mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        //Obtener del BluetoothSocket los streams de entrada y salida
        try{
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException ex){}
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        int bytes;
        while(true){
            try{
                //Leer del InputStream
                bytes = mmInStream.read(buffer);
                //Mensaje recibido
                mHandler.obtainMessage(Constantes.MENSAJE_RECIBIDO, bytes, -1, buffer).sendToTarget();
            } catch (IOException ex){
                EnviarCambioEstado(Constantes.CONEXION_PERDIDA);
                break;
            }
        }
    }

    public void EnviarCambioEstado(int i){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO, i, -1);
        mHandler.sendMessage(msg);
    }

    //Escribe en el OutStream
    public void write(byte[] buffer){
        try{
            mmOutStream.write(buffer);
            //Mensaje enviado
            mHandler.obtainMessage(Constantes.MENSAJE_ENVIADO, -1, -1, buffer).sendToTarget();
        } catch (IOException ex){}
    }

    public void cancel(){
        try{
            mmSocket.close();
        } catch (IOException ex){}
    }
}

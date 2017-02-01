package com.example.versus.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class ActividadBluetooth extends AppCompatActivity {
    BluetoothAdapter btAdapter;
    AcceptThread mAcceptThread;
    ConnectedThread mConnectedThread;
    ConnectThread mConnectThread;
    BluetoothDevice dispositivoConectado;
    EditText txtEnviar;
    EditText txtRecibir;
    TextView tvEstado;
    int estado, seleccionado;
    ListView lista_dispositivos;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    //Cuando se pulse el boton iniciar servidor
    public void IniciarServidor(View v){
        if(btAdapter != null){
            mAcceptThread = new AcceptThread(btAdapter, mHandler, this);
            mAcceptThread.start();
        } else{
            Toast.makeText(this,"Por favor, dale al botón iniciar", Toast.LENGTH_SHORT).show();
        }
    }

    public void Enviar(View v){
        enviarMensaje(txtEnviar.getText().toString());
    }

    private void enviarMensaje(String mensaje){
        if (estado == Constantes.SIN_CONECTAR){
            Toast.makeText(this,"Conecta primero a un servidor", Toast.LENGTH_SHORT).show();
        }

        //Comprobamos si hay algo que enviar
        if(mensaje.length() > 0){
            byte[] send = mensaje.getBytes(); //Obtener el mensaje en bytes
            mConnectedThread.write(send);
        }
    }

    public void IniciarCliente(View v){
        if(seleccionado == -1){
            Toast.makeText(this,"Elige un dispositivo al que conectarse primero", Toast.LENGTH_SHORT).show();
        } else{
            String x = lista_dispositivos.getItemAtPosition(seleccionado).toString();
            String address = x.substring(x.length() - 17);
            dispositivoConectado = btAdapter.getRemoteDevice(address);

            if(dispositivoConectado!=null){
                mConnectThread = new ConnectThread(dispositivoConectado, btAdapter, mHandler, this);
                mConnectThread.start();
            }else{
                Toast.makeText(this,"No pude obtener enlace al dispositivo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public synchronized void Conectar(BluetoothSocket socket, BluetoothDevice device){
        //Comienza la conexion
        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
    }

    private final Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case Constantes.CAMBIAR_ESTADO:
                    if(msg.arg1 == Constantes.ESTADO_CONECTADO)
                        CambiarEstado(msg.arg1, msg.getData().getString(Constantes.NOMBRE_DISPOSITIVO));
                    else
                        CambiarEstado(msg.arg1, "");
                    break;
                case Constantes.MENSAJE_ENVIADO:
                        CambiarEstado(Constantes.MENSAJE_ENVIADO,"Mensaje enviado");
                    break;
                case Constantes.MENSAJE_RECIBIDO:
                    byte[] readBuf = (byte[]) msg.obj;
                    //Construye una cadena de caracterer a partir de los caracteres del buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    txtRecibir.setText(readMessage);
                    CambiarEstado(Constantes.MENSAJE_RECIBIDO,"Mensaje recibido");
                    break;
            }
        }

        private void CambiarEstado(int arg1, String s) {
            tvEstado.setText(s);
        }
    };
}

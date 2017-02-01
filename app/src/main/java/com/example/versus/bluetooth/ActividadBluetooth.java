package com.example.versus.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Clase que
 */
public class ActividadBluetooth extends Activity implements CheckBox.OnCheckedChangeListener,ListView.OnItemClickListener{
    private int estado=Constantes.SIN_CONECTAR;
    private final static int HABILITA_BT = 1;
    BluetoothAdapter btAdapter;
    public boolean mActivado=false; //por defecto no está activado
    ArrayAdapter<String> arrayDispositivos;
    ListView lista_dispositivos;
    int seleccionado=-1;
    BluetoothDevice dispositivoConectado;
    BluetoothSocket socket;
    TextView txtEstado,txtEnviar,txtRecibir;
    CheckBox checkbox;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;


    public void IniciarBluetooth(View v){

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter== null) {
            // El dispositivo no soporta Bluetooth
            Toast.makeText(this,"El dispositivo no soporta bluetooth",Toast.LENGTH_LONG).show();
        }
        else
        if (!btAdapter.isEnabled()) {
            // El dispositivo soporta Bluetooth, pero no está activado
            // se solicita su conexión
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, HABILITA_BT);
        }
        else{
            mActivado=true;
        }
        onCheckedChanged(checkbox,checkbox.isChecked());
    }

    public void VerDispositivos(){
        EncuentraEmparejados();
        lista_dispositivos.setAdapter(arrayDispositivos);
    }

    public void EncuentraEmparejados(){
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // Si hay dispositivos emparejados
        if (pairedDevices.size() > 0) {
            // Iteramos por los dispositivos emparejados
            for (BluetoothDevice device : pairedDevices) {
                arrayDispositivos.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==HABILITA_BT)
            if(resultCode==RESULT_OK) {
                // El dispositivo activó el bluetooth
                Toast.makeText(this, "El dispositivo activo el bluetooth", Toast.LENGTH_LONG).show();
                mActivado = true;
            }else{
                Toast.makeText(this, "No se ha activado el bluetooth", Toast.LENGTH_LONG).show();
            }
    }

    // Crea un BroadcastReceiver para ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Cuando se descubre un dispositivo
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Obtener el BluetoothDevice del Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Añadir el nombre y su dirección a la lista
                arrayDispositivos.add(device.getName() + "\n" + device.getAddress());
            }

        }
    };

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(btAdapter!=null)
            if(b) {
                arrayDispositivos.clear();
                Descubre();
            }
            else{
                VerDispositivos();
            }
    }

    public void Descubre(){
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkbox=(CheckBox)findViewById(R.id.cbVinculadas);
        checkbox.setOnCheckedChangeListener(this);
        lista_dispositivos=(ListView)findViewById(R.id.lvDispositivos);
        lista_dispositivos.setOnItemClickListener(this);
        arrayDispositivos=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        txtEstado=(TextView)findViewById(R.id.tvEstado);
        txtEnviar=(TextView)findViewById(R.id.txtEnviar);
        txtRecibir=(TextView)findViewById(R.id.txtRecibido);
        estado=Constantes.SIN_CONECTAR;

        // Registra el receptor de descubrir dispositivos
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }


    public synchronized void Conectar(BluetoothSocket socket, BluetoothDevice device) {
        // Comienza la conexión!!
        mConnectedThread = new ConnectedThread(socket,mHandler);
        mConnectedThread.start();
    }

    public void Enviar(View v){
        enviarMensaje(txtEnviar.getText().toString());
    }

    public void IniciarServidor(View v){
        if(btAdapter!=null) {
            mAcceptThread = new AcceptThread(btAdapter, mHandler, this);
            mAcceptThread.start();
        }
        else{
            Toast.makeText(this,"Por favor, dale al boton de iniciar",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metodo que se encarga de iniciar un dispositivo como Cliente
     * @param v Boton Iniciar cliente
     */
    public void IniciarCliente(View v){
        //Si no se ha escogido un dispositivo al cual conectarse..
        if(seleccionado==-1)
            Toast.makeText(this, "elige un dispositivo al que conectarte primero", Toast.LENGTH_LONG).show();
        else { //Si se ha escogido..
            //Se recoge el nombre del dispositivo
            String x = lista_dispositivos.getItemAtPosition(seleccionado).toString();
            //Se obtiene la direccion
            String address = x.substring(x.length() - 17);
            //Se establece la conexion con el dispositivo
            dispositivoConectado = btAdapter.getRemoteDevice(address);
            if(dispositivoConectado != null) { //Si se ha establecido la conexion correctamente..
                //Se lanza el hilo ConnectThread
                mConnectThread = new ConnectThread(dispositivoConectado, btAdapter, mHandler, this);
                mConnectThread.start();
            }
            else
                Toast.makeText(this, "no pude obtener enlace al dispositivo", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Metodo que se ejecuta al cerrar la aplicacion
     * Se encarga de parar la recepcion de dispositivos conectados por bluetooth
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    /**
     * Metodo que se encarga de guardar un dispositivo cuando ha sido clicado
     * @param adapterView Adaptador de la lista de dispositivos
     * @param view Vista de la ListView
     * @param i Item seleccionado
     * @param l Longitud
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        seleccionado=i;
    }

    /**
     * Metodo que se encarga de enviar un mensaje escribiendolo en el OutputStream del socket
     * @param mensaje Mensaje que se desea enviar.
     */
    private void enviarMensaje(String mensaje) {
        if (estado==Constantes.SIN_CONECTAR) { //Si no esta conectado..
            Toast.makeText(this, "conecta primero a un servidor!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Comprobamos si hay algo que enviar
        if (mensaje.length() > 0) {
            // Se envia el mensaje deseado
            byte[] send = mensaje.getBytes();
            mConnectedThread.write(send);
        }
    }

    /**
     * Handler que recibe la información de los Threads
     * Actualiza el txtEstado de la interfaz informando al usuario del estado de la aplicacion,
     * dependiendo del mensaje que recibe de los Threads
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constantes.CAMBIAR_ESTADO:
                    if(msg.arg1==Constantes.ESTADO_CONECTADO)
                        CambiarEstado(msg.arg1, msg.getData().getString(Constantes.NOMBRE_DISPOSITIVO));
                    else
                        CambiarEstado(msg.arg1, "");
                    break;
                case Constantes.MENSAJE_ENVIADO:
                    CambiarEstado(Constantes.MENSAJE_ENVIADO,"");
                    break;
                case Constantes.MENSAJE_RECIBIDO:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construye una cadena de caracteres a partir de los caracteres del buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    txtRecibir.setText(readMessage);
                    CambiarEstado(Constantes.MENSAJE_RECIBIDO,"");
                    break;
            }
        }
    };

    /**
     * Metodo que se encarga de actualizar la interfaz de la aplicacion para poder visualizar en
     * que estado se encuentra la aplicacion
     * @param i Mensaje de la calse Constantes que se quiere notificar
     * @param extra Informacion extra que se quiere añadir, se utiliza para indicar el nombre del
     *              dispositivo con el que se ha establecido la conexion
     */
    public void CambiarEstado(int i, String extra){
        estado=i;
        switch(i){
            case Constantes.ESTADO_CONECTADO:
                txtEstado.setText("Conectado: "+extra);
                break;
            case Constantes.ESTADO_CONECTANDO:
                txtEstado.setText("Conectando");
                break;
            case Constantes.SIN_CONECTAR:
                txtEstado.setText("Sin conectar");
                break;
            case Constantes.MENSAJE_RECIBIDO:
                txtEstado.setText("Mensaje recibido");
                break;
            case Constantes.MENSAJE_ENVIADO:
                txtEstado.setText("Mensaje enviado");
                break;
        }

    }
}
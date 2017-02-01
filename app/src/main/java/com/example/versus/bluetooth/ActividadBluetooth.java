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
 * Clase ActividadBluetooth
 * Esta clase es la Activity principal del programa, nos mostrará la interfaz grafica
 * y nos permitira acceder a todas las funcionalidades de la aplicacion.
 */
public class ActividadBluetooth extends Activity implements CheckBox.OnCheckedChangeListener,ListView.OnItemClickListener{
    private int estado=Constantes.SIN_CONECTAR;
    private final static int HABILITA_BT = 1;
    private BluetoothAdapter btAdapter;
    private boolean mActivado=false; //por defecto no está activado
    private ArrayAdapter<String> arrayDispositivos;
    private ListView lista_dispositivos;
    private int seleccionado=-1;
    private BluetoothDevice dispositivoConectado;
    private BluetoothSocket socket;
    private TextView txtEstado,txtEnviar,txtRecibir;
    private CheckBox checkbox;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    /**
     * Metodo que se ejecuta al iniciar la Activity.
     * Carga la interfaz de usuario y registra los dispositivos encontrados
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkbox = (CheckBox)findViewById(R.id.cbVinculadas);
        checkbox.setOnCheckedChangeListener(this);
        lista_dispositivos = (ListView)findViewById(R.id.lvDispositivos);
        lista_dispositivos.setOnItemClickListener(this);
        arrayDispositivos = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        txtEstado = (TextView)findViewById(R.id.tvEstado);
        txtEnviar = (TextView)findViewById(R.id.txtEnviar);
        txtRecibir = (TextView)findViewById(R.id.txtRecibido);
        estado = Constantes.SIN_CONECTAR;

        //Registra el receptor de descubrir dispositivos
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //Empieza a buscar dispositivos conectados
        registerReceiver(mReceiver, filter);
    }

    /**
     * Método que comprueba si el dispositivo soporta Bluetooth, en ese caso comprueba si esta
     * activado, si no lo esta solicita al usuario que lo active.
     * @param v View que produce este evento
     */
    public void IniciarBluetooth(View v){
        //Obtiene el adaptador por defecto, si obtiene null significa que no tiene,
        //con lo cual el dispositivo no adminte bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter== null) {
            // El dispositivo no soporta Bluetooth
            Toast.makeText(this,"El dispositivo no soporta bluetooth",Toast.LENGTH_LONG).show();
        }
        else{
            if (!btAdapter.isEnabled()) {
                // El dispositivo soporta Bluetooth, pero no está activado
                // Se solicita al usuario que conecte el Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, HABILITA_BT);
            }
            else{
                //En este caso el dispositivo soporta Bluetooth y esta activado
                mActivado=true;
            }
        }

        //
        onCheckedChanged(checkbox, checkbox.isChecked());
    }

    /**
     * Método que obtiene la respuesta del usuario cuando se le solicita activar el Bluetooth.
     * @param requestCode Codigo de la solicitud
     * @param resultCode Codigo de la respuesta
     * @param data Intent que se lanza para recibir dicha respuesta
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==HABILITA_BT)
            if(resultCode==RESULT_OK) {
                //Se activo el Bluetooth
                Toast.makeText(this, "El dispositivo activo el bluetooth", Toast.LENGTH_LONG).show();
                mActivado = true;
            }else{
                //No se activo el Bluetooth
                Toast.makeText(this, "No se ha activado el bluetooth", Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(btAdapter != null)
            if(b) {
                arrayDispositivos.clear();
                Descubre();
            }
            else{
                VerDispositivos();
            }
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

    public void Descubre(){
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
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

    public void IniciarCliente(View v){
        if(seleccionado==-1)
            Toast.makeText(this, "elige un dispositivo al que conectarte primero", Toast.LENGTH_LONG).show();
        else {
            String x = lista_dispositivos.getItemAtPosition(seleccionado).toString();
            String address = x.substring(x.length() - 17);
            dispositivoConectado=btAdapter.getRemoteDevice(address);
            if(dispositivoConectado!=null) {
                mConnectThread = new ConnectThread(dispositivoConectado, btAdapter, mHandler, this);
                mConnectThread.start();
            }
            else
                Toast.makeText(this, "no pude obtener enlace al dispositivo", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        seleccionado = i;
    }

    private void enviarMensaje(String mensaje) {

        if (estado==Constantes.SIN_CONECTAR) {
            Toast.makeText(this, "conecta primero a un servidor!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Comprobamos si hay algo que enviar
        if (mensaje.length() > 0) {
            // Obtener
            byte[] send = mensaje.getBytes();
            mConnectedThread.write(send);
        }
    }
    public void CambiarEstado(int i, String extra){
        estado=i;
        switch(i){
            case Constantes.ESTADO_CONECTADO:
                txtEstado.setText("Conectado: " + extra);
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

    //Clases anónimas

    /**
     * The Handler que recibe la información de los Threads
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

}
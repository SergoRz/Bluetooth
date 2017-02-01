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
public class ActividadBluetooth extends Activity implements CheckBox.OnCheckedChangeListener,ListView.OnItemClickListener {
    private int estado = Constantes.SIN_CONECTAR;
    private final static int HABILITA_BT = 1;
    private BluetoothAdapter btAdapter;
    private boolean mActivado = false; //por defecto no está activado
    private ArrayAdapter<String> arrayDispositivos;
    private ListView lista_dispositivos;
    private int seleccionado = -1;
    private BluetoothDevice dispositivoConectado;
    private BluetoothSocket socket;
    private TextView txtEstado, txtEnviar, txtRecibir;
    private CheckBox checkbox;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    /**
     * Metodo que se ejecuta al iniciar la Activity.
     * Carga la interfaz de usuario y registra los dispositivos encontrados
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkbox = (CheckBox) findViewById(R.id.cbVinculadas);
        checkbox.setOnCheckedChangeListener(this);
        lista_dispositivos = (ListView) findViewById(R.id.lvDispositivos);
        lista_dispositivos.setOnItemClickListener(this);
        arrayDispositivos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        txtEstado = (TextView) findViewById(R.id.tvEstado);
        txtEnviar = (TextView) findViewById(R.id.txtEnviar);
        txtRecibir = (TextView) findViewById(R.id.txtRecibido);
        estado = Constantes.SIN_CONECTAR;

        //Registra el receptor de descubrir dispositivos
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //Empieza a buscar dispositivos conectados
        registerReceiver(mReceiver, filter);
    }

    /**
     * Método que comprueba si el dispositivo soporta Bluetooth, en ese caso comprueba si esta
     * activado, si no lo esta solicita al usuario que lo active.
     *
     * @param v View que produce este evento
     */
    public void IniciarBluetooth(View v) {
        //Obtiene el adaptador por defecto, si obtiene null significa que no tiene,
        //con lo cual el dispositivo no adminte bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            // El dispositivo no soporta Bluetooth
            Toast.makeText(this, "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {
                // El dispositivo soporta Bluetooth, pero no está activado
                // Se solicita al usuario que conecte el Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, HABILITA_BT);
            } else {
                //En este caso el dispositivo soporta Bluetooth y esta activado
                mActivado = true;
            }
        }

        //Si se cambia el estado del CheckBox
        onCheckedChanged(checkbox, checkbox.isChecked());
    }

    /**
     * Método que obtiene la respuesta del usuario cuando se le solicita activar el Bluetooth.
     * @param requestCode Codigo de la solicitud
     * @param resultCode  Codigo de la respuesta
     * @param data Intent que se lanza para recibir dicha respuesta
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == HABILITA_BT)
            if (resultCode == RESULT_OK) {
                //Se activo el Bluetooth
                Toast.makeText(this, "El dispositivo activo el bluetooth", Toast.LENGTH_LONG).show();
                mActivado = true;
            } else {
                //No se activo el Bluetooth
                Toast.makeText(this, "No se ha activado el bluetooth", Toast.LENGTH_LONG).show();
            }
    }

    /**
     * Método que se ejecuta cuando cambia el estado del del CheckBox.
     * Si se ha marcado el CheckBox este método va a limpiar el array de dispositivos, despues
     * va a comprobar si esta buscando dispositivos, en este caso reiniciara la busqueda.
     * Si se ha desmarcado el CheckBox lo que va a hacer va a ser buscar los dispositivos
     * vinculados y añadirlos al ListView.
     * @param compoundButton CheckBox que ejecuta este evento
     * @param checked Estado actual del CheckBox
     *
     */
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (btAdapter != null) {
            //Si se ha marcado el CheckBox
            if (checked) {
                //Limpia el array de dispositivos
                arrayDispositivos.clear();

                //Si esta busando, cancela la busqueda
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }
                //Vuelve a iniciar la busqueda
                btAdapter.startDiscovery();

            } else { //Si se ha desmarcado
                //Busca los dispositivos vinculados
                EncuentraVinculados();
                //Rellena el ListView
                lista_dispositivos.setAdapter(arrayDispositivos);
            }
        }
    }

    /**
     * Método que se encarga de obtener los dispositivos vinculados del nuestro dispositivo,
     * obtiene una coleccion de estos dispositivos y si tiene minimo uno los añadira al array
     * de dispositivos.
     */
    public void EncuentraVinculados() {
        //Obtiene los dispositivos vinculados
        Set<BluetoothDevice> vinculados = btAdapter.getBondedDevices();
        // Si hay dispositivos vinculados
        if (vinculados.size() > 0) {
            // Se recorren los dispositivos vinculados
            for (BluetoothDevice device : vinculados) {
                //Se añaden al array
                arrayDispositivos.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    public synchronized void Conectar(BluetoothSocket socket, BluetoothDevice device) {
        // Comienza la conexión!!
        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
    }

    /**
     * Metodo que se encarga de iniciar el dispositivo como Servidor
     * @param v Boton Iniciar Servidor
     */
    public void IniciarServidor(View v) {
        if (btAdapter != null) { //Si existe el adaptador
            //Se lanza el hilo AcceptThread
            mAcceptThread = new AcceptThread(btAdapter, mHandler, this);
            mAcceptThread.start();
        } else {
            Toast.makeText(this, "Por favor, dale al boton de iniciar", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metodo que se encarga de iniciar un dispositivo como Cliente
     * @param v Boton Iniciar cliente
     */
    public void IniciarCliente(View v) {
        //Si no se ha escogido un dispositivo al cual conectarse..
        if (seleccionado == -1)
            Toast.makeText(this, "elige un dispositivo al que conectarte primero", Toast.LENGTH_LONG).show();
        else { //Si se ha escogido..
            //Se recoge el nombre del dispositivo
            String x = lista_dispositivos.getItemAtPosition(seleccionado).toString();
            //Se obtiene la direccion
            String address = x.substring(x.length() - 17);
            //Se establece la conexion con el dispositivo
            dispositivoConectado = btAdapter.getRemoteDevice(address);
            if (dispositivoConectado != null) { //Si se ha establecido la conexion correctamente..
                //Se lanza el hilo ConnectThread
                mConnectThread = new ConnectThread(dispositivoConectado, btAdapter, mHandler, this);
                mConnectThread.start();
            } else
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
     *
     * @param adapterView Adaptador de la lista de dispositivos
     * @param view Vista de la ListView
     * @param i Item seleccionado
     * @param l Longitud
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        seleccionado = i;
    }

    /**
     * Metodo que se encarga de enviar un mensaje escribiendolo en el OutputStream del socket
     * @param v boton >>
     */
    public void enviarMensaje(View v) {
        String mensaje = txtEnviar.getText().toString();
        if (estado == Constantes.SIN_CONECTAR) { //Si no esta conectado..
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
     * Metodo que se encarga de actualizar la interfaz de la aplicacion para poder visualizar en
     * que estado se encuentra la aplicacion
     *
     * @param i Mensaje de la calse Constantes que se quiere notificar
     * @param extra Informacion extra que se quiere añadir, se utiliza para indicar el nombre del
     *              dispositivo con el que se ha establecido la conexion
     */
    public void CambiarEstado(int i, String extra) {
        estado = i;
        switch (i) {
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

    //----------------------------------------
    // CLASES ANONIMAS
    //----------------------------------------
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
                    if (msg.arg1 == Constantes.ESTADO_CONECTADO)
                        CambiarEstado(msg.arg1, msg.getData().getString(Constantes.NOMBRE_DISPOSITIVO));
                    else
                        CambiarEstado(msg.arg1, "");
                    break;
                case Constantes.MENSAJE_ENVIADO:
                    CambiarEstado(Constantes.MENSAJE_ENVIADO, "");
                    break;
                case Constantes.MENSAJE_RECIBIDO:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construye una cadena de caracteres a partir de los caracteres del buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    txtRecibir.setText(readMessage);
                    CambiarEstado(Constantes.MENSAJE_RECIBIDO, "");
                    break;
            }
        }
    };

    /**
     * Se crea un BroadcastReceiver para cuando se encuentre un dispositivo (ACTION_FOUND)
     */
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Cuando se encuentra un dispositivo
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Se obtiene el dispositivo bluetooth del intent el BluetoothDevice del Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Se añade el dispositivo al array de dispositivos
                arrayDispositivos.add(device.getName() + "\n" + device.getAddress());
            }

        }
    };

}
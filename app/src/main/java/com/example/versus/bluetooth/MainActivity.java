package com.example.versus.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter btAdapter;
    boolean mActivado = false;
    private static  final int HABILITA_BT = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void IniciarBluetooth(View v){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null){
            //El dispositivo no soporta Bluetooth
            Toast.makeText(this, "Su dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            //Si no esta activo se lanza una actividad con un intent de tipo BluetoothAdapter.ACTION_REQUEST_ENABLE
            if(!btAdapter.isEnabled()){
                //El dispositivo soporta Bluetooth, pero no está activado, se solicita su conexion
                //Para solicitar al usuario que lo active
                Intent enableBtIntent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, HABILITA_BT);
                //Si lo activa el result code sera RESULT_OK
            }
            else{
                mActivado = true;
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == HABILITA_BT){
            //El dispositivo activo el bluetooth
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Bluetooth activado",  Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "Bluetooth no activado",  Toast.LENGTH_LONG).show();
            }
        }
    }
}

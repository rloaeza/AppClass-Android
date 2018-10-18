package com.appclass.appclass;

import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;


import com.appclass.appclass.db.Alumno;
import com.appclass.appclass.db.Refs;
import com.appclass.appclass.db.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ClaseAsistencia extends AppCompatActivity {

    Button bFecha;
    Button bBuscarBT;
    Button bCrearLista;
    Button bTerminar;
    ListView lvAlumnos;
    String claseCodigo;
    String claseNombre;
    ImageView ivBorrarLista;


    AlumnoItemAdapter listaAlumnos;

    String fechaListaAnterior;
    String fechaLista;
    String correo;
    String correoFix;


    private FirebaseDatabase firebaseDatabase ;
    private DatabaseReference databaseReferenceClase;

    private List<Usuario> listaAlumnosClase;
    private ValueEventListener postListenerCargarListaAsistencia;
    private boolean ordenar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clase_asistencia);

        claseCodigo = getIntent().getStringExtra(AppClassReferencias.claseCodigo);
        claseNombre = getIntent().getStringExtra(AppClassReferencias.claseNombre);

        setTitle(claseNombre);
        bFecha = findViewById(R.id.bFecha);
        bBuscarBT = findViewById(R.id.bBuscarBT);
        bTerminar = findViewById(R.id.bTerminar);
        lvAlumnos = findViewById(R.id.lvAlumnos);
        bCrearLista = findViewById(R.id.bCrearLista);
        ivBorrarLista = findViewById(R.id.ivBorrarLista);



        ordenar =false;
        correo = Funciones.getCorreo();
        correoFix=Funciones.getCorreoFix(correo);


        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReferenceClase = firebaseDatabase.getReference(Refs.AppClass);



        fechaLista = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        fechaListaAnterior = "";
        bFecha.setText(fecha2Text());



        listaAlumnos= new AlumnoItemAdapter(getApplicationContext(), new ArrayList<>(), claseCodigo, databaseReferenceClase, correoFix);



        bFecha.setOnClickListener(e->{
            DatePickerFragment newFragment = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    fechaListaAnterior = fechaLista;
                    fechaLista = year + "-" + (month<9?"0":"")+(month+1) + "-" + (day<10?"0":"")+day;
                    bFecha.setText(fecha2Text());
                    cargarLista(ordenar);
                }
            });
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } );


        lvAlumnos.setAdapter(listaAlumnos);



        bTerminar.setOnClickListener(e -> finish() );




        bCrearLista.setOnClickListener(e-> {

            databaseReferenceClase.child(claseCodigo+"+"+fechaLista).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(Usuario alumno : listaAlumnosClase) {
                        alumno.setAsistio(false);
                        databaseReferenceClase.child(Refs.asistencia).child(claseCodigo+"+"+fechaLista).child(alumno.getIdControl()).setValue(alumno);
                    }
                    bCrearLista.setVisibility(View.INVISIBLE);
                    bBuscarBT.setVisibility(View.VISIBLE);
                    bTerminar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });





        listaAlumnosClase = new ArrayList<>();
        ValueEventListener postListenerCargarAlumno = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaAlumnosClase.clear();
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    Usuario alumno = item.getValue(Usuario.class);
                    listaAlumnosClase.add(alumno);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReferenceClase.child(Refs.clases).child(claseCodigo).child(Refs.alumnos).addValueEventListener(postListenerCargarAlumno);





        cargarLista(ordenar);




        //BT
        BTAdapter = BluetoothAdapter.getDefaultAdapter();


        bBuscarBT.setOnClickListener(e-> {

            if (!BTAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_BLUETOOTH);
            }
            else {

                boolean b = BTAdapter.startDiscovery();
                if(b) {
                    bBuscarBT.setEnabled(false);
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            bBuscarBT.setEnabled(true);
                            BTAdapter.cancelDiscovery();
                        }
                    }, 15000);
                }
                Log.e(AppClassReferencias.TAG_Debug, "Iniciando BT");
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(broadcastReceiver,filter);
            }
        });






        ivBorrarLista.setOnClickListener( e-> {

            AlertDialog.Builder builderClaseNueva = new AlertDialog.Builder(this);
            builderClaseNueva.setTitle(getString(R.string.claseListaConfirmarMSG).replace("#", getString(R.string.claseConfirmarCodigo)));

            final EditText etNombreClaseNueva = new EditText(this);
            etNombreClaseNueva.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
            builderClaseNueva.setView(etNombreClaseNueva);
            builderClaseNueva.setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                        String msg = etNombreClaseNueva.getText().toString();

                        if(!msg.equals(getString(R.string.claseConfirmarCodigo)))
                            return;
                        databaseReferenceClase.child(Refs.asistencia).child(claseCodigo+"+"+fechaLista).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    databaseReferenceClase.child(Refs.asistencia).child(claseCodigo+"+"+fechaLista).removeValue();
                                }

                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });



                    }

            );

            builderClaseNueva.setNegativeButton(getString(R.string.cancelar), (dialog, which) -> dialog.cancel());
            builderClaseNueva.show();

        });

    }

    private void cargarLista(boolean reOrdenar) {

        listaAlumnos.setFecha(fechaLista);



        postListenerCargarListaAsistencia = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaAlumnos.clear();
                if (dataSnapshot.exists()) {

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        Usuario alumno = item.getValue(Usuario.class);
                        listaAlumnos.add(alumno);
                    }

                    bCrearLista.setVisibility(View.INVISIBLE);
                    ivBorrarLista.setVisibility(View.VISIBLE);
                    bBuscarBT.setVisibility(View.VISIBLE);
                    bTerminar.setVisibility(View.VISIBLE);
                } else {

                    bCrearLista.setVisibility(View.VISIBLE);
                    ivBorrarLista.setVisibility(View.INVISIBLE);
                    bBuscarBT.setVisibility(View.INVISIBLE);
                    bTerminar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        databaseReferenceClase.child(Refs.asistencia).child(claseCodigo+"+"+fechaLista).orderByChild("asistio").addValueEventListener(postListenerCargarListaAsistencia);




    }

    private String fecha2Text() {
        Date fecha = Calendar.getInstance().getTime();
        try {
            fecha = new SimpleDateFormat("yyyy-MM-dd").parse(fechaLista);
        } catch (ParseException e1) { }
        return new SimpleDateFormat("dd / MMM / yyyy").format(fecha);
    }








    // BT

    public static int REQUEST_BLUETOOTH = 1;

    BluetoothAdapter BTAdapter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            // Log.e("BT", action);
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(bluetoothDevice!=null) {

                    if(bluetoothDevice.getName()!=null)
                        Log.e(AppClassReferencias.TAG_Debug, bluetoothDevice.getAddress()+"->"+bluetoothDevice.getName());

                    Usuario alumno=existeBT(bluetoothDevice.getAddress());
                    if( alumno !=null) {
                        if(!alumno.isAsistio())
                          databaseReferenceClase.child(Refs.asistencia).child(claseCodigo+"+"+fechaLista).child(alumno.getIdControl()).child(Refs.bdAsistio).setValue(true);
                    }

                }

            }
        }
    };

    private Usuario existeBT(String macBT) {
        for(int indice=0; indice<listaAlumnos.getCount(); indice++) {
            if( listaAlumnos.getItem(indice).getMacBT().equalsIgnoreCase(macBT) )
                return listaAlumnos.getItem(indice);

        }
        return null;
    }





}

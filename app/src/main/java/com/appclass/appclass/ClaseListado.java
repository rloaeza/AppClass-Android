package com.appclass.appclass;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.appclass.appclass.db.Instructor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ClaseListado extends AppCompatActivity {

    boolean doubleBackToExitPressedOnce = false;
    boolean existeUsuario = false;
    private FirebaseDatabase firebaseDatabase ;
    private DatabaseReference databaseReference;
    private String btMacLocal;
    private String correo;
    private String correoFix;

    private ClaseItemAdapter lvClasesAdapter;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_listado_clases);

        setTitle(R.string.listadoClases);
        ListView lvClases = findViewById(R.id.lvClases);
        lvClasesAdapter= new ClaseItemAdapter(getApplicationContext(), new ArrayList<>());

        lvClases.setAdapter(lvClasesAdapter);
        lvClases.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = new Intent(this, ClaseAsistencia.class);
            Clase clase = lvClasesAdapter.getItem(i);
            intent.putExtra(AppClassReferencias.claseCodigo, clase.getCodigo());
            intent.putExtra(AppClassReferencias.claseNombre, clase.getNombreClase());
            startActivity(intent);


        });

        btMacLocal = android.provider.Settings.Secure.getString(this.getContentResolver(), "bluetooth_address");
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(AppClassReferencias.AppClass);

        correo = FirebaseAuth.getInstance().getCurrentUser().getEmail().toString();
        correoFix=correo.replace(".", "+");




        databaseReference.child(AppClassReferencias.Personas).child(correoFix).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    databaseReference.child(AppClassReferencias.Personas).child(correoFix).setValue(
                            new Instructor("","", "", btMacLocal, correo)
                    );
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });








        ValueEventListener postListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                lvClasesAdapter.clear();
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    Clase clase = item.getValue(Clase.class);

                    lvClasesAdapter.add(clase);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        databaseReference.child(AppClassReferencias.Personas).child(correoFix).child(AppClassReferencias.Clases).addValueEventListener(postListener);





    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_clases, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menuAgregarClase:

                AlertDialog.Builder builderClaseNueva = new AlertDialog.Builder(this);
                builderClaseNueva.setTitle(getString(R.string.nombreClase));
                final EditText etNombreClaseNueva = new EditText(this);
                etNombreClaseNueva.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
                builderClaseNueva.setView(etNombreClaseNueva);
                builderClaseNueva.setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                    String clase = etNombreClaseNueva.getText().toString();
                    databaseReference.child(AppClassReferencias.Personas).child(correoFix).child(AppClassReferencias.Clases).child(clase).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()) {
                                String codigo = Clase.genCodigo(AppClassReferencias.tamCodigo);
                                Clase itemClase = new Clase(etNombreClaseNueva.getText().toString(), "", codigo, true, 10 + (int) (Math.random() * 40));
                                databaseReference.child(AppClassReferencias.Personas).child(correoFix).child(AppClassReferencias.Clases).child(codigo).setValue(
                                        itemClase
                                );
                                lvClasesAdapter.add(itemClase);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }

                );

                builderClaseNueva.setNegativeButton(getString(R.string.cancelar), (dialog, which) -> dialog.cancel());
                builderClaseNueva.show();
                
                break;
            case R.id.menuAgregarAlumno:
                Intent intent = new Intent(this, AlumnoDatos.class );
                startActivity(intent);
                break;
            case R.id.menuCerrarSesion:
                FirebaseAuth.getInstance().signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();

            finishAffinity();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.salirBackDoble), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }
}
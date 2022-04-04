package com.example.agendacdb;


import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaParser;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ProgressDialog progreso;
    JSONArray datosJSON;
    JSONObject jsonObject;
    Bundle parametros = new Bundle();
    int posicion=0;

    InputStreamReader idReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtenerDatos myAsync = new obtenerDatos();
        myAsync.execute();

        Button btn = (Button) findViewById(R.id.btnAgregar);
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                parametros.putString("accion", "nuevo");
                nuevo_agenda();
            }
        });
    }

    public void nuevo_agenda(){
        Intent agregar_agenda = new Intent(MainActivity.this, agregar_agenda.class);
        agregar_agenda.putExtras(parametros);
        startActivity(agregar_agenda);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.mimenu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        try{
            datosJSON.getJSONObject(info.position);
            menu.setHeaderTitle(datosJSON.getJSONObject(info.position).getJSONObject("value").getString("nombre").toString());
            posicion = info.position;
        }catch(Exception ex){

        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnxAgregar:
                parametros.putString("accion", "nuevo");
                nuevo_agenda();
                return true;

            case R.id.mnxModificar:
                parametros.putString("accion", "modificar");
                try{
                    parametros.putString("valores", datosJSON.getJSONObject(posicion).getJSONObject("value").toString());
                    nuevo_agenda();
                }catch (Exception ex){

                }
                return true;

            case R.id.mnxEliminar:
                JSONObject miData = new JSONObject();
                try{
                    miData.put("_id", datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id"));
                }catch (Exception ex){

                }
                eliminarDatos objEliminar = new eliminarDatos();
                objEliminar.execute(miData.toString());
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private class obtenerDatos extends AsyncTask<Void, Void, String> {
        HttpURLConnection urlConnection;
        @Override
        protected String doInBackground(Void... params){
            StringBuilder result = new StringBuilder();

            try{
                URL url = new URL("http://127.0.0.1:5984/_utils/#database/agenda/_all_docs");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null){
                    result.append(line);
                }
            }catch (Exception ex){
                Log.e("Mi error", "Error", ex);
                ex.printStackTrace();
            }finally{
                urlConnection.disconnect();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try{
                jsonObject = new JSONObject(s);
                datosJSON = jsonObject.getJSONArray("rows");

                ListView lstAgenda = (ListView)findViewById(R.id.ltsagenda);

                final ArrayList<String> alAgenda = new ArrayList<>();
                final ArrayAdapter<String> aaAgenda = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, alAgenda);
                lstAgenda.setAdapter(aaAgenda);

                for(int i=0; i<datosJSON.length(); i++){
                    alAgenda.add(datosJSON.getJSONObject(i).getJSONObject("value").getString("nombre").toString());
                }
                aaAgenda.notifyDataSetChanged();
                registerForContextMenu(lstAgenda);
            }catch (Exception ex){
                Toast.makeText(MainActivity.this, "Error : "+ ex.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class eliminarDatos extends AsyncTask<String,String,String> {
        HttpURLConnection urlConnection;
        @Override
        protected String doInBackground(String... params){
            StringBuilder result = new StringBuilder();

            String JsonResponse = null;
            String jsonDATA = params[0];
            BufferedReader reader = null;

            try{
                String uri = "http://127.0.0.1:5984/_utils/#database/agenda/"+
                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id")+"?rev="+
                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_rev");
                URL url = new URL(uri);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("DELETE");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }catch (Exception ex){
                Log.e("Mi Error", "Error", ex);
                ex.printStackTrace();
            }finally {
                urlConnection.disconnect();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String s){
            super.onPostExecute(s);

            try{
                JSONObject jsonObject = new JSONObject(s);

                if(jsonObject.getBoolean("Ok")){
                    Toast.makeText(MainActivity.this, "Registro eliminado con exito.", Toast.LENGTH_LONG).show();
                    Intent regresar = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(regresar);
                }else{
                    Toast.makeText(MainActivity.this, "Error al intentar eliminar el registro...", Toast.LENGTH_LONG).show();
                }
            }catch (Exception ex){
                Toast.makeText(MainActivity.this, "Error al enviar a la red: "+ ex.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
package mx.gob.jovenes.guanajuato.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLOutput;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.realm.Realm;
import mx.gob.jovenes.guanajuato.R;
import mx.gob.jovenes.guanajuato.activities.ContinousCaptureActivity;
import mx.gob.jovenes.guanajuato.activities.EstadisticasActivity;
import mx.gob.jovenes.guanajuato.adapters.RVEventosAdapter;
import mx.gob.jovenes.guanajuato.api.EventoAPI;
import mx.gob.jovenes.guanajuato.api.Response;
import mx.gob.jovenes.guanajuato.application.MyApplication;
import mx.gob.jovenes.guanajuato.connection.ConnectionUtilities;
import mx.gob.jovenes.guanajuato.model.Evento;
import mx.gob.jovenes.guanajuato.model.Lugar;
import mx.gob.jovenes.guanajuato.model.Region;
import mx.gob.jovenes.guanajuato.model.Usuario;
import mx.gob.jovenes.guanajuato.utils.DateUtilities;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

/**
 * Created by uriel on 21/06/16.
 */
public class DetalleEventoFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener{
    private static String ID_EVENTO = "id_evento";
    private Evento evento;
    private MapFragment mapaEvento;
    private TextView tvNombreEvento;
    private TextView tvDireccionEvento;
    private TextView tvDescripcionEvento;
    private TextView tvFechaEvento;
    private Button btnAsistencia;
    private IntentIntegrator qrScan;
    private Realm realm;
    private EventoAPI eventoAPI;
    private Retrofit retrofit;
    private String token;
    private Activity activity;
    private SharedPreferences prefs;
    private TextView textViewEventoAunNoEmpieza;
    private TextView textViewEventoCaducado;

    private Button botonEstadisticas;

    public static DetalleEventoFragment newInstance(int idEvento) {
        DetalleEventoFragment detalleEventoFragment = new DetalleEventoFragment();
        Bundle args = new Bundle();
        args.putInt(ID_EVENTO, idEvento);//cambia el valor de la variable por el id de la region seleccionada
        detalleEventoFragment.setArguments(args);
        return detalleEventoFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = MyApplication.getRealmInstance();
        activity = getActivity();
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplication());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_detalle_evento, container, false);

        evento = realm.where(Evento.class).equalTo("idEvento", getArguments().getInt(ID_EVENTO)).findFirst();

        mapaEvento = (MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.mapa_evento);
        mapaEvento.getMapAsync(this);

        tvNombreEvento = (TextView) v.findViewById(R.id.tv_nombre_evento);
        tvDireccionEvento = (TextView) v.findViewById(R.id.tv_direccion_evento);
        tvDescripcionEvento = (TextView) v.findViewById(R.id.tv_descripcion_evento);
        tvFechaEvento = (TextView) v.findViewById(R.id.tv_fechas_evento);
        btnAsistencia = (Button) v.findViewById(R.id.btn_registrar_evento);
        botonEstadisticas = (Button) v.findViewById(R.id.btn_estadisticas);
        textViewEventoAunNoEmpieza = (TextView) v.findViewById(R.id.textview_evento_aun_no_empieza);
        textViewEventoCaducado = (TextView) v.findViewById(R.id.textview_evento_caducado);

        tvNombreEvento.setText(evento.getTitulo());
        tvDireccionEvento.setText(evento.getDireccion());
        tvDescripcionEvento.setText(evento.getDescripcion());
        tvFechaEvento.setText(getFechaCast(evento.getFechaInicio()) + " - " + getFechaCast(evento.getFechaFin()));
        checkAsist();

        btnAsistencia.setOnClickListener(this);

        botonEstadisticas.setOnClickListener((View) -> {
            Intent intent = new Intent(getActivity(), EstadisticasActivity.class);
            Bundle b = new Bundle();
            b.putInt("idEvento", evento.getIdEvento());
            intent.putExtras(b);
            startActivity(intent);
        });

        return v;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getActivity(), ContinousCaptureActivity.class);
        Bundle b = new Bundle();
        b.putInt("idEvento", evento.getIdEvento());
        intent.putExtras(b);

        startActivity(intent);
    }

    public void checkAsist() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String dateInStringbegin = getFechaCast(evento.getFechaInicio());
        String dateInStringend = getFechaCast(evento.getFechaFin());
        try {
            Date fechainicio = formatter.parse(dateInStringbegin);
            Date fechafin = formatter.parse(dateInStringend);
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date newFormat = formatter.parse(dateFormat.format(date));

            boolean antesDeFecha = (newFormat.before(fechainicio));
            boolean enFecha = (newFormat.after(fechainicio) && newFormat.before(fechafin));
            boolean despuesDeFecha = (newFormat.after(fechafin));

            if (enFecha) {
                btnAsistencia.setVisibility(View.VISIBLE);
            } else if (despuesDeFecha) {
                textViewEventoCaducado.setVisibility(View.VISIBLE);
            } else if (antesDeFecha) {
                textViewEventoAunNoEmpieza.setVisibility(View.VISIBLE);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String getFechaCast(String fecha) {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat miFormato = new SimpleDateFormat("dd/MM/yyyy");

        try {
            String reformato = miFormato.format(formato.parse(fecha));
            return reformato;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        float zoomLevel = (float) 16.0;
        LatLng coordenadas = new LatLng(evento.getLatitud(), evento.getLongitud()); //coordenadas de la región
        googleMap.addMarker(new MarkerOptions().position(coordenadas).title(evento.getTitulo())); //pone el puntero en las coordenadas
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadas, zoomLevel)); //hace el zoom en el mapa
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapaEvento = (MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.mapa_evento);
        if (mapaEvento != null)
            getActivity().getFragmentManager().beginTransaction().remove(mapaEvento).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(evento.getTitulo());
    }

}

package mx.gob.jovenes.guanajuato.api;

import java.util.ArrayList;

import mx.gob.jovenes.guanajuato.model.DatosMensajes;
import mx.gob.jovenes.guanajuato.model.Mensaje;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by codigus on 27/07/2017.
 */

public interface ChatAPI {

    @POST("chat/enviar")
    Call<Response<Boolean>> enviarMensaje (
            @Query("api_token") String apiToken,
            @Query("mensaje") String mensaje
    );

    @POST("chat/mensajes")
    Call<Response<DatosMensajes>> obtenerMensajes (
            @Query("api_token") String apiToken,
            @Query("page") int page
    );

}

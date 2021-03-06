package les.ufcg.edu.br.povmt.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import les.ufcg.edu.br.povmt.R;
import les.ufcg.edu.br.povmt.activities.SplashActivity;
//import les.ufcg.edu.br.povmt.database.AtividadePersister;
import les.ufcg.edu.br.povmt.database.DataSource;
import les.ufcg.edu.br.povmt.models.Atividade;
import les.ufcg.edu.br.povmt.models.Categoria;
import les.ufcg.edu.br.povmt.models.Prioridade;
import les.ufcg.edu.br.povmt.models.TI;
import les.ufcg.edu.br.povmt.utils.AtividadeAdapter;
import les.ufcg.edu.br.povmt.utils.IonResume;
import les.ufcg.edu.br.povmt.utils.ServiceHandler;

import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements IonResume {

    private static String url = "http://lucasmatos.pythonanywhere.com/povmt/1/";
    public static final String TAG = "MAIN";
    List<Atividade> atividadeList;
    private static final int TRABALHO = 0;
    private static final int LAZER = 1;
    private String idUser;
//    private AtividadePersister atividadePersister;
    private ArrayList atividades;
    private RecyclerView listaAtividades;
    private TextView listaVazia;
    private CardView campoAtividades;
    private TextView horasInvestidas;
    public static AtividadeAdapter adapter;
    private SharedPreferences sharedPreferences;
    private DataSource dataSource;
    RequestQueue requestQueue;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sharedPreferences = getContext().getSharedPreferences(SplashActivity.PREFERENCE_NAME, Context.MODE_PRIVATE);
        idUser = sharedPreferences.getString(SplashActivity.USER_ID, "");
//        idUser = 123456;
//        atividadePersister = new AtividadePersister(getContext());
//        atividades = (ArrayList) atividadePersister.getAtividades(idUser);
        listaAtividades = (RecyclerView) view.findViewById(R.id.rview_atividades);
        campoAtividades = (CardView) view.findViewById(R.id.ll_atividades);
        listaVazia = (TextView) view.findViewById(R.id.sem_ti);
        horasInvestidas = (TextView) view.findViewById(R.id.tv_horasinv_semana);
        requestQueue = DataSource.getInstance(getActivity().getApplicationContext()).getRequestQueue();

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        listaAtividades.setLayoutManager(llm);
        getAtividadesUsuario();
        new GetaAtividade().execute();
        return view;
    }

    private ArrayList<TI> getTIsUsuario(final long idatividade){
        final String URL_GET_TIS = "http://lucasmatos.pythonanywhere.com/povmt/tilist/" + idatividade;
        final ArrayList<TI> listti = new ArrayList<TI>();
        final Response.ErrorListener genericErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleVolleyError(error);
            }
        };

        final Response.Listener<JSONArray> getTisResponseListener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for(int i =0; i < response.length(); i++){
                    try {
                        String data = response.getJSONObject(i).getString("data");
                        int semana = response.getJSONObject(i).getInt("semana");
                        int horas = response.getJSONObject(i).getInt("horas");
                        long idti= response.getJSONObject(i).getLong("id");
                        TI ti  = new TI(idti,data,semana, horas);
                        listti.add(ti);
                        if (dataSource.getTI(ti.getId()) == null) {
                            dataSource.inserirTI(ti, idatividade);
                        }
                        refresh();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        final JsonArrayRequest getTisRequest = new JsonArrayRequest(URL_GET_TIS,
                getTisResponseListener, genericErrorListener);

        requestQueue.add(getTisRequest);

        return listti;
    }

    private void getAtividadesUsuario(){
        final String URL_GET_ATIVIDADES = "http://lucasmatos.pythonanywhere.com/povmt/" + sharedPreferences.getString(SplashActivity.USER_ID, "");

        final Response.ErrorListener genericErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleVolleyError(error);
            }
        };

        final Response.Listener<JSONArray> getAtividadesResponseListener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for(int i = 0; i < response.length(); i++){
                    try {
                        String name = response.getJSONObject(i).getString("nome");
                        long idatividade= response.getJSONObject(i).getLong("id");
                        Categoria categoria_db = null;
                        String categoria = response.getJSONObject(i).getString("categoria");
                        if(categoria.equals("TRABALHO")) {
                            categoria_db = Categoria.TRABALHO;
                        } else if (categoria.equals("LAZER")) {
                            categoria_db = Categoria.LAZER;
                        }else{
                            categoria_db = Categoria.VAZIO;
                        }
                        Prioridade prioridade_db = null;
                        String prioridade = response.getJSONObject(i).getString("prioridade");
                        if(prioridade.equals("BAIXA")) {
                            prioridade_db = Prioridade.BAIXA;
                        } else if (prioridade.equals( "MEDIA")) {
                            prioridade_db = Prioridade.MEDIA;
                        } else if (prioridade.equals( "ALTA")) {
                            prioridade_db = Prioridade.ALTA;
                        }

                        String foto = response.getJSONObject(i).getString("url_imagem");
                        Atividade atividade = new Atividade(idatividade, name, categoria_db, prioridade_db, foto);

                        if (dataSource.getAtividade(atividade.getId()) == null) {
                            dataSource.inserirAtividade(atividade, sharedPreferences.getString(SplashActivity.USER_ID, ""));
                        }

                        ArrayList<TI> listati = getTIsUsuario(idatividade);
                        for (int j= 0; j < listati.size(); j++){
                            atividade.addTI(listati.get(j));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        };

        final JsonArrayRequest getAtividadesRequest = new JsonArrayRequest(URL_GET_ATIVIDADES,
                getAtividadesResponseListener, genericErrorListener);
        requestQueue.add(getAtividadesRequest);
    }

    private void handleVolleyError(VolleyError error) {
        NetworkResponse response = error.networkResponse;
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            Log.e(TAG, "Sem resposta!");
        } else if (error instanceof AuthFailureError) {
            Log.e(TAG, "Erro de autenticacao!");
        } else if (error instanceof ServerError) {
            Log.e(TAG, "Erro de servidor!");
        } else if (error instanceof NetworkError) {
            Log.e(TAG, "Erro de rede!");
        } else if (error instanceof ParseError) {
            Log.e(TAG, "Erro ao converter resposta!");
        } else {
            Log.e(TAG, "Erro desconhecido!");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        //            atividadePersister = AtividadePersister.getInstance(getContext());
        dataSource = DataSource.getInstance(getContext());
        atividades = (ArrayList) dataSource.getAtividades(idUser);
        adapter = new AtividadeAdapter(new ArrayList<Atividade>(atividades));
        listaAtividades.setAdapter(adapter);

        if (atividades.isEmpty()) {
            listaVazia.setVisibility(View.VISIBLE);
            campoAtividades.setVisibility(View.GONE);
        } else {
            listaVazia.setVisibility(View.GONE);
            campoAtividades.setVisibility(View.VISIBLE);
        }

        //TODO Calcular TI na Semana
        horasInvestidas.setText(String.valueOf(getHorasInvestidas()) + " " + getString(R.string.horas_investidas));
    }

    private int getHorasInvestidas() {
        int horasInvestidas = 0;
        List<Atividade> atividadesList = adapter.getmAtividades();
        for (int i = 0; i < atividadesList.size(); i++) {
            horasInvestidas += atividadesList.get(i).getTI();
        }
        return horasInvestidas;
    }


    /**
     * Async task class to get json by making HTTP call
     */
    private class GetaAtividade extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            /*pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage("Carregando ...");
            pDialog.setCancelable(false);
            pDialog.show();*/

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String json = sh.makeServiceCall(url, ServiceHandler.GET);

            if (json != null) {
                try {
                    JSONArray atividades = new JSONArray(json);

                    for (int i = 0; i < atividades.length(); i++) {
                        JSONObject d = atividades.getJSONObject(i);
                        Gson gson = new Gson();
                        //disciplinaList.add(gson.fromJson(d.toString(), Disciplina.class));
                        String TAG = "QuickNotesMainActivity";
                        Log.d(TAG, d.getString("id"));
                        Log.d(TAG, d.getString("nome"));
                        Log.d(TAG, d.getString("url_imagem"));
                        Log.d(TAG, d.getString("prioridade"));
                        Log.d(TAG, d.getString("categoria"));
                        Log.d(TAG, d.getString("categoria"));
                        Log.d(TAG, d.getString("id_usuario"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            /*if (pDialog.isShowing())
                pDialog.dismiss();*/
        }
    }
}

package sable.aqua.mars;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

// класс "учебная дисцилина"
class PredmetInfo
{
    private String prId = "";
    private String prName = null;
    private String prLongName = null;

    public PredmetInfo(String _id, String _name, String _lname)
    {
        prId = _id;
        prName = _name;
        prLongName = _lname;
    }


    public String getPrName()
    {
        return prName;
    }

    public String getPrId()
    {
        return prId;
    }

    public String getPrLongName()
    {
        return prLongName;
    }
}

class PredmetAdapter extends RecyclerView.Adapter<PredmetAdapter.ViewHolder>
{
    public ArrayList<PredmetInfo> predmList = new ArrayList<>();
    public Context predmAdapterContext = null;

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public CardView pv;
        public TextView prName;
        public TextView prLongName;
        public ImageView prSign;
        public RelativeLayout predmCardLayout;

        public ViewHolder(View itemView)
        {
            super(itemView);
            pv = itemView.findViewById(R.id.pv);
            prName = itemView.findViewById(R.id.predmet_name);
            prLongName = itemView.findViewById(R.id.predmet_long_name);
            prSign = itemView.findViewById(R.id.predmSign);
            predmCardLayout = itemView.findViewById(R.id.predmCardLayout);
        }
    }

    public PredmetAdapter(ArrayList<PredmetInfo> _prList, Context cntx)
    {
        predmList = _prList;
        predmAdapterContext = cntx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType)
    {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.predmet_card_layout, parent,false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position)
    {
        holder.prName.setText(predmList.get(position).getPrName());
        holder.prLongName.setText(predmList.get(position).getPrLongName());
        holder.prSign.setImageResource(R.drawable.predm);

        holder.predmCardLayout.setOnClickListener((View)->{

            Intent intent = new Intent(predmAdapterContext, ZanActivity.class);
            intent.putExtra("prId",predmList.get(position).getPrId());
            intent.putExtra("prName",predmList.get(position).getPrName());
            predmAdapterContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return predmList.size();
    }
}


// -------------------------------------------------


public class PredmActivity extends Activity {

    String grId = "";
    String grName = "";

    private RecyclerView predmView;
    private RecyclerView.Adapter prAdapter;
    private RecyclerView.LayoutManager prManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predm);

        if (getIntent().hasExtra("grId"))
        {
            grId = Integer.toString(getIntent().getIntExtra("grId",12));
            grName = getIntent().getStringExtra("grName");
            getActionBar().setTitle(grName.toUpperCase() + ": предметы");
        }

        predmView = findViewById(R.id.predmView);
        predmView.setHasFixedSize(true);

        prManager = new LinearLayoutManager(this);
        predmView.setLayoutManager(prManager);

        // список предметов
        final ArrayList<PredmetInfo> prList = new ArrayList<>();

        // класс для выполнения загрузки и парсинга сведений о группах
        // в отдельном потоке
        class HttpXMLGetter extends AsyncTask<String, Void, StringBuilder>
        {
            public boolean parsingResult = true;
            public StringBuilder xmlResult = new StringBuilder();

            @Override
            protected StringBuilder doInBackground(String ... address)
            {
                StringBuilder xmlRes = new StringBuilder();
                URL url = null;
                try
                {
                    url = new URL(address[0]);
                }
                catch (MalformedURLException e)
                {
                    parsingResult = false;
                }

                HttpURLConnection urlConnection = null;
                try
                {
                    urlConnection = (HttpURLConnection) url.openConnection();
                }
                catch (IOException e)
                {
                    parsingResult = false;
                }

                try
                {
                    urlConnection.setRequestMethod("GET");
                }
                catch (ProtocolException e)
                {
                    parsingResult = false;
                }
                urlConnection.setRequestProperty("Accept","application/xml");

                try
                {
                    int r = urlConnection.getResponseCode();

                    BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bin = new BufferedReader(new InputStreamReader(in));
                    String inLine = "";

                    while((inLine=bin.readLine())!=null)
                    {
                        xmlRes.append(inLine);
                    }
                }
                catch(IOException ioex)
                {
                    parsingResult = false;
                }
                finally
                {
                    urlConnection.disconnect();
                }

                return xmlRes;
            }


            @Override
            protected void onPostExecute(StringBuilder result)
            {
                // в result - строка с загруженным XML-документом

                if (!parsingResult) return;

                XmlPullParserFactory factory = null;
                try
                {
                    factory = XmlPullParserFactory.newInstance();
                }
                catch (XmlPullParserException xppe)
                {
                    parsingResult = false;
                    return;
                }

                factory.setNamespaceAware(true);
                try {

                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(new StringReader(result.toString()));

                    int eventType = 0;

                    try {
                        eventType = xpp.nextTag();
                    }
                    catch(Exception ioe)
                    {
                        parsingResult = false;
                        return;
                    }

                    while(eventType!=XmlPullParser.END_DOCUMENT)
                    {
                        if (xpp.getName().equals("predmet"))
                        {
                            try
                            {
                                xpp.nextTag();
                                String prId = xpp.getAttributeValue(0);
                                String prName = xpp.getAttributeValue(1);
                                String prLongName = xpp.getAttributeValue(2);
                                prList.add(new PredmetInfo(prId, prName, prLongName));
                            }
                            catch(IOException ioe)
                            {
                                parsingResult = false;
                                return;
                            }
                        }

                        try {
                            eventType = xpp.nextTag();
                        }
                        catch(IOException ioe)
                        {
                            parsingResult = false;
                            return;
                        }
                    }
                }
                catch (XmlPullParserException e)
                {
                    //xmlText.append(e.getLocalizedMessage());
                    parsingResult = false;
                    //return;
                }

                prAdapter = new PredmetAdapter(prList, getApplicationContext());
                predmView.setAdapter(prAdapter);

            }
        }

        // выполнить загрузку и вывод сведений о группе в окно вывода
        // в отдельном потоке исполнения
        HttpXMLGetter hxg = new HttpXMLGetter();
        hxg.execute(String.format(
                "http://www.sakhiepi.ru/mobile/zhurnal/build_predmet_listMRS.aspx?gr_id=%s",
                grId));
        hxg = null;
    }
}

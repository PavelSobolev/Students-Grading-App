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
class ZadanieInfo
{
    private String zadId = "";
    private String zadName = null;
    private String zadBall = null;

    public ZadanieInfo(String _id, String _zname, String _ball)
    {
        zadId = _id;
        zadName = _zname;
        zadBall = _ball;
    }

    public String getZadName()
    {
        return zadName;
    }

    public String getZadId()
    {
        return zadId;
    }

    public String getZadBall()
    {
        return zadBall;
    }
}


class ZadanieAdapter extends RecyclerView.Adapter<ZadanieAdapter.ViewHolder>
{
    public ArrayList<ZadanieInfo> zadList = new ArrayList<>();
    public Context zadAdapterContext = null;

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public CardView zav;
        public TextView zadName;
        public TextView zadBall;
        public ImageView zadSign;
        public RelativeLayout zadCardLayout;

        public ViewHolder(View itemView)
        {
            super(itemView);
            zav = itemView.findViewById(R.id.zav);
            zadName = itemView.findViewById(R.id.zad_name);
            zadBall = itemView.findViewById(R.id.zad_ball);
            zadSign = itemView.findViewById(R.id.zadSign);
            zadCardLayout = itemView.findViewById(R.id.zadanCardLayout);
        }
    }

    public ZadanieAdapter(ArrayList<ZadanieInfo> _zadList, Context cntx)
    {
        zadList = _zadList;
        zadAdapterContext = cntx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType)
    {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zadanie_card_layout, parent,false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position)
    {
        holder.zadName.setText(zadList.get(position).getZadName());
        holder.zadBall.setText("(кол-во баллов: " + zadList.get(position).getZadBall() + ")");
        holder.zadSign.setImageResource(R.drawable.task2);

        holder.zadCardLayout.setOnClickListener((View)->{

            Intent intent = new Intent(zadAdapterContext,OtmActivity.class);
            intent.putExtra("zadId",zadList.get(position).getZadId());
            intent.putExtra("zadName",zadList.get(position).getZadName());
            intent.putExtra("zadBall", zadList.get(position).getZadBall());
            zadAdapterContext.startActivity(intent);
        });
    }

    /*@Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }*/

    @Override
    public int getItemCount() {
        return zadList.size();
    }
}


// -------------------------------------------------

public class ZadanieActivity extends Activity {

    String zanId = "";
    String zanName = "";
    private RecyclerView zadView;
    private RecyclerView.Adapter zadAdapter;
    private RecyclerView.LayoutManager zadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zadanie);

        if (getIntent().hasExtra("zanId"))
        {
            zanId = getIntent().getStringExtra("zanId");
            //getIntent().getStringExtra("pdId");
            //Integer.toString(getIntent().getIntExtra("prId",12));
            zanName = getIntent().getStringExtra("zanName");

        }
        else {
            zanId = "0";
            zanName = "--";
        }

        getActionBar().setTitle(zanName + ": задания");

        zadView = findViewById(R.id.zadView);
        zadView.setHasFixedSize(true);

        zadManager = new LinearLayoutManager(getApplicationContext());
        zadView.setLayoutManager(zadManager);

        // список предметов
        final ArrayList<ZadanieInfo> zadList = new ArrayList<>();

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

            /*@Override
            protected void onProgressUpdate(Integer ... progress)
            {

            }*/

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
                        if (xpp.getName().equals("zadan"))
                        {
                            try // <zadan zid="3078" zname="Посещение занятия" ztxt="" zball="0,5" />
                            {
                                xpp.nextTag();
                                String zadId = xpp.getAttributeValue(0);
                                String zadName = xpp.getAttributeValue(1);
                                String zadBall = xpp.getAttributeValue(3).replace(",",".");
                                zadList.add(new ZadanieInfo(zadId, zadName, zadBall));

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

                zadAdapter = new ZadanieAdapter(zadList, getApplicationContext());
                zadView.setAdapter(zadAdapter);

            }
        }

        // выполнить загрузку и вывод сведений о группе в окно вывода
        // в отдельном потоке исполнения
        HttpXMLGetter hxg = new HttpXMLGetter();
        hxg.execute(String.format(
                "http://www.sakhiepi.ru/mobile/zhurnal/build_zadan_listMRS.aspx?zad_id=%s",
                zanId));
        //boolean grGetResult = hxg.parsingResult;
        hxg = null;

    }
}

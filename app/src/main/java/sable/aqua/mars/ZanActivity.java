package sable.aqua.mars;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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
import android.content.Intent;

// класс "учебная дисцилина"
class ZanyatInfo
{
    private String zanId = "";
    private String zanName = null;
    private String zanLongName = null;

    public ZanyatInfo(String _id, String _name, String _lname)
    {
        zanId = _id;
        zanName = _name;
        zanLongName = _lname;
    }

    public String getZanName()
    {
        return zanName;
    }

    public String getZanId()
    {
        return zanId;
    }

    public String getZanLongName()
    {
        return zanLongName;
    }
}

class ZanyatAdapter extends RecyclerView.Adapter<ZanyatAdapter.ViewHolder>
{
    public ArrayList<ZanyatInfo> zanList = new ArrayList<>();
    public Context zanAdapterContext = null;

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public CardView zv;
        public TextView zanName;
        public TextView zanLongName;
        public ImageView zanSign;
        public RelativeLayout zanCardLayout;

        public ViewHolder(View itemView)
        {
            super(itemView);
            zv = itemView.findViewById(R.id.zv);
            zanName = itemView.findViewById(R.id.zan_name);
            zanLongName = itemView.findViewById(R.id.zan_long_name);
            zanSign = itemView.findViewById(R.id.zanSign);
            zanCardLayout = itemView.findViewById(R.id.zanCardLayout);
        }
    }

    public ZanyatAdapter(ArrayList<ZanyatInfo> _zanList, Context cntx)
    {
        zanList = _zanList;
        zanAdapterContext = cntx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType)
    {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zanyat_card_layout, parent,false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position)
    {
        holder.zanName.setText(zanList.get(position).getZanName());
        holder.zanLongName.setText(zanList.get(position).getZanLongName());
        holder.zanSign.setImageResource(R.drawable.zan_two);

        holder.zanCardLayout.setOnClickListener((View)->{
            Intent intent = new Intent(zanAdapterContext,ZadanieActivity.class);
            intent.putExtra("zanId",zanList.get(position).getZanId());
            intent.putExtra("zanName",zanList.get(position).getZanName());
            zanAdapterContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return zanList.size();
    }
}


// -------------------------------------------------


public class ZanActivity extends Activity {

    String predmId = "";
    String predmName = "";
    private RecyclerView zanView;
    private RecyclerView.Adapter zanAdapter;
    private RecyclerView.LayoutManager zanManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zan);


        if (getIntent().hasExtra("prId"))
        {
            predmId = getIntent().getStringExtra("prId");
                    //getIntent().getStringExtra("pdId");
                    //Integer.toString(getIntent().getIntExtra("prId",12));
            predmName = getIntent().getStringExtra("prName");

        }
        else {
            predmId = "0";
            predmName = "--";
        }

        getActionBar().setTitle(predmName + ": занятия");

        zanView = findViewById(R.id.zanView);
        zanView.setHasFixedSize(true);

        zanManager = new LinearLayoutManager(getApplicationContext());
        zanView.setLayoutManager(zanManager);

        // список предметов
        final ArrayList<ZanyatInfo> zanList = new ArrayList<>();

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
                        if (xpp.getName().equals("plan"))
                        {
                            try
                            {
                                xpp.nextTag();
                                String zanId = xpp.getAttributeValue(0);
                                String zanName = xpp.getAttributeValue(1);
                                String zanLongName = xpp.getAttributeValue(2);
                                zanList.add(new ZanyatInfo(zanId, zanName, zanLongName));

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
                    parsingResult = false;
                    //return;
                }

                zanAdapter = new ZanyatAdapter(zanList, getApplicationContext());
                zanView.setAdapter(zanAdapter);

            }
        }

        // выполнить загрузку и вывод сведений о группе в окно вывода
        // в отдельном потоке исполнения
        HttpXMLGetter hxg = new HttpXMLGetter();
        hxg.execute(String.format(
                "http://www.sakhiepi.ru/mobile/zhurnal/build_predmet_plan.aspx?pr_id=%s",
                predmId));
        hxg = null;

    }
}

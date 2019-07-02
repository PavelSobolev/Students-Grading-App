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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import android.util.Log;

import android.app.AlertDialog;

// класс "группа"
class GroupInfo
{
    private int grId = 0;
    private String grName = null;

    public GroupInfo(String _id, String _name)
    {
        grId = Integer.parseInt(_id);
        grName = _name;
    }

    public int getGrId()
    {
        return grId;
    }

    public String getGrName()
    {
        return grName;
    }

    @Override
    public String toString()
    {
        return String.format("Ид группы: %d, название группы: %s", getGrId(), getGrName());
    }

    public static String getString(String _id, String _name)
    {
        return String.format("Ид: %s, название: %s", _id, _name);
    }
}


class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder>
{
    public ArrayList<GroupInfo> grList = new ArrayList<>();
    public Context grAdapterContext = null;

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public CardView cv;
        public TextView grName;
        public TextView grId;
        public ImageView grSign;
        public RelativeLayout cardLayout;

        public ViewHolder(View itemView)
        {
            super(itemView);
            cv = itemView.findViewById(R.id.cv);
            grName = itemView.findViewById(R.id.group_name);
            grId = itemView.findViewById(R.id.group_id);
            grSign = itemView.findViewById(R.id.sign);
            cardLayout = itemView.findViewById(R.id.cardLayout);
        }
    }

    public GroupAdapter(ArrayList<GroupInfo> _grList, Context cntx)
    {
        grList = _grList;
        grAdapterContext = cntx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType)
    {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_card_layout, parent,false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position)
    {
        holder.grName.setText(grList.get(position).getGrName());
        holder.grId.setText(String.format("%d", grList.get(position).getGrId()));
        holder.grSign.setImageResource(R.drawable.group_two);


            holder.cardLayout.setOnClickListener((View)->{
                Intent intent = new Intent(grAdapterContext,PredmActivity.class);
                intent.putExtra("grId",grList.get(position).getGrId());
                intent.putExtra("grName",grList.get(position).getGrName());
                grAdapterContext.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return grList.size();
    }
}


// -------------------------------------------------

public class GroupActivity extends Activity {


    private RecyclerView groupView;
    private RecyclerView.Adapter grAdapter;
    private RecyclerView.LayoutManager grManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        getActionBar().setTitle("Список групп");

        groupView = findViewById(R.id.groupView);
        groupView.setHasFixedSize(true);

        grManager = new LinearLayoutManager(this);
        groupView.setLayoutManager(grManager);


        // вывод данных в текстовый редактор
        //xmlText = findViewById(R.id.xmlText);
        // список групп
        final ArrayList<GroupInfo> grList = new ArrayList<>();

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
                        if (xpp.getName().equals("group"))
                        {
                            try
                            {
                                xpp.nextTag();
                                String grId = xpp.getAttributeValue(0);
                                String grName = xpp.getAttributeValue(1);
                                grList.add(new GroupInfo(grId, grName));
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


                grAdapter = new GroupAdapter(grList,getApplicationContext());
                groupView.setAdapter(grAdapter);

            }
        }

        // выполнить загрузку и вывод сведений о группе в окно вывода
        // в отдельном потоке исполнения
        HttpXMLGetter hxg = new HttpXMLGetter();
        hxg.execute("http://www.sakhiepi.ru/mobile/zhurnal/build_grup_listMRS.aspx");
        boolean grGetResult = hxg.parsingResult;
        hxg = null;
    }
}



// =============================================================================
/*Toast.makeText(grAdapterContext,
                    grList.get(position).toString(),
                    Toast.LENGTH_LONG).show();*/

        /*AlertDialog.Builder builder = new AlertDialog.Builder(grAdapterContext);
        builder.setMessage(grList.get(position).toString())
                    .setTitle("Info");

        AlertDialog dialog = builder.create();

            /*Snackbar.make(holder, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();*/
// =================================================================================
package sable.aqua.mars;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;


// класс "информация о выполнении задания"
class VipolnenieInfo
{
    private String vipId = "";
    private String studentName = null;
    private String isVip = null;
    private String vipDate = null;

    public VipolnenieInfo(String _id, String _stname, String _isvip, String _vipDate)
    {
        vipId = _id;
        studentName = _stname;
        isVip = _isvip;
        vipDate = _vipDate;
    }


    public String getStudentName()
    {
        return studentName;
    }

    public double getVip()
    {
        return Double.parseDouble(isVip);
    }

    public String getIsVip()
    {
        if (isVip.compareTo("0")!=0)
            return "1";
        else
            return "0";
    }

    public String getVipId()
    {
        return vipId;
    }

    public String getVipDate()
    {
        return vipDate;
    }
}


class VipolnenieAdapter extends RecyclerView.Adapter<VipolnenieAdapter.ViewHolder> {
    public ArrayList<VipolnenieInfo> vipList = new ArrayList<>();
    public Context vipAdapterContext = null;
    public double zadBall = 0.0;
    public String zadName = "";

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public CardView ov;
        public TextView stud_vip;
        public Button vipBtn;
        public EditText vipEdit;
        public RelativeLayout vipCardLayout;

        public ViewHolder(View itemView)
        {
            super(itemView);
            ov = itemView.findViewById(R.id.ov);
            stud_vip = itemView.findViewById(R.id.studentNameMark);
            vipBtn = itemView.findViewById(R.id.studentBtn);
            vipCardLayout = itemView.findViewById(R.id.otmCardLayout);
            vipEdit = itemView.findViewById(R.id.editMark);

        }
    }

    public VipolnenieAdapter(ArrayList<VipolnenieInfo> _vipList, Context cntx, double _ball, String _zadname)
    {
        vipList = _vipList;
        vipAdapterContext = cntx;
        zadBall = _ball;
        zadName = _zadname;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType)
    {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.otmetka_card_layout, parent,false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position)
    {
        double resBall = vipList.get(position).getVip() * zadBall;

        holder.stud_vip.setText(
                String.format("%s [%.1f]\n%s", vipList.get(position).getStudentName(), resBall,
                        vipList.get(position).getVipDate()));
        //holder.vipBtn.setImageResource(R.drawable.flag);
        final String Id = vipList.get(position).getVipId();
        holder.vipEdit.setText(String.format("%.1f",vipList.get(position).getVip()));

        holder.vipCardLayout.setOnClickListener((View)->{
        });

        holder.vipBtn.setOnClickListener(V->{
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
                }
            }

            double newBall = Double.parseDouble(holder.vipEdit.getText().toString());

            String Query = String.format(
                    "update student_zadanie set vipolnenie = %f, data_otm = getdate() where id=%s", newBall, Id);

            // выполнить загрузку и вывод сведений о группе в окно вывода
            // в отдельном потоке исполнения
            HttpXMLGetter hxg = new HttpXMLGetter();
            hxg.execute(String.format(
                    "http://www.sakhiepi.ru/mobile/zhurnal/save_student_zadan_balls.aspx?sql=%s",
                    Query));
            hxg = null;

            double resBall2 = newBall * zadBall;
            Date d = new Date();
            //DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            String outStr = String.format("%s [%.1f]\n>>%s<<", vipList.get(position).getStudentName(), resBall2,
                    sdfDate.format(d));
            holder.stud_vip.setText(outStr);

        });

    }


    //@Override
    public int getItemCount() {
        return vipList.size();
    }
}

// -------------------------------------------------

public class OtmActivity extends Activity {

    String zadId = "0";
    String zadName = "-";
    String zadBall = "0";
    private RecyclerView otmView;
    private RecyclerView.Adapter otmAdapter;
    private RecyclerView.LayoutManager otmManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otm);

        if (getIntent().hasExtra("zadId"))
        {
            zadId = getIntent().getStringExtra("zadId");
            zadName = getIntent().getStringExtra("zadName");
            zadBall = getIntent().getStringExtra("zadBall");
        }

        if (getActionBar()!=null)
        getActionBar().setTitle(
                ((zadName.length()>7)?zadName.substring(0,5)+ "..." : zadName)
                        + " [" + zadBall + "]: выполнение");

        otmView = findViewById(R.id.otmView);
        otmView.setHasFixedSize(true);

        otmManager = new LinearLayoutManager(getApplicationContext());
        otmView.setLayoutManager(otmManager);

        // список предметов
        final ArrayList<VipolnenieInfo> vipList = new ArrayList<>();

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
                        // build_student_zadan_list.aspx?zad_id=3120
                        // <studzadan fio="Боровиков М. Д." stid="6479" studzadid="36190" ball="0" data_otm="" />

                        if (xpp.getName().equals("studzadan"))
                        {
                            try
                            {
                                xpp.nextTag();
                                String vipId = xpp.getAttributeValue(2);
                                String studName = xpp.getAttributeValue(0);
                                String isVip = xpp.getAttributeValue(3).replace(",",".");
                                String vipDate = xpp.getAttributeValue(4);
                                vipList.add(new VipolnenieInfo(vipId, studName, isVip, vipDate));
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

                otmAdapter = new VipolnenieAdapter(vipList, getApplicationContext(),
                        Double.parseDouble(zadBall), zadName);
                otmView.setAdapter(otmAdapter);

            }
        }

        // выполнить загрузку и вывод сведений о группе в окно вывода
        // в отдельном потоке исполнения
        HttpXMLGetter hxg = new HttpXMLGetter();
        hxg.execute(String.format(
                "http://www.sakhiepi.ru/mobile/zhurnal/build_student_zadan_list.aspx?zad_id=%s",
                zadId));
        //boolean grGetResult = hxg.parsingResult;
        hxg = null;


    }
}

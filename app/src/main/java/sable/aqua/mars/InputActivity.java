package sable.aqua.mars;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class InputActivity extends Activity
{

    Button B1;
    Button B2;
    SeekBar Ball;
    TextView BallTxt;

    public double ballFromOtm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        B1 = findViewById(R.id.byes);
        B2 = findViewById(R.id.bno);
        Ball = findViewById(R.id.ball);
        BallTxt = findViewById(R.id.ballText);

        B1.setOnClickListener(V->{
            super.onBackPressed();
        });

        B2.setOnClickListener(V->{
            //navigateUpToFromChild();
            super.onBackPressed();
        });

        Ball.setOnSeekBarChangeListener(seekBarChangeListener);
    }


    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            BallTxt.setText(String.format("%.2f", progress/100.));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
}

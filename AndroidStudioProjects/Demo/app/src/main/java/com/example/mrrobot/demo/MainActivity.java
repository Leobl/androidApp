package com.example.mrrobot.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBaseHC4;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button camera_button;
    ImageView imageView;
    TextView result_tv;
    Bitmap bitmap;
    File myFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera_button= (Button)findViewById(R.id.camera_button_id);
        imageView=(ImageView)findViewById(R.id.image_id);
        result_tv=(TextView)findViewById(R.id.resultTV_id);

        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bitmap=(Bitmap)data.getExtras().get("data");

        try{
            bitmap.compress(Bitmap.CompressFormat.JPEG,90,new FileOutputStream(
                    myFile=new File(getFilesDir(), "temp.jpg")
            ));
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        imageView.setImageBitmap(bitmap);
        callAPI();
    }


    public void callAPI(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String myUrl = "https://southcentralus.api.cognitive.microsoft.com/customvision/v1.1/Prediction/e7d62f14-afb6-4d22-8e3c-746ee6818dce/image?";
                HttpClient httpClient= HttpClients.createDefault();
                try{
                    URIBuilder builder = new URIBuilder(myUrl);
                    URI uri=builder.build();

                    HttpPost request = new HttpPost(uri);

                    request.setHeader("Content-Type", "application/json");
                    request.setHeader("Prediction-Key", "be3da62f10e2470ba88aee2b1811ee5c");

                    int size = (int)myFile.length();
                    byte[] bytes= new byte[size];
                    try{
                        BufferedInputStream buf = new BufferedInputStream(
                                new FileInputStream(myFile)
                        );
                        buf.read(bytes, 0,bytes.length);
                        buf.close();

                    }catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }

                    request.setEntity( new ByteArrayEntity(bytes));

                    HttpResponse response = httpClient.execute(request);
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        String jsonString = EntityUtils.toString(entity);
                        JSONObject json = new JSONObject(jsonString);
                        System.out.println("REST Response: \n");

                        System.out.println(json.toString(2));
                        JSONArray pred = json.getJSONArray("Predictions");

                        ArrayList<Double> prob=new ArrayList<>();
                        int index=0;
                        prob.add(pred.getJSONObject(index).getDouble("Probability"));
                        for (int i=1; i<pred.length();i++) {
                            int j=i;
                            prob.add(pred.getJSONObject(i).getDouble("Probability"));
                            if(prob.get(j)>prob.get(j--)){
                                index=i;
                            }
                        }

                        final String tag = pred.getJSONObject(index).getString("Tag");
                        final Double probal=pred.getJSONObject(index).optDouble("Probability");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (probal<0.20) {
                                    result_tv.setText("It is not flower");
                                }
                                else
                                    result_tv.setText("It is a " + tag + "with probability" + probal);
                            }
                        });

                    }

                }catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
}

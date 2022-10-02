package com.example.museum;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.museum.ml.SavedModel;

import org.tensorflow.TensorFlow;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.TensorFlowLite;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button camera,Settings,Home;
    private ImageView picture,vol;
    Interpreter tflite;
    int imageSize = 224;
    TextView arttitle;
    EditText descr;
    ImageClassifier imageClassifier;
    TextToSpeech textToSpeech;
    int count=0;
//

    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.camera_btn);
        Settings = findViewById(R.id.settings);
        Home = findViewById(R.id.homebtn);
        picture = findViewById(R.id.pictured);
        vol = findViewById(R.id.vol);
        arttitle = findViewById(R.id.arttitle);
        descr = findViewById(R.id.descr);

        try {
            imageClassifier = new ImageClassifier(this);
        } catch (IOException e) {
           Log.e("Image Classifier Errror","Error: "+e);
        }

       //text to speech
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status ==TextToSpeech.SUCCESS){
                    int lang= textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });
        Bundle extras = getIntent().getExtras();
        int check=0;
        if (extras != null) {
            check = extras.getInt("checked");
        }
        if(check!=0){
            vol.setVisibility(View.VISIBLE);
        }

        vol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 count=1;

                if(count==1)
                {
                    speak();
                    count=0;
                }
                else
                {
                textToSpeech.shutdown();
                textToSpeech.stop();
                count++;
                }
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            }
        }

        camera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                    startforResult.launch(cameraIntent);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }


    });

//        Intent intent = new Intent();
//        Bitmap bmp =intent.getParcelableExtra("bmp_Image");
//        picture.setImageBitmap(bmp);

    }

    @Override
    protected void onDestroy() {
        if(textToSpeech!=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void classifyImage(Bitmap image){
        try {
            SavedModel model = SavedModel.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
            int pixel=0;
            //iterate over each pixel and extract RGB values and add those values to byte buffer
            for(int i=0; i < imageSize; i++){
                for(int j=0; j < imageSize; j++){
                    int val = intValues[pixel++]; //RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f/255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f/255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f/255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            SavedModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            //find the index of the class with biggest confidence
            int maxPos=0;
            float maxConfidence = 0;
            for(int i=0;i<confidences.length;i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence=confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Basin", "Bezique", "Commode", "Two Heads"};
            arttitle.setText(classes[maxPos]);
            setFields(arttitle.getText().toString());

            // Releases model resources if no longer used.
//            String s = "";
//            for(int i = 0; i < classes.length; i++){
//                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
//            }
//            //set textview box with words
//            confidences.setText(s);
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK && data!=null) {

            Bitmap image =data.getParcelableExtra("data");
            int dimension = Math.min(image.getWidth(),image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image,dimension,dimension);
            picture.setImageBitmap(image);

            image=Bitmap.createScaledBitmap(image,imageSize,imageSize,true);
          classifyImage(image);


        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    public void Settings(View view) {
        Settings = findViewById(R.id.settings);
        Settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
            }
        });
    }

    public void speak(){
        String s = descr.getText().toString();
        String d = arttitle.getText().toString();
        textToSpeech.speak(d,TextToSpeech.QUEUE_FLUSH,null);
        textToSpeech.speak(s,TextToSpeech.QUEUE_FLUSH,null);
    }

    public void setFields(String artText){
        switch (artText){
            case "Basin":
                descr.setText("Title : Basin inscribed with the name of Bonifilius \n"+" This ablution basin was decorated by the sculptor Bonifilius with various animals of ambivalent symbolic significance: lions, as a symbol of power, were sometimes positive and sometimes negative in the Middle Ages, as were dragons. The porcupine, however, was a symbol of avarice for a long time before coming to represent military valour. Inside the basin is a salamander, a creature that could extinguish any flame. So the decoration evokes the struggle between good and evil, echoing water’s role of purifying the faithful before prayer.");

                break;
            case "Bezique":

                descr.setText("Title: The Bezique game \n"+" An active member of the Impressionist movement and participant in its exhibitions, Gustave Caillebotte (1848−1894) also collected works by his friends, many of which he then left to public collections in France. While the game of cards is a common subject of Flemish genre painting, Caillebotte endows it here with the monumental dimension of a history painting while deliberately embedding it in the modernity of his time. The Bezique Game was first shown at the seventh Impressionist exhibition in 1882 and appeared first in the catalogue. Continuing his depictions of fashionable pastimes of the period like rowing, which he practised himself, Caillebotte painted his friends in the setting of the luxurious apartment on Boulevard Haussmann that he shared with his brother, the composer Martial Caillebotte, who is shown on the right smoking a pipe.");
                break;
            case "Commode":

                descr.setText("Title: Commode Decorated with Red Lacquer from China \n"+" This chest of drawers is the work of Bernard II van Risenburgh (1696–1766), one of the great cabinetmakers active in Paris during the reign of Louis XV. Born into a family of craftsmen of Dutch origin, he was the first to embellish his pieces of furniture with panels of Chinese lacquer set off by frames of gilt bronze. He produced five similar items around 1750 but only two with genuine lacquer panels, the three others being imitations made with the cheaper local product called vernis Martin.");
                break;
            case "Two Heads":
                descr.setText("This bust, with its two fascinating faces, is one of the oldest monumental statues in the history of humanity. Whether the pair of figures represents two ancestors or divinities, the statue reflects the beliefs that were held in the Neolithic village of Ain Ghazal.");
                break;
            default:
                descr.setText(" sorry we have Basin, Bezique, Commode and Two Heads images ");
                break;
        }
    }
}
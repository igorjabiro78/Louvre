package com.example.museum;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class ArabicMainActivity extends AppCompatActivity {

    Button camera,Settings,Home;
    private ImageView picture;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;
    int imageSize = 224;
    TextView arttitle;
    EditText descr;
    private ImageView vol;
    ImageClassifier imageClassifier;
    TextToSpeech textToSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arabic_main);


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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        int lang= textToSpeech.setLanguage(Locale.forLanguageTag("ar"));
                    }
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
                speak();
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
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            Bitmap image  =data.getParcelableExtra("data");
            int dimension = Math.min(image.getWidth(),image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image,dimension,dimension);
            picture.setImageBitmap(image );

            image=Bitmap.createScaledBitmap(image,imageSize,imageSize,true);
            classifyImage(image);


        }
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

        textToSpeech.speak(s,TextToSpeech.QUEUE_FLUSH,null);
    }

    public void setFields(String artText){
        switch (artText){
            case "Basin":
//                descr.setText("The strange and the marvelous In the Middle Ages, the term \"marvelous\" was used to refer to the mysteries of the spiritual world in both Eastern and Western culture. Divine miracles combined with ancient legends to inspire the creation of all kinds of \"marvelous\" beings, an entire universe of fabulous creatures - some heavenly, some earthly, such as griffons, sphinxes, genies, and fairies - which symbolized and embodied supernatural power. Elaborate images of these \"marvels\" began to appear in books and in the decorative arts of the educated and cultured elite of the time. ");
                descr.setText("العنوان : حوض نقش عليه اسم بونيفيليوس - قام النحات بونيفيليوس بزخرفة هذا الحوض الخاص بالوضوء بالعديد من الحيوانات ذات الأهمية الرمزية المتناقضة: فالأسود، التي هي رمز القوة، كانت في العصور الوسطى تُعتبر أحياناً خيًرة وأحياناً أخرى مؤذية، وكذلك التنانين. أما النيص فكان رمزاً للجشع لمدةٍ طويلة قبل أن يصبح رمزاً للشجاعة العسكرية. وداخل الحوض هناك سمندل قادر على التغلب على كل ألسنة اللهب. ترمز النقوش من ثم إلى الصراع ما بين الخير والشر محاكيةً دور الماء في تطهير المؤمنين قبل الصلاة.");
                break;
            case "Bezique":

                descr.setText("العنوان : لعبة الورق - كان غوستاف كايبوت (1848-1894) عضوا ناشطا في الحركة الانطباعية - وأحد المشاركين في معارضها الفنية. وكان أيضا يهوى جمع الأعمال الفنية الخاصة بأصدقائه. وقد كان له الفضل في دخول عدد من هذه الأعمال الفنّية ضمن المجموعات الفنية الحكومية في فرنسا. وكان موضوع لاعبي الورق من الموضوعات الشائعة التي عالجها في لوحاته الفلمنكية، بوصفها مشاهد تمثّل هذا النوع الفني. وقد أضفى كايبوت على هذه اللوحة الطابعَ الأثري للرسم التاريخي عامدا إلى إرسائها ضمن حداثة عصره. عُرضت لوحة \"لعبة البيزيغ\" للمرّة الأولى في معرض الفنّ الانطباعي السابع عام 1882 وظهرت أوّلاً في كتالوج اللوحات الفنية. رسم غوستاف كايبوت في بقية لوحاته التي تتناول أنواع الترفيه، مثل رياضة التجديف التي كان يمارسها بنفسه، أصدقاءه في مشهد داخلي تدور أحداثه في الشقّة المريحة التي يسكنها مع أخيه في شارع هوسمان، إذ يظهر أخوه - المؤلف الموسيقي مارسيال كايبوت - على يمين الصورة وهو يدخن الغليون، ويلعب لعبة البيزيغ – وهي لعبة تتفرّع عن لعبة البيلوت المشهورة - مع ثلاثة من أصدقائه الجالسين معه حول المنضدة، في حين يظهر شخص رابع في الصورة واقفا بالقرب منهم .");
                break;
            case "Commode":
                descr.setText("العنوان : خزانة مزخرفة بلكّ أحمر من الصين - هذه الخزانة الصغيرة هي من صُنْع النجَّار بيرنار فان رايزن بيرغ الثاني (نحو 1696-1766) الذي ينحدر من عائلة من الحرفيين من أصل هولندي، وهو من أمهر النجّارين الذين عملوا في باريس في عهد لويس الخامس عشر. فقد كان أوَّل من استعمل ألواحًا من اللك الصينيّ في تزيين قطعه من الأثاث وطَعَّمَها بإطارات من البرونز المُذهَّب. قام هذا النجَّار عام 1750 بصُنع خمس خزائن مشابهة، ولم يستخدم سوى في اثنين منها ألواح اللك الصينيّ الأصليّة، إذ استخدم في الخزائن الثلاثة الأخرى ألواحًا من \"برنيق مارتان\" المحلي الصنع والأرخص.");
                break;
            case "Two Heads":
                descr.setText("العنوان : تمثال ضخم ذو رأسين - يعتبر هذا التمثال النصفي ذو الوجهين من أقدم التماثيل الأثرية الضخمة في تاريخ البشرية. وسواء كان الوجهان يجسدان شخصيتان من الأسلاف أو الآلهة، فإن التمثال برمز إلى المعتقدات التي كانت منتشرة في قرية عين غزال في العصر الحجري الحديث.");
                break;
            default:
                descr.setText(" sorry we have Basin, Bezique, Commode and Two Heads images ");
                break;
        }
    }
}
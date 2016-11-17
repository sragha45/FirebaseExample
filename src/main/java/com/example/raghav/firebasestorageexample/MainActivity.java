package com.example.raghav.firebasestorageexample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    AppCompatButton uploadBtn;
    ImageView duckImage;
    ProgressBar progressBar;
    TextView urlTextView;
    Button downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uploadBtn = (AppCompatButton) findViewById(R.id.uploadbtn);
        duckImage = (ImageView)findViewById(R.id.imageview);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        urlTextView = (TextView) findViewById(R.id.urltextview);
        downloadButton = (Button) findViewById(R.id.downloadImage);


        progressBar.setVisibility(View.GONE);
        uploadBtn.setOnClickListener(new UploadBtn());
        downloadButton.setOnClickListener(new DownloadBtn());


    }
    class UploadBtn implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            //Convert the image (Rubber Duck) into a RAW byte Array

            duckImage.setDrawingCacheEnabled(true);                      //Recycling the Bitmap?? ==> Got that error resolved
            duckImage.buildDrawingCache();
            Bitmap bitmap = duckImage.getDrawingCache();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);        //While compression, setDrawingCacheEnabled must be set tot true
            byte []data = baos.toByteArray();
            duckImage.setDrawingCacheEnabled(false);                    //Reset it

            //In case of the FirebaseApp must be registered first error, resolve by
            // downloading the google-services.json file and modifying the
            // build.gradle files in the app  and the gradle levels


            FirebaseStorage storage = FirebaseStorage.getInstance();    //Get an instance of the FirebaseStorage

            String path = "rubberDucky/" + UUID.randomUUID() + ".png";  //Fix a random bullshit Unique ID to the image
            StorageReference rubberDuckyRef = storage.getReference(path);

            StorageMetadata metadata = new StorageMetadata.Builder()    //Store some metadata about the image or whatever it is
                    .setCustomMetadata("Text","Hi from the RubberDuck in the APP").build();

            //Display progress bar while uploading
            progressBar.setVisibility(View.VISIBLE);
            uploadBtn.setEnabled(false);

            //Kick off an UploadTask and monitor to show changes on UI

            UploadTask uploadTask = rubberDuckyRef.putBytes(data,metadata);
            uploadTask.addOnProgressListener(MainActivity.this, new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    Double progress = 100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
                    progressBar.setProgress(progress.intValue());
                }
            });
            uploadTask.addOnSuccessListener(MainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Done Uploading, Disable Progress Bar
                    progressBar.setVisibility(View.GONE);
                    uploadBtn.setEnabled(true);

                    //Fetch the URL of the image and display it.
                    Uri url = taskSnapshot.getDownloadUrl();
                    urlTextView.setClickable(true);
                    urlTextView.setMovementMethod(LinkMovementMethod.getInstance());
                    urlTextView.setText(url.toString());
                    urlTextView.setVisibility(View.VISIBLE);
                }
            });

        }
    }
    class DownloadBtn implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            //You can get references to files only via FirebaseStorage object

            FirebaseStorage storage = FirebaseStorage.getInstance();

            //The absolute and the relative paths
            StorageReference storageReference =  storage.
                    getReferenceFromUrl("gs://my-awesome-project-95336.appspot.com");
            StorageReference childRef = storageReference.child("rubberDucky/de066a3c-d3c2-46e8-b9d5-6805ec701286.png");

            //Glide is an API just like Picasso. It's been integrated with the Firebase.
            //You can use it download images and directly inject them into the ImageViews

            Glide.with(MainActivity.this).using(new FirebaseImageLoader()).load(childRef).into(duckImage);

            //This is to get data of any type that has been saved in the Firebase Storage.
            //It takes MAX Data downloadable from the source.
            //It stores them into the memory as it downloads, so you can make sure that the APP won't
            //crash in case of low memory issues

            childRef.getBytes(1024*1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    duckImage.setImageBitmap(bitmap);
                }
            });

        }
    }
}


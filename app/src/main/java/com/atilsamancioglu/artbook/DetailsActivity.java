package com.atilsamancioglu.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.atilsamancioglu.artbook.databinding.ActivityDetailsBinding;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DetailsActivity extends AppCompatActivity {

    Bitmap selectedImage;
    SQLiteDatabase database;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    private ActivityDetailsBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);



        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.matches("new")) {
            binding.artNameText.setText("");
            binding.painterNameText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);

//            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectimage);
//            binding.imageView.setImageBitmap(selectImage);
            //image risoruce direkte verilebilir hata vermedi
            binding.imageView.setImageResource(R.drawable.selectimage);


        } else {
            int artId = intent.getIntExtra("artId",1);
            binding.button.setVisibility(View.INVISIBLE);

            try {
                //new String[] {String.valueOf(artId) burda ? ile verilen verileri s??ralyla girilebilir insert into ta parametere tek tek girmi??tik bunun gibide denenebilir denemedim
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});


                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()) {

                    binding.artNameText.setText(cursor.getString(artNameIx));
                    binding.painterNameText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    //byte kodunu bitmapa ??evirdik en sonra atad??k
                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);


                }

                cursor.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void selectImage(View view) {
        //izin baz?? apilerde zorunlu k??ld?????? i??in
        //Manifestteki READ_EXTERNAL_STORAGE izni i??in onaylatmam??z laz??m // PERMISSION_GRANTED izin verilmi?? demektir //bu izin zaten uygulama kurulurken al??n??r
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //bu izni almam??z??n zorunlulu??u varm?? yani baz?? apilerde gerek yok baz??lar??nda zorunlu k??l??yolar //bu android ilkte bunu g??r??r ve kendi snak bara benzer sorgulamas??n?? ister verilmezzse Permisson needed! toast mesaj??n g??r??r kullan??c?? 2.istemede direk Snackbar ?? g??sterir butona t??klarsa galeriye gider yine evet demezse ise  Permisson needed!" toast mesaj?? verir ve snark bar?? mesaj?? altta hep vurur evet diuyene kadar
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //toast mesaja benzer fark?? kendi buton var en altta ????kar Snackbar.LENGTH_INDEFINITE onaylanmad?????? zaman s??resiz g??r??ncektir butona basana kadar yada ba??ka butona//k??sa ve uzunlar??da var
                //"Give Permission" buton ismidir
                //Snackbar.make(view,   buton i??inde de??ilse yani binding.getRoot() da denebilir
                Snackbar.make(view,"Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            //telefon galeriyi a??mas??n?? sa??lam???? oluruz
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }

    }
    //galeriye girildi??inde resim se??tiyse bu yap??lcak
    public void registerLauncher() {
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //se??ilirse
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            //se??ilen resmi almak
                            Intent intentFromResult = result.getData();
                            if (intentFromResult != null) {
                                //se??ilen resmin nerede kay??tl?? oldu??u bilgisi
                                Uri imageData = intentFromResult.getData();
                                try {
                                    //telefon versiyon (api) 28ve ??ss?? ise
                                    if (Build.VERSION.SDK_INT >= 28) {
                                        ImageDecoder.Source source = ImageDecoder.createSource(DetailsActivity.this.getContentResolver(),imageData);
                                        selectedImage = ImageDecoder.decodeBitmap(source);
                                        binding.imageView.setImageBitmap(selectedImage);

                                    } else {
                                        //bura 18 ve alt?? i??in izin istemez direk galeriye gider 19 20 ise tabi android kendi izinlerini ister en ba??larda
                                        selectedImage = MediaStore.Images.Media.getBitmap(DetailsActivity.this.getContentResolver(),imageData);
                                        binding.imageView.setImageBitmap(selectedImage);
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                });

        //izin verildiyse direk galeri a????labilirlilik olarak atamas??n?? yapacak
        permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        //onayl?? ise
                        if(result) {
                            //permission granted
                            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            activityResultLauncher.launch(intentToGallery);
                        } else {
                            //permission denied
                            Toast.makeText(DetailsActivity.this,"Permisson needed!",Toast.LENGTH_LONG).show();
                        }
                    }

                });
    }


    public void save(View view) {

        String artName = binding.artNameText.getText().toString();
        String painterName = binding.painterNameText.getText().toString();
        String year = binding.yearText.getText().toString();
        //resmi k??????kltm???? olduk tabi 300 deneyerekte sorun yoksa azaltabilirsiniz
        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        //veriye ??evirmemiz laz??m yani bayt(1lere0lara) lara ??evirmemiz laz??m
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //genelde png yada jpeg se??ilebilir
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            //CREATE TABLE IF NOT EXISTS ??unlar k??????k harf olmaz dikkat edelim b??y??k harf olmas?? ??art di??erlerininde
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES (?, ?, ?, ?)";
            //sorguyu bildirdik
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            //ilk soru i??areti i??in parametre girdik ...vs di??erleride ayn??
            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            //??al????t??rd??k
            sqLiteStatement.execute();


        } catch (Exception e) {

        }
        //bu alttaki olan aktiviteyi kapatmas?? demektir.
        //finish();
        Intent intent = new Intent(DetailsActivity.this,MainActivity.class);
        //bu olan aktivite de dahil olan t??m a????lm???? ama arkada olanlar?? tamamen temizlemek demek
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);



    }
    //1mb fazla resimler de hata verice??indne doly?? k??????lt??lcektir
    //resmi k??????kltme i??lemi ??a????r??rken yine tahminim b??y??lt??lcektir
    public Bitmap makeSmallerImage(Bitmap image, int maximumSize) {
        //resmin geni??li??ini al??r??z
        int width = image.getWidth();
        //resmin y??ksekli??ini al??r??z
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        //1den  b??y??kse yatay with 300 d??????n height 100 b??lersen 3 ????kar di??eride yatay
        if (bitmapRatio > 1) {
            //landscape image //yatay g??rsel
            //mesela  yukarda witdh 300 height ise 100 ise maximumSize ise 100 verildiyse width 100 olur height ise 33 sorun giderilmi?? olucakt??r
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {
            //portrait image //dikey g??rsel
            //mesela  yukarda witdh 100 height ise 300 ise maximumSize ise 100 verildiyse height 100 olur width ise 33 sorun giderilmi?? olucakt??r
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        //sonra bunu d??nd??r??cek zaten
        return Bitmap.createScaledBitmap(image,width,height,true);
    }

}

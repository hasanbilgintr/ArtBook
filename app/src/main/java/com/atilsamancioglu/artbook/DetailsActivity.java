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
                //new String[] {String.valueOf(artId) burda ? ile verilen verileri sıralyla girilebilir insert into ta parametere tek tek girmiştik bunun gibide denenebilir denemedim
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});


                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()) {

                    binding.artNameText.setText(cursor.getString(artNameIx));
                    binding.painterNameText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    //byte kodunu bitmapa çevirdik en sonra atadık
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
        //izin bazı apilerde zorunlu kıldığı için
        //Manifestteki READ_EXTERNAL_STORAGE izni için onaylatmamız lazım // PERMISSION_GRANTED izin verilmiş demektir //bu izin zaten uygulama kurulurken alınır
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //bu izni almamızın zorunluluğu varmı yani bazı apilerde gerek yok bazılarında zorunlu kılıyolar //bu android ilkte bunu görür ve kendi snak bara benzer sorgulamasını ister verilmezzse Permisson needed! toast mesajın görür kullanıcı 2.istemede direk Snackbar ı gösterir butona tıklarsa galeriye gider yine evet demezse ise  Permisson needed!" toast mesajı verir ve snark barı mesajı altta hep vurur evet diuyene kadar
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //toast mesaja benzer farkı kendi buton var en altta çıkar Snackbar.LENGTH_INDEFINITE onaylanmadığı zaman süresiz görüncektir butona basana kadar yada başka butona//kısa ve uzunlarıda var
                //"Give Permission" buton ismidir
                //Snackbar.make(view,   buton içinde değilse yani binding.getRoot() da denebilir
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
            //telefon galeriyi açmasını sağlamış oluruz
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }

    }
    //galeriye girildiğinde resim seçtiyse bu yapılcak
    public void registerLauncher() {
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //seçilirse
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            //seçilen resmi almak
                            Intent intentFromResult = result.getData();
                            if (intentFromResult != null) {
                                //seçilen resmin nerede kayıtlı olduğu bilgisi
                                Uri imageData = intentFromResult.getData();
                                try {
                                    //telefon versiyon (api) 28ve üssü ise
                                    if (Build.VERSION.SDK_INT >= 28) {
                                        ImageDecoder.Source source = ImageDecoder.createSource(DetailsActivity.this.getContentResolver(),imageData);
                                        selectedImage = ImageDecoder.decodeBitmap(source);
                                        binding.imageView.setImageBitmap(selectedImage);

                                    } else {
                                        //bura 18 ve altı için izin istemez direk galeriye gider 19 20 ise tabi android kendi izinlerini ister en başlarda
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

        //izin verildiyse direk galeri açılabilirlilik olarak atamasını yapacak
        permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        //onaylı ise
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
        //resmi küçükltmüş olduk tabi 300 deneyerekte sorun yoksa azaltabilirsiniz
        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        //veriye çevirmemiz lazım yani bayt(1lere0lara) lara çevirmemiz lazım
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //genelde png yada jpeg seçilebilir
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            //CREATE TABLE IF NOT EXISTS şunlar küçük harf olmaz dikkat edelim büyük harf olması şart diğerlerininde
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES (?, ?, ?, ?)";
            //sorguyu bildirdik
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            //ilk soru işareti için parametre girdik ...vs diğerleride aynı
            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            //çalıştırdık
            sqLiteStatement.execute();


        } catch (Exception e) {

        }
        //bu alttaki olan aktiviteyi kapatması demektir.
        //finish();
        Intent intent = new Intent(DetailsActivity.this,MainActivity.class);
        //bu olan aktivite de dahil olan tüm açılmış ama arkada olanları tamamen temizlemek demek
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);



    }
    //1mb fazla resimler de hata vericeğindne dolyı küçültülcektir
    //resmi küçükltme işlemi çağırırken yine tahminim büyültülcektir
    public Bitmap makeSmallerImage(Bitmap image, int maximumSize) {
        //resmin genişliğini alırız
        int width = image.getWidth();
        //resmin yüksekliğini alırız
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        //1den  büyükse yatay with 300 düşün height 100 bölersen 3 çıkar diğeride yatay
        if (bitmapRatio > 1) {
            //landscape image //yatay görsel
            //mesela  yukarda witdh 300 height ise 100 ise maximumSize ise 100 verildiyse width 100 olur height ise 33 sorun giderilmiş olucaktır
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {
            //portrait image //dikey görsel
            //mesela  yukarda witdh 100 height ise 300 ise maximumSize ise 100 verildiyse height 100 olur width ise 33 sorun giderilmiş olucaktır
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        //sonra bunu döndürücek zaten
        return Bitmap.createScaledBitmap(image,width,height,true);
    }

}

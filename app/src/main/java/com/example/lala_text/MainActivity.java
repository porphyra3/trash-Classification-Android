package com.example.lala_text;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author 14548
 */
public class MainActivity extends AppCompatActivity {
    /**
     * 展示图片
     */
    ImageView imageView;
    /**
     * 拍照获取图片按钮
     */
    private Button btn;
    /**
     * 上传图片到服务器识别按钮
     */
    private Button upload;

    /**
     * 识别结果
     */
    private TextView classifyResult;
    private Bitmap bitmapImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn);
        upload = findViewById(R.id.upload);
        classifyResult = findViewById(R.id.result);

        imageView=findViewById(R.id.imageView);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //申请权限部分
                String write=Manifest.permission.WRITE_EXTERNAL_STORAGE;
                String read=Manifest.permission.READ_EXTERNAL_STORAGE;

                final String[] WriteReadPermission = new String[] {write, read};

                int checkWrite= ContextCompat.checkSelfPermission(MainActivity.this,write);
                int checkRead= ContextCompat.checkSelfPermission(MainActivity.this,read);
                int ok=PackageManager.PERMISSION_GRANTED;

                if (checkWrite!= ok && checkRead!=ok){
                    //申请权限，读和写都申请一下，不然容易出问题
                    ActivityCompat.requestPermissions(MainActivity.this,WriteReadPermission,1);
                }else {
                    openAlbum();
                }
            }
        });
        initViews();
    }
    void openAlbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        //把所有照片显示出来
        intent.setType("image/*");
        startActivityForResult(intent,0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==1&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
        { openAlbum();}
        else {
            Toast.makeText(this, "你拒绝了打开相册的权限", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //当选择完相片，就会回到这里，然后相片的相关信息会保存在data中，后面想办法取出来
        if (requestCode == 0){

            //通过getData方法取得它的图片地址，后面的操作都是对这个地址的解析
            Uri uri=data.getData();
            //选择了一张在相册中id为26的照片，它的uri地址如下：
            //uri=content://com.android.providers.media.documents/document/image%3A26

            if (DocumentsContract.isDocumentUri(this,uri)){

                //判断是document类型的图片，所以获取它的doc id
                String docId=DocumentsContract.getDocumentId(uri);
                //docId=image:26
                //docId是将该资源的关键信息提取出来，比如该资源是一张id为26的image

                //获取它的uri的已解码的授权组成部分，来判断这张图片是在相册文件夹下还是下载文件夹下
                String uri_getAuthority=uri.getAuthority();
                //在相册文件夹的照片标识字段如下
                //uri_getAuthority=com.android.providers.media.documents

                //注意这里的字符串很容易写错，providers和documents都是有带s的
                if ("com.android.providers.media.documents".equals(uri_getAuthority)){
                    //当判断该照片在相册文件夹下时，使用字符串的分割方法split将它id取出来
                    String id=docId.split(":")[1];
                    //id="26"
                    Uri baseUri=Uri.parse("content://media/external/images/media");
                    imageView.setImageURI(Uri.withAppendedPath(baseUri, id));
                    //直接传入Uri地址，该地址为content://media/external/images/media/26
                    try {
                        bitmapImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Bundle bundle = data.getExtras();
            bitmapImage = (Bitmap) bundle.get("data");
            imageView.setImageBitmap(bitmapImage);
        }
    }
    private void openCamera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1);
    }
    private void initViews(){
        btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://10.0.2.2:5000/androidUpload";

                String img = bitMapToBase64(bitmapImage);

                OkHttpClient client = new OkHttpClient();
                FormBody.Builder formBuilder = new FormBody.Builder();
                formBuilder.add("img",img);

                Request request = new Request.Builder()
                        .url(url)
                        .post(formBuilder.build())
                        .build();

                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "服务器错误", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String info = null;
                                try {
                                    info = response.body().string();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
                                Log.i("MainActivity",info);
                                classifyResult.setText(info);
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * bitmap类型转为string
     * @param bitmap 图片的bitmap类
     * @return 图片的base64的编码
     */
    private String bitMapToBase64(Bitmap bitmap){
        String result = null;
        ByteArrayOutputStream baos = null;
        try{
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return result;
    }
}

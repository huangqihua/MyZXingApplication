package com.example.huangqihua.zxingapplication.com.example.zxingdemo;

import android.net.Uri;
import android.text.TextUtils;
import com.example.huangqihua.zxingapplication.R;
import com.example.huangqihua.zxingapplication.com.karics.library.zxing.android.CaptureActivity;
import com.example.huangqihua.zxingapplication.com.karics.library.zxing.encode.CodeCreator;
import com.google.zxing.WriterException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int REQUEST_CODE_SCAN = 0x0000;

	private static final String DECODED_CONTENT_KEY = "codedContent";
	private static final String DECODED_BITMAP_KEY = "codedBitmap";

	TextView qrCoded;
	ImageView qrCodeImage;
	Button creator, scanner;
	EditText qrCodeUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		qrCoded = (TextView) findViewById(R.id.ECoder_title);
		qrCodeImage = (ImageView) findViewById(R.id.ECoder_image);
		creator = (Button) findViewById(R.id.ECoder_creator);
		scanner = (Button) findViewById(R.id.ECoder_scaning);
		qrCodeUrl = (EditText) findViewById(R.id.ECoder_input);


		scanner.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this,
						CaptureActivity.class);
				startActivityForResult(intent, REQUEST_CODE_SCAN);
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// 扫描二维码/条码回传
		if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
			if (data != null) {

				String content = data.getStringExtra(DECODED_CONTENT_KEY);
				if (isNormalUrl(content)){
					openWebView(content);
				}else {
					qrCoded.setText("解码结果： \n" + content);
				}
				Bitmap bitmap = data.getParcelableExtra(DECODED_BITMAP_KEY);


				qrCodeImage.setImageBitmap(bitmap);
			}
		}
	}

	private void openWebView(String url) {

		Uri  uri = Uri.parse(url);
		Intent  intent = new  Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);

	}


	public static boolean isNormalUrl(String url) {
		return (url != null && url.length() > 0 &&(url.startsWith("http://") || url.startsWith("https://")));
	}

}

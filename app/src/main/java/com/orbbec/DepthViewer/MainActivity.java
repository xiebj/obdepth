package com.orbbec.DepthViewer;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.orbbec.NativeNI.NativeMethod;
import com.orbbec.astrakernel.AstraContext;
import com.orbbec.astrakernel.DepthData;
import com.orbbec.astrakernel.PermissionCallbacks;

import org.openni.*;

public class MainActivity extends Activity {

	String VERSION = "1.0.8";

	String TAG = "obDepth";

	float pDepthHist[];

	AstraContext mContext;
	DepthData mDepthData;

	Bitmap mUserBitmap;
	Bitmap showBitmap;
	int[] mPixels;

	DepthMap getDepthData;

	boolean mExit = false;
	int mWidth;
	int mHeight;
	int mMaxDepth = 10000;
	private FrameLayout mCursorPanel;
	private HomeKeyListener mHomeListener;

	private ImageView imageView;
	int count = 0;
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				showBitmap = convertGreyImg((Bitmap)msg.obj);

				imageView.setImageBitmap(showBitmap);
			}
		}
	};
	private PermissionCallbacks m_callbacks = new PermissionCallbacks() {

	

		@Override
		public void onDevicePermissionGranted() {
			try {
				mDepthData = new DepthData(mContext);
				mDepthData.setMapOutputMode(320, 240, 30);

				mWidth = mDepthData.GetDepthMap().getXRes();
				mHeight = mDepthData.GetDepthMap().getYRes();

				Log.i(TAG, "mWidth  = " + mWidth);
				Log.i(TAG, "mHeight  = " + mHeight);

				mDepthData.setMirror(false);// true,false

				mUserBitmap = Bitmap.createBitmap(mWidth, mHeight,
						Bitmap.Config.ARGB_8888);
				mPixels = new int[mWidth * mHeight];

			} catch (Exception e) {
				// e.printStackTrace();
			}

			try {
				mContext.start();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				// e1.printStackTrace();
			}

			AstraView xv = new AstraView(MainActivity.this);
			mCursorPanel = (FrameLayout) findViewById(R.id.DepthPanel);
			mCursorPanel.addView(xv);
			imageView = (ImageView) findViewById(R.id.image_show);
		}

		@Override
		public void onDevicePermissionDenied() {

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.i(TAG, "version  = " + VERSION);

		try {
			mContext = new AstraContext(this, m_callbacks);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		registerHomeListener();
		pDepthHist = new float[mMaxDepth];

	}
	
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		mExit = true;
	}

	protected void onDestroy() {
		super.onDestroy();

		if (mHomeListener != null) {
			mHomeListener.stopWatch();
		}
		
		if (mDepthData != null) {
			mDepthData.Close();
		}
		
		if(mContext != null)
		{
			mContext.Close();
		}
		
		if(mCursorPanel != null)
		{
			mCursorPanel.removeAllViews();
		}	
		Log.i(TAG, "onDestroy!");
		unRegisterHomeListener();
		System.exit(0);
	}


	public class AstraView extends SurfaceView implements
			SurfaceHolder.Callback, Runnable {

		private SurfaceHolder m_holder;
		private Canvas m_canvas;
		private Thread m_surfaceThread = new Thread(this);

		public AstraView(Context context) {
			super(context);
			m_holder = this.getHolder();
			m_holder.addCallback(this);
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
			if(m_surfaceThread == null || m_surfaceThread != null && !m_surfaceThread.isAlive()){
				mExit = false;
				m_surfaceThread = new Thread(this);
				m_surfaceThread.start();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
            m_surfaceThread = null;
            mExit = true;	
		}

		@Override
		public void run() {
			while (!mExit) {
				try {
					//更新深度生成器的数据
					mContext.waitforupdate();

					getDepthData = mDepthData.GetDepthMap();

					int index = 0;
					for (int y = 0; y < mHeight; y++) {
						for (int x = 0; x < mWidth; x++) {
							mPixels[index] = getDepthData.readPixel(x, y);
							mPixels[index] = handlemPixels(mPixels[index]);
							index++;
						}
					}
					Log.i("old", String.valueOf(Integer.toBinaryString(mPixels[1000])));
					//NativeMethod.CoventFromDepthTORGB(mPixels, mWidth, mHeight);
					Log.i("new", String.valueOf(Integer.toBinaryString(mPixels[1000])));
					/*
					public void setPixels (int[] pixels, int offset, int stride, int x, int y, int width, int height)
					用数组中的颜色值替换位图的像素值。数组中的每个元素是包装的整型，代表了颜色值。
					参数
      				pixels        写到位图中的颜色值
					offset        从pixels[]中读取的第一个颜色值的索引
					stride        位图行之间跳过的颜色个数。通常这个值等于位图宽度，但它可以更更大(或负数)
					X               被写入位图中第一个像素的x坐标。
					Y               被写入位图中第一个像素的y坐标
					width        从pixels[]中拷贝的每行的颜色个数
					height       写入到位图中的行数
					 */
					mUserBitmap.setPixels(mPixels, 0, mWidth, 0, 0, mWidth,
							mHeight);
					m_canvas = m_holder.lockCanvas();

					Paint paint = new Paint();
					paint.setXfermode(new PorterDuffXfermode(
							PorterDuff.Mode.CLEAR));
					m_canvas.drawPaint(paint);

					if (mUserBitmap != null) {

						m_canvas.drawBitmap(mUserBitmap, new Rect(0, 0, mWidth,
								mHeight), new RectF(0f, 0f, getWidth(),
								getHeight()), null);
					}
					m_holder.unlockCanvasAndPost(m_canvas);
					count++;
					Message message = new Message();
					message.what = 1;
					message.obj = mUserBitmap;
					handler.sendMessage(message);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}

			}

			Log.i(TAG, "Astra stop, out of while!!");
		}
	}
	
	/**
	 * 注册Home键的监听
	 */
	private void registerHomeListener() {
		mHomeListener = new HomeKeyListener(this);
		mHomeListener
				.setOnHomePressedListener(new HomeKeyListener.OnHomePressedListener() {

					@Override
					public void onHomePressed() {
						// TODO 进行点击Home键的处理
						finish();
					}

					@Override
					public void onHomeLongPressed() {
						// TODO 进行长按Home键的处理
					}
				});
		mHomeListener.startWatch();
	}

	private void unRegisterHomeListener() {
		if (mHomeListener != null) {
			mHomeListener.stopWatch();
		}
	}

	public int handlemPixels(int mpixels) {
		/*
		原生深度图的信息为13位，将其等比例的减为8位的深度信息，并同时赋予rgb相等的深度值
		 */
		int newPixel;
		int alpha = 0xFF << 24;
		int red = ((mpixels*256/0x0fff) << 16);
		int green = ((mpixels*256/0x0fff) << 8);
		int blue = (mpixels*256/0x0fff);
		newPixel = alpha | red | green | blue;
		return newPixel;
	}
	/**
	 * 将彩色图转换为灰度图
	 * @param img 位图
	 * @return  返回转换好的位图
	 */
	public Bitmap convertGreyImg(Bitmap img) {
		int width = img.getWidth();         //获取位图的宽
		int height = img.getHeight();       //获取位图的高

		int []pixels = new int[width * height]; //通过位图的大小创建像素点数组

		img.getPixels(pixels, 0, width, 0, 0, width, height);
		int alpha = 0xFF << 24;
		for(int i = 0; i < height; i++)  {
			for(int j = 0; j < width; j++) {
				if (i == 80) {
					pixels[width*i+j] = 0x000000FF;
				} else {
					int grey = pixels[width * i + j];

					int red = ((grey & 0x00FF0000) >> 16);
					int green = ((grey & 0x0000FF00) >> 8);
					int blue = (grey & 0x000000FF);

					grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
					grey = alpha | (grey << 16) | (grey << 8) | grey;
					pixels[width * i + j] = grey;
				}
			}
		}
		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		result.setPixels(pixels, 0, width, 0, 0, width, height);
		return result;
	}
}

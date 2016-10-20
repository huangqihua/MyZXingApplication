/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.huangqihua.zxingapplication.com.karics.library.zxing.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import com.example.huangqihua.zxingapplication.R;
import com.example.huangqihua.zxingapplication.com.karics.library.zxing.camera.CameraManager;
import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points. 这是一个位于相机顶部的预览view,它增加了一个外部部分透明的取景框，以及激光扫描动画和结果组件
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;
	private static final int OPAQUE = 0xFF;

	private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192,
			128, 64 };
//	private static final long ANIMATION_DELAY = 80L;
	private static final int CURRENT_POINT_OPACITY = 0xA0;
	private static final int MAX_RESULT_POINTS = 20;
	private static final int POINT_SIZE = 6;

	private CameraManager cameraManager;
	private final Paint paint;
	private Bitmap resultBitmap;
	private final int maskColor; // 取景框外的背景颜色
	private final int resultColor;// result Bitmap的颜色
	private final int statusColor; // 提示文字颜色
	private List<ResultPoint> possibleResultPoints;
	private List<ResultPoint> lastPossibleResultPoints;
	// 扫描线移动的y
	private int scanLineTop;
	// 扫描线移动速度
	private final int SCAN_VELOCITY = 15;
	// 扫描线
	Bitmap scanLight;

	/**
	 * 四个绿色边角对应的长度
	 */
	private int ScreenRate;
	private int corWidth = 5;
	/**
	 * 手机的屏幕密度
	 */
	private static float density;

	/**
	 * 扫描框中的中间线的宽度
	 */
	private static final int MIDDLE_LINE_WIDTH = 6;

	/**
	 * 扫描框中的中间线的与扫描框左右的间隙
	 */
	private static final int MIDDLE_LINE_PADDING = 5;

	/**
	 * 中间那条线每次刷新移动的距离
	 */
	private static final int SPEEN_DISTANCE = 5;
	private boolean isFirst;
	/**
	 * 中间滑动线的最顶端位置
	 */
	private int slideTop;



	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		statusColor = resources.getColor(R.color.status_text);
		possibleResultPoints = new ArrayList<ResultPoint>(5);
		lastPossibleResultPoints = null;
		scanLight = BitmapFactory.decodeResource(resources,
				R.drawable.scan_light);
		density = context.getResources().getDisplayMetrics().density;
		ScreenRate = (int)(10 * density);
	}

	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // not ready yet, early draw before done configuring
		}

		// frame为取景框
		Rect frame = cameraManager.getFramingRect();
		Rect previewFrame = cameraManager.getFramingRectInPreview();
		if (frame == null || previewFrame == null) {
			return;
		}

		//初始化中间线滑动的最上边和最下边
		if(!isFirst){
			isFirst = true;
			slideTop = frame.top;
		}


		int width = canvas.getWidth();
		int height = canvas.getHeight();

		//画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		//扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		paint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);// Rect_1
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint); // Rect_2
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint); // Rect_3
		canvas.drawRect(0, frame.bottom + 1, width, height, paint); // Rect_4

		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			// 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
			paint.setAlpha(CURRENT_POINT_OPACITY);
			canvas.drawBitmap(resultBitmap, null, frame, paint);
		} else {
			// Draw a red "laser scanner" line through the middle to show
			// decoding is active
			drawFrameBounds(canvas, frame);
			drawStatusText(canvas, frame, width);

			// 绘制扫描线

			drawScanLight(canvas, frame);

//			float scaleX = frame.width() / (float) previewFrame.width();
//			float scaleY = frame.height() / (float) previewFrame.height();

//			// 绘制扫描线周围的特征点
//			List<ResultPoint> currentPossible = possibleResultPoints;
//			List<ResultPoint> currentLast = lastPossibleResultPoints;
//			int frameLeft = frame.left;
//			int frameTop = frame.top;
//			if (currentPossible.isEmpty()) {
//				lastPossibleResultPoints = null;
//			} else {
//				possibleResultPoints = new ArrayList<ResultPoint>(5);
//				lastPossibleResultPoints = currentPossible;
//				paint.setAlpha(CURRENT_POINT_OPACITY);
//				paint.setColor(resultPointColor);
//				synchronized (currentPossible) {
//					for (ResultPoint point : currentPossible) {
//						canvas.drawCircle(frameLeft
//								+ (int) (point.getX() * scaleX), frameTop
//								+ (int) (point.getY() * scaleY), POINT_SIZE,
//								paint);
//					}
//				}
//			}
//			if (currentLast != null) {
//				paint.setAlpha(CURRENT_POINT_OPACITY / 2);
//				paint.setColor(resultPointColor);
//				synchronized (currentLast) {
//					float radius = POINT_SIZE / 2.0f;
//					for (ResultPoint point : currentLast) {
//						canvas.drawCircle(frameLeft
//								+ (int) (point.getX() * scaleX), frameTop
//								+ (int) (point.getY() * scaleY), radius, paint);
//					}
//				}
//			}

			// Request another update at the animation interval, but only
			// repaint the laser line,
			//只刷新扫描框的内容，其他地方不刷新
//			postInvalidateDelayed(ANIMATION_DELAY, frame.left - POINT_SIZE,
//					frame.top - POINT_SIZE, frame.right + POINT_SIZE,
//					frame.bottom + POINT_SIZE);
			//只刷新扫描框的内容，其他地方不刷新
			postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
					frame.right, frame.bottom);

		}
	}

	/**
	 * 绘制取景框边框
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawFrameBounds(Canvas canvas, Rect frame) {

		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(1);
		paint.setStyle(Paint.Style.STROKE);

		canvas.drawRect(frame, paint);

		paint.setColor(Color.parseColor("#62b925"));
		paint.setStyle(Paint.Style.FILL);


		canvas.drawRect(frame.left, frame.top, frame.left + ScreenRate,
				frame.top + corWidth, paint);
		canvas.drawRect(frame.left, frame.top, frame.left + corWidth, frame.top
				+ ScreenRate, paint);
		canvas.drawRect(frame.right - ScreenRate, frame.top, frame.right,
				frame.top + corWidth, paint);
		canvas.drawRect(frame.right - corWidth, frame.top, frame.right, frame.top
				+ ScreenRate, paint);
		canvas.drawRect(frame.left, frame.bottom - corWidth, frame.left
				+ ScreenRate, frame.bottom, paint);
		canvas.drawRect(frame.left, frame.bottom - ScreenRate,
				frame.left + corWidth, frame.bottom, paint);
		canvas.drawRect(frame.right - ScreenRate, frame.bottom - corWidth,
				frame.right, frame.bottom, paint);
		canvas.drawRect(frame.right - corWidth, frame.bottom - ScreenRate,
				frame.right, frame.bottom, paint);

	}

	/**
	 * 绘制提示文字
	 * 
	 * @param canvas
	 * @param frame
	 * @param width
	 */
	private void drawStatusText(Canvas canvas, Rect frame, int width) {

		String statusText1 = getResources().getString(
				R.string.viewfinderview_status_text1);
		String statusText2 = getResources().getString(
				R.string.viewfinderview_status_text2);
		int statusTextSize = 45;
		int statusPaddingTop = 180;

		paint.setColor(statusColor);
		paint.setTextSize(statusTextSize);

		int textWidth1 = (int) paint.measureText(statusText1);
		canvas.drawText(statusText1, (width - textWidth1) / 2, frame.top
				- statusPaddingTop, paint);

		int textWidth2 = (int) paint.measureText(statusText2);
		canvas.drawText(statusText2, (width - textWidth2) / 2, frame.top
				- statusPaddingTop + 60, paint);
	}

	/**
	 * 绘制移动扫描线
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawScanLight(Canvas canvas, Rect frame) {

//		if (scanLineTop == 0) {
//			scanLineTop = frame.top;
//		}
//
//		if (scanLineTop >= frame.bottom) {
//			scanLineTop = frame.top;
//		} else {
//			scanLineTop += SCAN_VELOCITY;
//		}
//		Rect scanRect = new Rect(frame.left, scanLineTop, frame.right,
//				scanLineTop + 30);
//		canvas.drawBitmap(scanLight, null, scanRect, paint);

		slideTop += SPEEN_DISTANCE;
		if(slideTop >= frame.bottom){
			slideTop = frame.top;
		}
//		Rect lineRect = new Rect();
//		lineRect.left = frame.left;
//		lineRect.right = frame.right;
//		lineRect.top = slideTop;
//		lineRect.bottom = slideTop + 18;
//		canvas.drawBitmap(((BitmapDrawable)(getResources().getDrawable(R.drawable.qrcode_scan_line))).getBitmap(), null, lineRect, paint);

		canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH/2, frame.right - MIDDLE_LINE_PADDING,slideTop + MIDDLE_LINE_WIDTH/2, paint);

	}

	public void drawViewfinder() {
		Bitmap resultBitmap = this.resultBitmap;
		this.resultBitmap = null;
		if (resultBitmap != null) {
			resultBitmap.recycle();
		}
		invalidate();
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		List<ResultPoint> points = possibleResultPoints;
		synchronized (points) {
			points.add(point);
			int size = points.size();
			if (size > MAX_RESULT_POINTS) {
				// trim it
				points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
			}
		}
	}

}

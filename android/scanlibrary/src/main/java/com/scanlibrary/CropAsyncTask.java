package com.scanlibrary;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.AsyncTask;

import java.util.Map;

public class CropAsyncTask extends AsyncTask<Void, Void, Bitmap> {

    private ScanFragment scanFragment;
    private Bitmap bitmap;
    private Map<Integer, PointF> points;
    private Map<String, Float> containerDimensions;

    public CropAsyncTask(
            ScanFragment scanFragment,
            Bitmap bitmap,
            Map<Integer, PointF> points,
            Map<String, Float> containerDimensions) {
        this.scanFragment = scanFragment;
        this.bitmap = bitmap;
        this.points = points;
        this.containerDimensions = containerDimensions;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        scanFragment.showProgressDialog(scanFragment.getString(R.string.loading));
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        if (points.size() != 4) {
            scanFragment.showErrorDialog();
        } else {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float xRatio = (float) width / containerDimensions.get("width");
            float yRatio = (float) height / containerDimensions.get("height");

            float x1 = (points.get(0).x) * xRatio;
            float x2 = (points.get(1).x) * xRatio;
            float x3 = (points.get(2).x) * xRatio;
            float x4 = (points.get(3).x) * xRatio;
            float y1 = (points.get(0).y) * yRatio;
            float y2 = (points.get(1).y) * yRatio;
            float y3 = (points.get(2).y) * yRatio;
            float y4 = (points.get(3).y) * yRatio;

            return ((ScanActivity) scanFragment.getActivity())
                    .getScannedBitmap(bitmap, x1, y1, x2, y2, x3, y3, x4, y4);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        scanFragment.dismissDialog();
        if(bitmap != null) {
            super.onPostExecute(bitmap);
            scanFragment.setCroppedPhoto(bitmap);
            bitmap.recycle();
        }
    }

}
package com.scanlibrary;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;

public class RotateAsyncTask extends AsyncTask<Void, Void, Bitmap> {

    private ScanFragment scanFragment;
    private Bitmap bitmap;
    final int angle = 90;

    public RotateAsyncTask(ScanFragment scanFragment, Bitmap bitmap) {
        this.scanFragment = scanFragment;
        this.bitmap = bitmap;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        scanFragment.showProgressDialog(scanFragment.getString(R.string.loading));
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.postRotate(angle);

        return Bitmap.createBitmap(bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                rotateMatrix,
                true);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        scanFragment.dismissDialog();
        scanFragment.setRotatedPhoto(bitmap);
        bitmap.recycle();
    }
}
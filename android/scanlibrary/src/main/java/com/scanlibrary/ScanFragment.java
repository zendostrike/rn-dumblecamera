package com.scanlibrary;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jhansi on 29/03/15.
 */
public class ScanFragment extends Fragment {

    private ImageView sourceImageView;
    private FrameLayout sourceFrame;
    private PolygonView polygonView;
    private View view;
    private ProgressDialogFragment progressDialogFragment;
    private IScanner scanner;
    private Bitmap original;
    private FrameLayout.LayoutParams layoutParams;
    private Map<Integer, PointF> pointFs;
    private ViewGroup transitionsContainer;
    private ViewGroup confirmContainer;
    private Button scanButton;
    private Button cropButton;
    private Button cancelButton;
    private Button rotateButton;
    private Button filtersButton;
    private Button backToCamera;
    private boolean polygonVisible;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof IScanner)) {
            throw new ClassCastException("Activity must implement IScanner");
        }
        this.scanner = (IScanner) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scan_fragment_layout, null);
        init();
        return view;
    }

    private void init() {
        polygonVisible = false;
        sourceImageView = (ImageView) view.findViewById(R.id.sourceImageView);
        sourceFrame = (FrameLayout) view.findViewById(R.id.sourceFrame);
        polygonView = (PolygonView) view.findViewById(R.id.polygonView);
        sourceFrame.post(new Runnable() {
            @Override
            public void run() {
                original = getBitmap();
                if (original != null) {
                    setBitmap(original);
                }
            }
        });
        backToCamera = (Button) view.findViewById(R.id.backToCamera);
        confirmContainer = (ViewGroup) view.findViewById(R.id.confirmBar);
        scanButton = (Button) confirmContainer.findViewById(R.id.scanButton);
        cancelButton = (Button) confirmContainer.findViewById(R.id.cancelButton);

        transitionsContainer = (ViewGroup) view.findViewById(R.id.editionBar);
        cropButton = (Button) transitionsContainer.findViewById(R.id.cropButton);
        rotateButton = (Button) transitionsContainer.findViewById(R.id.rotateButton);
        filtersButton = (Button) transitionsContainer.findViewById(R.id.filtersButton);
        filtersButton.setOnClickListener(new FiltersButtonClickListener());
        scanButton.setOnClickListener(new ScanButtonClickListener());
        cancelButton.setOnClickListener(new CancelButtonClickListener());
        cropButton.setOnClickListener(new CropButtonClickListener());
        rotateButton.setOnClickListener(new RotateButtonClickListener());
        backToCamera.setOnClickListener(new BackToCameraClickListener());
    }

    private class FiltersButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            showProgressDialog(getResources().getString(R.string.loading));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        scanner.onScanFinish(Utils.getUri(getActivity(), original));
                        dismissDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private class CancelButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            toggleConfirmBar();
        }
    }

    private class CropButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            toggleConfirmBar();
        }
    }

    private class ScanButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            crop();
        }
    }

    private class RotateButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            rotate();
        }
    }

    private class BackToCameraClickListener implements View.OnClickListener  {
        @Override
        public void onClick(View v) {
            getFragmentManager().popBackStack();
        }
    }

    private void rotate() {
        new RotateAsyncTask(this, original).execute();
    }

    private void crop() {
        // Get image view dimensions
        Map<String, Float> containerDimensions = new HashMap<>();
        containerDimensions.put("width", (float) sourceImageView.getWidth());
        containerDimensions.put("height", (float) sourceImageView.getHeight());

        new CropAsyncTask(this, original, polygonView.getPoints(), containerDimensions)
                .execute();
    }

    protected void setRotatedPhoto(Bitmap bitmap){
        Uri uri = Utils.getUri(getActivity(), bitmap);
        replaceCurrentImage(uri);
    }

    protected void setCroppedPhoto(Bitmap bitmap){
        Uri uri = Utils.getUri(getActivity(), bitmap);
        replaceCurrentImage(uri);
        toggleConfirmBar();
    }

    private void replaceCurrentImage(final Uri uri) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try{
                    original = Utils.getBitmap(getActivity(), uri);
                    getActivity().getContentResolver().delete(uri, null, null);
                    sourceImageView.setImageBitmap(original);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void toggleConfirmBar() {
        polygonVisible = !polygonVisible;
        TransitionManager.beginDelayedTransition(confirmContainer);
        confirmContainer.setVisibility(polygonVisible ? View.VISIBLE : View.GONE);
        transitionsContainer.setVisibility(polygonVisible ? View.GONE : View.VISIBLE);
        showPolygon(polygonVisible);
    }

    private void showPolygon(Boolean visible) {
        if(visible){
            polygonView.setPoints(pointFs);
            polygonView.setVisibility(View.VISIBLE);
            polygonView.setLayoutParams(layoutParams);
        } else {
            polygonView.setVisibility(View.GONE);
        }
    }

    private Bitmap getBitmap() {
        Uri uri = getUri();
        try {
            Bitmap bitmap = Utils.getBitmap(getActivity(), uri);
            getActivity().getContentResolver().delete(uri, null, null);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Uri getUri() {
        if(getArguments().getBoolean(ScanConstants.AFTER_FILTER_APPLY)) {
            return getArguments().getParcelable(ScanConstants.FILTER_RESULT);
        } else {
            return getArguments().getParcelable(ScanConstants.SELECTED_BITMAP);
        }
    }

    private void setBitmap(Bitmap original) {
        Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        Bitmap tempBitmap = ((BitmapDrawable) sourceImageView.getDrawable()).getBitmap();
        pointFs = getEdgePoints(tempBitmap);
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        float[] points = ((ScanActivity) getActivity()).getPoints(tempBitmap);
        float x1 = points[0];
        float x2 = points[1];
        float x3 = points[2];
        float x4 = points[3];

        float y1 = points[4];
        float y2 = points[5];
        float y3 = points[6];
        float y4 = points[7];

        List<PointF> pointFs = new ArrayList<>();
        pointFs.add(new PointF(x1, y1));
        pointFs.add(new PointF(x2, y2));
        pointFs.add(new PointF(x3, y3));
        pointFs.add(new PointF(x4, y4));
        return pointFs;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    protected void showProgressDialog(String message) {
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected void showErrorDialog() {
        SingleButtonDialogFragment fragment = new SingleButtonDialogFragment(R.string.ok, getString(R.string.cantCrop), "Error", true);
        FragmentManager fm = getActivity().getFragmentManager();
        fragment.show(fm, SingleButtonDialogFragment.class.toString());
    }

    protected void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }

}
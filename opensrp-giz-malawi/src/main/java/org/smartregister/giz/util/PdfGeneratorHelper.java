package org.smartregister.giz.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.RequiresApi;

import org.smartregister.giz.listener.PdfGeneratorListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PdfGeneratorHelper {
    public static double postScriptThreshold = 0.75;
    public static int a4HeightInPX = 3508;
    public static int a4WidthInPX = 2480;
    public static int a5HeightInPX = 1748;
    public static int a5WidthInPX = 2480;

    public static int a4HeightInPostScript = (int) (a4HeightInPX * postScriptThreshold);
    public static int a4WidthInPostScript = (int) (a4WidthInPX * postScriptThreshold);

    public static int WRAP_CONTENT_WIDTH = 0;
    public static int WRAP_CONTENT_HEIGHT = 0;

    public static ContextStep getBuilder() {
        return new Builder();
    }

    public static List<View> getViewListFromID(Activity activity, @IdRes List<Integer> viewIDList) {
        List<View> viewList = new ArrayList<>();
        if (activity != null) {
            for (int viewID : viewIDList)
                viewList.add(activity.findViewById(viewID));
        }
        return viewList;
    }

    public static List<View> getViewListFromLayout(Context context,
                                                   PdfGeneratorListener pdfGeneratorListener,
                                                   List<Integer> layoutList) {
        List<View> viewList = new ArrayList<>();

        try {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            for (int layout : layoutList) {
                View generatedView = inflater.inflate(layout, null);
                viewList.add(generatedView);
            }

        } catch (Exception e) {
            Timber.e(e,"Could not inflate Layout");
        }
        return viewList;
    }

    public enum PageSize {
        /**
         * For standard A4 size page
         */
        A4,

        /**
         * For standard A5 size page
         */
        A5,
        /**
         * For print the page as much as they are big.
         */
        WRAP_CONTENT
    }


    public interface ContextStep {
        FromSourceStep setContext(Context context);
    }

    public interface FromSourceStep {
        LayoutXMLSourceIntakeStep fromLayoutXMLSource();

        ViewIDSourceIntakeStep fromViewIDSource();

        ViewSourceIntakeStep fromViewSource();
    }

    public interface ViewSourceIntakeStep {
        PageSizeStep fromView(View... viewList);

        PageSizeStep fromViewList(List<View> viewList);
    }


    public interface LayoutXMLSourceIntakeStep {
        PageSizeStep fromLayoutXML(@LayoutRes Integer... layoutXMLs);

        PageSizeStep fromLayoutXMLList(@LayoutRes List<Integer> layoutXMLList);
    }


    public interface ViewIDSourceIntakeStep {
        PageSizeStep fromViewID(Activity activity, @IdRes Integer... xmlResourceList);

        PageSizeStep fromViewIDList(Activity activity, @IdRes List<Integer> xmlResourceList);

    }

    public interface PageSizeStep {
        FileNameStep setDefaultPageSize(PageSize pageSize);

        /**
         * Need to improvement.
         *
         * @param widthInPX
         * @param heightInPX
         * @return
         */
        FileNameStep setCustomPageSize(int widthInPX, int heightInPX);
    }


    public interface FileNameStep {
        Build setFileName(String fileName);
    }

    public interface Build {
        void build(PdfGeneratorListener pdfGeneratorListener);

        Build setFolderName(String folderName);

        Build openPDFafterGeneration(boolean open);

    }

    public static class Builder implements Build, FileNameStep, PageSizeStep
            , LayoutXMLSourceIntakeStep, ViewSourceIntakeStep, ViewIDSourceIntakeStep
            , FromSourceStep, ContextStep {

        private int pageWidthInPixel = a4WidthInPX;
        private int pageHeightInPixel = a4HeightInPX;
        private Context context;
        private PageSize pageSize;
        private PdfGeneratorListener pdfGeneratorListener;
        private List<View> viewList = new ArrayList<>();
        private String fileName;
        private String targetPdf;
        private boolean openPdfFile = true;
        private String folderName;
        private String directory_path;
        private Disposable disposable;

        private void postFailure(String errorMessage) {
            FailureResponse failureResponse = new FailureResponse(errorMessage);
            postLog(errorMessage);
            if (pdfGeneratorListener != null)
                pdfGeneratorListener.onFailure(failureResponse);
        }

        private void postFailure(Throwable throwable) {
            FailureResponse failureResponse = new FailureResponse(throwable);
            if (pdfGeneratorListener != null)
                pdfGeneratorListener.onFailure(failureResponse);
        }

        private void postLog(String logMessage) {
            if (pdfGeneratorListener != null)
                pdfGeneratorListener.showLog(logMessage);
        }

        private void postSuccess(PdfDocument pdfDocument, File file, int widthInPS, int heightInPS) {
            if (pdfGeneratorListener != null)
                pdfGeneratorListener.onSuccess(new SuccessResponse(pdfDocument, file, widthInPS, heightInPS));
        }

        private void openGeneratedPDF() {
            File file = new File(targetPdf);
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(file);
                intent.setDataAndType(uri, "*/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    postFailure(e);
                }
            } else {
                String path = TextUtils.isEmpty(directory_path) ? "null" : directory_path;
                postFailure("PDF file is not existing in storage. Your Generated path is " + path);
            }
        }

        @RequiresApi(api = android.os.Build.VERSION_CODES.KITKAT)
        private void print() {
            try {
                if (context != null) {
                    PdfDocument document = new PdfDocument();

                    if (pageSize != null) {
                        if (pageSize == PageSize.A4) {
                            pageHeightInPixel = a4HeightInPX;
                            pageWidthInPixel = a4WidthInPX;
                        } else if (pageSize == PageSize.A5) {
                            pageHeightInPixel = a5HeightInPX;
                            pageWidthInPixel = a5WidthInPX;
                        } else if (pageSize == PageSize.WRAP_CONTENT) {
                            pageWidthInPixel = WRAP_CONTENT_WIDTH;
                            pageHeightInPixel = WRAP_CONTENT_HEIGHT;
                        }
                    } else {
                        postLog("Default page size is not found. Your custom page width is " +
                                pageWidthInPixel + " and custom page height is " + pageHeightInPixel);
                    }

                    getLayoutAspects(document);

                    //This is for prevent crashing while opening generated PDF.
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());

                    setUpDirectoryPath(context);

                    if (TextUtils.isEmpty(directory_path)) {
                        postFailure("Environment.getExternalStorageDirectory() and " +
                                "context.getExternalFilesDir()" +
                                " is returning null");
                        return;
                    }

                    directory_path = directory_path + "/" + folderName + "/";

                    File file = new File(directory_path);
                    if ((!file.exists()) && (!file.mkdirs())) {
                        postLog("Folder is not created." +
                                "file.mkdirs() is returning false");
                        //Folder is made here
                    }

                    targetPdf = directory_path + fileName + ".pdf";

                    createFile(document);

                } else {
                    postFailure("Context is null");
                }
            } catch (Exception e) {
                postFailure(e);
            }

        }

        @RequiresApi(api = android.os.Build.VERSION_CODES.KITKAT)
        private void createFile(PdfDocument document) {
            File filePath = new File(targetPdf);
            //File is created under the folder but not yet written.

            disposeDisposable();
            disposable = Completable.fromAction(() -> document.writeTo(new FileOutputStream(filePath)))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                        document.close();
                        disposeDisposable();
                    })
                    .subscribe(() -> {
                        postSuccess(document, filePath, pageWidthInPixel, pageHeightInPixel);
                        document.close();
                        if (openPdfFile) {
                            openGeneratedPDF();
                        }
                    }, this::postFailure);
        }

        @RequiresApi(api = android.os.Build.VERSION_CODES.KITKAT)
        private void getLayoutAspects(PdfDocument document) {
            if (viewList == null || viewList.size() == 0)
                postLog("View list null or zero sized");
            for (int i = 0; i < viewList.size(); i++) {
                View content = viewList.get(i);

                if (pageWidthInPixel == WRAP_CONTENT_HEIGHT && pageHeightInPixel == WRAP_CONTENT_WIDTH) {

                    content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    pageHeightInPixel = content.getMeasuredHeight();
                    pageWidthInPixel = content.getMeasuredWidth();
                }
                            /*If page size is less then standard A4 size then assign it A4 size otherwise
                            the view will be messed up or so minimized that it will be not print in pdf*/
                pageWidthInPixel = Math.max(pageWidthInPixel, a4WidthInPX);
                pageHeightInPixel = Math.max(pageWidthInPixel, a4HeightInPX);

                /*Convert page size from pixel into post script because PdfDocument takes
                 * post script as a size unit*/
                pageHeightInPixel = (int) (pageHeightInPixel * postScriptThreshold);
                pageWidthInPixel = (int) (pageWidthInPixel * postScriptThreshold);

                content.measure(View.MeasureSpec.makeMeasureSpec(pageWidthInPixel, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
                pageHeightInPixel = (Math.max(content.getMeasuredHeight(), a4HeightInPostScript));


                PdfDocument.PageInfo pageInfo =
                        new PdfDocument.PageInfo.Builder((pageWidthInPixel), (pageHeightInPixel), i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);

                content.layout(0, 0, pageWidthInPixel, pageHeightInPixel);
                content.draw(page.getCanvas());

                document.finishPage(page);

                /*Finally invalidate it and request layout for restore the previous state
                 * of the view as like as the xml. Otherwise for generating PDF by view id,
                 * the main view is being messed up because this a view is not cloneable and
                 * being modified in the above view related tasks for printing PDF. */
                content.invalidate();
                content.requestLayout();
            }
        }

        private void disposeDisposable() {
            if (disposable != null && !disposable.isDisposed())
                disposable.dispose();
        }

        private void setUpDirectoryPath(Context context) {
            if (Environment.getExternalStorageDirectory() != null &&
                    !TextUtils.isEmpty(Environment.getExternalStorageDirectory().getPath())) {
                directory_path = Environment.getExternalStorageDirectory().getPath();
            }
            if (((android.os.Build.VERSION.SDK_INT >= 29) || TextUtils.isEmpty(directory_path)) &&
                    (context.getExternalFilesDir(null) != null &&
                            !TextUtils.isEmpty(context.getExternalFilesDir(null).getAbsolutePath()))) {
                directory_path = context.getExternalFilesDir(null).getAbsolutePath();
                postLog("Environment.getExternalStorageDirectory() is returning null or" +
                        " you are using Android API level 30+ which prevents to get external storage path" +
                        "instead of using storage scope. Using context.getExternalFilesDir(null) which is " +
                        "returning the absolute path - " + directory_path + "");
            }
        }

        @RequiresApi(api = android.os.Build.VERSION_CODES.KITKAT)
        @Override
        public void build(PdfGeneratorListener pdfGeneratorListener) {
            this.pdfGeneratorListener = pdfGeneratorListener;
            print();
        }

        @Override
        public PageSizeStep fromView(View... viewArrays) {
            viewList = new ArrayList<>(Arrays.asList(viewArrays));
            return this;
        }

        @Override
        public PageSizeStep fromViewList(List<View> viewList) {
            this.viewList = viewList;
            return this;
        }

        @Override
        public Build openPDFafterGeneration(boolean openPdfFile) {
            this.openPdfFile = openPdfFile;
            return this;
        }

        @Override
        public FromSourceStep setContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        public FileNameStep setDefaultPageSize(PageSize pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        @Override
        public FileNameStep setCustomPageSize(int widthInPX, int heightInPX) {
            this.pageWidthInPixel = widthInPX;
            this.pageHeightInPixel = heightInPX;
            return this;
        }

        @Override
        public Build setFolderName(String folderName) {
            this.folderName = folderName;
            return this;
        }

        @Override
        public Build setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        @Override
        public PageSizeStep fromViewID(Activity activity, @IdRes Integer... viewIDs) {
            viewList = getViewListFromID(activity, Arrays.asList(viewIDs));
            return this;
        }

        @Override
        public PageSizeStep fromViewIDList(Activity activity, List<Integer> viewIDList) {
            viewList = getViewListFromID(activity, viewIDList);
            return this;
        }

        @Override
        public PageSizeStep fromLayoutXML(@LayoutRes Integer... layouts) {
            viewList = getViewListFromLayout(context, pdfGeneratorListener, Arrays.asList(layouts));
            return this;
        }

        @Override
        public PageSizeStep fromLayoutXMLList(@LayoutRes List<Integer> layoutXMLList) {
            viewList = getViewListFromLayout(context, pdfGeneratorListener, layoutXMLList);
            return this;
        }

        @Override
        public LayoutXMLSourceIntakeStep fromLayoutXMLSource() {
            return this;
        }

        @Override
        public ViewIDSourceIntakeStep fromViewIDSource() {
            return this;
        }

        @Override
        public ViewSourceIntakeStep fromViewSource() {
            return this;
        }
    }


    public static class FailureResponse {
        protected Throwable throwable;
        protected String errorMessage;

        public FailureResponse(Throwable throwable) {
            this.throwable = throwable;
            if (throwable != null) errorMessage = throwable.getLocalizedMessage();
        }

        public FailureResponse(Throwable throwable, String errorMessage) {
            this.throwable = throwable;
            this.errorMessage = errorMessage;
        }

        public FailureResponse(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class SuccessResponse {
        protected PdfDocument pdfDocument;
        protected File file;
        protected String path;
        //Because PdfDocument is using PostScript as unit.
        protected int widthInPostScripUnit;
        protected int heightInPostScripUnit;


        public SuccessResponse(PdfDocument pdfDocument, File file, int widthInPostScripUnit, int heightInPostScripUnit) {
            this.pdfDocument = pdfDocument;
            this.file = file;
            if (file != null && !TextUtils.isEmpty(file.getAbsolutePath()))
                path = file.getAbsolutePath();
            this.heightInPostScripUnit = heightInPostScripUnit;
            this.widthInPostScripUnit = widthInPostScripUnit;
        }

        public PdfDocument getPdfDocument() {
            return pdfDocument;
        }

        public void setPdfDocument(PdfDocument pdfDocument) {
            this.pdfDocument = pdfDocument;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getPath() {
            if (file != null && !TextUtils.isEmpty(file.getAbsolutePath()))
                return file.getAbsolutePath();
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getWidthInPostScripUnit() {
            return widthInPostScripUnit;
        }

        public void setWidthInPostScripUnit(int widthInPostScripUnit) {
            this.widthInPostScripUnit = widthInPostScripUnit;
        }

        public int getHeightInPostScripUnit() {
            return heightInPostScripUnit;
        }

        public void setHeightInPostScripUnit(int heightInPostScripUnit) {
            this.heightInPostScripUnit = heightInPostScripUnit;
        }
    }
}
package org.smartregister.giz.presenter;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.Nullable;

import org.smartregister.giz.contract.FragmentBaseActivityContract;
import org.smartregister.giz.listener.PdfGeneratorListener;
import org.smartregister.giz.util.PdfGeneratorHelper;

import timber.log.Timber;

public class FragmentBaseActivityPresenter implements FragmentBaseActivityContract.Presenter {

    @Nullable
    private final FragmentBaseActivityContract.View view;

    public FragmentBaseActivityPresenter(@Nullable FragmentBaseActivityContract.View view) {
        this.view = view;
    }

    @Override
    public void sendEmail(String[] pdfPath, String emailAttachmentName) {
// export as image
        assert view != null;
        PdfGeneratorHelper.getBuilder()
                .setContext((Context) getView())
                .fromViewSource()
                .fromView(view.getPDFRootView())
                .setDefaultPageSize(PdfGeneratorHelper.PageSize.WRAP_CONTENT)
                .setFileName(emailAttachmentName)
                .setFolderName(Environment.DIRECTORY_DOWNLOADS)
                .openPDFafterGeneration(false)
                .build(new PdfGeneratorListener() {
                    @Override
                    public void onFailure(PdfGeneratorHelper.FailureResponse failureResponse) {
                        super.onFailure(failureResponse);
                        Timber.e(failureResponse.getThrowable(), "Pdf Could not be Generated");
                    }

                    @Override
                    public void onSuccess(PdfGeneratorHelper.SuccessResponse response) {
                        super.onSuccess(response);
                        pdfPath[0] = response.getPath();
                        view.sendEmailWithAttachment(view.getEmailSubject(), pdfPath[0]);
                    }
                });
    }
    @Nullable
    public FragmentBaseActivityContract.View getView() {
        return view;
    }
}

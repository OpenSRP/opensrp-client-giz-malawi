package org.smartregister.giz.contract;

import android.widget.RelativeLayout;

public interface FragmentBaseActivityContract {

    interface View {
        void sendEmailWithAttachment(String subject, String pathToMyAttachedFile);
        String getEmailSubject();
        RelativeLayout getPDFRootView();
    }

    interface Presenter {
        void sendEmail(String[] pdfPath, String emailAttachmentName);
    }
}

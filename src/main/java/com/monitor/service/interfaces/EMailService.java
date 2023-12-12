package com.monitor.service.interfaces;

import com.monitor.service.parameter.CommonResponse;
import com.monitor.service.parameter.SubmitFormParam;
import org.springframework.web.multipart.MultipartFile;

public interface EMailService {

    /**
     * Send email to support email box
     */
    CommonResponse submit(SubmitFormParam submitFormParam);

    /**
     * Upload file to server
     */
    CommonResponse upload(MultipartFile file);
}

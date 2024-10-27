/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.inxedu.os.common.controller.BaseController
 *  com.inxedu.os.common.controller.ImageUploadController
 *  com.inxedu.os.common.util.DateUtils
 *  com.inxedu.os.common.util.FileUploadUtils
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestMethod
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.multipart.MultipartFile
 */
package com.inxedu.os.common.controller;

import com.inxedu.os.common.constants.CommonConstants;
import com.inxedu.os.common.controller.BaseController;
import com.inxedu.os.common.controller.ImageUploadController;
import com.inxedu.os.common.util.DateUtils;
import com.inxedu.os.common.util.FileUploadUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(value={"/video"})
public class VideoUploadController
extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(ImageUploadController.class);
    private List<String> fileTypeList;

    private String getProjectRootDirPath(HttpServletRequest request) {
        return request.getSession().getServletContext().getRealPath("/");
    }

    @RequestMapping(value={"/uploadvideo"}, method={RequestMethod.POST})
    public String gok4(HttpServletRequest request, HttpServletResponse response, @RequestParam(value="uploadfile", required=true) MultipartFile uploadfile, @RequestParam(value="param", required=false) String param, @RequestParam(value="fileType", required=true) String fileType) {
        try {
            String[] type = fileType.split(",");
            this.setFileTypeList(type);
            String ext = FileUploadUtils.getSuffix((String)uploadfile.getOriginalFilename());
            if (!fileType.contains(ext)) {
                return this.responseErrorData(response, 1, "\u6587\u4ef6\u683c\u5f0f\u9519\u8bef\uff0c\u4e0a\u4f20\u5931\u8d25\u3002");
            }
            String filePath = this.getPath(request, ext, param);
            File file = new File(this.getProjectRootDirPath(request) + filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            uploadfile.transferTo(file);
            return this.responseData(filePath, 0, "\u4e0a\u4f20\u6210\u529f", response);
        }
        catch (Exception e) {
            logger.error("gok4()--error", (Throwable)e);
            return this.responseErrorData(response, 2, "\u7cfb\u7edf\u7e41\u5fd9\uff0c\u4e0a\u4f20\u5931\u8d25");
        }
    }

    @RequestMapping(value={"/uploadaudio"}, method={RequestMethod.POST})
    public String uploadAudio(HttpServletRequest request, HttpServletResponse response, @RequestParam(value="uploadfile", required=true) MultipartFile uploadfile, @RequestParam(value="param", required=false) String param, @RequestParam(value="fileType", required=true) String fileType) {
        try {
            String[] type = fileType.split(",");
            this.setFileTypeList(type);
            String ext = FileUploadUtils.getSuffix((String)uploadfile.getOriginalFilename());
            if (!fileType.contains(ext)) {
                return this.responseErrorData(response, 1, "\u6587\u4ef6\u683c\u5f0f\u9519\u8bef\uff0c\u4e0a\u4f20\u5931\u8d25\u3002");
            }
            String filePath = this.getPath(request, ext, param);
            File file = new File(this.getProjectRootDirPath(request) + filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            uploadfile.transferTo(file);
            return this.responseData(filePath, 0, "\u4e0a\u4f20\u6210\u529f", response);
        }
        catch (Exception e) {
            logger.error("gok4()--error", (Throwable)e);
            return this.responseErrorData(response, 2, "\u7cfb\u7edf\u7e41\u5fd9\uff0c\u4e0a\u4f20\u5931\u8d25");
        }
    }

    private String getPath(HttpServletRequest request, String ext, String param) {
        String filePath = "/images/upload/";
        filePath = param != null && param.trim().length() > 0 ? filePath + param : filePath + CommonConstants.projectName;
        filePath = filePath + "/" + DateUtils.toString((Date)new Date(), (String)"yyyyMMdd") + "/" + System.currentTimeMillis() + "." + ext;
        return filePath;
    }

    public String responseData(String path, int error, String message, HttpServletResponse response) throws IOException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("url", path);
        map.put("error", error);
        map.put("message", message);
        response.getWriter().write(gson.toJson(map));
        return null;
    }

    public String responseErrorData(HttpServletResponse response, int error, String message) {
        try {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("error", error);
            map.put("message", message);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().print(gson.toJson(map));
            response.getWriter().flush();
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    public void setFileTypeList(String[] type) {
        this.fileTypeList = new ArrayList<String>();
        for (String _t : type) {
            this.fileTypeList.add(_t);
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.inxedu.os.common.controller.BaseController
 *  com.inxedu.os.common.entity.PageEntity
 *  com.inxedu.os.common.util.WebUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.WebDataBinder
 *  org.springframework.web.bind.annotation.InitBinder
 *  org.springframework.web.bind.annotation.ModelAttribute
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.ResponseBody
 *  org.springframework.web.servlet.ModelAndView
 */
package com.inxedu.os.edu.controller.website;

import com.inxedu.os.common.controller.BaseController;
import com.inxedu.os.common.entity.PageEntity;
import com.inxedu.os.common.util.WebUtils;
import com.inxedu.os.edu.entity.website.WebsiteCourse;
import com.inxedu.os.edu.entity.website.WebsiteCourseDetail;
import com.inxedu.os.edu.entity.website.WebsiteCourseDetailDTO;
import com.inxedu.os.edu.service.website.WebsiteCourseDetailService;
import com.inxedu.os.edu.service.website.WebsiteCourseService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value={"/admin"})
public class AdminWebsiteCourseDetailController
extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(AdminWebsiteCourseDetailController.class);
    @Autowired
    private WebsiteCourseDetailService websiteCourseDetailService;
    @Autowired
    private WebsiteCourseService websiteCourseService;

    @InitBinder(value={"dto"})
    public void initBinder(WebDataBinder binder) {
        binder.setFieldDefaultPrefix("dto.");
    }

    @RequestMapping(value={"/detail/list"})
    public ModelAndView queryDetail(HttpServletRequest request, @ModelAttribute(value="dto") WebsiteCourseDetailDTO dto, @ModelAttribute(value="page") PageEntity page) {
        ModelAndView model = new ModelAndView();
        try {
            model.setViewName(AdminWebsiteCourseDetailController.getViewPath((String)"/admin/website/course/websiteCourseDetail_list"));
            List<WebsiteCourseDetailDTO> dtoList = this.websiteCourseDetailService.queryCourseDetailPage(dto, page);
            model.addObject("dtoList", dtoList);
            model.addObject("page", (Object)page);
            List<WebsiteCourse> websiteCourseList = this.websiteCourseService.queryWebsiteCourseList();
            model.addObject("websiteCourseList", websiteCourseList);
            request.getSession().setAttribute("detailListUri", (Object)WebUtils.getServletRequestUriParms((HttpServletRequest)request));
        }
        catch (Exception e) {
            model.setViewName(this.setExceptionRequest(request, e));
            logger.error("queryDetail()---error", (Throwable)e);
        }
        return model;
    }

    @RequestMapping(value={"/detail/deletedeail/{id}"})
    public ModelAndView deleteDetail(HttpServletRequest request, @PathVariable(value="id") int id) {
        ModelAndView model = new ModelAndView();
        try {
            this.websiteCourseDetailService.deleteDetailById(id);
            Object uri = request.getSession().getAttribute("detailListUri");
            if (uri != null) {
                model.setViewName("redirect:" + uri.toString());
            } else {
                model.setViewName("redirect:/admin/detail/list");
            }
        }
        catch (Exception e) {
            model.setViewName(this.setExceptionRequest(request, e));
            logger.error("deleteDetail()---error", (Throwable)e);
        }
        return model;
    }

    @RequestMapping(value={"/detail/updatesort/{id}/{sort}"})
    @ResponseBody
    public Map<String, Object> updateSort(@PathVariable(value="id") int id, @PathVariable(value="sort") int sort) {
        HashMap<String, Object> json = new HashMap();
        try {
            this.websiteCourseDetailService.updateSort(id, sort);
            json = this.setJson(true, null, null);
        }
        catch (Exception e) {
            this.setAjaxException(json);
            logger.error("updateSort()---error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/detail/addrecommendCourse"})
    @ResponseBody
    public Map<String, Object> addrecommendCourse(HttpServletRequest request) {
        Map<String, Object> json = new HashMap<String, Object>();
        try {
            Object uri;
            int count;
            String courseIds = request.getParameter("courseIds");
            int recommendId = request.getParameter("recommendId") == null ? 0 : Integer.parseInt(request.getParameter("recommendId"));
            WebsiteCourse websiteCourse = this.websiteCourseService.queryWebsiteCourseById(recommendId);
            List<WebsiteCourseDetail> detailList = this.websiteCourseDetailService.queryDetailListByrecommendId(recommendId);
            int n = count = detailList == null ? 0 : detailList.size();
            if (courseIds == null || courseIds.trim().length() == 0) {
                json = this.setJson(false, "\u8bf7\u9009\u62e9\u8bfe\u7a0b", null);
                return json;
            }
            String[] courseArr = this.handleCourseId(detailList, courseIds);
            count = courseArr.length + count;
            if (count > websiteCourse.getCourseNum()) {
                json = this.setJson(false, "[" + websiteCourse.getName() + "]\u63a8\u8350\u7c7b\u578b\u6700\u591a\u53ea\u80fd\u6dfb\u52a0[" + websiteCourse.getCourseNum() + "]\u4e2a\u8bfe\u7a0b", null);
                return json;
            }
            if (courseArr.length > 0) {
                StringBuffer val = new StringBuffer();
                for (int i = 0; i < courseArr.length; ++i) {
                    if (i < courseArr.length - 1) {
                        val.append("(0," + recommendId + "," + courseArr[i] + ",0),");
                        continue;
                    }
                    val.append("(0," + recommendId + "," + courseArr[i] + ",0)");
                }
                this.websiteCourseDetailService.createWebsiteCourseDetail(val.toString());
            }
            json = (uri = request.getSession().getAttribute("detailListUri")) != null ? this.setJson(true, null, uri.toString()) : this.setJson(true, null, "/admin/detail/list");
        }
        catch (Exception e) {
            this.setAjaxException(json);
            logger.error("addrecommendCourse()--error", (Throwable)e);
        }
        return json;
    }

    private String[] handleCourseId(List<WebsiteCourseDetail> detailList, String courseIds) {
        if (courseIds.trim().startsWith(",")) {
            courseIds = courseIds.trim().substring(1, courseIds.trim().length());
        }
        if (courseIds.trim().endsWith(",")) {
            courseIds = courseIds.trim().substring(0, courseIds.trim().length() - 1);
        }
        String[] _arr = courseIds.split(",");
        ArrayList<String> arr = new ArrayList<String>();
        if (detailList != null && detailList.size() > 0) {
            for (String id : _arr) {
                boolean _index = false;
                for (WebsiteCourseDetail wcd : detailList) {
                    if (Integer.parseInt(id) != wcd.getCourseId()) continue;
                    _index = true;
                }
                if (_index) continue;
                arr.add(id);
            }
            String[] courseArr = new String[arr.size()];
            arr.toArray(courseArr);
            return courseArr;
        }
        return _arr;
    }
}

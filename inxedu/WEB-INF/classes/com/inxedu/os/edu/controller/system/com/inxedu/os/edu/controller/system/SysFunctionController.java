/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.inxedu.os.common.controller.BaseController
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
package com.inxedu.os.edu.controller.system;

import com.inxedu.os.common.controller.BaseController;
import com.inxedu.os.edu.entity.system.SysFunction;
import com.inxedu.os.edu.service.system.SysFunctionService;
import java.util.Date;
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
@RequestMapping(value={"/admin/sysfunctioin"})
public class SysFunctionController
extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(SysFunctionController.class);
    @Autowired
    private SysFunctionService sysFunctionService;

    @InitBinder(value={"sysFunction"})
    public void initBinderSysFunction(WebDataBinder binder) {
        binder.setFieldDefaultPrefix("sysFunction.");
    }

    @RequestMapping(value={"/showfunctionztree"})
    public ModelAndView showSysFunctonZtree(HttpServletRequest request) {
        ModelAndView model = new ModelAndView();
        try {
            model.setViewName(SysFunctionController.getViewPath((String)"/admin/system/sys-function-list"));
            List<SysFunction> functionList = this.sysFunctionService.queryAllSysFunction();
            if (functionList != null) {
                model.addObject("jsonFunction", (Object)gson.toJson(functionList));
            }
        }
        catch (Exception e) {
            model.setViewName(this.setExceptionRequest(request, e));
            logger.error("showSysFunctonZtree()--error", (Throwable)e);
        }
        return model;
    }

    @RequestMapping(value={"/addfunction"})
    @ResponseBody
    public Map<String, Object> addFunction(@ModelAttribute(value="sysFunction") SysFunction sysFunction) {
        Map<String, Object> json = new HashMap<String, Object>();
        try {
            sysFunction.setCreateTime(new Date());
            sysFunction.setFunctionType(1);
            sysFunction.setFunctionName("\u65b0\u5efa\u7684\u6743\u9650");
            this.sysFunctionService.cresateSysFunction(sysFunction);
            json = this.setJson(true, null, gson.toJson((Object)sysFunction));
        }
        catch (Exception e) {
            this.setAjaxException(json);
            logger.error("addFunction()--error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/updatefunction"})
    @ResponseBody
    public Map<String, Object> udpateFunction(@ModelAttribute(value="sysFunction") SysFunction sysFunction) {
        Map json = new HashMap();
        try {
            if (sysFunction.getFunctionId() <= 0) {
                json = this.setJson(false, "\u8bf7\u9009\u62e9\u8981\u4fee\u6539\u7684\u6743\u9650", null);
                return json;
            }
            if (sysFunction.getFunctionName() == null || sysFunction.getFunctionName().trim().equals("")) {
                json = this.setJson(false, "\u8bf7\u586b\u5199\u6743\u9650\u540d!", null);
                return json;
            }
            this.sysFunctionService.updateFunction(sysFunction);
            json = this.setJson(true, "\u4fdd\u5b58\u6210\u529f", sysFunction);
        }
        catch (Exception e) {
            this.setAjaxException(json);
            logger.error("udpateFunction()---error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/updateparentid/{parentId}/{functionId}"})
    @ResponseBody
    public Map<String, Object> updateParnetid(@PathVariable(value="parentId") int parentId, @PathVariable(value="functionId") int functionId) {
        HashMap<String, Object> json = new HashMap();
        try {
            this.sysFunctionService.updateFunctionParentId(parentId, functionId);
            json = this.setJson(true, "\u64cd\u4f5c\u6210\u529f", null);
        }
        catch (Exception e) {
            this.setAjaxException(json);
            logger.error("updateParnetid()--error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/deletefunction/{ids}"})
    @ResponseBody
    public Map<String, Object> deleteFunction(@PathVariable(value="ids") String ids) {
        HashMap<String, Object> json = new HashMap();
        try {
            this.sysFunctionService.deleteFunctionByIds(ids);
            json = this.setJson(true, null, null);
        }
        catch (Exception e) {
            this.setAjaxException(json);
            logger.error("deleteFunction()--error", (Throwable)e);
        }
        return json;
    }
}

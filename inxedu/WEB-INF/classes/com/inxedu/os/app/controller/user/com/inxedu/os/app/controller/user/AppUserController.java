/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.inxedu.os.common.cache.EHCacheUtil
 *  com.inxedu.os.common.controller.BaseController
 *  com.inxedu.os.common.entity.PageEntity
 *  com.inxedu.os.common.util.MD5
 *  com.inxedu.os.common.util.StringUtils
 *  com.inxedu.os.common.util.WebUtils
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.ModelAttribute
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.ResponseBody
 */
package com.inxedu.os.app.controller.user;

import com.inxedu.os.common.cache.EHCacheUtil;
import com.inxedu.os.common.constants.CacheConstans;
import com.inxedu.os.common.controller.BaseController;
import com.inxedu.os.common.entity.PageEntity;
import com.inxedu.os.common.util.MD5;
import com.inxedu.os.common.util.StringUtils;
import com.inxedu.os.common.util.WebUtils;
import com.inxedu.os.edu.constants.enums.WebSiteProfileType;
import com.inxedu.os.edu.entity.course.CourseDto;
import com.inxedu.os.edu.entity.course.CourseFavorites;
import com.inxedu.os.edu.entity.course.FavouriteCourseDTO;
import com.inxedu.os.edu.entity.user.User;
import com.inxedu.os.edu.entity.user.UserLoginLog;
import com.inxedu.os.edu.service.course.CourseFavoritesService;
import com.inxedu.os.edu.service.course.CourseService;
import com.inxedu.os.edu.service.letter.MsgReceiveService;
import com.inxedu.os.edu.service.user.UserLoginLogService;
import com.inxedu.os.edu.service.user.UserService;
import com.inxedu.os.edu.service.website.WebsiteProfileService;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value={"/webapp"})
public class AppUserController
extends BaseController {
    private static Logger logger = Logger.getLogger(AppUserController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private UserLoginLogService userLoginLogService;
    @Autowired
    private WebsiteProfileService websiteProfileService;
    @Autowired
    private MsgReceiveService msgReceiveService;
    @Autowired
    private CourseFavoritesService courseFavoritesService;
    @Autowired
    private CourseService courseService;

    @RequestMapping(value={"/login"})
    @ResponseBody
    public Map<String, Object> userLogin(HttpServletRequest request, HttpServletResponse response) {
        Map json = new HashMap();
        try {
            String account = request.getParameter("account");
            String password = request.getParameter("password");
            if (!StringUtils.isNotEmpty((String)account)) {
                json = this.setJson(false, "\u8bf7\u8f93\u5165\u767b\u5f55\u5e10\u53f7", null);
                return json;
            }
            if (!StringUtils.isNotEmpty((String)password)) {
                json = this.setJson(false, "\u8bf7\u8f93\u5165\u767b\u5f55\u5bc6\u7801", null);
                return json;
            }
            User user = this.userService.getLoginUser(account, MD5.getMD5((String)password));
            if (user == null) {
                json = this.setJson(false, "\u5e10\u53f7\u6216\u5bc6\u7801\u9519\u8bef", null);
                return json;
            }
            EHCacheUtil.remove((String)(CacheConstans.WEB_USER_LOGIN_PREFIX + user.getUserId()));
            if (user.getIsavalible() == 2) {
                json = this.setJson(false, "\u5e10\u53f7\u5df2\u88ab\u7981\u7528", null);
                return json;
            }
            UserLoginLog loginLog = new UserLoginLog();
            loginLog.setIp(WebUtils.getIpAddr((HttpServletRequest)request));
            loginLog.setLoginTime(new Date());
            String userAgent = WebUtils.getUserAgent((HttpServletRequest)request);
            if (StringUtils.isNotEmpty((String)userAgent)) {
                loginLog.setOsName(userAgent.split(";")[1]);
                loginLog.setUserAgent(userAgent.split(";")[0]);
            }
            loginLog.setUserId(user.getUserId());
            this.userLoginLogService.createLoginLog(loginLog);
            json = this.setJson(true, "\u767b\u5f55\u6210\u529f", user);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"userLogin()--error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/createuser"})
    @ResponseBody
    public Map<String, Object> createUser(HttpServletRequest request, HttpServletResponse response) {
        Map json = new HashMap();
        try {
            String email = request.getParameter("email");
            String mobile = request.getParameter("mobile");
            String password = request.getParameter("password");
            String confirmPwd = request.getParameter("confirmPwd");
            if (email == null || email.trim().length() == 0 || !WebUtils.checkEmail((String)email, (int)50)) {
                json = this.setJson(false, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u53f7", null);
                return json;
            }
            if (this.userService.checkEmail(email.trim())) {
                json = this.setJson(false, "\u8be5\u90ae\u7bb1\u53f7\u5df2\u88ab\u4f7f\u7528", null);
                return json;
            }
            if (mobile == null || mobile.trim().length() == 0 || !WebUtils.checkMobile((String)mobile)) {
                json = this.setJson(false, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7", null);
                return json;
            }
            if (this.userService.checkMobile(mobile)) {
                json = this.setJson(false, "\u8be5\u624b\u673a\u53f7\u5df2\u88ab\u4f7f\u7528", null);
                return json;
            }
            if (password == null || password.trim().length() == 0 || !WebUtils.isPasswordAvailable((String)password)) {
                json = this.setJson(false, "\u5bc6\u7801\u6709\u5b57\u6bcd\u548c\u6570\u5b57\u7ec4\u5408\u4e14\u22656\u4f4d\u226416\u4f4d", null);
                return json;
            }
            if (!password.equals(confirmPwd)) {
                json = this.setJson(false, "\u4e24\u6b21\u5bc6\u7801\u4e0d\u4e00\u81f4", null);
                return json;
            }
            User user = new User();
            user.setEmail(email);
            user.setMobile(mobile);
            user.setPassword(password);
            user.setCreateTime(new Date());
            user.setIsavalible(1);
            user.setPassword(MD5.getMD5((String)user.getPassword()));
            user.setMsgNum(0);
            user.setSysMsgNum(0);
            user.setLastSystemTime(new Date());
            this.userService.createUser(user);
            request.getSession().removeAttribute("COMMON_RAND_CODE");
            json = this.setJson(true, "\u6ce8\u518c\u6210\u529f", user);
            Map<String, Object> websitemap = this.websiteProfileService.getWebsiteProfileByType(WebSiteProfileType.web.toString());
            Map web = (Map)websitemap.get("web");
            String company = web.get("company").toString();
            String conent = "\u6b22\u8fce\u6765\u5230" + company + ",\u5e0c\u671b\u60a8\u80fd\u591f\u5feb\u4e50\u7684\u5b66\u4e60";
            this.msgReceiveService.addSystemMessageByCusId(conent, Long.valueOf(user.getUserId()));
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"createUser()--eror", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/queryUserById"})
    @ResponseBody
    public Map<String, Object> queryUserById(HttpServletRequest request) {
        Map json = new HashMap();
        try {
            String id = request.getParameter("id");
            if (id == null || id.equals("")) {
                json = this.setJson(false, "\u7528\u6237Id\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            User user = this.userService.queryUserById(Integer.parseInt(id));
            json = this.setJson(true, "\u83b7\u53d6\u7528\u6237\u6210\u529f", user);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"queryUserById()--eror", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/front/createfavorites"})
    @ResponseBody
    public Map<String, Object> createFavorites(HttpServletRequest request) {
        Map json = new HashMap();
        try {
            String userId = request.getParameter("userId");
            if (userId == null || userId.trim().equals("")) {
                json = this.setJson(false, "\u7528\u6237Id\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            String courseId = request.getParameter("courseId");
            if (courseId == null || courseId.trim().equals("")) {
                json = this.setJson(false, "\u8bfe\u7a0bId\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            boolean is = this.courseFavoritesService.checkFavorites(Integer.parseInt(userId), Integer.parseInt(courseId));
            if (is) {
                json = this.setJson(false, "\u8be5\u8bfe\u7a0b\u4f60\u5df2\u7ecf\u6536\u85cf\u8fc7\u4e86\uff01", null);
                return json;
            }
            CourseFavorites courseFavorites = new CourseFavorites();
            courseFavorites.setCourseId(Integer.parseInt(courseId));
            courseFavorites.setUserId(Integer.parseInt(userId));
            courseFavorites.setAddTime(new Date());
            this.courseFavoritesService.createCourseFavorites(courseFavorites);
            json = this.setJson(true, "\u6536\u85cf\u6210\u529f", null);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"createFavorites()--error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/deleteFaveorite"})
    @ResponseBody
    public Map<String, Object> deleteFavorite(HttpServletRequest request) {
        Map json = new HashMap();
        try {
            String id = request.getParameter("id");
            if (id == null || id.trim().equals("")) {
                json = this.setJson(false, "id\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            this.courseFavoritesService.deleteCourseFavoritesById(id);
            json = this.setJson(true, "\u53d6\u6d88\u6536\u85cf\u6210\u529f", null);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"deleteFavorite()---error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/myCourse"})
    @ResponseBody
    public Map<String, Object> myCourse(HttpServletRequest request, @ModelAttribute(value="page") PageEntity page) {
        Map json = new HashMap();
        try {
            String userId;
            String currentPage = request.getParameter("currentPage");
            if (currentPage == null || currentPage.trim().equals("")) {
                json = this.setJson(false, "\u9875\u7801\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            page.setCurrentPage(Integer.parseInt(currentPage));
            page.setPageSize(10);
            String pageSize = request.getParameter("pageSize");
            if (pageSize != null) {
                page.setPageSize(Integer.parseInt(pageSize));
            }
            if ((userId = request.getParameter("userId")) == null || userId.trim().equals("")) {
                json = this.setJson(false, "\u7528\u6237Id\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            List<CourseDto> courseList = this.courseService.queryMyCourseList(Integer.parseInt(userId), page);
            json = this.setJson(true, "\u67e5\u8be2\u6210\u529f--\u6211\u7684\u8bfe\u7a0b", courseList);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"myCourse()---error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/myFavorites"})
    @ResponseBody
    public Map<String, Object> myFavorites(HttpServletRequest request, @ModelAttribute(value="page") PageEntity page) {
        Map json = new HashMap();
        try {
            String userId;
            String currentPage = request.getParameter("currentPage");
            if (currentPage == null || currentPage.trim().equals("")) {
                json = this.setJson(false, "\u9875\u7801\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            page.setCurrentPage(Integer.parseInt(currentPage));
            page.setPageSize(10);
            String pageSize = request.getParameter("pageSize");
            if (pageSize != null) {
                page.setPageSize(Integer.parseInt(pageSize));
            }
            if ((userId = request.getParameter("userId")) == null || userId.trim().equals("")) {
                json = this.setJson(false, "\u7528\u6237Id\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            List<FavouriteCourseDTO> favoriteList = this.courseFavoritesService.queryFavoritesPage(Integer.parseInt(userId), page);
            json = this.setJson(true, "\u67e5\u8be2\u6210\u529f--\u6211\u7684\u6536\u85cf\u8bfe\u7a0b\u5217\u8868", favoriteList);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"myFavorites()---error", (Throwable)e);
        }
        return json;
    }

    @RequestMapping(value={"/updateUser"})
    @ResponseBody
    public Map<String, Object> updateUserInfo(HttpServletRequest request) {
        Map json = new HashMap();
        try {
            String userName = request.getParameter("userName");
            if (userName == null || userName.trim().equals("")) {
                json = this.setJson(false, "\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            String showName = request.getParameter("showName");
            if (showName == null || showName.trim().equals("")) {
                json = this.setJson(false, "\u6635\u79f0\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            String sex = request.getParameter("sex");
            if (sex == null || sex.trim().equals("")) {
                json = this.setJson(false, "\u6027\u522b\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            String age = request.getParameter("age");
            if (age == null || age.trim().equals("")) {
                json = this.setJson(false, "\u5e74\u9f84\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            String userId = request.getParameter("userId");
            if (userId == null || userId.trim().equals("")) {
                json = this.setJson(false, "\u7528\u6237Id\u4e0d\u80fd\u4e3a\u7a7a", null);
                return json;
            }
            User user = new User();
            user.setUserId(Integer.parseInt(userId));
            user.setUserName(userName);
            user.setShowName(showName);
            user.setSex(Integer.parseInt(sex));
            user.setAge(Integer.parseInt(age));
            this.userService.updateUser(user);
            json = this.setJson(true, "\u4fee\u6539\u6210\u529f", user);
        }
        catch (Exception e) {
            json = this.setJson(false, "\u5f02\u5e38", null);
            logger.error((Object)"updateUserInfo()---error", (Throwable)e);
        }
        return json;
    }
}

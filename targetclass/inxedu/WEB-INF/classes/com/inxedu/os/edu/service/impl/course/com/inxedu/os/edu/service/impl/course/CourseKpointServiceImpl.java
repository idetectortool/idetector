/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.inxedu.os.edu.service.impl.course;

import com.inxedu.os.edu.dao.course.CourseKpointDao;
import com.inxedu.os.edu.entity.kpoint.CourseKpoint;
import com.inxedu.os.edu.entity.kpoint.CourseKpointDto;
import com.inxedu.os.edu.service.course.CourseKpointService;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(value="courseKpointService")
public class CourseKpointServiceImpl
implements CourseKpointService {
    @Autowired
    private CourseKpointDao courseKpointDao;

    @Override
    public int addCourseKpoint(CourseKpoint courseKpoint) {
        return this.courseKpointDao.addCourseKpoint(courseKpoint);
    }

    @Override
    public List<CourseKpoint> queryCourseKpointByCourseId(int courseId) {
        return this.courseKpointDao.queryCourseKpointByCourseId(courseId);
    }

    @Override
    public CourseKpointDto queryCourseKpointById(int kpointId) {
        return this.courseKpointDao.queryCourseKpointById(kpointId);
    }

    @Override
    public void updateKpoint(CourseKpoint kpoint) {
        this.courseKpointDao.updateKpoint(kpoint);
    }

    @Override
    public void deleteKpointByIds(String ids) {
        if (ids != null && ids.trim().length() > 0) {
            if (ids.trim().endsWith(",")) {
                ids = ids.trim().substring(0, ids.trim().length() - 1);
            }
            this.courseKpointDao.deleteKpointByIds(ids);
        }
    }

    @Override
    public void updateKpointParentId(int kpointId, int parentId) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("kpointId", kpointId);
        map.put("parentId", parentId);
        this.courseKpointDao.updateKpointParentId(map);
    }

    @Override
    public int getSecondLevelKpointCount(Long courseId) {
        return this.courseKpointDao.getSecondLevelKpointCount(courseId);
    }
}

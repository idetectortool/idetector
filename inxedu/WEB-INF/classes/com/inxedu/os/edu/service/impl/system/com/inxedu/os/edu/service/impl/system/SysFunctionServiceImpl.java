/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.inxedu.os.edu.service.impl.system;

import com.inxedu.os.edu.dao.system.SysFunctionDao;
import com.inxedu.os.edu.entity.system.SysFunction;
import com.inxedu.os.edu.service.system.SysFunctionService;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(value="sysFunctionService")
public class SysFunctionServiceImpl
implements SysFunctionService {
    @Autowired
    private SysFunctionDao sysFunctionDao;

    @Override
    public List<SysFunction> queryAllSysFunction() {
        return this.sysFunctionDao.queryAllSysFunction();
    }

    @Override
    public int cresateSysFunction(SysFunction sysFunction) {
        return this.sysFunctionDao.cresateSysFunction(sysFunction);
    }

    @Override
    public void updateFunction(SysFunction sysFunction) {
        this.sysFunctionDao.updateFunction(sysFunction);
    }

    @Override
    public void updateFunctionParentId(int parentId, int functionId) {
        HashMap<String, Object> paramrs = new HashMap<String, Object>();
        paramrs.put("parentId", parentId);
        paramrs.put("functionId", functionId);
        this.sysFunctionDao.updateFunctionParentId(paramrs);
    }

    @Override
    public void deleteFunctionByIds(String ids) {
        if (ids != null && ids.trim().length() > 0) {
            if (ids.trim().endsWith(",")) {
                ids = ids.trim().substring(0, ids.trim().length() - 1);
            }
            this.sysFunctionDao.deleteFunctionByIds(ids);
        }
    }

    @Override
    public List<SysFunction> querySysUserFunction(int userId) {
        return this.sysFunctionDao.querySysUserFunction(userId);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.inxedu.os.edu.service.impl.system;

import com.inxedu.os.edu.dao.system.SysRoleDao;
import com.inxedu.os.edu.entity.system.SysRole;
import com.inxedu.os.edu.service.system.SysRoleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(value="sysRoleService")
public class SysRoleServiceImpl
implements SysRoleService {
    @Autowired
    private SysRoleDao sysRoleDao;

    @Override
    public int createRoel(SysRole sysRole) {
        return this.sysRoleDao.createRoel(sysRole);
    }

    @Override
    public void updateRole(SysRole sysRole) {
        this.sysRoleDao.updateRole(sysRole);
    }

    @Override
    public List<SysRole> queryAllRoleList() {
        return this.sysRoleDao.queryAllRoleList();
    }

    @Override
    public void deleteRoleByIds(String ids) {
        if (ids != null && ids.trim().length() > 0) {
            if (ids.trim().endsWith(",")) {
                ids = ids.trim().substring(0, ids.trim().length() - 1);
            }
            this.sysRoleDao.deleteRoleByIds(ids);
        }
    }

    @Override
    public void deleteRoleFunctionByRoleId(int roleId) {
        this.sysRoleDao.deleteRoleFunctionByRoleId(roleId);
    }

    @Override
    public void createRoleFunction(String value) {
        if (value != null && value.trim().length() > 0) {
            if (value.endsWith(",")) {
                value = value.trim().substring(0, value.trim().length() - 1);
            }
            this.sysRoleDao.createRoleFunction(value);
        }
    }

    @Override
    public List<Integer> queryRoleFunctionIdByRoleId(int roleId) {
        return this.sysRoleDao.queryRoleFunctionIdByRoleId(roleId);
    }
}

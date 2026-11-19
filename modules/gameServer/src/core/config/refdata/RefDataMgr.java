/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.config.refdata;

import com.ddm.server.common.data.AbstractRefDataMgr;
import core.config.refdata.ref.RefBaseGame;
import core.config.refdata.ref.RefSignIn;

import java.io.File;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public class RefDataMgr extends AbstractRefDataMgr {

    private static final String Defaule_RefData_Path = "data" + File.separatorChar + "refData";
    private String _refPath = System.getProperty("GameServer.RefPath", Defaule_RefData_Path);

    private static RefDataMgr _instance = new RefDataMgr();

    public static RefDataMgr getInstance() {
        return _instance;
    }

    @Override
    protected void onCustomLoad() {
        RefSignIn.Reset();
        load(RefBaseGame.class);
    }

    @Override
    protected boolean onCustomLoad(String reloadName) {
        if ("signin".equals(reloadName.toLowerCase())) {
            RefSignIn.Reset();
        }
        return load(RefBaseGame.class, reloadName);
    }

    @Override
    public String getRefPath() {
        return _refPath;
    }

    @Override
    protected boolean assertAll() {
        return true;
    }


}

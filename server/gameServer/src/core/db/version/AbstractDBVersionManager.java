package core.db.version;

import com.ddm.server.common.CommLogD;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * abstract class, the basic function of DB version manager and automatic
 * updates
 *
 * @author Aaron
 */
public abstract class AbstractDBVersionManager {

    protected Map<String, IUpdateDBVersion> m_vesionUpdateDict;

    protected AbstractDBVersionManager() {
        m_vesionUpdateDict = new ConcurrentHashMap<>();
    }

    public abstract String getCurVersion();

    protected abstract boolean _setCurVersion(String version);

    public abstract String getNewestVersion();

    public abstract String getSourceName();

    public abstract String getCatalog();

    protected abstract boolean initCurrentVersion();

    public abstract Connection getConnection();

    public boolean regVersionUpdate(IUpdateDBVersion versionUpdataObj) {
        String version = versionUpdataObj.getRequestVersion();

        if (m_vesionUpdateDict.containsKey(version) == false) {
            m_vesionUpdateDict.put(version, versionUpdataObj);
            return true;
        } else {
            CommLogD.error("RegVersionUpdate: version=[" + version + "] has registered!!!");
            return false;
        }
    }

    protected boolean _updateContent() {
        String curVersion = this.getCurVersion();

        while (m_vesionUpdateDict.containsKey(curVersion)) {
            IUpdateDBVersion versionUpdataObj = m_vesionUpdateDict.get(curVersion);

            if (versionUpdataObj.run() == false) {
                CommLogD.error("AutoUpdate: version=[" + curVersion + "] update failed!!!");
                return false;
            }

            String targetVersion = versionUpdataObj.getTargetVersion();
            if (this._setCurVersion(targetVersion) == false) {
                CommLogD.error("AutoUpdate: set db version=[" + targetVersion + "] failed!!!");
                return false;
            }

            CommLogD.info("AutoUpdate: update from [" + curVersion + "] to [" + targetVersion + "]");
            curVersion = targetVersion;
        }

        String newestVersion = this.getNewestVersion();
        if (curVersion.equals(newestVersion)) {
            CommLogD.info("AutoUpdate: [" + this.getCatalog() + "] successfully update to the version=[" + newestVersion + "]");
            return true;
        } else {
            CommLogD.error("AutoUpdate: [" + this.getCatalog() + "] failed by version current=[" + curVersion + "] is not newest=[" + newestVersion
                    + "]!!!");

            return false;
        }
    }

    protected boolean _notNeedUpdate() {
        CommLogD.info("AutoUpdate: [" + this.getCatalog() + "] current version=[" + this.getNewestVersion() + "] is the newest.");
        return true;
    }

    protected Boolean run() {
        boolean isOk = this.initCurrentVersion();
        if (isOk) {
            if (this.getCurVersion().equalsIgnoreCase(this.getNewestVersion())) {
                isOk = this._notNeedUpdate();
            } else {
                isOk = this._updateContent();
            }
        }

        return isOk;
    }

}

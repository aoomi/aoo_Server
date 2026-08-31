package core.db.other;

import core.db.version.IUpdateDBVersion;

public class UpdateBase implements IUpdateDBVersion {

    @Override
    public String getRequestVersion() {
        String name = this.getClass().getSimpleName();
        String version = name.split("_To_")[0].replace("Update_", "");
        return version.replace("_", ".");
    }

    @Override
    public String getTargetVersion() {
        String name = this.getClass().getSimpleName();
        String version = name.split("_To_")[1];
        return version.replace("_", ".");
    }

    @Override
    public boolean run() {
        return true;
    }
}

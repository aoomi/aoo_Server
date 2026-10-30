package core.db.version;

/**
 * interfac
 *
 * @author Aaron
 */
public interface IUpdateDBVersion {

    /**
     * get the version for updating requested
     *
     * @return
     */
    String getRequestVersion();

    /**
     * get the version after updated
     *
     * @return
     */
    String getTargetVersion();

    /**
     * do logic for updating db structure
     *
     * @return true if updating successfully, else false
     */
    boolean run();

}

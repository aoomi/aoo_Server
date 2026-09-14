package core.db.entity;

import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import core.db.dao.clarkGame.IBaseClarkGameDao;
import core.db.dao.clarkLog.IBaseClarkLogDao;
import core.db.dao.dbZle.IBaseDbZleDao;
import core.db.persistence.Repository;
import core.db.service.BaseService;
import core.db.service.clarkLog.BaseLogWriterService;
import core.ioc.ContainerMgr;

import java.io.Serializable;

public abstract class BaseEntity<T> implements Serializable {

    public String getTableName() {
        TableName tableName = this.getClass().getAnnotation(TableName.class);
        TableName.DbDayEnum dayEnum = tableName.dbDay();
        if (TableName.DbDayEnum.NOT_DAY.equals(dayEnum)) {
            return tableName.value();
        } else if (TableName.DbDayEnum.EVERY_DAY.equals(dayEnum)) {
            return tableName.value() + CommTime.getNowTimeYMD();
        } else if (TableName.DbDayEnum.NEXT_DAY.equals(dayEnum)) {
            return tableName.value() + CommTime.getNextTimeYMD();
        } else if (TableName.DbDayEnum.BEFORE_DAY.equals(dayEnum)) {
            return tableName.value() + CommTime.getBeforeTimeYMD();
        } else if (TableName.DbDayEnum.EVERY_6DAY.equals(dayEnum)) {
            return tableName.value() + CommTime.getCycleNowTime6YMD();
        } else if (TableName.DbDayEnum.NEXT_6DAY.equals(dayEnum)) {
            return tableName.value() + CommTime.getNextTime6YMD();
        } else if (TableName.DbDayEnum.BEFORE_6DAY.equals(dayEnum)) {
            return tableName.value() + CommTime.getBeforeTime6YMD();

        }
        return tableName.value();
    }

    public BaseService<T> getBaseService() {
        return ContainerMgr.get().getComponent(this.getClass().getSimpleName() + "Service");
    }

    public Repository<?> getBaseDao() {
        Repository<T> repository = ContainerMgr.get().getDao(this.getClass().getSimpleName() + "Dao");
        if (repository == null) {
            String[] packageNames = this.getClass().getPackage().getName().split("\\.");
            String packageName = packageNames[packageNames.length - 1];
            switch (packageName.trim()) {
                case "dbZle":
                    return ContainerMgr.get().getDao(IBaseDbZleDao.class);
                case "clarkLog":
                    return ContainerMgr.get().getDao(IBaseClarkLogDao.class);
                case "clarkGame":
                    return ContainerMgr.get().getDao(IBaseClarkGameDao.class);
                default:
                    return ContainerMgr.get().getDao(IBaseClarkGameDao.class);
            }
        }
        return repository;
    }

    public BaseService getLogBaseService() {
        return ContainerMgr.get().getComponent(BaseLogWriterService.class);
    }

}

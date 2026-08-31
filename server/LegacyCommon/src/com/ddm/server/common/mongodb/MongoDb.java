package com.ddm.server.common.mongodb;

import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MongoDb {
    Document doc();

    CompoundIndexes indexes();

}

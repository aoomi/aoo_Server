package com.ddm.server.common.utils;

import org.apache.commons.lang3.StringUtils;

public class TypeUtils {

    public static int longTypeInt(long typeLong) {
        int typeInt = 0;
        try {
            typeInt = (int) typeLong;
        } catch (NumberFormatException e) {
            CommLogD.error("longTypeInt[{}] typeLong:{}", e.getMessage(), typeLong, e);
        } catch (Exception e) {
            CommLogD.error("longTypeInt[{}]", e.getMessage(), e);
        }
        return typeInt;
    }

    public static long StringTypeLong(String typeStr) {
        long typelong = 0L;
        if (StringUtils.isNumeric(typeStr)) {
            typelong = Long.parseLong(typeStr);
        }
        return typelong;
    }

    public static int StringTypeInt(String typeStr) {
        int typeIni = 0;
        if (StringUtils.isNumeric(typeStr)) {
            typeIni = Integer.parseInt(typeStr);
        }
        return typeIni;
    }


}

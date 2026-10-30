package com.ddm.server.common.utils;

import java.security.SecureRandom;

import java.util.Random;

import java.util.concurrent.ThreadLocalRandom;

public class UtilId {
/**

* 生成16位不重复的随机数，含数字+大小写

* @return

*/

public static String getGUID() {
StringBuilder uid = new StringBuilder();

//产生16位的强随机数

Random rd = new SecureRandom();

for (int i = 0; i < 16; i++) {
//产生0-2的3位随机数

int type = rd.nextInt(3);

switch (type){
case 0:

//0-9的随机数

uid.append(rd.nextInt(10));

/*int random = ThreadLocalRandom.current().ints(0, 10)

.distinct().limit(1).findFirst().getAsInt();*/

break;

case 1:

//ASCII在65-90之间为大写,获取大写随机

uid.append((char)(rd.nextInt(25)+65));

break;

case 2:

//ASCII在97-122之间为小写，获取小写随机

uid.append((char)(rd.nextInt(25)+97));

break;

default:

break;

}

}

return uid.toString();

}

    public static void main(String[] args) {
        System.out.println(UtilId.getGUID());
    }

}


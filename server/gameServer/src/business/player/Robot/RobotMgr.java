package business.player.Robot;

import BaseCommon.CommLog;
import business.global.room.GoldRoomMgr;
import business.global.room.base.AbsBaseRoom;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.task.ScheduledExecutorServiceMgr;
import com.ddm.server.common.utils.CommMath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RobotMgr {

    private static RobotMgr instance = new RobotMgr();
    private Lock _lock = new ReentrantLock();// 锁对象

    HashMap<Integer, Robot> availableLst = new HashMap<Integer, Robot>();
    HashMap<Integer, Robot> onlineLst = new HashMap<Integer, Robot>();
    int min_entertime = 1;
    int max_entertime = 50;
    int min_entercnt = 1;
    int max_entercnt = 1;
    int mintime = 0;
    int maxtime = 3;
    int mincoin = 10000;
    int maxcoin = 1000000;

    public final int limitID = 69000000;

    public static RobotMgr getInstance() {
        return instance;
    }

    public void init() {
        loadRobots("robots.txt");
        Random random = new Random();
        ScheduledExecutorServiceMgr.getInstance().getScheduledFuture(() -> {

            try {
                List<AbsBaseRoom> querys = GoldRoomMgr.getInstance().queryExistEmptyPos();
                if (null != querys) {
                    for (AbsBaseRoom room : querys) {
                        if (null == room) {
                            continue;
                        }
                        // 房间里面的空位
                        int emptyPosSize = room.getRoomPosMgr().getEmptyPosCount();
                        if (0 < emptyPosSize) {
                            int s = random.nextInt(min_entercnt, max_entercnt + 1);
                            s = s < emptyPosSize ? s : emptyPosSize;
                            for (int i = 0; i < s; i++) {
                                Robot rber = getRobot();
                                if (null != rber) {
                                    rber.setGold(
                                            CommMath.randomInt(room.getBaseRoomConfigure().getRobotRoomCfg().getMin(),
                                                    room.getBaseRoomConfigure().getRobotRoomCfg().getMax()));
                                    GoldRoomMgr.getInstance().findAndEnter(room, rber.getPid(), true);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                CommLog.error("[RobotMgr] error:{}", e.getMessage(), e);
            }


        }, 0, 2000);
    }

    public boolean isRobot(Integer pid) {
        if (onlineLst.containsKey(pid)) {
            return true;
        }
        return false;
    }

    public Robot getRobot(int pid) {
        if (this.onlineLst.containsKey(pid)) {
            return this.onlineLst.get(pid);
        }
        return null;
    }

    // ===========================================================================================
    // ===========================================================================================
    // ===========================================================================================
    // ===========================================================================================

    // 载入机器人
    private void loadRobots(String filePath) {
        try {
            String encoding = "GBK";
            File file = new File(filePath);
            if (file.isFile() && file.exists()) { // 判断文件是否存在
                try (InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                     BufferedReader bufferedReader = new BufferedReader(read)) {
                    String lineTxt = null;
                    int num = 69001000;

                    while ((lineTxt = bufferedReader.readLine()) != null) {
                        String[] aa = lineTxt.split(";");
                        if (lineTxt.contains("entertime")) {
                            min_entertime = Integer.parseInt(aa[1]);
                            max_entertime = Integer.parseInt(aa[2]);
                        } else if (lineTxt.contains("entercount")) {
                            min_entercnt = Integer.parseInt(aa[1]);
                            max_entercnt = Integer.parseInt(aa[2]);
                        } else if (lineTxt.contains("thinktime")) {
                            mintime = Integer.parseInt(aa[1]);
                            maxtime = Integer.parseInt(aa[2]);
                        } else if (lineTxt.contains("coin")) {
                            mincoin = Integer.parseInt(aa[1]);
                            maxcoin = Integer.parseInt(aa[2]);
                        } else {
                            if (aa.length >= 2) {
                                availableLst.put(num, new Robot(num, aa));
                                num++;
                            }
                        }
                    }
                    CommLogD.info("机器人加载完成！{}", this.availableLst.size());
                } catch (Exception e) {
                    CommLogD.error("loadRobots：" + e.getMessage());
                }
            } else {
                CommLogD.warn("找不到机器人文件: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            CommLogD.error("机器人读取出错", e);
        }
    }

    private Robot getRobot() {
        if (this.availableLst.size() > 0) {
            Robot randomValue = null;
            try {
                this._lock.lock();
                Integer[] keys = availableLst.keySet().toArray(new Integer[0]);
                Integer randomKey = keys[java.util.concurrent.ThreadLocalRandom.current().nextInt(keys.length)];
                randomValue = this.availableLst.remove(randomKey);// availableLst.get(randomKey);
                onlineLst.put(randomKey, randomValue);
            } finally {
                this._lock.unlock();
            }
            return randomValue;

        }
        return null;
    }

    public void freeRobot(int pid) {
        if (this.onlineLst.containsKey(pid)) {
            try {
                this._lock.lock();
                Robot rb = this.onlineLst.remove(pid);
                this.availableLst.put(pid, rb);
            } finally {
                this._lock.unlock();
            }
        }
    }

    public int getThinkTime() {
        return CommMath.randomInt(1000, 3000);
    }

}

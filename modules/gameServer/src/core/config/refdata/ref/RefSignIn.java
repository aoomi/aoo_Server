package core.config.refdata.ref;

import cenum.SignInType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RefSignIn extends RefBaseGame {
    @RefField(iskey = true)
    public int id; // 唯一id
    public int Day; // 天数
    public SignInType Type; // 签到类型
    public String UniformitemId; // 奖励ID
    public String Count; // 奖励数量


    @RefField(isfield = false)
    public static int DailySignInMaxDay = 1;
    @RefField(isfield = false)
    public static int DailySignInMinDay = 1;
    @RefField(isfield = false)
    public static int DailySignInCount = 0;
    @RefField(isfield = false)
    public static int LoginInMaxDay = 1;//最大的登录天数
    @RefField(isfield = false)
    public static int SignInPrizeMaxDay = 1;

    public static void Reset() {
        DailySignInMaxDay = 1;
        DailySignInMinDay = 1;
        DailySignInCount = 0;
        LoginInMaxDay = 1;
        SignInPrizeMaxDay = 1;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean Assert() {
        if (id % 1000 != Day) {
            CommLogD.error("[DailySignIn]数据出错,ID%1000要等于day");
            return false;
        }
        if (Type == SignInType.SignIn) {
            DailySignInCount++;
            DailySignInMaxDay = Day > DailySignInMaxDay ? Day : DailySignInMaxDay;
            DailySignInMinDay = Day < DailySignInMinDay ? Day : DailySignInMinDay;
        }

        if (Type == SignInType.LoginIn) {
            LoginInMaxDay = Day > LoginInMaxDay ? Day : LoginInMaxDay;
        }
        return true;
    }

    public List<Integer> getUniformitemIdList() {
        return Arrays.stream(StringUtils.split(UniformitemId, ","))
                .filter(id -> StringUtils.isNumeric(id))
                .map(id -> Integer.parseInt(id.trim()))
                .collect(Collectors.toList());
    }

    public List<Integer> getCountList() {
        return Arrays.stream(StringUtils.split(Count, ","))
                .filter(id -> StringUtils.isNumeric(id))
                .map(id -> Integer.parseInt(id.trim()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean AssertAll(RefContainer<?> all) {
        if (DailySignInMinDay != 1) {

            CommLogD.error("[RefSignIn表DailySignIn]签到天数最小为1 DailySignInMinDay:{}", DailySignInMinDay);
            return false;
        }


        if (DailySignInCount == 0) {
            CommLogD.error("[RefSignIn表DailySignIn]缺少数据,签到的记录数目为0 DailySignInCount:{}", DailySignInCount);
            return false;
        }

        if (DailySignInMaxDay != DailySignInCount) {
            CommLogD.error("[RefSignIn表DailySignIn]活动数目出错,天数出错 DailySignInMaxDay:{},DailySignInCount{}", DailySignInMaxDay, DailySignInCount);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RefSignIn{" +
                "id=" + id +
                ", Day=" + Day +
                ", Type=" + Type +
                ", UniformitemId='" + UniformitemId + '\'' +
                ", Count='" + Count + '\'' +
                '}';
    }
}

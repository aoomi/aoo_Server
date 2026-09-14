package jsproto.c2s.cclass.redactivity;

public class RedActivity {

//	/**
//	 * 活动状态
//	 *活动状态  1:未开始 2：进行中 3：活动已关闭或活动时间已结束
//	 */
//	public enum HongBaoStatus {
//		None(0),
//	    /**
//	     * 1:未开始
//	     */
//	    UnStart(1),
//	    /**
//	     * 2：进行中
//	     */
//	    playing(2),
//	    /**
//	     * 3活动已关闭或活动时间已结
//	     */
//	    End(3),;
//		
//		private int value;
//		HongBaoStatus(int vlaue) {
//			this.value = vlaue;
//		}
//	    // 获取值
//	    public int value() {
//	    	return this.value;
//	    }
//		public static HongBaoStatus valueOf(int value) {
//			for (HongBaoStatus flow : HongBaoStatus.values()) {
//				if (flow.value == value)
//					return flow;
//			}
//			return HongBaoStatus.None;
//		}
//
//	}


    /**
     * 活动人群 1:所有玩家   2 指定代理
     */
    public enum JoinActivityType {
        None(0),
        /**
         * 1:所有玩家
         */
        AllPlayer(1),
        /**
         * 2 指定代理及代理名下的玩家
         */
        Agent(2),
        /**
         * 3 除指定代理及代理名下的玩家
         */
        AgentUnPlaying(3),
        ;

        private int value;

        JoinActivityType(int vlaue) {
            this.value = vlaue;
        }

        // 获取值
        public int value() {
            return this.value;
        }

        public static JoinActivityType valueOf(int value) {
            for (JoinActivityType flow : JoinActivityType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return JoinActivityType.None;
        }

    }


}

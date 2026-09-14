package jsproto.c2s.cclass;

import java.io.Serializable;

public class Player {

    public static class ShortPlayer implements Serializable {
        private long pid;
        private long accountID;//accountID
        private String name;
        private String iconUrl;
        private int icon = 0;
        private int sex = 0;

        @Override
        public String toString() {
            return "ShortPlayer [pid=" + this.pid + ", accountID=" + this.accountID + ", name=" + this.name + ", iconUrl="
                    + this.iconUrl + ", icon=" + this.icon + ", sex=" + this.sex + "]";
        }

        public long getPid() {
            return pid;
        }

        public void setPid(long pid) {
            this.pid = pid;
        }

        public long getAccountID() {
            return accountID;
        }

        public void setAccountID(long accountID) {
            this.accountID = accountID;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        public int getIcon() {
            return icon;
        }

        public void setIcon(int icon) {
            this.icon = icon;
        }

        public int getSex() {
            return sex;
        }

        public void setSex(int sex) {
            this.sex = sex;
        }


    }


    public static class Property implements Serializable {
        public Property(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String name;
        public int value;
    }

    public static class PropertyLong {
        public PropertyLong(String name, long value) {
            this.name = name;
            this.value = value;
        }

        public String name;
        public long value;
    }

}

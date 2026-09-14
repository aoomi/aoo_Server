//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package core.db.other;

/**
 * 模糊匹配器
 */
public enum MatchMode {
    EXACT {
        @Override
        public String toMatchString(String pattern) {
            return pattern;
        }
    },
    START {
        @Override
        public String toMatchString(String pattern) {
            return pattern + '%';
        }
    },
    END {
        @Override
        public String toMatchString(String pattern) {
            return '%' + pattern;
        }
    },
    ANYWHERE {
        @Override
        public String toMatchString(String pattern) {
            return '%' + pattern + '%';
        }
    };

    MatchMode() {
    }

    public abstract String toMatchString(String var1);
}

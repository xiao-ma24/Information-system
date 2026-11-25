package edu.campus.authclient;

/** 请求级当前用户，由 CampusAuthFilter 写入，业务代码直接读取。 */
public final class CampusUserContext {

    private static final ThreadLocal<CampusUser> HOLDER = new ThreadLocal<>();

    private CampusUserContext() {
    }

    public static CampusUser get() {
        CampusUser user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("当前请求没有通过统一认证，请检查 CampusAuthFilter 是否生效");
        }
        return user;
    }

    /** 需要自定义 401 响应时使用，避免直接抛异常。 */
    public static CampusUser getOrNull() {
        return HOLDER.get();
    }

    static void set(CampusUser user) {
        HOLDER.set(user);
    }

    static void clear() {
        HOLDER.remove();
    }
}

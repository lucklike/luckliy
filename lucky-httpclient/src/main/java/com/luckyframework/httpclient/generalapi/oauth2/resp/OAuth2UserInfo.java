package com.luckyframework.httpclient.generalapi.oauth2.resp;

/**
 * OAuth2 用户信息响应（OIDC UserInfo 端点简化版，RFC 7519 / OIDC Core §5.3）。
 * 字段名与标准 JSON 响应保持下划线风格一致。
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class OAuth2UserInfo {

    /** 用户唯一标识（OIDC Subject Identifier，必返） */
    private String sub;

    /** 用户姓名 */
    private String name;

    /** 用户邮箱 */
    private String email;

    /** 邮箱是否已验证 */
    private Boolean email_verified;

    /** 头像地址 */
    private String picture;

    /** 用户首选登录名 */
    private String preferred_username;

    /** 名 */
    private String given_name;

    /** 姓 */
    private String family_name;

    /** 中间名 */
    private String middle_name;

    /** 用户个人资料页面地址 */
    private String profile;

    /** 用户网站地址 */
    private String website;

    /** 昵称 */
    private String nickname;

    /** 电话号码 */
    private String phone_number;

    /** 电话号码是否已验证 */
    private Boolean phone_number_verified;

    /** 信息最后更新时间（Unix时间戳字符串） */
    private String updated_at;

    /** IANA时区标识（如 Asia/Shanghai） */
    private String zoneinfo;

    /** 区域设置（BCP47语言标签，如 zh-CN） */
    private String locale;

    public OAuth2UserInfo() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmail_verified() {
        return email_verified;
    }

    public void setEmail_verified(Boolean email_verified) {
        this.email_verified = email_verified;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getPreferred_username() {
        return preferred_username;
    }

    public void setPreferred_username(String preferred_username) {
        this.preferred_username = preferred_username;
    }

    public String getGiven_name() {
        return given_name;
    }

    public void setGiven_name(String given_name) {
        this.given_name = given_name;
    }

    public String getFamily_name() {
        return family_name;
    }

    public void setFamily_name(String family_name) {
        this.family_name = family_name;
    }

    public String getMiddle_name() {
        return middle_name;
    }

    public void setMiddle_name(String middle_name) {
        this.middle_name = middle_name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public Boolean getPhone_number_verified() {
        return phone_number_verified;
    }

    public void setPhone_number_verified(Boolean phone_number_verified) {
        this.phone_number_verified = phone_number_verified;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getZoneinfo() {
        return zoneinfo;
    }

    public void setZoneinfo(String zoneinfo) {
        this.zoneinfo = zoneinfo;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return "OAuth2UserInfo{sub=" + sub
                + ", name=" + name
                + ", email=" + email
                + ", email_verified=" + email_verified
                + ", picture=" + picture
                + ", preferred_username=" + preferred_username
                + ", given_name=" + given_name
                + ", family_name=" + family_name
                + ", middle_name=" + middle_name
                + ", profile=" + profile
                + ", website=" + website
                + ", nickname=" + nickname
                + ", phone_number=" + phone_number
                + ", phone_number_verified=" + phone_number_verified
                + ", updated_at=" + updated_at
                + ", zoneinfo=" + zoneinfo
                + ", locale=" + locale + '}';
    }

    public static class Builder {
        private final OAuth2UserInfo userInfo = new OAuth2UserInfo();

        Builder() {
        }

        public Builder sub(String sub) {
            this.userInfo.sub = sub;
            return this;
        }

        public Builder name(String name) {
            this.userInfo.name = name;
            return this;
        }

        public Builder email(String email) {
            this.userInfo.email = email;
            return this;
        }

        public Builder email_verified(Boolean email_verified) {
            this.userInfo.email_verified = email_verified;
            return this;
        }

        public Builder picture(String picture) {
            this.userInfo.picture = picture;
            return this;
        }

        public Builder preferred_username(String preferred_username) {
            this.userInfo.preferred_username = preferred_username;
            return this;
        }

        public Builder given_name(String given_name) {
            this.userInfo.given_name = given_name;
            return this;
        }

        public Builder family_name(String family_name) {
            this.userInfo.family_name = family_name;
            return this;
        }

        public Builder middle_name(String middle_name) {
            this.userInfo.middle_name = middle_name;
            return this;
        }

        public Builder profile(String profile) {
            this.userInfo.profile = profile;
            return this;
        }

        public Builder website(String website) {
            this.userInfo.website = website;
            return this;
        }

        public Builder nickname(String nickname) {
            this.userInfo.nickname = nickname;
            return this;
        }

        public Builder phone_number(String phone_number) {
            this.userInfo.phone_number = phone_number;
            return this;
        }

        public Builder phone_number_verified(Boolean phone_number_verified) {
            this.userInfo.phone_number_verified = phone_number_verified;
            return this;
        }

        public Builder updated_at(String updated_at) {
            this.userInfo.updated_at = updated_at;
            return this;
        }

        public Builder zoneinfo(String zoneinfo) {
            this.userInfo.zoneinfo = zoneinfo;
            return this;
        }

        public Builder locale(String locale) {
            this.userInfo.locale = locale;
            return this;
        }

        public OAuth2UserInfo build() {
            return this.userInfo;
        }
    }
}

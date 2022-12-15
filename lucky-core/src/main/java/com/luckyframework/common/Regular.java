package com.luckyframework.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Regular {

    /** ${}*/
    public static final String $_$="\\$\\{[\\S\\s]+?\\}";
    /** #{} */
    public static final String Sharp="\\#\\{[\\S\\s]+?\\}";

    public static final String SQL_PLACEHOLDER="(\\@:[_a-zA-Z][_a-zA-Z0-9]*|\\?\\d+|\\?(c|e|C|D)\\d+|\\?(c|e|C|D)|\\?)";

    public static final String SIMPLE_SQL_PLACEHOLDER="(\\@:[_a-zA-Z][_a-zA-Z0-9]*|\\?\\d+)";

    /**
     * 带数字标识的预编译SQL  ?1 ?2
     */
    public static final  String NUMSQL="\\?\\d+";

    public static final String SQL_DY_NUN="\\?(c|e|C|D)\\d+";

    /**
     * eg -> @:name @:age
     */
    public static final String $SQL="\\@:[_a-zA-Z][_a-zA-Z0-9]*";


    /**
     * 邮箱
     */
    public static final String Email="^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    /**
     * 手机号码
     */
    public static final String PhoneNumber="^([1][3,4,5,6,7,8,9])\\d{9}$";

    /**
     * 身份证号码
     */
    public static final String IdCard="^\\d{15}|\\d{}18$";

    /**
     * 域名
     */
    public static final String DomainName="[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(/.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+/.?";

    /**
     * URL
     */
    public static final String URL="[a-zA-z]+://[^\\s]*";

    /**
     *  帐号是否合法(字母开头，允许5-16字节，允许字母数字下划线)
     */
    public static final String Account="^[a-zA-Z][a-zA-Z0-9_]{4,15}$";

    /**
     * 短身份证号码(数字、字母x结尾)
     */
    public static final String ShortIdCard="^([0-9]){7,18}(x|X)?$";

    /**
     * 腾讯QQ
     */
    public static final String QQ="[1-9][0-9]{4,}";

    /**
     * 邮政编码
     */
    public final static String ZipCode="[1-9]\\d{5}(?!\\d)";

    /**
     * Ip地址
     */
    public static final String IP="\\d+\\.\\d+\\.\\d+\\.\\d+";

    /**
     * 强密码(必须包含大小写字母和数字的组合，不能使用特殊字符，长度在8-10之间)
     */
    public static final String StrongPassword="^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,10}$";

    public static boolean check(String tarStr,String regular){
        Pattern pattern=Pattern.compile(regular);
        return pattern.matcher(tarStr).matches();
    }

    public static boolean check(String tarStr,String[] regulars){
        Pattern pattern;
        for(String regex:regulars){
            pattern=Pattern.compile(regex);
            if(pattern.matcher(tarStr).matches())
                return true;
        }
        return false;
    }

    public static List<String> getArrayByExpression(String original, String reg){
        List<String> expressions=new ArrayList<>();
        Pattern patten = Pattern.compile(reg);//编译正则表达式
        Matcher matcher = patten.matcher(original);// 指定要匹配的字符串

        while (matcher.find()) { //此处find（）每次被调用后，会偏移到下一个匹配
            expressions.add(matcher.group());//获取当前匹配的值
        }
        return expressions;
    }

    public static int questionMarkCount(String precompiledSql,int count){
        if(!precompiledSql.contains("?")){
            return count;
        }
        count++;
        String copySql=precompiledSql.replaceFirst("\\?","「」");
        return questionMarkCount(copySql,count);
    }

    public static void main(String[] args) {
        String sql="SELECT * FROM user WHERE a=@:name AND b=?3 AND c=@:price AND g=?2 OR f=?C5 OR h=?";
        List<String> plas = getArrayByExpression(sql, SQL_PLACEHOLDER);
        System.out.println(plas);
        for (String pla : plas) {
            if(check(pla,$SQL))
                System.out.println(pla);
        }
        String SQL = sql.replaceAll(SIMPLE_SQL_PLACEHOLDER, "?");
        System.out.println(questionMarkCount(SQL, 0));
        System.out.println(SQL);
        System.out.println(getArrayByExpression(SQL, "\\?"));
        System.out.println(check("@er53", $SQL));
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        String FIND_BY="^((find|get|read)([\\s\\S]*)By)([\\s\\S]*)$";
        String jpa="getNameBydf";
        System.out.println(check(jpa,FIND_BY));
        System.out.println(jpa.substring(jpa.indexOf("By")+2));

        System.out.println(getArrayByExpression("${${ok}nihao}", "\\$\\{[\\S\\s]+?\\}"));

    }
}

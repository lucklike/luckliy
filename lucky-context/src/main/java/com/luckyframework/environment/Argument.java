package com.luckyframework.environment;

import com.luckyframework.common.ContainerUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/17 16:13
 */
public class Argument {

    public static final String FIXED_PREFIX = "--";
    public static final String SEPARATOR = "=";


    private String name;

    private String value;

    public Argument(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Argument{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public static Argument[] createArguments(String ...args) {
        if(ContainerUtils.isEmptyArray(args)){
            return new Argument[0];
        }
        List<Argument> list = new ArrayList<>();
        for (String arg : args) {
            int index = arg.indexOf(SEPARATOR);
            if(arg.startsWith(FIXED_PREFIX) && index != -1){
                String name = arg.substring(FIXED_PREFIX.length(), index);
                String value = arg.substring(index + 1);
                list.add(new Argument(name, value));
            }
        }
        return list.toArray(new Argument[0]);
    }
}

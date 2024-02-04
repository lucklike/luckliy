package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;

/**
 * 命令行属性源
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/17 15:52
 */
public class CommandLinePropertySource extends ConfigurationMapPropertySource{

    public static final String COMMAND_LINE_SOURCE_NAME = "[COMMAND-LINE] lucky command line arguments";

    private static CommandLinePropertySource instance;

    private CommandLinePropertySource() {
        super(COMMAND_LINE_SOURCE_NAME, new ConfigurationMap());
    }

    public static CommandLinePropertySource getInstance() {
        if(instance == null){
            instance = new CommandLinePropertySource();
        }
        return instance;
    }

    public static void addArguments(String ...args) {
        Argument[] arguments = Argument.createArguments(args);
        for (Argument argument : arguments) {
            addArgument(argument);
        }
    }

    public static void addArgumentTrees(String ...args) {
        Argument[] arguments = Argument.createArguments(args);
        for (Argument argument : arguments) {
            addArgumentTree(argument);
        }
    }

    public static void addArgument(String name, Object value){
        getSourceData().put(name, value);
    }

    public static void addArgumentTree(String teeName, Object value){
        getSourceData().addProperty(teeName, value);
    }

    public static void addArgument(Argument arg){
        addArgument(arg.getName(), arg.getValue());
    }

    public static void addArgumentTree(Argument arg){
        addArgumentTree(arg.getName(), arg.getValue());
    }

    public boolean isEmpty(){
        return source.isEmpty();
    }

    private static ConfigurationMap getSourceData(){
        return getInstance().getSource();
    }
}

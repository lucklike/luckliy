package com.luckyframework.common;

/**
 * 两个临时变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/24 下午7:31
 */
public class TempPair<O,T> {

    private O one;
    private T two;

    public void setOne(O one) {
        this.one = one;
    }

    public void setTwo(T two) {
        this.two = two;
    }

    public static <O,T> TempPair<O,T> of(O one, T two){
        return new TempPair<>(one, two);
    }

    private TempPair(O one,T two){
        this.one = one;
        this.two = two;
    }

    public O getOne() {
        return one;
    }

    public T getTwo() {
        return two;
    }

    @Override
    public String toString() {
        String oneStr = one == null ? "null" : one.toString();
        String twoStr = two == null ? "null" : two.toString();
        return "{one["+oneStr+"] , two["+twoStr+"]}";
    }
}

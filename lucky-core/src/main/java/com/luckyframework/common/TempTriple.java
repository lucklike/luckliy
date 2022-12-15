package com.luckyframework.common;

/**
 * 三个临时变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/24 下午7:32
 */
public class TempTriple<O,T,Th> {

    private O one;
    private T two;
    private Th three;

    public void setOne(O one) {
        this.one = one;
    }

    public void setTwo(T two) {
        this.two = two;
    }

    public void setThree(Th three) {
        this.three = three;
    }

    public static <O,T,Th> TempTriple<O,T,Th> of(O one, T two, Th three){
        return new TempTriple<>(one, two, three);
    }

    private TempTriple(O one, T two, Th three) {
        this.one = one;
        this.two = two;
        this.three = three;
    }

    public O getOne() {
        return one;
    }

    public T getTwo() {
        return two;
    }

    public Th getThree() {
        return three;
    }

    @Override
    public String toString() {
        String oneStr = one == null ? "null" : one.toString();
        String twoStr = two == null ? "null" : two.toString();
        String threeStr = three == null ? "null" : three.toString();
        return "{one["+oneStr+"] , two["+twoStr+"] , three["+threeStr+"]}";
    }
}

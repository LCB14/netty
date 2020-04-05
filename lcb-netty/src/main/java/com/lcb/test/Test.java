package com.lcb.test;

/**
 * @author changbao.li
 * @since 18 八月 2019
 */
public class Test {

    public static void main(String[] args) {
        while (!Thread.interrupted()){
            System.out.println(Thread.currentThread().getName());
        }
    }
}

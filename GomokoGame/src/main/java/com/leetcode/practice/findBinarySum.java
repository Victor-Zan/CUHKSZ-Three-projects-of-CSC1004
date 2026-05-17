package com.leetcode.practice;

import java.util.Scanner;

public class findBinarySum {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String a = sc.next();
        String b = sc.next();
        int a1 = binaryToDecimal(a);
        int b1 = binaryToDecimal(b);
        int result = a1+b1;
        String sresult = Integer.toString(result);
        String realResult = dicimalToBinary(sresult);
        System.out.println(realResult);

    }

    private static String dicimalToBinary(String a) {
        int ia = Integer.parseInt(a);
        StringBuilder sb = new StringBuilder();
        while(ia >0){
            sb.append(ia%2);
            ia = ia/2;
        }
        return sb.reverse().toString();
    }
    public static int binaryToDecimal(String binary) {
        return Integer.parseInt(binary, 2);
    }

}

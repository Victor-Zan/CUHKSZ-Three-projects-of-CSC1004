package com.leetcode.practice;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class jewelryAndStone {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String jewelry = sc.next();
        String stone = sc.next();
       int result = classification(jewelry,stone);
        System.out.println(result);
    }

    private static int classify(String jewelry,String stone) {
        String[] jewelryArray = jewelry.split("");
        String[] stoneArray = stone.split("");
        int count = 0;
        for (int i = 0; i < stoneArray.length; i++) {
            for (int j = 0; j < jewelryArray.length; j++) {
                if(stoneArray[i].equals(jewelryArray[j])){
                    count++;
                }
            }
        }
        return count;
    }
    private static int classification(String jewelry, String stone) {
        Set<Character> jewelrySet = new HashSet<>();
        for (char c : jewelry.toCharArray()) {
            jewelrySet.add(c);
        }

        int count = 0;
        for (char c : stone.toCharArray()) {
            if (jewelrySet.contains(c)) {
                count++;
            }
        }
        return count;
    }
}

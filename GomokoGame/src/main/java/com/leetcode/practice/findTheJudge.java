package com.leetcode.practice;

public class findTheJudge {
    public static void main(String[] args) {
        int n = 3;
        int[][]trust = {{1,3},{2,3},{3,1}};
        int result =  findjudge(n,trust);
        System.out.println(result);
    }

    private static int findjudge(int n, int[][] trust) {
        if(n==1){
            return 1;
        }
        int[] indegree = new int[n+1];
        int[] outdegree = new int[n+1];
        for(int[] t : trust){
            int believer = t[0];
            int believed = t[1];
            outdegree[believer]++;
            indegree[believed]++;
        }
        int judge = -1;
        int count = 0;
        for (int i = 0; i < n + 1; i++) {
            if(indegree[i]==n-1 && outdegree[i]==0){
                count++;
                judge = i;
            }
        }
        return count==1 ? judge : -1;
    }
}

package com.mastermoh.near;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded word alignment: silence/noise cannot make a timed scroll run ahead. */
final class ScriptMatcher {
    private static final Pattern WORD=Pattern.compile("[\\p{L}\\p{N}]+(?:['’][\\p{L}\\p{N}]+)*");
    private final List<String> words=new ArrayList<>();
    private final List<Integer> starts=new ArrayList<>();
    private final int length;
    private int cursor;
    ScriptMatcher(String script) {
        length=script.length(); Matcher match=WORD.matcher(script);
        while(match.find()) { words.add(normalize(match.group())); starts.add(match.start()); }
    }
    private static String normalize(String word) { return word.toLowerCase(Locale.ROOT).replace("'","").replace("’",""); }
    int character() { return cursor>=starts.size() ? length : starts.get(cursor); }
    int wordPosition() { return cursor; }
    void seekCharacter(int character) {
        cursor=0;
        while(cursor<starts.size() && starts.get(cursor)<character) cursor++;
    }
    int accept(String speech) {
        List<String> heard=new ArrayList<>(); Matcher tokens=WORD.matcher(speech);
        while(tokens.find()) heard.add(normalize(tokens.group()));
        if(heard.isEmpty() || words.isEmpty()) return character();
        if(heard.size()>8) heard=heard.subList(heard.size()-8,heard.size());
        int count=heard.size(), bestCursor=cursor;
        double bestScore=Double.NEGATIVE_INFINITY;
        int first=Math.max(0,cursor-24), last=Math.min(words.size(),cursor+60);
        for(int start=first;start<last;start++) {
            int max=Math.min(count+2,last-start);
            int[][] costs=new int[count+1][max+1], hits=new int[count+1][max+1];
            for(int i=0;i<=count;i++) costs[i][0]=i;
            for(int j=0;j<=max;j++) costs[0][j]=j;
            for(int i=1;i<=count;i++) for(int j=1;j<=max;j++) {
                boolean equal=heard.get(i-1).equals(words.get(start+j-1));
                costs[i][j]=costs[i-1][j-1]+(equal?0:1); hits[i][j]=hits[i-1][j-1]+(equal?1:0);
                if(costs[i-1][j]+1<costs[i][j]) { costs[i][j]=costs[i-1][j]+1; hits[i][j]=hits[i-1][j]; }
                if(costs[i][j-1]+1<costs[i][j]) { costs[i][j]=costs[i][j-1]+1; hits[i][j]=hits[i][j-1]; }
            }
            for(int size=Math.max(1,count-2);size<=max;size++) {
                int end=start+size, matched=hits[count][size], errors=costs[count][size];
                if(count==1) {
                    if(matched!=1 || size!=1 || start<cursor || start>cursor+1) continue;
                } else if(matched<2 || errors>Math.max(1,count/3) || matched*3<count*2 || size>matched+2) continue;
                if(end<cursor && (count<3 || matched<3)) continue;
                if(matched<3 && end>cursor+6) continue;
                double score=matched*4-errors*3-Math.abs(end-cursor)*0.12;
                if(score>bestScore) { bestScore=score; bestCursor=end; }
            }
        }
        cursor=bestCursor;
        return character();
    }
}

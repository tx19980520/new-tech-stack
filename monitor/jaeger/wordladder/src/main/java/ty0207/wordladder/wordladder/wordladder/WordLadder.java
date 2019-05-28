package ty0207.wordladder.wordladder.wordladder;

import net.sf.json.JSONObject;
import org.apache.commons.lang.ObjectUtils;

import java.io.*;
import java.util.ArrayList;

import net.sf.json.JSONArray;


public class WordLadder {
    private InputStream input;
    private  ArrayList<String> dict;
    public WordLadder(InputStream input)
    {
        this.input = input;
        StringBuilder jsonString = new StringBuilder();
        try{
            InputStreamReader reader =  new InputStreamReader(this.input);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String strLine = null;
            while(null != (strLine = bufferedReader.readLine()))
            {
                jsonString.append(strLine);
            }

        }catch (FileNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        JSONArray dict_json = JSONArray.fromObject(jsonString.toString());

         this.dict = (ArrayList<String>) JSONArray.toList(dict_json);
    }

    public Boolean find(String word)
    {
        return this.dict.contains(word);
    }

    public ArrayList<String> BFS(String start_word, String end_word)
    {
        // dict init
        int s_idx = this.dict.indexOf(start_word);
        ArrayList<String> result_string = new ArrayList<>();
        ArrayList<Integer>result = this.search(s_idx, end_word);
        if(result == null)
        {
            return null;
        }
        for(int i = 0; i < result.size(); ++i)
        {
            result_string.add(this.dict.get(result.get(i)));
        }
       return result_string;
    }

    private ArrayList<Integer> search(int s_idx, String t) {
        ArrayList<Integer> visited = new ArrayList<>();
        for (int i=0; i<this.dict.size(); ++i) visited.add(-1);
        visited.set(s_idx, s_idx);
        // bfs
        ArrayList<Integer> q = new ArrayList<>();
        q.add(s_idx);
        int nw = 0;
        int last = -1;
        while (nw < q.size()) {
            int cur_idx = q.get(nw++);
            for (int i=0; i<this.dict.size(); ++i) {
                if (visited.get(i) == -1 && this.isNear(this.dict.get(cur_idx), this.dict.get(i))) {
                    visited.set(i, cur_idx);
                    if (this.dict.get(i).equals(t)) {
                        last = i;
                        break;
                    }
                    q.add(i);
                }
            }
            if (last != -1) break;
        }
        // if success: print Result, else return null
        if (last == -1) return null;
        ArrayList<Integer> reversed_result = new ArrayList<>();
        while (visited.get(last) != last) {
            reversed_result.add(last);
            last = visited.get(last);
        }
        ArrayList<Integer> ret = new ArrayList<>();
        ret.add(s_idx);
        for (int i=reversed_result.size()-1; i >= 0; --i) ret.add(reversed_result.get(i));
        return ret;
    }

    private boolean isNear(String a, String b) {
        if (a.length() > b.length()) {
            String c = a;
            a = b;
            b = c;
        }
        if (b.length() - a.length() > 1) return false;
        if (a.length() == b.length()) {
            int cnt = 0;
            for (int i=0; i<a.length(); ++i) {
                if (a.charAt(i) != b.charAt(i)) {
                    ++cnt;
                    if (cnt > 1) return false;
                }
            }
            if (cnt == 0) return false;
            return true;
        } {
            int cnt = 0;
            for (int i=0; i<a.length(); ++i) {
                while (a.charAt(i) != b.charAt(i+cnt)) {
                    ++cnt;
                    if (cnt > 1) return false;
                }
            }
        }
        return true;
    }
}

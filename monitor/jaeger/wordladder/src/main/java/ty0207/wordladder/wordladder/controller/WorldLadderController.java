package ty0207.wordladder.wordladder.controller;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


import java.io.IOException;
import java.util.ArrayList;

import ty0207.wordladder.wordladder.wordladder.WordLadder;

@RestController
@RequestMapping(value="/api")
public class WorldLadderController {

    @RequestMapping(value = "/BFS", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> getWordLadder(@RequestParam("input") String input, @RequestParam("output") String output) throws IOException
    {
        /*to do*/
        ClassPathResource dict = new ClassPathResource("static/small.json");
        WordLadder wl = new WordLadder(dict.getInputStream());
        ArrayList<String> list = wl.BFS(input, output);
        JSONArray result = JSONArray.fromObject(list);
        JSONObject response = new JSONObject();
        response.put("result", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> search(@RequestParam("word") String word) throws IOException
    {
        ClassPathResource dict = new ClassPathResource("static/small.json");
        WordLadder wl = new WordLadder(dict.getInputStream());
        JSONObject result = new JSONObject();
        result.put("has", wl.find(word));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}

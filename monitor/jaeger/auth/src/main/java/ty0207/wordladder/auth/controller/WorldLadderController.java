package ty0207.wordladder.auth.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import ty0207.wordladder.auth.service.WordladderService;

@RestController
@RequestMapping(value = "/api")
public class WorldLadderController {

    @Autowired
    WordladderService wordladderService;

    @RequestMapping(value = "/BFS", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> getWordLadder(@RequestParam("input") String input, @RequestParam("output") String output) throws IOException {

        JSONObject result = wordladderService.requestWordladder(input, output);
        if (result != null)
            return new ResponseEntity<>(result, HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> search(@RequestParam("word") String word) throws IOException {
        JSONObject result = wordladderService.requestSearch(word);
        if (result != null)
            return new ResponseEntity<>(result, HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}

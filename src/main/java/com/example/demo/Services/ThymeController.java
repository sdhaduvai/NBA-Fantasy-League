package com.example.demo.Services;


import com.example.demo.Repositories.UserRepository;
import com.example.demo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ThymeController{

    @Autowired
    private UserRepository userRepository;
    private Map<String, String> teamNames = new HashMap<>();

    public ThymeController() {
        teamNames.put("LAC", "Los Angeles Clippers");
        teamNames.put("MH", "Miami Heat");
        teamNames.put("WW", "Washington Wizards");
    }

    @GetMapping("/index")
    public ModelAndView renderIndex(){
        ModelAndView m = new ModelAndView();
        m.setViewName("index");
        m.addObject("teams");
        return m;
    }

    @GetMapping("/teamSelection")
    public String renderteamSelection () {
        return "teamSelection";
    }

    @PostMapping("/saveTeams")
    @ResponseBody
    public ModelAndView saveTeams(@RequestParam String teams){
        String[] teamsList = teams.split(",");
        ModelAndView teamView = new ModelAndView("index");
        Map<String, String> teamObj;
        List<Map<String, String>> resp = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for(String team : teamsList) {
           teamObj = new HashMap<>();
           teamObj.put("name", teamNames.get(team));
           names.add(teamNames.get(team));
           teamObj.put("abbr", team);
           resp.add(teamObj);
        }

        User user = new User();
        user.setAbbr(teams);
        user.setTeamName(names.toString());
        userRepository.save(user);

        teamView.addObject("response", resp);
        return teamView;

        /**Sample Response
         [
           {
                name: Los Angeles Clippers,
                abbr: LAC
           },
           {
                name: Washnington Wizards,
                abbr: WW
           }
         ]
         */
    }
}
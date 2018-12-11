package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDate;

@Controller
public class ThymeController{

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private fbService fbservice;
    @Autowired
    private FacebookDBRepository fbrepository;
    @Autowired
    private FavoriteTeamsRepository favoriteTeamsRepository;
    private Map<String, String> teamNames = new HashMap<>();


    public ThymeController() {
        teamNames.put("LAC", "Los Angeles Clippers");
        teamNames.put("MH", "Miami Heat");
        teamNames.put("WW", "Washington Wizards");
    }

    @GetMapping("/login")
    public String renderLogin()
    {
        return "Facebook";
    }

    @GetMapping("/admin")
    public String renderAdmin()
    {
        return "admin";
    }

    @PostMapping("/http://ec2-13-59-200-144.us-east-2.compute.amazonaws.com:8080/")
    public  String startIndex(){
        return "index";
    }

    @GetMapping("/index")
    public String renderIndex(HttpSession session){
        String id = session.getAttribute("userID").toString();
        return "redirect:/saveFBUser?id="+id;
    }

    @PostMapping("/saveselections")
    public String renderteamSelections (@RequestParam("teams") String teams, HttpSession session) {
        String[] allTeams = teams.split(",");

        String userID = (String) session.getAttribute("userID");
        String name = session.getAttribute("name").toString();
        Iterable<FavoriteTeams> listOfFavorites = favoriteTeamsRepository.findAll();
        int m = 0;

        for(String team : allTeams) {
            for (FavoriteTeams onerow : listOfFavorites) {
                if (onerow.getTeamName().equals(team) && onerow.getUserID().equals(userID)) {
                    m = 1;
                }
            }
                if(m == 1) {
                    m = 0;
                    continue;
                }
                else {

                    FavoriteTeams newRow = new FavoriteTeams();
                    newRow.setUserID(userID);
                    newRow.setTeamName(team);
                    newRow.setLastChecked(todayDate());
                    favoriteTeamsRepository.save(newRow);
                }
            }


        return "redirect:/saveFBUser?id="+userID;


    }



    @PostMapping("/saveFBUser")
    public ModelAndView addFBEntry(@RequestParam("fbid") String id, @RequestParam("fbname") String name, HttpSession session){
        if (id.equals("2135197849824569")){
            ModelAndView admin = new ModelAndView("admin");
            return admin;
        }
        ModelAndView homepage = new ModelAndView("homepage");
        homepage.addObject("gameDetails", getTeamScores());
        homepage.addObject("todayMatch", todayMatch());
        Iterable<FacebookDB> listOfUsers = fbrepository.findAll();
        for(FacebookDB oneUser : listOfUsers){
            if(oneUser.getId().equals(id) && oneUser.isBlocked()){
                ModelAndView blocked = new ModelAndView("blocked");
                blocked.addObject("name", oneUser.getname());
                return blocked;
            }
            if(oneUser.getId().equals(id)){
                homepage.addObject("name", name);
                Iterable<FavoriteTeams> listOfFavorites = favoriteTeamsRepository.findAll();
                List<String> userFavorites = new ArrayList<String>();
                for (FavoriteTeams oneRow: listOfFavorites){
                    if(oneRow.getUserID().equals(id)){
                        userFavorites.add(oneRow.getTeamName());
                    }
                }
                oneUser.setDates(oneUser.getDates()+","+todayDate());
                homepage.addObject("favorites", userFavorites);
                session.setAttribute("userID", id);
                session.setAttribute("name", name);
                fbrepository.save(oneUser);
                return homepage;
            }
        }
        FacebookDB fb = new FacebookDB();
        fb.setname(name);
        fb.setId(id);
        fb.setDates(todayDate());
        fbrepository.save(fb);
        session.setAttribute("userID",id);
        homepage.addObject("name", name);
        return homepage;
    }

    @GetMapping("/saveFBUser")
    public ModelAndView renderNewSelection(@RequestParam String id, HttpSession session){
        String name = session.getAttribute("name").toString();
        if (id.equals("2135197849824569")){
            ModelAndView admin = new ModelAndView("admin");
            return admin;
        }
        ModelAndView homepage = new ModelAndView("homepage");
        homepage.addObject("gameDetails", getTeamScores());
        homepage.addObject("todayMatch", todayMatch());
        Iterable<FacebookDB> listOfUsers = fbrepository.findAll();
        for(FacebookDB oneUser : listOfUsers){
            if(oneUser.getId().equals(id) && oneUser.isBlocked()){
                ModelAndView blocked = new ModelAndView("blocked");
                blocked.addObject("name", oneUser.getname());
                return blocked;
            }
            if(oneUser.getId().equals(id)){
                Iterable<FavoriteTeams> listOfFavorites = favoriteTeamsRepository.findAll();
                List<String> userFavorites = new ArrayList<String>();
                for (FavoriteTeams oneRow: listOfFavorites){
                    if(oneRow.getUserID().equals(id)){
                        userFavorites.add(oneRow.getTeamName());
                    }
                }
                oneUser.setDates(oneUser.getDates()+","+todayDate());
                homepage.addObject("favorites", userFavorites);
                homepage.addObject("name", name);
                session.setAttribute("userID", id);
                return homepage;
            }
        }
        return homepage;
    }

    @GetMapping("/viewAllUsers")
    public ModelAndView viewAllUsers() {
        ModelAndView adminView = new ModelAndView("viewAllUsers");
        List<FacebookDB> listOfUsers = new ArrayList<FacebookDB>();
        fbservice.findAllUsers();
        adminView.addObject("allUsers", fbrepository.findAll());
        System.out.println("\n**********\n"+fbservice.findAllUsers()+"\n********");
        return adminView;
    }

    @GetMapping("/viewBlockedUsers")
    public ModelAndView viewBlockedUsers() {
        ModelAndView adminView = new ModelAndView("viewBlockedUsers");
        List<FacebookDB> listOfBlocked = new ArrayList<FacebookDB>();
        Iterable<FacebookDB> listOfUSers = fbrepository.findAll();
        for(FacebookDB oneUser: listOfUSers){
            if(oneUser.isBlocked()){
                listOfBlocked.add(oneUser);
            }
        }
        adminView.addObject("allUsers", listOfBlocked);
        System.out.println("\n**********\n"+fbservice.findAllUsers()+"\n********");
        return adminView;
    }

    @GetMapping("/blockUser")
    public ModelAndView blockUser(@RequestParam("userId") String userID) {
        ModelAndView adminView = new ModelAndView("viewAllUsers");
        Iterable<FacebookDB> listOfUsers = fbrepository.findAll();
        for(FacebookDB oneUser : listOfUsers){
            if(oneUser.getId().equals(userID)){
                oneUser.setBlocked(true);
                fbrepository.save(oneUser);
            }
        }
        adminView.addObject("allUsers", fbrepository.findAll());
//        System.out.println("\n**********\n"+fbservice.findAllUsers()+"\n********");
//        return adminView;
        return adminView;
    }

    @GetMapping("/unblockUser")
    public ModelAndView unblockUser(@RequestParam("userId") String userID) {
        ModelAndView adminView = new ModelAndView("viewAllUsers");
        Iterable<FacebookDB> listOfUsers = fbrepository.findAll();
        for(FacebookDB oneUser : listOfUsers){
            if(oneUser.getId().equals(userID)){
                oneUser.setBlocked(false);
                fbrepository.save(oneUser);
            }
        }
        adminView.addObject("allUsers", fbrepository.findAll());
//        System.out.println("\n**********\n"+fbservice.findAllUsers()+"\n********");
//        return adminView;
        return adminView;
    }


    @GetMapping("/selectTeam")
    public String selectTeam(@RequestParam("teamName") String teamName, HttpSession session){
        String userID = (String) session.getAttribute("userID");
        String name = session.getAttribute("name").toString();
        Iterable<FavoriteTeams> listOfFavorites = favoriteTeamsRepository.findAll();

        for(FavoriteTeams onerow : listOfFavorites){
            if(onerow.getTeamName().equals(teamName) && onerow.getUserID().equals(userID)){
                return "redirect:/saveFBUser?id="+userID;
            }
        }

        FavoriteTeams newRow = new FavoriteTeams();
        newRow.setUserID(userID);
        newRow.setTeamName(teamName);
        newRow.setLastChecked(todayDate());
        favoriteTeamsRepository.save(newRow);
        return "redirect:/saveFBUser?id="+userID;
    }

    //Using PoJo Classes
    @GetMapping("/teams")
    public ModelAndView getAllTeams(HttpSession session) {
        ModelAndView showTeams = new ModelAndView("showAllTeams");
        String name = session.getAttribute("name").toString();
        showTeams.addObject("name", name);

        //Endpoint to call
        String url ="https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/overall_team_standings.json";
        //Encode Username and Password
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());
        // TOKEN:PASS
        //Add headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        //Make the call
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<NBATeamStanding> response = restTemplate.exchange(url, HttpMethod.GET, request, NBATeamStanding.class);
        NBATeamStanding ts = response.getBody();
        //Send the object to view
        showTeams.addObject("teamStandingEntries", ts.getOverallteamstandings().getTeamstandingsentries());

        return showTeams;
    }

    public ArrayList<HashMap<String, String>> generateTeams(HttpSession session) {
        String name = session.getAttribute("name").toString();
        ArrayList<HashMap<String, String>> gameDetails = new ArrayList<HashMap<String, String>>();

        //Endpoint to call
        String url ="https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/overall_team_standings.json";
        //Encode Username and Password
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());
        // TOKEN:PASS
        //Add headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        //Make the call
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String str = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(str);
            JsonNode gameScores = root.get("overallteamstandings").get("teamstandingsentry");

            if(gameScores != null) {

                for(JsonNode score : gameScores){
                    JsonNode game = score.get("team");
                    HashMap<String, String> gameDetail = new HashMap<>();
                    gameDetail.put("id", game.get("ID").asText());
                    gameDetail.put("city", game.get("City").asText());
                    gameDetail.put("name", game.get("Name").asText());
                    gameDetail.put("Abbreviation", game.get("Abbreviation").asText());
                    gameDetails.add(gameDetail);

                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return gameDetails;
    }

    //Using PoJo Classes
    @GetMapping("/teamRankings")
    public ModelAndView getTeams(HttpSession session) {
        ModelAndView showTeams = new ModelAndView("showTeams");
        String name = session.getAttribute("name").toString();

        showTeams.addObject("name", name);

        //Endpoint to call
        String url ="https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/overall_team_standings.json";
        //Encode Username and Password
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());
        // TOKEN:PASS
        //Add headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        //Make the call
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<NBATeamStanding> response = restTemplate.exchange(url, HttpMethod.GET, request, NBATeamStanding.class);
        NBATeamStanding ts = response.getBody();
        //Send the object to view
        showTeams.addObject("teamStandingEntries", ts.getOverallteamstandings().getTeamstandingsentries());

        return showTeams;
    }

    //Using objectMapper
    @GetMapping("/team")
    public ModelAndView getTeamInfo(
            @RequestParam("id") String teamID, HttpSession session) {
        int numberOfWins = 0;
        int numberOfLosses = 0;
        String id = session.getAttribute("userID").toString();

        ModelAndView teamInfo = new ModelAndView("teamInfo");
        ArrayList<HashMap<String, String>> gameDetails = new ArrayList<HashMap<String, String>>();
        String url = "https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/team_gamelogs.json?team=" + teamID;
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);



        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String str = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        String Abbr = "";
        String name = "";
        try {
            JsonNode root = mapper.readTree(str);
            System.out.println(str);
            //JsonNode jsonNode1 = actualObj.get("lastUpdatedOn");
            System.out.println(root.get("teamgamelogs").get("lastUpdatedOn").asText());
            System.out.println(root.get("teamgamelogs").get("gamelogs").getNodeType());
            JsonNode gamelogs = root.get("teamgamelogs").get("gamelogs");

            if(gamelogs.isArray()) {

                for(JsonNode gamelog : gamelogs) {
                    JsonNode game = gamelog.get("game");
                    JsonNode stats = gamelog.get("stats");
                    HashMap<String, String> gameDetail = new HashMap<String, String>();
                    gameDetail.put("id", game.get("id").asText());
                    gameDetail.put("date", game.get("date").asText());
                    gameDetail.put("time", game.get("time").asText());
                    gameDetail.put("homeTeam", game.get("homeTeam").get("Abbreviation").asText());
                    gameDetail.put("awayTeam", game.get("awayTeam").get("Abbreviation").asText());
                    gameDetail.put("location", game.get("location").asText());
                    if(game.get("awayTeam").get("ID").asText().equals(teamID)){
                        Abbr = game.get("awayTeam").get("Abbreviation").asText();
                        name = game.get("awayTeam").get("Name").asText();
                    }
                    else{
                        Abbr = game.get("homeTeam").get("Abbreviation").asText();
                        name = game.get("awayTeam").get("Name").asText();
                    }
                    if (stats.get("Wins").get("#text").asInt() == 1) {
                        numberOfWins += 1;
                        gameDetail.put("status", "won");
                    }
                    else{
                        numberOfLosses += 1;
                        gameDetail.put("status", "lost");
                    }
                    gameDetails.add(gameDetail);
                }

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        teamInfo.addObject("gameDetails", gameDetails);
        teamInfo.addObject("wins", numberOfWins);
        teamInfo.addObject("losses", numberOfLosses);
        teamInfo.addObject("fullSchedule", fullGameSchedule(Abbr));
        teamInfo.addObject("teamName", name);

        Iterable<FavoriteTeams> listOfFavorites = favoriteTeamsRepository.findAll();
        for(FavoriteTeams oneRow : listOfFavorites){
            if(oneRow.getUserID().equals(id) && oneRow.getTeamName().equals(name)){
                oneRow.setLastChecked(todayDate());
            }
        }

        return teamInfo;

    }

    @GetMapping("/selectTeams")
    public ModelAndView selectTeams(HttpSession session){
        ModelAndView teamSelection = new ModelAndView("teamSelection");
        ArrayList<HashMap<String, String>> allTeams = generateTeams(session);
        teamSelection.addObject("allTeams", allTeams);
        return teamSelection;
    }



    @GetMapping("/notifications")
    public ModelAndView getNotifications(HttpSession session) {
        ModelAndView displayNotifications = new ModelAndView("notifications");
        String userID = (String) session.getAttribute("userID");
        List<String> simpleList = new ArrayList<String>();
        List<ArrayList<HashMap<String, String>>> forRendering = new ArrayList<ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> allRecords = new ArrayList<HashMap<String, String>>();
        Iterable<FavoriteTeams> listOfFavorites = favoriteTeamsRepository.findAll();
        for (FavoriteTeams row : listOfFavorites) {
            if (row.getUserID().equals(userID)) {
                HashMap<String, String> usersFavorites = new HashMap<>();
                usersFavorites.put("name", row.getTeamName());
                usersFavorites.put("lastChecked", row.getLastChecked());
                allRecords.add(usersFavorites);
                simpleList.add(row.getTeamName());
            }
        }

        for (HashMap<String, String> element : allRecords) {
            List<Date> allDates = new ArrayList();
            DateTime lastChecked = new DateTime(element.get("lastChecked"));
            DateTime currentDate = new DateTime(todayDate());

            while (lastChecked.isBefore(currentDate)) {
                allDates.add(lastChecked.toDate());
                lastChecked = lastChecked.plusDays(1);
            }

            String newDates = "";

                for (Date date:allDates) {
                    ArrayList<HashMap<String, String>> oneDate = new ArrayList<HashMap<String, String>>();
                    oneDate = fetchAllNotifications(element.get("name"), date.toString());
                    forRendering.add(oneDate);
                }


        }
        displayNotifications.addObject("notifications", forRendering);
        return displayNotifications;
    }

    public ArrayList<HashMap<String, String>> fetchAllNotifications(String name, String date) {
        ArrayList<HashMap<String, String>> gameDetails = new ArrayList<HashMap<String, String>>();
        String url = "https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/scoreboard.json?fordate="+date;
//        System.out.println("********************\nhttps://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/scoreboard.json?fordate="+getDateInString()+"\n*****************");
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String str = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(str);
            JsonNode gameScores = root.get("scoreboard").get("gameScore");

            if(gameScores != null) {

                for(JsonNode score : gameScores) {
                    JsonNode game = score.get("game");
                    String team1 = game.get("awayTeam").get("Name").asText();
                    String team2 = game.get("homeTeam").get("Name").asText();
                    if(name.equals(team1) || name.equals(team2)){
                        HashMap<String, String> gameDetail = new HashMap<>();
                        gameDetail.put("id", game.get("ID").asText());
                        gameDetail.put("date", game.get("date").asText());
                        gameDetail.put("time", game.get("time").asText());
                        gameDetail.put("homeTeam", game.get("homeTeam").get("Abbreviation").asText());
                        gameDetail.put("awayTeam", game.get("awayTeam").get("Abbreviation").asText());
                        gameDetail.put("Score", score.get("homeScore").asInt() + " - " + score.get("awayScore").asInt());
                        gameDetails.add(gameDetail);
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



        return gameDetails;
    }


    //Using objectMapper
    @GetMapping("/teamscores")
    public ArrayList<HashMap<String, String>> getTeamScores() {
        ModelAndView dailyScores = new ModelAndView("dailyScores");
        ArrayList<HashMap<String, String>> gameDetails = new ArrayList<HashMap<String, String>>();
        String url = "https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/scoreboard.json?fordate=20181207"; //+getDateInString();
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String str = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(str);
            JsonNode gameScores = root.get("scoreboard").get("gameScore");

            if(gameScores != null) {

                gameScores.forEach(score -> {
                    JsonNode game = score.get("game");
                    HashMap<String, String> gameDetail = new HashMap<>();
                    gameDetail.put("id", game.get("ID").asText());
                    gameDetail.put("date", game.get("date").asText());
                    gameDetail.put("time", game.get("time").asText());
                    gameDetail.put("homeTeam", game.get("homeTeam").get("Abbreviation").asText());
                    gameDetail.put("awayTeam", game.get("awayTeam").get("Abbreviation").asText());
                    gameDetail.put("Score", score.get("homeScore").asInt() + " - " + score.get("awayScore").asInt());
                    gameDetails.add(gameDetail);

                });
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        dailyScores.addObject("gameDetails", gameDetails);


        return gameDetails;
    }

    //Using objectMapper
    @GetMapping("/todayMatch")
    public ArrayList<HashMap<String, String>> todayMatch() {
        ArrayList<HashMap<String, String>> gameDetails = new ArrayList<HashMap<String, String>>();
        String url = "https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/daily_game_schedule.json?fordate="+todayDate();
//        System.out.println("********************\nhttps://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/scoreboard.json?fordate="+getDateInString()+"\n*****************");
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String str = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(str);
            JsonNode gameScores = root.get("dailygameschedule").get("gameentry");

            if(gameScores != null) {

                gameScores.forEach(score -> {
                    HashMap<String, String> gameDetail = new HashMap<>();
                    gameDetail.put("id", score.get("id").asText());
                    gameDetail.put("date", score.get("date").asText());
                    gameDetail.put("time", score.get("time").asText());
                    gameDetail.put("homeTeam", score.get("homeTeam").get("Name").asText());
                    gameDetail.put("awayTeam", score.get("awayTeam").get("Name").asText());
                    gameDetail.put("location", score.get("location").asText());
                    gameDetails.add(gameDetail);

                });
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



        return gameDetails;
    }



//    Using objectMapper
    @GetMapping("/fullGameSchedule")
    public ArrayList<HashMap<String, String>> fullGameSchedule(String Abbr) {
        ArrayList<HashMap<String, String>> gameDetails = new ArrayList<HashMap<String, String>>();
        String url = "https://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/full_game_schedule.json";
//        System.out.println("********************\nhttps://api.mysportsfeeds.com/v1.2/pull/nba/2018-2019-regular/scoreboard.json?fordate="+getDateInString()+"\n*****************");
        String encoding = Base64.getEncoder().encodeToString("5d290d3c-ce38-4147-9af5-43ab32:codename045".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic "+encoding);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String str = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(str);
            JsonNode gameScores = root.get("fullgameschedule").get("gameentry");

            if(gameScores != null) {

                for(JsonNode score : gameScores) {
                    if (score.get("homeTeam").get("Abbreviation").asText().equals(Abbr) || score.get("awayTeam").get("Abbreviation").asText().equals(Abbr)) {
                        HashMap<String, String> gameDetail = new HashMap<>();

                        gameDetail.put("id", score.get("id").asText());
                        gameDetail.put("date", score.get("date").asText());
                        gameDetail.put("time", score.get("time").asText());
                        gameDetail.put("homeTeam", score.get("homeTeam").get("Name").asText());
                        gameDetail.put("awayTeam", score.get("awayTeam").get("Name").asText());
                        gameDetail.put("location", score.get("location").asText());

                        gameDetails.add(gameDetail);

                    }

                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



        return gameDetails;
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session)
    {
        session.invalidate();
        return "redirect:/login";
    }

    public String todayDate(){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        return formatter.format(date);
    }

    public String getDateInString(){
        String yesterday = LocalDate.now().minusDays(1L).toString();
        String[] toAppend = yesterday.split("-");
        String realDate="";
        for(int i = 0; i < toAppend.length; i++){
            realDate += toAppend[i];
        }
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return realDate;
    }
}
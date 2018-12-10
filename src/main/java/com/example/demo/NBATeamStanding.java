package com.example.demo;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
class team{
	@JsonProperty("ID")
	String ID;
	@JsonProperty("City")
	String City;
	@JsonProperty("Name")
	String Name;
	@JsonProperty("Abbreviation")
	String Abbreviation;

}

@Data
class teamstandingsentry{
	team team;
	Long rank;	
}

@Data
class overallteamstandings{
	String lastUpdatedOn;
	@JsonProperty("teamstandingsentry")
	ArrayList<teamstandingsentry> teamstandingsentries;

	public ArrayList<teamstandingsentry> getTeamstandingsentries() {
		return teamstandingsentries;
	}

	public void setTeamstandingsentries(ArrayList<teamstandingsentry> teamstandingsentries) {
		this.teamstandingsentries = teamstandingsentries;
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NBATeamStanding {
	overallteamstandings overallteamstandings;

	public com.example.demo.overallteamstandings getOverallteamstandings() {
		return overallteamstandings;
	}

	public void setOverallteamstandings(com.example.demo.overallteamstandings overallteamstandings) {
		this.overallteamstandings = overallteamstandings;
	}
}

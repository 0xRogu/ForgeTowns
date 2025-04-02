package dev.rogu.forgetowns.data;

import java.util.ArrayList;
import java.util.List;

public class Nation {

    private String name;
    private Town leaderTown;
    private List<Town> memberTowns = new ArrayList<>();
    private boolean atWar;
    private GovernmentType governmentType = GovernmentType.ANARCHY; // Default

    public Nation(String name, Town leaderTown) {
        this.name = name;
        this.leaderTown = leaderTown;
        this.memberTowns.add(leaderTown);
        this.atWar = false;
    }

    public String getName() {
        return name;
    }

    public Town getLeaderTown() {
        return leaderTown;
    }

    public List<Town> getMemberTowns() {
        return memberTowns;
    }

    public boolean isAtWar() {
        return atWar;
    }

    public GovernmentType getGovernmentType() {
        return governmentType;
    }

    public void setGovernmentType(GovernmentType type) {
        this.governmentType = type;
    }

    public void addTown(Town town) {
        memberTowns.add(town);
    }

    public void setWar(boolean atWar) {
        this.atWar = atWar;
    }
}

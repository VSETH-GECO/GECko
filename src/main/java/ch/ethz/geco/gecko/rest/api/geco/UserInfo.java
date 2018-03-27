package ch.ethz.geco.gecko.rest.api.geco;

import java.util.Map;

public class UserInfo {
    /**
     * Describes the type of an account.<br>
     * Possible values:<br>
     * * STEAM - A Steam account<br>
     * * RIOT - A Riot (LoL) account<br>
     * * BLIZZARD - A Blizzard/Battle.net account<br>
     * * DICORD - A Discord account
     */
    public enum AccountType {
        STEAM,
        RIOT,
        BLIZZARD,
        DISCORD;
    }

    private Integer webID;
    private String username;
    private String usergroup;
    private Map<AccountType, String> accounts;

    public UserInfo(Integer webID, String username, String usergroup, Map<AccountType, String> accounts) {
        this.webID = webID;
        this.username = username;
        this.usergroup = usergroup;
        this.accounts = accounts;
    }

    private Integer lanID;
    private String seat;
    private String firstName;
    private String lastName;
    private Long birthday;

    public void addLanUser(Integer lanID, String seat, String firstName, String lastName, Long birthday) {
        this.lanID = lanID;
        this.seat = seat;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
    }

    /**
     * Returns whether or not the user has a LAN user.
     *
     * @return true if the user has a LAN user, false otherwise
     */
    public boolean hasLanUser() {
        return lanID != null;
    }

    /**
     * Returns the web ID of the user. This is the ID, the user has on the website.
     *
     * @return the web ID of the user
     */
    public Integer getWebID() {
        return webID;
    }

    /**
     * Returns the username of the user.
     *
     * @return the username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the usergroup of the user. This describes how much power the user has on the website.
     *
     * @return the usergroup of the user
     */
    public String getUsergroup() {
        return usergroup;
    }

    /**
     * Returns the account of the given type associated with the user.
     *
     * @param type the account type
     * @return the account
     */
    public String getAccount(AccountType type) {
        return accounts.get(type);
    }

    /**
     * Returns the LAN ID of the user. This is the ID of the user at the LAN and is different from
     * the web user ID.
     *
     * @return the LAN ID of the user or null if the user has no LAN user
     */
    public Integer getLanID() {
        return lanID;
    }

    /**
     * Returns the seat of the user.
     *
     * @return the seat of the user or null if the user did not choose a seat yet or has no LAN user
     */
    public String getSeat() {
        return seat;
    }

    /**
     * Returns the first name of the user.
     *
     * @return the first name of the user or null if the user has no LAN user
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the last name of the user.
     *
     * @return the last name of the user or null if the user has no LAN user
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the birthday of the user.
     *
     * @return the birthday of the user or null if the user has no LAN user
     */
    public Long getBirthday() {
        return birthday;
    }
}

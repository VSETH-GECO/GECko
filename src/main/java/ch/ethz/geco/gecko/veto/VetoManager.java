package ch.ethz.geco.gecko.veto;

import java.util.ArrayList;
import java.util.List;

/*
 * Map Veto Concept:
 * 1.   One Player issues a map veto with another player
 *      -   This could be done automatically via team captains
 *          depending on how "integrated" the tournament is into discord
 *
 * 2.   A map veto voting message appears in one of these places:
 *      a)  In the channel where the map veto was issued
 *      b)  In the private channels of both players with the bot
 *          (the bot will be the proxy)
 *      c)  In a newly created channel where only the two teams have access
 *
 * 3.   Both teams veto on the map pool
 *      -   Different veto modes (BAN until one left, BAN PICK ...)
 *          -   Maybe even vote on the veto mode?
 *
 * 4.   After the map has been chosen either
 *      a)  Leave the message
 *      b)  Leave the message
 *      c)  Close the newly created channel and report the results to both teams
 */

/**
 * This class will handle all current and past vetoes.
 */
public class VetoManager {
    private static final List<Veto> vetoes = new ArrayList<>();

}

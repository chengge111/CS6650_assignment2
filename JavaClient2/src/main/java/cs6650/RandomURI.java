package cs6650;

import java.util.concurrent.ThreadLocalRandom;

public class RandomURI {

    public RandomURI() {}

    public static String getRandomURI() {
        int resortID = ThreadLocalRandom.current().nextInt(1, 10);
        int seasonID = 2025;
        int dayID = 1;
        int skierID = ThreadLocalRandom.current().nextInt(1, 100001);
        String IPAddress = "loadbalancer-953598013.us-west-2.elb.amazonaws.com";
        return String.format("http://%s/Servlet_war/skiers/%d/seasons/%d/days/%d/skiers/%d", IPAddress, resortID, seasonID, dayID, skierID);
    }
}

package com.neatlogic.autoexecrunner.util.tagent;


import java.util.Random;

public class RandomUtils {

    private static char[] PASSWORD_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
            'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z' };

    public static String getRandomStr(int length) {
        int rdGet = 0;
        StringBuffer password = new StringBuffer();
        Random random = new Random();
        int count = 0;
        while (count < length) {
            rdGet = Math.abs(random.nextInt(PASSWORD_CHAR.length));
            if (rdGet >= 0 && rdGet < PASSWORD_CHAR.length) {
                password.append(PASSWORD_CHAR[rdGet]);
                count++;
            }
        }
        return password.toString();
    }

}

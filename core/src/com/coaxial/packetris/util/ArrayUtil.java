package com.coaxial.packetris.util;

import java.util.Arrays;

/**
 * A collection of utils for array manipulation.
 * Highly used in {@link com.coaxial.packetris.elements.Packet}.
 */

public class ArrayUtil
{
    /**
     * Create a deep clone of a two-dimensional boolean array.
     * @param s the array to be cloned
     * @return the array
     */
    public static boolean[][] cloneArray(boolean[][] s)
    {
        boolean[][] newar = new boolean[s.length][s[0].length];
        for(int i=0; i<s.length; ++i)
            newar[i] = Arrays.copyOf(s[i], s[i].length);
        return newar;
    }

    /**
     * Transposes a two dimensional boolean array.
     * Returns a independent array, the original will not be altered.
     * @param array the array to be transposed
     * @return a new array representing the transposed array
     */
    public static boolean[][] transpose(boolean[][] array) {
        if (array == null || array.length == 0)//empty or unset array, nothing do to here
            return array;

        int width = array.length;
        int height = array[0].length;

        boolean[][] array_new = new boolean[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                array_new[y][x] = array[x][y];
            }
        }
        return array_new;
    }

    /**
     * Reverse a one dimensional boolean array.
     * The original array will be reversed itself, so the return type is void.
     * @param s the array to be reversed
     */
    public static void reverse(boolean[] s)
    {
        for(int i = 0; i < s.length / 2; i++)
        {
            boolean temp = s[i];
            s[i] = s[s.length - i - 1];
            s[s.length - i - 1] = temp;
        }
    }
}

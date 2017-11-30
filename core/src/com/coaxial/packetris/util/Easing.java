package com.coaxial.packetris.util;

/**
 * Provides tools for smoothly easing objects in and out, or wiggling a value.
 */

public class Easing
{
    /**
     * Smoothly eases in a value, from {@code begin} to {@code begin + change} in the specified time.
     * The value change will become slower the higher {@code time} is.
     * @param time Current progress of time
     * @param begin the start value
     * @param change the value by which the start value should be altered
     * @param duration the duration of the operation
     * @return {@code begin + change} if {@code time >= duration}; an interpolated value between {@code begin} and {@code begin + change} otherwise
     */
    public static float easeIn(float time, float begin, float change, float duration)
    {
        return time >= duration ? begin + change : begin + change * ((float)(-1D*Math.pow(time/duration-1, 4)+1));
    }

    /**
     * Smoothly eases out a value, from {@code begin} to {@code begin + change} in the specified time.
     * The value change will become faster the higher {@code time} is.
     * @param time Current progress of time
     * @param begin the start value
     * @param change the value by which the start value should be altered
     * @param duration the duration of the operation
     * @return {@code begin + change} if {@code time >= duration}; an interpolated value between {@code begin} and {@code begin + change} otherwise
     */
    public static float easeOut(float time, float begin, float change, float duration)
    {
        return time >= duration ? begin + change : begin + change * (1-((float)(-1D*Math.pow(time/duration, 4) + 1)));
    }

    /**
     * Smoothly eases a value between {@code begin} and {@code begin + change}.
     * The value change is fastest if {@code |time % duration - 0.5 * duration| = duration * 0.25}.
     * The value will reach {@code begin + change} if {@code time % duration = duration / 2}.
     * @param time current progess of time
     * @param begin the start value
     * @param change the value by which the start value should be altered
     * @param duration the duration of one wiggle
     * @return
     */
    public static float easeInOutRepeated(float time, float begin, float change, float duration)
    {
        return (float)((Math.sin((2*time/duration-0.5)*Math.PI)+1)/2D*change+begin);
    }


}


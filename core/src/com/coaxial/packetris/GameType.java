package com.coaxial.packetris;

/**
 * An enum representing the different game modes, alongside with the fall down speed of the packets
 * in each mode.
 */

public enum GameType
{
    DEFAULT(300), SPEED(600);

    private int speed;

    GameType(int speed)
    {
        this.speed = speed;
    }

    public int getSpeed()
    {
        return speed;
    }
}

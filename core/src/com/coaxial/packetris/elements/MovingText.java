package com.coaxial.packetris.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Renders a moving up text to show how much points were earned with a placed packet.
 *
 * @see Packet
 * @see com.coaxial.packetris.GameScreen
 */

public class MovingText
{
    public static final int MOVE_UP_HEIGHT = 300;
    public static final float MOVE_UP_TIME = 2f;

    private String text;
    private int x; //curent position
    private int y;
    private float time;

    public MovingText(String text, int x, int y)
    {
        this.text = text;
        this.x = x;
        this.y = y;
    }

    /**
     * Renders the text of this object onto the batch.
     * Respects an opacity factor if needed for fading out in {@link com.coaxial.packetris.GameScreen}.
     * @param batch the SpriteBatch to draw the text on
     * @param font the font to use
     * @param opFactor optional opacity factor; set to 1 if you don't want to use it
     */
    public void render(SpriteBatch batch, BitmapFont font, float opFactor)
    {
        time += Gdx.graphics.getDeltaTime(); //count up live time of this text
        if (time > MOVE_UP_TIME) //set this object inactive so that it will be erased in the next frame in the GameScreen class
            return;
        float percent = time / MOVE_UP_TIME;
        font.setColor(1,1,1,opFactor * (1f-percent));
        font.draw(batch, text, x, y + percent * MOVE_UP_HEIGHT);
    }

    public boolean isActive()
    {
        return time < MOVE_UP_TIME;
    }
}

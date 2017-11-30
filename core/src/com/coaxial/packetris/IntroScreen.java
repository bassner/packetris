package com.coaxial.packetris;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.coaxial.packetris.util.Easing;

/**
 * A simple intro screen blending in "by COAXIAL" for a short time.
 * After that, it sets the current screen of the {@link PacketrisGame} instance to a new {@link MainMenuScreen}.
 */

public class IntroScreen implements Screen
{
    private final PacketrisGame game;
    private OrthographicCamera camera;
    private GlyphLayout layout = new GlyphLayout();

    private float time = 0;


    public IntroScreen(final PacketrisGame gam)
    {
        game = gam;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, PacketrisGame.GAME_WIDTH, PacketrisGame.GAME_HEIGHT);
    }

    /**
     * Render the text onto the batch provided by the {@link PacketrisGame} instance.
     * @param delta auto set by libgdx - time since last frame
     */
    @Override
    public void render(float delta)
    {
        //Clear screen
        Gdx.gl.glClearColor(0, 0, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //update camera and batch
        camera.update();
        game.getBatch().setProjectionMatrix(camera.combined);

        time += delta; //count up time

        game.getBatch().begin();

        //After 1 second, show the "by"
        if(time > 1)
        {
            float internalTime = time - 1;
            //Opacity: Fade out after 3 seconds
            game.getIngameFont().setColor(1,1,1, internalTime>1?(internalTime > 2?Math.max(0, 3-internalTime):1):internalTime);
            layout.setText(game.getIngameFont(), "by");
            game.getIngameFont().draw(game.getBatch(), "by", PacketrisGame.GAME_WIDTH / 2 - layout.width / 2,
                    Easing.easeIn(internalTime, PacketrisGame.GAME_HEIGHT * 0.5f + layout.height - 80, 100, 1));
        }

        //After 1.5 seconds, show the "COAXIAL"
        if(time > 1.5f)
        {
            float internalTime = time - 1.5f;
            game.getTitleFont().setColor(1,1,1,internalTime>1?(internalTime > 2?Math.max(0, 3-internalTime):1):internalTime);
            layout.setText(game.getTitleFont(), "COAXIAL");
            game.getTitleFont().draw(game.getBatch(), "COAXIAL", PacketrisGame.GAME_WIDTH / 2 - layout.width / 2,
                    Easing.easeIn(internalTime, PacketrisGame.GAME_HEIGHT * 0.5f - layout.height - 100, 100, 1));
        }

        //After all texts have faded out, switch to the main menu screen
        if(time >= 4.5f)
        {
            game.setScreen(new MainMenuScreen(game, false));
            dispose();
        }

        game.getBatch().end();
    }

    @Override
    public void resize(int width, int height)
    {
    }

    @Override
    public void show()
    {
    }

    @Override
    public void hide()
    {
    }

    @Override
    public void pause()
    {
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void dispose()
    {
    }
}

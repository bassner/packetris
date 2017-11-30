package com.coaxial.packetris;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.coaxial.packetris.util.Easing;

/**
 * The Main Welcome Menu Screen, providing the title of the game and
 * buttons to choose the game mode.
 */

public class MainMenuScreen implements Screen
{
    private final PacketrisGame game;
    private OrthographicCamera camera;
    private Texture title;
    private Texture standardbutton;
    private Texture speedButton;
    private Texture background;
    private Music backgroundMusic;


    private float time = 0;
    private float fadeOutStart = 0; //start time for disposing to a new GameScreen
    private boolean doNotExitImmediately; //if true, prevents the app exit when user holds the back button too long coming from a GameScreen
    private int lastButton = -1; //indicates the last pressed button. 0=STANDARD, 1=SPEED

    /**
     * Creates a new instance, loads resources.
     *
     * @param gam                  The main {@link PacketrisGame} instance.
     * @param doNotExitImmediately set to true if you want the back button to be released once before exiting; false otherwise
     */
    public MainMenuScreen(final PacketrisGame gam, boolean doNotExitImmediately)
    {
        game = gam;
        this.doNotExitImmediately = doNotExitImmediately;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, PacketrisGame.GAME_WIDTH, PacketrisGame.GAME_HEIGHT);

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("main_menu_bg.mp3"));
        title = new Texture(Gdx.files.internal("title.png"));
        standardbutton = new Texture(Gdx.files.internal("standardbutton.png"));
        speedButton = new Texture(Gdx.files.internal("speedbutton.png"));
        background = new Texture(Gdx.files.internal("raw_bg.png"));

        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
    }


    /**
     * Renders the menu and checks if the user presses one button or presses the back button.
     * Heavily using functions provided by the {@link Easing} class.
     *
     * @param delta auto set by libgdx - time since last frame
     */
    @Override
    public void render(float delta)
    {
        time += delta;
        float fadeOutPercent = fadeOutStart > 0 ? (time - fadeOutStart > 1 ? 1 : time - fadeOutStart) : 0;

        Gdx.gl.glClearColor(0, 0, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.getBatch().setProjectionMatrix(camera.combined);

        game.getBatch().begin();

        //Opacity: Fade in for 1.5s; respect disposing for a new GameScreen
        game.getBatch().setColor(1, 1, 1, time < 1.5f ? time / 1.5f : (1 - fadeOutPercent));

        game.getBatch().draw(background, 0, 0, PacketrisGame.GAME_WIDTH, PacketrisGame.GAME_HEIGHT);

        //After 1.5s, ease in the title
        if (time > 1.5f)
        {
            float internalTime = time - 1.5f;
            game.getBatch().setColor(1, 1, 1, internalTime > 1 ? 1 : internalTime); //Opacity: fade in for 1s
            game.getBatch().draw(title,
                    PacketrisGame.GAME_WIDTH * 0.125f - Easing.easeOut(fadeOutPercent, 0, //Respect disposing
                            -PacketrisGame.GAME_WIDTH * 0.125f - title.getWidth(), 1),
                    //Ease in vertically for 1 second
                    Easing.easeIn(internalTime, PacketrisGame.GAME_HEIGHT * 0.8f - 100, 100, 1),
                    PacketrisGame.GAME_WIDTH * 0.75f, (PacketrisGame.GAME_WIDTH * 0.75f) / title.getWidth() * title.getHeight());
        }

        //After 2 seconds, ease in the first button (STANDARD)
        if (time > 2f)
        {
            float internalTime = time - 2;
            if (lastButton == 0) //Mark the button red if selected
                game.getBatch().setColor(Color.RED);
            else
                game.getBatch().setColor(1, 1, 1, internalTime > 1 ? 1 : internalTime); //Opacity: fade in for 1s
            game.getBatch().draw(standardbutton,
                    PacketrisGame.GAME_WIDTH * 0.125f - Easing.easeOut(fadeOutPercent, 0, //Respect disposing
                            -PacketrisGame.GAME_WIDTH * 0.125f - standardbutton.getWidth(), 1),
                    //Ease in vertically for 1 seconds
                    Easing.easeIn(internalTime, PacketrisGame.GAME_HEIGHT * 0.5f - 100, 100, 1),
                    PacketrisGame.GAME_WIDTH * 0.75f, (PacketrisGame.GAME_WIDTH * 0.75f) / standardbutton.getWidth() * standardbutton.getHeight());
        }

        //After 2.3 seconds, ease in the second button (SPEED)
        if (time > 2.3f)
        {
            float internalTime = time - 2.3f;
            if (lastButton == 1) //Mark the button red if selected
                game.getBatch().setColor(Color.RED);
            else
                game.getBatch().setColor(1, 1, 1, internalTime > 1 ? 1 : internalTime); //Opacity: fade in for 1s
            game.getBatch().draw(speedButton,
                    PacketrisGame.GAME_WIDTH * 0.125f - Easing.easeOut(fadeOutPercent, 0, //Respect disposing
                            -PacketrisGame.GAME_WIDTH * 0.125f - speedButton.getWidth(), 1),
                    //Ease in vertically for 1 seconds
                    Easing.easeIn(internalTime, PacketrisGame.GAME_HEIGHT * 0.4f - 100, 100, 1),
                    PacketrisGame.GAME_WIDTH * 0.75f,
                    (PacketrisGame.GAME_WIDTH * 0.75f) / speedButton.getWidth() * speedButton.getHeight());
        }

        game.getBatch().end();


        //Process user input

        if (Gdx.input.isTouched())
        {
            //Get touch position
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            //Check that touch coordinate could hit a button by looking at the x position
            if (touchPos.x > PacketrisGame.GAME_WIDTH * 0.125f && touchPos.x < PacketrisGame.GAME_WIDTH * 0.875f)
            {
                //Match y coordinate of touch if it hits a button
                if (Math.abs(touchPos.y - (PacketrisGame.GAME_HEIGHT * 0.5f +
                        (PacketrisGame.GAME_WIDTH * 0.75f) / standardbutton.getWidth() *
                                standardbutton.getHeight() * 0.5))
                        < (PacketrisGame.GAME_WIDTH * 0.75f) / standardbutton.getWidth() * standardbutton.getHeight() * 0.5f)
                    lastButton = 0;
                else if (Math.abs(touchPos.y - (PacketrisGame.GAME_HEIGHT * 0.4f +
                        (PacketrisGame.GAME_WIDTH * 0.75f) / speedButton.getWidth() *
                                speedButton.getHeight() * 0.5)) < (PacketrisGame.GAME_WIDTH * 0.75f) /
                        speedButton.getWidth() * speedButton.getHeight() * 0.5f)
                    lastButton = 1;
                else
                    lastButton = -1; //no button selected
            } else
                lastButton = -1; //no button selected
        } else //screen not touched
        {
            if (lastButton != -1 && fadeOutStart == 0) //if button selected before releasing the touch
                fadeOutStart = time;                   //start the fading out to run the game soon
            if (fadeOutStart == 0) //if no button selected before touch
                lastButton = -1; //set no button selected
        }

        //If back button pressed - respect doNotExitImmediately once
        if (Gdx.input.isKeyPressed(Input.Keys.BACK))
        {
            if (!doNotExitImmediately) Gdx.app.exit();
        } else
            doNotExitImmediately = false;

        //If fully faded out
        if (fadeOutPercent >= 1)
        {
            if (lastButton == 0)
            {
                game.setScreen(new GameScreen(game, GameType.DEFAULT, false));
                dispose();
            } else if (lastButton == 1)
            {
                game.setScreen(new GameScreen(game, GameType.SPEED, false));
                dispose();
            }
        }
    }

    @Override
    public void resize(int width, int height)
    {
    }

    @Override
    public void show()
    {
        backgroundMusic.play();
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
        title.dispose();
        standardbutton.dispose();
        speedButton.dispose();
        background.dispose();
        backgroundMusic.dispose();
    }


}

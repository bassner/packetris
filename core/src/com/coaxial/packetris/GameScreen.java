package com.coaxial.packetris;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.coaxial.packetris.elements.MovingText;
import com.coaxial.packetris.elements.Packet;
import com.coaxial.packetris.util.Easing;

import java.util.Iterator;

/**
 * Represents a single game round. Renders all packets, backgrounds, point indicator, controls etc. that are necessary for the game to be playpable.
 * <p>
 * Displays results after the game finishes.
 * <p>
 * The class is able to represent to different game modes - DEFAULT (shown to the user as STANDARD) and SPEED.
 * While in the DEFAULT-Mode complex packet structures are possible, i
 * n the SPEED-Mode only simplified packet forms are used (so called block-Only-Packets). Please see {@link Packet} class.
 * Also, in the SPEED Mode the speed of the packets moving down is doubled, which makes it really difficult to place them in time.
 *
 * @see GameType
 * @see MovingText
 */
public class GameScreen implements Screen
{
    public static final int BOTTOM_SPACE = 200; //space under the main field
    public static final int SIDE_SPACE = 64; //space left and right of the main field
    public static final int CTRL_LENGTH = 140; //side length of control buttons

    private final PacketrisGame game;
    private final GameType type;

    private Texture right_move; //control button
    private Texture left_move;
    private Texture right_rotate;
    private Texture left_rotate;
    private Texture background;
    private Texture redo;
    private Texture cup;
    private Sound hitSound;
    private Sound gameOver;
    private Music backgroundMusic;
    private Music afterGameMusic;
    private OrthographicCamera camera; //needed by libgdx
    private Array<Packet> packets = new Array<Packet>(); //contains all currently existing packets
    private Array<MovingText> texts = new Array<MovingText>(); //contains all currently existing MovingTexts

    private int fullScore = 0; //count score
    private boolean touched = false; //touched in last frame?
    private boolean dospawn = true; //spawn new packets? = is the game over?
    private float initial_waiting = 2; //waiting before the first packet
    private float result_time = 0; //time passed since game over
    private float overall_time = 0; //time passed since game start
    private GlyphLayout glyphLayout; //glyphlayout used to calculate text widths

    private Preferences save = Gdx.app.getPreferences("ScoreSave"); //libgdx Preferences to save highscore
    private boolean newbest = false; //will be set to true if the newscore is the new highscore
    private int best; //current highscore
    private float fadeOutStarter = -1; //start time for fading out this instance for a replay (fade out)
    private float fadeOutForMainMenu = -1; //start time for fading out this instance for returning to main menu (fade out)

    private boolean restarted; //true if this instance has been created by a another GameScreen instance

    /**
     * Create a new game screen - the game is started immediately right after the creation.
     * Loads all resources and spawns the first packet.
     *
     * @param gam       the main game instance
     * @param type      the type of this game round
     * @param restarted should be true if this instance is created from anywhere else than a menu; false otherwise
     */
    public GameScreen(final PacketrisGame gam, GameType type, boolean restarted)
    {
        this.game = gam;
        this.restarted = restarted;
        this.type = type;
        best = save.getInteger("score" + type.toString(), 0);

        glyphLayout = new GlyphLayout();

        right_move = new Texture(Gdx.files.internal("right_move.png"));
        right_rotate = new Texture(Gdx.files.internal("right_rotate.png"));
        left_move = new Texture(Gdx.files.internal("left_move.png"));
        left_rotate = new Texture(Gdx.files.internal("left_rotate.png"));
        background = new Texture(Gdx.files.internal("newbg.png"));
        redo = new Texture(Gdx.files.internal("redo.png"));
        cup = new Texture(Gdx.files.internal("cup.png"));

        hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.wav"));
        gameOver = Gdx.audio.newSound(Gdx.files.internal("game_over.wav"));
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(type == GameType.DEFAULT ? "game_bg2.mp3" : "game_bg_fast.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(type == GameType.SPEED ? 0.25f : 0.4f);
        afterGameMusic = Gdx.audio.newMusic(Gdx.files.internal("main_menu_bg.mp3")); //same as main menu bg music
        afterGameMusic.setLooping(false);
        afterGameMusic.setVolume(0.5f);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, PacketrisGame.GAME_WIDTH, PacketrisGame.GAME_HEIGHT);

        spawnPacket();
    }


    /**
     * Main rendering loop of libgdx.
     * All game rendering is done here, besides the initiation of all logic updates.
     * Please see sectional comments for more details.
     *
     * @param delta auto-set by libgdx - time since last frame in seconds
     */
    @Override
    public void render(float delta)
    {
        //Clear the screen
        Gdx.gl.glClearColor(0, 0, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        /*
         * Check if user presses the back button; if yes, start initiation of fading out and fading out
         * before returning. See next block for screen changing statements.
         */
        if (Gdx.input.isKeyPressed(Input.Keys.BACK) && fadeOutForMainMenu == -1)
            fadeOutForMainMenu = overall_time;

        //Calculate fading out for main menu progress in percent
        float dfMPercent = fadeOutForMainMenu > 0 ? (overall_time > fadeOutForMainMenu + 1 ? 0 :
                1 - (overall_time - fadeOutForMainMenu)) : 1;


        //Do net let initial_waiting get < 0 since this may cause crazy behavior
        if (initial_waiting < 0) initial_waiting = 0;

        //set libgdx camera projection matrix for each rendering object
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getRenderer().setProjectionMatrix(camera.combined);
        game.getShapeRenderer().setProjectionMatrix(camera.combined);


        game.getBatch().begin(); //start the SpriteBatch. Renderings will be flushed to OpenGL by .end() later
        /*
         * Opacity: dfMPercent * (restarted ? 1 : (1 - Math.max(0, initial_waiting) / 2))
         *          Fading out for main menu factor
         *                        If restarted, do not fade in
         *                                        Otherwise: Fade in during initial waiting phase
         */
        game.getBatch().setColor(1, 1, 1,
                dfMPercent * (restarted ? 1 : (1 - Math.max(0, initial_waiting) / 2)));

        //Draw background image
        game.getBatch().draw(background, 0, 0, PacketrisGame.GAME_WIDTH, PacketrisGame.GAME_HEIGHT);

        /*
         * Drawing controls.
         * y: (i.e) restarted ? 40 : (initial_waiting > 1.5f ? -CTRL_LENGTH :
         *          Do not ease in vertically if restarted
         *                            Otherwise: Wait 0.5s before start moving in by putting the control under the bottom edge of the display
         *
         * Easing.easeIn(1.5f - initial_waiting, -CTRL_LENGTH, CTRL_LENGTH + 40, 1)
         * After waiting time: EaseIn vertically in 1 second
         */
        game.getBatch().draw(left_move, 40, restarted ? 40 : (initial_waiting > 1.5f ? -CTRL_LENGTH :
                        Easing.easeIn(1.5f - initial_waiting, -CTRL_LENGTH, CTRL_LENGTH + 40, 1)),
                CTRL_LENGTH, CTRL_LENGTH);
        game.getBatch().draw(left_rotate, CTRL_LENGTH + 160, restarted ? 40 : (initial_waiting > 1f ? -CTRL_LENGTH :
                Easing.easeIn(1f - initial_waiting, -CTRL_LENGTH, CTRL_LENGTH + 40, 1)), CTRL_LENGTH, CTRL_LENGTH);
        game.getBatch().draw(right_rotate, PacketrisGame.GAME_WIDTH - CTRL_LENGTH * 2 - 160, restarted ? 40 : (initial_waiting > 1f ? -CTRL_LENGTH :
                Easing.easeIn(1f - initial_waiting, -CTRL_LENGTH, CTRL_LENGTH + 40, 1)), CTRL_LENGTH, CTRL_LENGTH);
        game.getBatch().draw(right_move, PacketrisGame.GAME_WIDTH - CTRL_LENGTH - 40, restarted ? 40 : (initial_waiting > 1.5f ? -CTRL_LENGTH :
                Easing.easeIn(1.5f - initial_waiting, -CTRL_LENGTH, CTRL_LENGTH + 40, 1)), CTRL_LENGTH, CTRL_LENGTH);

        //close batch while drawing using the renderers since leaving it open may cause drawn shapes to hide
        game.getBatch().end();

        //Enable blending (using opacity)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        //set up both the Line-Renderer and the Filled Shape renderer
        game.getRenderer().begin(ShapeRenderer.ShapeType.Line);
        game.getShapeRenderer().begin(ShapeRenderer.ShapeType.Filled);

        /*
         * Opacity: dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 :
         *          Fading out for main menu factor
         *                        If fading out for new game round
         *                                             Fade out, but do not go lower than 0 since this may cause crazy behavior
         *
         *               fadeOutStarter - result_time + 1) : 1)
         *                                                   Use full opacity if not fading out
         *
         *  When reading the code, remember this structure as it will appear multiple times.
         */
        game.getRenderer().setColor(0, 0, 0,
                dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 :
                        fadeOutStarter - result_time + 1) : 1));

        //Draw rectangle around main field to show why the last packet does not fit
        game.getRenderer().rect(SIDE_SPACE, BOTTOM_SPACE, PacketrisGame.GAME_WIDTH - 2 * SIDE_SPACE, PacketrisGame.GAME_HEIGHT - BOTTOM_SPACE * 2);

        //Set cardboard color, then tell each packet to render itself. See Packet class
        game.getShapeRenderer().setColor(new Color(0.80f, 0.52f, 0.25f,
                dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : 1)));
        for (int i = 0; i < packets.size; ++i)
            packets.get(i).render(game.getRenderer(), game.getShapeRenderer(), dfMPercent * (
                    fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : 1));
        //Flush rendererd packets to OpenGL
        game.getShapeRenderer().end();
        game.getRenderer().end();
        //Disable blending before reenabling the SpriteBatch
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.getBatch().begin();

        //Tell Texts to render - see MovingText Class
        for (MovingText t : texts)
            t.render(game.getBatch(), game.getIngameFont(), dfMPercent);



        //Spawn new Packet if necessary
        if (dospawn && !packets.get(packets.size - 1).isMoving())
            spawnPacket();
        Packet p = packets.get(packets.size - 1);



        /*
         * Processing user input.
         */

        //Set touched-Variable to false if screen is not touched
        if (!Gdx.input.isTouched())
            touched = false;
        else if (dospawn && initial_waiting <= 0) //if screen touched, game still running and game already started
        {
            game.getBatch().setColor(1, 0, 0, dfMPercent); //respect fading out for main menu

            //get touchpos on screen
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            if (touchPos.y < BOTTOM_SPACE) //touch in control section
            {
                //match for pressed button
                if (touchPos.x < CTRL_LENGTH + 40)
                {
                    // with !touched we do not allow the packet to move more than once per touch
                    if (!touched) p.setCheckedPosX(p.getPosX() - 1, packets);
                    //draw control red to indicate successful touch
                    game.getBatch().draw(left_move, 40, 40, CTRL_LENGTH, CTRL_LENGTH);
                } else if (touchPos.x < CTRL_LENGTH * 2 + 160)
                {
                    if (!touched) p.rotate(false, packets);
                    game.getBatch().draw(left_rotate, CTRL_LENGTH + 160, 40, CTRL_LENGTH, CTRL_LENGTH);
                } else if (touchPos.x > PacketrisGame.GAME_WIDTH - CTRL_LENGTH - 40)
                {
                    if (!touched) p.setCheckedPosX(p.getPosX() + 1, packets);
                    game.getBatch().draw(right_move, PacketrisGame.GAME_WIDTH - CTRL_LENGTH - 40, 40, CTRL_LENGTH, CTRL_LENGTH);
                } else if (touchPos.x > PacketrisGame.GAME_WIDTH - 2 * CTRL_LENGTH - 160)
                {
                    if (!touched) p.rotate(true, packets);
                    game.getBatch().draw(right_rotate, PacketrisGame.GAME_WIDTH - CTRL_LENGTH * 2 - 160, 40, CTRL_LENGTH, CTRL_LENGTH);
                }
            }

            touched = true;
        } else if (!dospawn && fadeOutStarter == -1 && dfMPercent == 1) //if game over and nothing has been done on result screen
        {
            //get touch position
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            //check if replay button has been touched
            if (touchPos.y > PacketrisGame.GAME_HEIGHT * 0.1f
                    && touchPos.y < PacketrisGame.GAME_HEIGHT * 0.1 + redo.getHeight()
                    && touchPos.x > PacketrisGame.GAME_WIDTH / 2 - redo.getWidth() / 2
                    && touchPos.x < PacketrisGame.GAME_WIDTH / 2 - redo.getWidth() / 2 + redo.getWidth())
                fadeOutStarter = result_time; //initiate fading out for replay by setting up the start time value
        }

        //see line for an explanation of the opacity pattern
        game.getIngameFont().setColor(1, 1, 1, dfMPercent * (restarted ? 1 : (1 - Math.max(0, initial_waiting) / 2)));

        //render score text on near top screen edge
        //str: counting down score value back to zero while fading out for replay to avoid a hard cut to zero at replay
        game.getIngameFont().draw(game.getBatch(), "Score: " +
                        (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 :
                                Math.max(0, (int) Math.round((fadeOutStarter - result_time + 1) * fullScore * 1D) - 300)) : fullScore), 40,
                PacketrisGame.GAME_HEIGHT - 80);

        //Flush batch to oGL
        game.getBatch().end();


        /*
         * Section for checking packet hitboxes
         */
        //If game running and not over
        if (dospawn)
        {
            //Calculate height difference from last frame
            int d = Math.round(type.getSpeed() * Gdx.graphics.getDeltaTime());

            //move packet if initial_waiting is over; p has been assigned at beginning of method
            p.setPosY(p.getPosY() - ((initial_waiting -= delta) > 0 ? 0 : d));
            if (p.getPosY() < BOTTOM_SPACE + 1) //if packet hits the ground. Note that it may also hit other packets at the same time
            {
                //add points.  bonus point determined by summing up bottom line of packet (ground line) + overlays of any other packets
                handleNewPoints(p.setMoving(false) + (p.bottomLine() + calculateOverlaysOf(p)) * 10, p);
                p.setPosY(BOTTOM_SPACE);
            } else //if packet hits not the ground
            {
                //Check if it hits any of the other packets
                int n = calculateOverlaysOf(p);
                if (n > 0) //hits other packet
                {
                    //correct position if necessary
                    int realH = (p.getPosY() - BOTTOM_SPACE + d) -
                            //using modulo to get exact height of the line of the game grid where
                            //the packet should be placed
                            (p.getPosY() - BOTTOM_SPACE + d) % Packet.BLOCK_SIDE_LENGTH
                            + BOTTOM_SPACE;
                    p.setPosY(realH);

                    //if packet does not fit into the field
                    if (realH + p.getHeight() > PacketrisGame.GAME_HEIGHT - BOTTOM_SPACE)
                    {
                        hitSound.play();
                        dospawn = false; //set game over - stop spawning new packets

                        if (fullScore > best) //check highscore
                        {
                            best = fullScore;
                            newbest = true;
                            save.putInteger("score" + type.toString(), best);
                            save.flush(); //save new highscore
                        }

                        p.setRed(); //mark not fitting packet red
                        backgroundMusic.stop();
                        gameOver.play(); //play failure sound
                        afterGameMusic.play();
                    } else //if packet fits into field, stop moving the packet and add points to score
                        //100 points per block + 10 per downwards touching surface
                        handleNewPoints(p.setMoving(false) + n * 10, p);
                } //if p hits no other packet do nothing
            }
        }



        /*
         * GAME OVER - RENDERING RESULTS
        */
        else //if game over
        {
            //Grey out main field to highlight result information, respecting any fading
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            game.getRenderer().begin(ShapeRenderer.ShapeType.Filled);

            game.getRenderer().setColor(0, 0, 0,
                    dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : (fadeOutStarter - result_time + 1) * 0.8f) : (
                            result_time < 1 ? result_time * 0.8f : 0.8f)));
            game.getRenderer().rect(0, 0, PacketrisGame.GAME_WIDTH, PacketrisGame.GAME_HEIGHT);
            game.getRenderer().end();

            Gdx.gl.glDisable(GL20.GL_BLEND);


            game.getBatch().begin();

            //render game over text 1 second after game over
            if (result_time >= 1)
            {
                float internal_time = result_time - 1;
                game.getTitleFont().setColor(1, 1, 1,
                        dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : (
                                Easing.easeIn(internal_time, 0, 1, 1))));
                game.getTitleFont().draw(game.getBatch(), "GAME OVER", PacketrisGame.GAME_WIDTH * 0.2f,
                        Easing.easeIn(internal_time, PacketrisGame.GAME_HEIGHT * 0.8f - 100, 100, 1));
            }

            //render winners cup if it was a new highscore
            if (newbest && result_time >= 2.5f)
            {
                float internal_time = result_time - 2.5f;
                float scale = internal_time > 1 ? 1 : Easing.easeIn(internal_time, 2, -1, 1);
                game.getBatch().setColor(1, 1, 1,
                        dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : (internal_time > 1 ? 1 : internal_time)));
                game.getBatch().draw(cup, PacketrisGame.GAME_WIDTH / 2 - cup.getWidth() / 2, PacketrisGame.GAME_HEIGHT * 0.3f,
                        cup.getWidth() / 2, redo.getHeight() / 2, cup.getWidth(), cup.getHeight(), scale, scale, 1, 0, 0,
                        cup.getWidth(), cup.getHeight(), false, false);
            }

            //render own score
            if (result_time >= 1.5f)
            {
                float internal_time = result_time - 1.5f;
                game.getIngameFont().setColor(1, 1, 1, dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : (
                        Easing.easeIn(internal_time, 0, 1, 1))));
                glyphLayout.setText(game.getIngameFont(), "Score: " + fullScore);
                game.getIngameFont().draw(game.getBatch(), "Score: " + fullScore, PacketrisGame.GAME_WIDTH / 2 - glyphLayout.width / 2,
                        Easing.easeIn(internal_time, PacketrisGame.GAME_HEIGHT * 0.7f - 100, 100, 1));
            }

            //render best core, differently formatted if new highscore
            if (result_time >= 2.0f)
            {
                float internal_time = result_time - 2;
                //setting color to red if new highscore
                game.getIngameFont().setColor(1, newbest ? 0 : 1, newbest ? 0 : 1,
                        dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : (
                                Easing.easeIn(internal_time, 0, 1, 1))));
                game.getIngameFont().getData().setScale(newbest ? 1.5f : 1); //scale up best score if new highscore
                glyphLayout.setText(game.getIngameFont(), "Best: " + best);
                game.getIngameFont().draw(game.getBatch(), "Best: " + best, PacketrisGame.GAME_WIDTH / 2 - glyphLayout.width / 2,
                        Easing.easeIn(internal_time, newbest ? PacketrisGame.GAME_HEIGHT * 0.6f - 100 : PacketrisGame.GAME_HEIGHT * 0.52f - 100,
                                100, 1));
                game.getIngameFont().getData().setScale(1);
            }

            //display big "NEW HIGHSCORE" if it was a new highscore
            if (newbest && result_time >= 3.0f)
            {
                float internal_time = result_time - 3;
                game.getTitleFont().setColor(1, 0, 0, dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : (
                        internal_time > 1 ? 1 : internal_time)));
                game.getTitleFont().getData().setScale(Easing.easeIn(internal_time, 2, -1, 1));
                glyphLayout.setText(game.getTitleFont(), "NEW HIGHSCORE");
                game.getTitleFont().draw(game.getBatch(), "NEW HIGHSCORE", PacketrisGame.GAME_WIDTH / 2 - glyphLayout.width / 2,
                        PacketrisGame.GAME_HEIGHT * 0.42f);
                game.getTitleFont().getData().setScale(1);
            }

            //Display redo button
            if (result_time >= (newbest ? 4 : 2))
            {
                float internal_time = result_time - (newbest ? 4 : 2);
                //wiggle scale to motivate the user to touch
                float scale = result_time < 4 ? 0.8f : Easing.easeInOutRepeated(internal_time, 0.8f, 0.2f, 2);
                game.getBatch().setColor(1, 1, 1, dfMPercent * (fadeOutStarter > 0 ? (fadeOutStarter - result_time + 1 < 0 ? 0 : fadeOutStarter - result_time + 1) : (
                        Easing.easeIn(internal_time, 0, 1, 1))));
                game.getBatch().draw(redo, PacketrisGame.GAME_WIDTH / 2 - redo.getWidth() / 2,
                        Easing.easeIn(internal_time, PacketrisGame.GAME_HEIGHT * 0.1f - 100, 100, 1),
                        redo.getWidth() / 2, redo.getHeight() / 2, redo.getWidth(), redo.getHeight(),
                        scale, scale, 1, 0, 0, redo.getWidth(), redo.getHeight(), false, false);
            }

            result_time += delta; //count up current delta to time since game over
            game.getBatch().end();
        }

        //Delete inactive moving texts
        Iterator<MovingText> iter = texts.iterator();
        while (iter.hasNext())
            if (!iter.next().isActive())
                iter.remove();

        //Check if fading out for main menu is complete; if yes, set current screen to a main menu instance
        if (dfMPercent <= 0)
        {
            game.setScreen(new MainMenuScreen(game, true));
            dispose();
        }

        /*
         * Check if fading out result screen for a new round of the same game type (pressing replay)
         * is done; if yes, restart the game by creating a new GameScreen instance
         */

        if (fadeOutStarter > 0 && fadeOutStarter - result_time + 1 <= 0)
        {
            game.setScreen(new GameScreen(game, type, true));
            dispose();
        }


        overall_time += delta; //count up current delta to overall time
    }

    /**
     * Calculates the amount of blocks touching the ground and/or other packets downwards
     *
     * @param p The packet whose touches should be counted
     * @return the amount of touches
     */
    private int calculateOverlaysOf(Packet p)
    {
        int n = 0;
        for (Packet p2 : packets)
            if (p2 != p)
                n += p.overlapsCount(p2, true);
        return n;
    }

    /**
     * Spawn a new packet by adding a new random packet to the packets list.
     * Respects the game type when it comes to packet generation.
     * Sets up the new packet to fall down at a random position
     */
    private void spawnPacket()
    {
        int n = MathUtils.random(1, 3);
        int x = (int) Math.pow(2, n);
        int y = 16 / x;


        Packet p = Packet.random(x, y, type == GameType.SPEED);
        p.setPosX(MathUtils.random(0, ((PacketrisGame.GAME_WIDTH - SIDE_SPACE) / Packet.BLOCK_SIDE_LENGTH)
                - 1 - p.getWidth() / Packet.BLOCK_SIDE_LENGTH));
        p.setPosY(PacketrisGame.GAME_HEIGHT);
        packets.add(p);
    }

    /**
     * Adds new points to the full score counter, play the hit sound and adds a moving text indicating
     * how much points were earned.
     *
     * @param points The amount of points earned
     * @param p the packet that caused the earnings
     */
    private void handleNewPoints(int points, Packet p)
    {
        hitSound.play(0.7f);
        fullScore += points;
        texts.add(new MovingText("" + points, Math.round(p.getPosXInPixels() + p.getWidth() * 0.25f),
                Math.round(p.getPosY() + p.getHeight() * 0.75f)));
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
        right_move.dispose();
        left_move.dispose();
        right_rotate.dispose();
        left_rotate.dispose();
        background.dispose();
        hitSound.dispose();
        backgroundMusic.dispose();
        afterGameMusic.dispose();
        gameOver.dispose();
        redo.dispose();
        cup.dispose();
    }

}

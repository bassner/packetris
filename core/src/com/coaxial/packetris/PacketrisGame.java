package com.coaxial.packetris;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Game starter class.
 * Holds the sprite batch und ShapeRenderers to use.
 */
public class PacketrisGame extends Game {
	private SpriteBatch batch;
	private ShapeRenderer renderer;
	private ShapeRenderer lineRenderer;
	private BitmapFont ingameFont;
	private BitmapFont titleFont;
	public static final int GAME_WIDTH = 960;
	public static final int GAME_HEIGHT = 1600;

	/**
	 * Initiating the game by creating the SpriteBatch, all Renderers and all fonts.
	 */
	public void create() {
		Gdx.input.setCatchBackKey(true);
		batch = new SpriteBatch();
		renderer = new ShapeRenderer();
		lineRenderer = new ShapeRenderer();
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Whimsy.TTF"));
		FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
		param.size = 66;
		ingameFont = generator.generateFont(param);
		param.size = 100;
		titleFont = generator.generateFont(param);
		generator.dispose();

		//start rendering the game by switching to the IntroScreen
		this.setScreen(new IntroScreen(this));
	}

	public SpriteBatch getBatch()
	{
		return batch;
	}

	public ShapeRenderer getRenderer()
	{
		return renderer;
	}

	public ShapeRenderer getShapeRenderer()
	{
		return lineRenderer;
	}

	public BitmapFont getIngameFont()
	{
		return ingameFont;
	}

	public BitmapFont getTitleFont()
	{
		return titleFont;
	}

	public void render() {
		super.render();	}

	public void dispose() {
		batch.dispose();
		ingameFont.dispose();
		titleFont.dispose();
		renderer.dispose();
	}

}

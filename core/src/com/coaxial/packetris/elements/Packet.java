package com.coaxial.packetris.elements;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.coaxial.packetris.GameScreen;
import com.coaxial.packetris.PacketrisGame;
import com.coaxial.packetris.util.ArrayUtil;

import java.util.Arrays;

/**
 * Class to logical represent a packet.
 */

public class Packet
{
    public static final int BLOCK_SIDE_LENGTH = 64; //side length of a single block
    private boolean[][] shape; //boolean array representing the form/shape of the packet
    private int posX;
    private int posY;
    private boolean moving = true; //should be true if this is the currently moving packet
    private Rectangle comparison = new Rectangle(0, 0, 0, 0); //a pseudo rectange - later used for overlapping check
    private boolean red = false; //should be true if the packet should be rendered in a red color

    /**
     * Create a packet.
     * @param boundaryX Maximum width in blocks
     * @param boundaryY Maximum height in blocks
     */
    public Packet(int boundaryX, int boundaryY)
    {
        if (boundaryX < 1 || boundaryY < 1 ||
                boundaryX > (PacketrisGame.GAME_WIDTH - 2 * GameScreen.SIDE_SPACE) / BLOCK_SIDE_LENGTH
                || boundaryY > (PacketrisGame.GAME_HEIGHT - 2 * GameScreen.BOTTOM_SPACE) / BLOCK_SIDE_LENGTH)
            throw new IllegalArgumentException("Boundary invalid");
        shape = new boolean[boundaryX][boundaryY];
    }

    /**
     * Safely rotates a packet. Adjusts the position (bottom left corner) so that it looks like the packet
     * has been rotated around its center.
     * Does not perform the rotation if this is not possible due to collisions with other packets, but
     * adjusts the position to avoid collisions with any map edge.
     *
     * @param clockwise Should be true if the packet is to be rotated clockwise; false otherwise (counter-clockwise)
     * @param packets A list of all other packets for collision checks.
     */
    public void rotate(boolean clockwise, Array<Packet> packets)
    {
        boolean[][] oldshape = ArrayUtil.cloneArray(shape);
        boolean[][] newshape = ArrayUtil.cloneArray(shape);
        if (!clockwise)
            for (boolean[] aNewshape : newshape) ArrayUtil.reverse(aNewshape);
        newshape = ArrayUtil.transpose(newshape);
        if (clockwise)
            for (boolean[] aNewshape : newshape) ArrayUtil.reverse(aNewshape);

        int newposx = Math.max(0, Math.min((PacketrisGame.GAME_WIDTH - 2 * GameScreen.SIDE_SPACE) / BLOCK_SIDE_LENGTH - newshape.length,
                (getPosX() + oldshape.length / 2) - newshape.length / 2));
        int newposy = shape[0].length * BLOCK_SIDE_LENGTH / 2 + getPosY() - newshape[0].length * BLOCK_SIDE_LENGTH / 2;
        int oldPosX = getPosX();
        int oldPosY = getPosY();
        setPosX(newposx);
        setPosY(newposy);
        shape = newshape;

        for (Packet p : packets)
            if (p != this)
                if (overlaps(p))
                {
                    setPosX(oldPosX);
                    setPosY(oldPosY);
                    shape = oldshape;
                    return;
                }
    }

    public void setRed()
    {
        red = true;
    }

    /**
     * Adds or removes a block to/from the shape at the specified position.
     * @param x the width value in blocks
     * @param y the height value in blocks
     * @param add true if the block should be added; false if it should be removed
     */
    private void setShapeAt(int x, int y, boolean add)
    {
        if (x >= shape.length || y >= shape[0].length || y < 0 || x < 0)
            throw new IllegalArgumentException("Out of bounds");
        shape[x][y] = add;
    }

    /**
     * Renders this packet using the specified renderers. Fills out every block with the
     * {@code renderer} and outlines the boundaries with lines using the {@code borderRenderer}.
     * Any color configurations must be performed BEFORE calling this method.
     * Note: This method respects the {@code red} attribute of this packet.
     *
     * @param borderRenderer the renderer that should be used to draw the boundaries of the packet
     * @param renderer the renderer that should be used to fill the blocks
     * @param opacity the opacity that should be respected if the color is switched to red.
     */
    public void render(ShapeRenderer borderRenderer, ShapeRenderer renderer, float opacity)
    {
        if(red)
            renderer.setColor(1,0,0,opacity);
        for (int i = 0; i < shape.length; ++i)
            for (int j = 0; j < shape[0].length; ++j)
                if (shape[i][j])
                {
                    int cornerX = posX * BLOCK_SIDE_LENGTH + i * BLOCK_SIDE_LENGTH + GameScreen.SIDE_SPACE;
                    int cornerY = posY + j * BLOCK_SIDE_LENGTH;
                    renderer.rect(cornerX, cornerY, BLOCK_SIDE_LENGTH, BLOCK_SIDE_LENGTH);
                    if(i==0 || !shape[i-1][j])
                        borderRenderer.line(cornerX,cornerY,cornerX,cornerY+ BLOCK_SIDE_LENGTH);
                    if(i==shape.length-1 || !shape[i+1][j])
                        borderRenderer.line(cornerX+ BLOCK_SIDE_LENGTH,cornerY,cornerX+ BLOCK_SIDE_LENGTH,cornerY+ BLOCK_SIDE_LENGTH);
                    if(j==0 || !shape[i][j-1])
                        borderRenderer.line(cornerX, cornerY, cornerX+ BLOCK_SIDE_LENGTH, cornerY);
                    if(j==shape[0].length-1 || !shape[i][j+1])
                        borderRenderer.line(cornerX, cornerY + BLOCK_SIDE_LENGTH, cornerX + BLOCK_SIDE_LENGTH, cornerY+ BLOCK_SIDE_LENGTH);
                }
    }


    /**
     * Check if this packet overlaps another.
     * @param other the other packet
     * @return true if this packets overlaps the other packets; false otherwise
     */
    public boolean overlaps(Packet other)
    {
        return overlapsCount(other, false) > 0;
    }

    /**
     * Counts the amount of downward block edges overlapping another packet.
     * @param other an other packet to be checked for overlappings
     * @param yPaddingEnabled should be true if this packet should be considered 5% of an blocks height lower than it really is.
     * @return the amount of downward block edges overlapping the other packet
     */
    public int overlapsCount(Packet other, boolean yPaddingEnabled)
    {
        int n = 0;
        for (int i = 0; i < shape.length; ++i)
            for (int j = 0; j < shape[0].length; ++j)
            {
                if (shape[i][j])
                {
                    int rectPosX = posX * BLOCK_SIDE_LENGTH + i * BLOCK_SIDE_LENGTH;
                    int rectPosY = posY + j * BLOCK_SIDE_LENGTH -
                            (yPaddingEnabled ? Math.round(BLOCK_SIDE_LENGTH * 0.05f) : 0);
                    comparison.set(rectPosX, rectPosY, BLOCK_SIDE_LENGTH, BLOCK_SIDE_LENGTH);
                    if (other.overlapsRectangle(comparison))
                        n++;
                }
            }
        return n;
    }

    /**
     * Check whether this packet overlaps the specified rectangle.
     * @param r the rectangle
     * @return true if this packet overlaps the rectangle; false otherwise
     */
    public boolean overlapsRectangle(Rectangle r)
    {
        for (int i = 0; i < shape.length; ++i)
            for (int j = 0; j < shape[0].length; ++j)
            {
                if (shape[i][j])
                {
                    int rectPosX = posX * BLOCK_SIDE_LENGTH + i * BLOCK_SIDE_LENGTH;
                    int rectPosY = posY + j * BLOCK_SIDE_LENGTH;
                    comparison.set(rectPosX, rectPosY, BLOCK_SIDE_LENGTH, BLOCK_SIDE_LENGTH);
                    if (comparison.overlaps(r))
                        return true;
                }
            }
        return false;
    }

    /**
     * Calculates the length of the lowest line of blocks of this packet.
     * @return the amount
     */
    public int bottomLine()
    {
        int n = 0;
        for (boolean[] col : shape)
            if (col[0]) n++;
        return n;
    }


    /**
     * Creates a random packet. If {@code blockOnly} is set, the generated packet will have a convex shape.
     * @param xBoundary maximum width in blocks
     * @param yBoundary maximum height in blocks
     * @param blockOnly true if only convex shaptes should be used; false otherwise
     * @return the random packet
     */
    public static Packet random(int xBoundary, int yBoundary, boolean blockOnly)
    {
        Packet newPacket = new Packet(xBoundary, yBoundary);
        int set = 0; //counts how many blocks have already been set
        //only multiples of two in convex shapes in all possible shapes with more than one line or
        //more than one column - therefore we limit the target to multiples of two
        int target = blockOnly ? 2*MathUtils.random(1, 4) : MathUtils.random(4, 8);
        if (blockOnly)
        {
            int x = Math.min((int)(Math.floor(xBoundary/2.0))*2, //adjust to multiple of two and respect this adjusted boundary
                    (int) Math.floor(2*MathUtils.random(1, target/2)));
            int y = Math.min((int)(Math.floor(yBoundary/2.0))*2, target / x);
            for (int i = 0; i < x; ++i)
                for (int j = 0; j < y; ++j)
                    newPacket.setShapeAt(i, j, true);
        } else
            while (set < target)
            {
                int x = MathUtils.random(0, xBoundary - 1);
                int y = set == 0 ? 0 : MathUtils.random(0, yBoundary - 1);

                //Only set blocks that touch at least one other block
                if (set == 0 || (!newPacket.isShapeActive(x, y) &&
                        (newPacket.isShapeActive(x - 1, y) ||
                                newPacket.isShapeActive(x + 1, y) || newPacket.isShapeActive(x, y - 1)) &&
                        (y == 0 || newPacket.isShapeActive(x, y - 1))))
                {
                    newPacket.setShapeAt(x, y, true);
                    set++;
                }
            }

        //Find the first used column in the random shape
        int fcol = 0;
        out:
        while (true)
        {
            for (int i = 0; i < newPacket.shape[fcol].length; ++i)
                if (newPacket.isShapeActive(fcol, i))
                    break out;
            fcol++;
        }

        //Find the last used column in the random shape
        int lcol = newPacket.shape.length - 1;
        out:
        while (true)
        {
            for (int i = 0; i < newPacket.shape[lcol].length; ++i)
                if (newPacket.isShapeActive(lcol, i))
                    break out;
            lcol--;
        }

        //Find the last used row of the new shape
        int lrow = newPacket.shape[0].length - 1;
        out:
        while (true)
        {
            for (int i = 0; i < newPacket.shape.length; ++i)
                if (newPacket.isShapeActive(i, lrow))
                    break out;
            lrow--;
        }

        //construct new shape with adjusted size
        boolean[][] newshape = new boolean[lcol - fcol + 1][lrow + 1];
        for (int i = 0; i < (lcol - fcol + 1); ++i)
            for (int j = 0; j < (lrow + 1); ++j)
                newshape[i][j] = newPacket.isShapeActive(fcol + i, j);
        newPacket.shape = newshape;

        return newPacket;
    }

    /**
     * Check if there is an block set up at the specified position in the shape
     * @param x x coordinate of the position to check in blocks
     * @param y y coordingate of the position to check in blocks
     * @return true if there is an block at the specified position; false otherwise
     */
    private boolean isShapeActive(int x, int y)
    {
        if (x < 0 || y < 0 || x >= shape.length || y >= shape[0].length) return false;
        return shape[x][y];
    }


    public int getPosXInPixels()
    {
        return posX * BLOCK_SIDE_LENGTH;
    }

    public int getPosX()
    {
        return posX;
    }

    public void setPosX(int posX)
    {
        this.posX = posX;
    }

    /**
     * Sets the position only if this would not cause an overlapping with other packets
     * @param posX the new position
     * @param packets a list of all other packets
     */
    public void setCheckedPosX(int posX, Array<Packet> packets)
    {
        if (posX < 0 || posX > ((PacketrisGame.GAME_WIDTH - 2 * GameScreen.SIDE_SPACE) - getWidth()) / BLOCK_SIDE_LENGTH)
            return;
        int oldPosX = this.posX;
        setPosX(posX);
        for (Packet p : packets)
            if (p != this)
                if (overlaps(p))
                {
                    setPosX(oldPosX);
                    return;
                }
    }

    public int getPosY()
    {
        return posY;
    }

    public int getHeight()
    {
        return BLOCK_SIDE_LENGTH * shape[0].length;
    }

    public int getWidth()
    {
        return BLOCK_SIDE_LENGTH * shape.length;
    }

    public void setPosY(int posY)
    {
        this.posY = posY;
    }

    public boolean isMoving()
    {
        return moving;
    }

    /**
     * If {@code moving} is set to false, this method returns the amount of base points (no bonus points)
     * the player earned by placing this packet.
     * @param moving true if the packet should keep moving; false otherwise
     * @return amount of points; 0 if !moving
     */
    public int setMoving(boolean moving)
    {
        this.moving = moving;
        if (!moving)
        {
            return 100 * Arrays.stream(shape).mapToInt(shapeColumn ->
            {
                int sum = 0;
                for (boolean b : shapeColumn)
                    if (b) sum++;
                return sum;
            }).reduce((a, b) -> a + b).getAsInt();
        }
        return 0;
    }
}

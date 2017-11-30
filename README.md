# Packetris
A tetris like android game, simulating the situation of a packet delivery man loading his vehicle. 
Packets must be arranged as they are pushed into the vehicle to fit as much packets as possible. The more packets you fit into the available space, the higher your score becomes.

## About the game
### Modes
The game contains two modes:
- Standard: In this mode all packages fall down with moderate speed, but may have a complex, concave shape.
- Speed: In this mode the fall down speed of the packages is doubled, but you only have to deal with simplified, convex shapes.

You will notice that the game seems really easy at the beginning, but the more packets have been placed, the shorter is the time for you to arrange new packets. Especially in the speed mode, you will see that this almost becomes impossible in the second half of a round.
### Scoring
You will receive points for each successfully placed packet. You may notice that every packet consists of multiple small square blocks. For each of them, you will earn 100 points. Additionally, for each downward facing surface touching the wall or any other package, you will get 10 additional bonus points. This means: The better you place your packets and the better you fill left space, the more points you get. Happy Scoring!

### Controls
To control the game, just use the movement and rotation controls on the bottom edge of the game screen. With them, you can easily rotate and move the currently falling down package.

## Installation
Just use the the <code>Packetris.apk</code> provided in the latest release in <code>release/</code> and install it on your Android Phone.
You need at least Android 7.0 (Sdk Version 24) to run the game. For older versions of Android, you may be able to compile it for this version yourself.

## License
Please see the License File.

import tester.*;
import javalib.funworld.*;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Random;

interface IGamePiece {
  int bulletVelocity = 8;
  int shipVelocity = bulletVelocity / 2;
  int screenWidth = 500;
  int screenHeight = 300;
  int shipRadius = screenHeight / 30;

  // compute the radius of this IGamePiece
  int computeRadius();
}

abstract class AGamePiece implements IGamePiece {
  int x;
  int y;

  AGamePiece(int x, int y) {
    this.x = x;
    this.y = y;
  }

  // compute the radius of the IGamePiece
  public int computeRadius() {
    return shipRadius;
  }

  // is this IGamePiece off the screen?
  public boolean isOffScreen() {
    return this.x > (screenWidth + this.computeRadius()) || this.x < (0 - this.computeRadius())
        || this.y > (screenHeight + this.computeRadius()) || this.y < (0 - this.computeRadius());
  }

  // has this game piece collided with the given game piece?
  public boolean hasCollided(AGamePiece other) {
    return this.computeDist(other) <= this.computeRadius() + other.computeRadius();
  }

  // compute the distance between the centers of this game piece and the given one
  public double computeDist(AGamePiece other) {
    return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
  }
}

// the x- and y- coordinates of a Ship, and its direction
class Ship extends AGamePiece {
  boolean isGoingLeft;

  Ship(int x, int y, boolean isGoingLeft) {
    super(x, y);
    this.isGoingLeft = isGoingLeft;
  }

  // Draws the Ships in this game onto the accumulated image of the game so far
  public WorldScene drawShip(WorldScene acc) {
    return acc.placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN), this.x, this.y);
  }

  // updates the coordinates of the ship depending on its velocity
  public Ship moveShip() {
    if (this.isGoingLeft) {
      return new Ship(this.x - shipVelocity, this.y, this.isGoingLeft);
    }
    else {
      return new Ship(this.x + shipVelocity, this.y, this.isGoingLeft);
    }
  }
}

// the x- and y- coordinates of a bullet, the angle its moving at, and how many past ships the 
// bullet has hit
class Bullet extends AGamePiece {
  double direction;
  int pastShips;

  Bullet(int x, int y, double direction, int pastShips) {
    super(x, y);
    this.direction = direction;
    this.pastShips = pastShips;
  }

  // Draws the Bullets in this game onto the accumulated image of the game so far
  WorldScene drawBullet(WorldScene acc) {
    return acc.placeImageXY(new CircleImage(this.computeRadius(), OutlineMode.SOLID, Color.PINK),
        this.x, this.y);
  }

  // compute the radius of this Bullet depending on how many Ships it has
  // previously hit
  public int computeRadius() {
    if (this.pastShips >= 4) {
      return 10;
    }
    else {
      return (2 * this.pastShips) + 2;
    }
  }

  // moves the bullet depending on its angle and velocity
  public Bullet moveBullet() {
    return new Bullet(this.x + this.changeX(), this.y + changeY(), this.direction, this.pastShips);
  }

  // calculate the change in the Bullets x-position
  public int changeX() {
    return (int) (bulletVelocity * Math.cos(Math.toRadians(this.direction)));
  }

  // calculate the change in the Bullets y-position
  public int changeY() {
    return (int) (bulletVelocity * Math.sin(Math.toRadians(this.direction)));
  }

  // create an explosion of Bullets at the location of this Bullet, with the given
  // int representing an accumulation of how many bullets have been made
  public ILoBullet explosion(int toBeMade, ILoBullet others) {
    if (toBeMade == 0) {
      return others;
    }
    else {
      return new ConsLoBullet(this.newBullet(this.pastShips, toBeMade),
          this.explosion(toBeMade - 1, others));
    }
  }

  // create a new Bullet whose direction and past ships depends on the supplied
  // values of the previous ships and which number Bullet is being created in the
  // explosion
  public Bullet newBullet(int previous, int count) {
    return new Bullet(this.x, this.y, this.determineNewDirection(count), previous + 1);
  }

  // determine the direction of this Bullet based on how many Bullets have been
  // fired. The int given must be positive
  public double determineNewDirection(int count) {
    return count * (360.0 / ((this.pastShips + 2) * 1.0));
  }
}

interface ILoShip {

  // Draws the Ships and Bullets in this game onto the accumulated image of the
  // game so far
  WorldScene drawShips(WorldScene acc);

  // move all Ships in this list
  ILoShip moveShips();

  // remove all Ships in this list that are off the screen
  ILoShip removeShips();

  // how many ships in this list have collided with a Bullet in the given list?
  int howManyDestroyed(ILoBullet bullets);

  // create one explosion of Bullets where the given Ship has collided with a
  // Bullet in the list
  ILoBullet oneExplosion(Bullet b, ILoBullet others);

  // remove all Ships in this list that have been hit by a bullet
  ILoShip removeHitShips(ILoBullet bullets);

}

class MtLoShip implements ILoShip {

  // Draws the Ships and Bullets in this game onto the accumulated image of the
  // game so far
  public WorldScene drawShips(WorldScene acc) {
    return acc;
  }

  // move all Ships in this empty list
  public ILoShip moveShips() {
    return new MtLoShip();
  }

  // remove all Ships in this list that are off the screen
  public ILoShip removeShips() {
    return new MtLoShip();
  }

  // how many ships in this empty list have collided with a Bullet in the given
  // list?
  public int howManyDestroyed(ILoBullet bullets) {
    return 0;
  }

  // create one explosion of Bullets where the given Bullet has collided with a
  // Ship in this list
  public ILoBullet oneExplosion(Bullet b, ILoBullet others) {
    return new ConsLoBullet(b, others);
  }

  // remove all Ships in this list that have been hit by a bullet
  public ILoShip removeHitShips(ILoBullet bullets) {
    return new MtLoShip();
  }
}

class ConsLoShip implements ILoShip {
  Ship first;
  ILoShip rest;

  ConsLoShip(Ship first, ILoShip rest) {
    this.first = first;
    this.rest = rest;
  }

  // Draws the Ships and Bullets in this game onto the accumulated image of the
  // game so far
  public WorldScene drawShips(WorldScene acc) {
    return this.rest.drawShips(this.first.drawShip(acc));
  }

  // move all Ships in this list
  public ILoShip moveShips() {
    return new ConsLoShip(this.first.moveShip(), this.rest.moveShips());
  }

  // remove all Ships in this list that are off the screen
  public ILoShip removeShips() {
    if (this.first.isOffScreen()) {
      return this.rest.removeShips();
    }
    else {
      return new ConsLoShip(this.first, this.rest.removeShips());
    }
  }

  // how many ships in this list have collided with a Bullet in the given list?
  public int howManyDestroyed(ILoBullet bullets) {
    return bullets.hasCollidedWith(this.first) + this.rest.howManyDestroyed(bullets);
  }

  // create one explosion of Bullets where the given Bullet has collided with a
  // Ship in this list
  public ILoBullet oneExplosion(Bullet b, ILoBullet others) {
    if (this.first.hasCollided(b)) {
      return b.explosion(b.pastShips + 2, others);
    }
    else {
      return this.rest.oneExplosion(b, others);
    }
  }

  // remove all Ships in this list that have been hit by a bullet
  public ILoShip removeHitShips(ILoBullet bullets) {
    if (bullets.hasCollided(this.first)) {
      return this.rest.removeHitShips(bullets);
    }
    else {
      return new ConsLoShip(this.first, this.rest.removeHitShips(bullets));
    }
  }
}

interface ILoBullet {

  // Draws the Ships and Bullets in this game onto the accumulated image of the
  // game so far
  WorldScene drawBullets(WorldScene acc);

  // are there any bullets left on the screen?
  boolean anyOnScreen();

  // move all bullets in the list
  ILoBullet moveBullets();

  // remove all Bullets in this list that have gone off the screen
  ILoBullet removeBullets();

  // how many Bullets in this list have collided with the given Ship?
  int hasCollidedWith(Ship s);

  // cause an explosion of Bullets with all in this list that have collided with a
  // ship in the given list
  ILoBullet makeExplosions(ILoShip ships);

  // have any bullets in this list collided with the given ship
  boolean hasCollided(Ship s);

}

class MtLoBullet implements ILoBullet {

  // Draws the Ships and Bullets in this game onto the accumulated image of the
  // game so far
  public WorldScene drawBullets(WorldScene acc) {
    return acc;
  }

  // are there any bullets left on the screen?
  public boolean anyOnScreen() {
    return false;
  }

  // move all bullets in this empty list
  public ILoBullet moveBullets() {
    return this;
  }

  // remove all Bullets in this list that have gone off the screen
  public ILoBullet removeBullets() {
    return this;
  }

  // how many Bullets in this list have collided with the given Ship?
  public int hasCollidedWith(Ship s) {
    return 0;
  }

  // cause an explosion of Bullets with all in this list that have collided with a
  // ship in the given list
  public ILoBullet makeExplosions(ILoShip ships) {
    return new MtLoBullet();
  }

  // have any bullets in this list collided with the given ship
  public boolean hasCollided(Ship s) {
    return false;
  }
}

class ConsLoBullet implements ILoBullet {
  Bullet first;
  ILoBullet rest;

  ConsLoBullet(Bullet first, ILoBullet rest) {
    this.first = first;
    this.rest = rest;
  }

  // Draws the Ships and Bullets in this game onto the accumulated image of the
  // game so far
  public WorldScene drawBullets(WorldScene acc) {
    return this.rest.drawBullets(this.first.drawBullet(acc));
  }

  // are there any bullets left on the screen?
  public boolean anyOnScreen() {
    return true;
  }

  // move all bullets in this non-empty bullets
  public ILoBullet moveBullets() {
    return new ConsLoBullet(this.first.moveBullet(), this.rest.moveBullets());
  }

  // remove all Bullets in this list that have gone off the screen
  public ILoBullet removeBullets() {
    if (this.first.isOffScreen()) {
      return this.rest.removeBullets();
    }
    else {
      return new ConsLoBullet(this.first, this.rest.removeBullets());
    }
  }

  // how many Bullets in this list have collided with the given Ship?
  public int hasCollidedWith(Ship s) {
    if (this.first.hasCollided(s)) {
      return 1 + this.rest.hasCollidedWith(s);
    }
    else {
      return this.rest.hasCollidedWith(s);
    }
  }

  // cause an explosion of Bullets with all in this list that have collided with a
  // ship in the given list
  public ILoBullet makeExplosions(ILoShip ships) {
    return ships.oneExplosion(this.first, this.rest.makeExplosions(ships));
  }

  // have any bullets in this list collided with the given ship
  public boolean hasCollided(Ship s) {
    return this.first.hasCollided(s) || this.rest.hasCollided(s);
  }
}

class NBullets extends World {
  int bulletsLeft;
  int shipsDestroyed;
  ILoBullet bullets;
  ILoShip ships;
  int time;
  Random rand;

  NBullets(int bulletsLeft, int shipsDestroyed, ILoBullet bullets, ILoShip ships, int time,
      Random rand) {
    this.bulletsLeft = bulletsLeft;
    this.shipsDestroyed = shipsDestroyed;
    this.bullets = bullets;
    this.ships = ships;
    this.time = time;
    this.rand = rand;
  }

  NBullets(int bulletsLeft) {
    this(bulletsLeft, 0, new MtLoBullet(), new MtLoShip(), 0, new Random());
  }

  ////////////////////////////////////////////////////////////////////////////

  // draws the current state of the game
  public WorldScene makeScene() {
    return this.drawGame().placeImageXY(this.drawText(), 110, 280);
  }

  // draws the Ships and Bullets of the current game
  public WorldScene drawGame() {
    return this.ships.drawShips(this.bullets.drawBullets(new WorldScene(500, 300)));
  }

  // draws the information in the current game
  public WorldImage drawText() {
    return new TextImage("bullets left: " + Integer.toString(this.bulletsLeft)
        + "; ships destroyed: " + Integer.toString(this.shipsDestroyed), 13, Color.BLACK);
  }

  ////////////////////////////////////////////////////////////////////////////

  // update the world state when space is pressed
  public NBullets onKeyEvent(String ke) {
    if (ke.equals(" ")) {
      return this.handleSpace();
    }
    else {
      return this;
    }
  }

  // Adds a new Bullet to the list when space is pressed, if the user has any left
  public NBullets handleSpace() {
    if (this.bulletsLeft > 0) {
      return new NBullets(this.bulletsLeft - 1, this.shipsDestroyed, this.newBullet(), this.ships,
          this.time, this.rand);
    }
    else {
      return this;
    }
  }

  // adds a new bullet to the list when the space bar has been pressed
  public ILoBullet newBullet() {
    return new ConsLoBullet(new Bullet(250, 300, 270.0, 0), this.bullets);
  }

  ////////////////////////////////////////////////////////////////////////////

  // creates the ending of the game if it is finished
  public WorldEnd worldEnds() {
    if (this.isGameOver()) {
      return new WorldEnd(true, this.drawEnding());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  // draws the ending scene when the game is over
  public WorldScene drawEnding() {
    return this.drawGame().placeImageXY(new TextImage("You ran out of bullets!", 20, Color.RED),
        250, 150);
  }

  // is this game over?
  public boolean isGameOver() {
    return this.bulletsLeft == 0 && !this.bullets.anyOnScreen();
  }

  ////////////////////////////////////////////////////////////////////////////

  // advance the time of the program
  public NBullets onTick() {
    return this.advanceTimer().spawnShips().moveAll().removeOffScreen().handleCollisions();

  }

  // advances the timer in the current state of the game
  public NBullets advanceTimer() {
    return new NBullets(this.bulletsLeft, this.shipsDestroyed, this.bullets, this.ships,
        this.time + 1, this.rand);
  }

  // move all Bullets and Ships in the game
  public NBullets moveAll() {
    return new NBullets(this.bulletsLeft, this.shipsDestroyed, this.bullets.moveBullets(),
        this.ships.moveShips(), this.time, this.rand);
  }

  // remove all Bullets and Ships that have moved off the screen
  public NBullets removeOffScreen() {
    return new NBullets(this.bulletsLeft, this.shipsDestroyed, this.bullets.removeBullets(),
        this.ships.removeShips(), this.time, this.rand);
  }

  // spawns new random Ships at a constant time interval
  public NBullets spawnShips() {
    if (this.time % 28 == 0) {
      return new NBullets(this.bulletsLeft, this.shipsDestroyed, this.bullets,
          this.generateShips(this.rand.nextInt(3) + 1), this.time, this.rand);
    }
    else {
      return this;
    }
  }

  // generates a given amount of Ships in a list
  public ILoShip generateShips(int amount) {
    if (amount == 0) {
      return this.ships;
    }
    else {
      return new ConsLoShip(this.randomShip(), this.generateShips(amount - 1));
    }
  }

  // creates a new Ship with a random height and direction
  public Ship randomShip() {
    int leftOrRight = this.rand.nextInt(2);
    int randomHeight = this.rand.nextInt(230) + 30;

    if (leftOrRight == 0) {
      return new Ship(509, randomHeight, true);
    }
    else {
      return new Ship(-9, randomHeight, false);
    }
  }

  // handle all Collisions of the game
  public NBullets handleCollisions() {
    return new NBullets(this.bulletsLeft,
        this.shipsDestroyed + this.ships.howManyDestroyed(this.bullets),
        this.bullets.makeExplosions(this.ships), this.ships.removeHitShips(this.bullets), this.time,
        this.rand);
  }

}

class ExamplesNBullets {

  WorldScene ws = new WorldScene(500, 300);
  Random rand1 = new Random(10);
  Random rand2 = new Random(20);
  Random rand3 = new Random(30);
  Random rand4 = new Random(40);
  Random rand5 = new Random(50);
  Random rand6 = new Random(60);
  Random rand7 = new Random(70);
  Random rand8 = new Random(80);
  Random rand9 = new Random(90);
  Random rand10 = new Random(100);

  Ship ship1 = new Ship(250, 250, true);
  Ship ship2 = new Ship(100, 200, false);
  Ship ship3 = new Ship(50, 50, true);
  Ship ship4 = new Ship(400, 180, false);
  Ship ship5 = new Ship(1000, 1000, true);

  Bullet bullet1 = new Bullet(200, 250, 90, 0);
  Bullet bullet2 = new Bullet(250, 100, 75, 1);
  Bullet bullet3 = new Bullet(100, 280, 135, 4);
  Bullet bullet4 = new Bullet(150, 230, 0, 7);
  Bullet bullet5 = new Bullet(1000, 1000, 33, 2);
  Bullet bullet6 = new Bullet(250, 251, 45, 0);

  IGamePiece igpship1 = new Ship(250, 250, true);
  IGamePiece igpship2 = new Ship(100, 200, false);
  IGamePiece igpbullet1 = new Bullet(200, 250, 90, 0);
  IGamePiece igpbullet2 = new Bullet(250, 100, 75, 1);

  ILoShip ships1 = new MtLoShip();
  ILoShip ships2 = new ConsLoShip(this.ship1, new MtLoShip());
  ILoShip ships3 = new ConsLoShip(this.ship2, this.ships2);
  ILoShip ships4 = new ConsLoShip(this.ship3, this.ships3);
  ILoShip ships5 = new ConsLoShip(this.ship4, this.ships4);
  ILoShip ships6 = new ConsLoShip(this.ship5, this.ships5);
  ILoShip ships2v2 = new ConsLoShip(this.ship2, this.ships1);
  ILoShip ships3v2 = new ConsLoShip(this.ship3, this.ships2v2);
  ILoShip ships4v2 = new ConsLoShip(this.ship4, this.ships3v2);
  ILoShip ships5v2 = new ConsLoShip(this.ship5, this.ships4v2);

  ILoBullet bullets1 = new MtLoBullet();
  ILoBullet bullets2 = new ConsLoBullet(this.bullet1, new MtLoBullet());
  ILoBullet bullets3 = new ConsLoBullet(this.bullet2, this.bullets2);
  ILoBullet bullets4 = new ConsLoBullet(this.bullet3, this.bullets3);
  ILoBullet bullets5 = new ConsLoBullet(this.bullet4, this.bullets4);
  ILoBullet bullets6 = new ConsLoBullet(this.bullet6, this.bullets5);

  NBullets game1 = new NBullets(10, 0, this.bullets1, this.ships1, 0, this.rand1);
  NBullets game2 = new NBullets(5, 10, this.bullets2, this.ships3, 100, this.rand2);
  NBullets game3 = new NBullets(0, 20, this.bullets4, this.ships5, 280, this.rand3);
  NBullets game4 = new NBullets(20, 20, this.bullets5, this.ships5, 700, this.rand4);
  NBullets game5 = new NBullets(10, 0, this.bullets1, this.ships1, 0, this.rand5);
  NBullets game6 = new NBullets(10, 0, this.bullets1, this.ships1, 0, this.rand6);
  NBullets game7 = new NBullets(10, 0, this.bullets1, this.ships1, 0, this.rand7);
  NBullets game8 = new NBullets(10, 0, this.bullets1, this.ships1, 0, this.rand8);
  NBullets game9 = new NBullets(10, 0, this.bullets2, this.ships2, 0, this.rand9);
  NBullets game10 = new NBullets(10, 0, this.bullets1, this.ships1, 0, this.rand10);

  boolean testBigBang(Tester t) {
    int worldWidth = 500;
    int worldHeight = 300;
    double tickRate = (1.0 / 28.0);
    return this.game1.bigBang(worldWidth, worldHeight, tickRate);
  }

  // tests computeRadius method
  boolean testComputeRadius(Tester t) {
    return t.checkExpect(this.igpship1.computeRadius(), 10)
        && t.checkExpect(this.igpbullet1.computeRadius(), 2)
        && t.checkExpect(this.igpbullet2.computeRadius(), 4);
  }

  // tests isOffScreen method
  boolean testIsOffScreen(Tester t) {
    return t.checkExpect(this.ship1.isOffScreen(), false)
        && t.checkExpect(this.ship2.isOffScreen(), false)
        && t.checkExpect(this.bullet1.isOffScreen(), false)
        && t.checkExpect(this.bullet2.isOffScreen(), false)
        && t.checkExpect(this.ship5.isOffScreen(), true)
        && t.checkExpect(this.bullet5.isOffScreen(), true);
  }

  // tests hasCollided method
  boolean testHasCollided(Tester t) {
    return t.checkExpect(this.ship1.hasCollided(this.bullet2), false)
        && t.checkExpect(this.ship1.hasCollided(this.bullet1), false)
        && t.checkExpect(this.ship5.hasCollided(this.bullet5), true)
        && t.checkExpect(this.ship1.hasCollided(this.bullet6), true);
  }

  // tests computeDist method
  boolean testComputeDist(Tester t) {
    return t.checkExpect(this.ship1.computeDist(this.bullet1), 50.0)
        && t.checkExpect(this.ship2.computeDist(this.bullet2), 180.27756377319946);
  }

  // tests drawShip method
  boolean testDrawShip(Tester t) {
    return t.checkExpect(this.ship1.drawShip(this.ws),
        this.ws.placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN), 250, 250))
        && t.checkExpect(this.ship2.drawShip(this.ws),
            this.ws.placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN), 100, 200));
  }

  // tests moveShip method
  boolean testMoveShip(Tester t) {
    return t.checkExpect(this.ship1.moveShip(), new Ship(250 - 4, 250, true))
        && t.checkExpect(this.ship2.moveShip(), new Ship(100 + 4, 200, false));
  }

  // tests drawBullet method
  boolean testDrawBullet(Tester t) {
    return t.checkExpect(this.bullet1.drawBullet(this.ws),
        this.ws.placeImageXY(new CircleImage(2, OutlineMode.SOLID, Color.PINK), 200, 250))
        && t.checkExpect(this.bullet2.drawBullet(this.ws),
            this.ws.placeImageXY(new CircleImage(4, OutlineMode.SOLID, Color.PINK), 250, 100));
  }

  // tests moveBullet method
  boolean testMoveBullet(Tester t) {
    return t.checkExpect(this.bullet1.moveBullet(), new Bullet(200, 258, 90, 0))
        && t.checkExpect(this.bullet2.moveBullet(), new Bullet(252, 107, 75, 1));
  }

  // tests changeX method
  boolean testChangeX(Tester t) {
    return t.checkExpect(this.bullet1.changeX(), 0) && t.checkExpect(this.bullet2.changeX(), 2);
  }

  // tests changeY method
  boolean testChangeY(Tester t) {
    return t.checkExpect(this.bullet1.changeY(), 8) && t.checkExpect(this.bullet2.changeY(), 7);
  }

  // tests explosion method
  boolean testExplosion(Tester t) {
    return t.checkExpect(this.bullet1.explosion(0, this.bullets1), this.bullets1)
        && t.checkExpect(this.bullet2.explosion(1, this.bullets2),
            new ConsLoBullet(new Bullet(250, 100, 120.0, 2), this.bullets2));
  }

  // tests newBullet method
  boolean testNewBullet(Tester t) {
    return t.checkExpect(this.bullet1.newBullet(0, 0), new Bullet(200, 250, 0.0, 1))
        && t.checkExpect(this.bullet2.newBullet(1, 1), new Bullet(250, 100, 120.0, 2));
  }

  // tests determineNewDirection method
  boolean testDetermineNewDirection(Tester t) {
    return t.checkExpect(this.bullet1.determineNewDirection(0), 0.0)
        && t.checkExpect(this.bullet2.determineNewDirection(2), 240.0);
  }

  // tests drawShips method
  boolean testDrawShips(Tester t) {
    return t.checkExpect(this.ships1.drawShips(this.ws), this.ws) && t.checkExpect(
        this.ships2.drawShips(this.ws), this.ships1.drawShips(this.ship1.drawShip(this.ws)));
  }

  // tests moveShips method
  boolean testMoveShips(Tester t) {
    return t.checkExpect(this.ships1.moveShips(), this.ships1)
        && t.checkExpect(this.ships2.moveShips(),
            new ConsLoShip(new Ship(250 - 4, 250, true), this.ships1))
        && t.checkExpect(this.ships3.moveShips(), new ConsLoShip(new Ship(100 + 4, 200, false),
            new ConsLoShip(new Ship(250 - 4, 250, true), this.ships1)));
  }

  // tests removeShips method
  boolean testRemoveShips(Tester t) {
    return t.checkExpect(this.ships1.removeShips(), this.ships1)
        && t.checkExpect(this.ships2.removeShips(), this.ships2)
        && t.checkExpect(this.ships6.removeShips(), this.ships5);
  }

  // tests howManyDestroyed method
  boolean testHowManyDestroyed(Tester t) {
    return t.checkExpect(this.ships1.howManyDestroyed(this.bullets1), 0)
        && t.checkExpect(this.ships1.howManyDestroyed(this.bullets3), 0)
        && t.checkExpect(this.ships2.howManyDestroyed(this.bullets1), 0)
        && t.checkExpect(this.ships2.howManyDestroyed(this.bullets2), 0)
        && t.checkExpect(this.ships5.howManyDestroyed(this.bullets6), 1);
  }

  // tests oneExplosion method
  boolean testOneExplosion(Tester t) {
    return t.checkExpect(this.ships1.oneExplosion(this.bullet1, this.bullets2),
        new ConsLoBullet(this.bullet1, this.bullets2))
        && t.checkExpect(this.ships2.oneExplosion(this.bullet1, this.bullets2),
            new ConsLoBullet(this.bullet1, this.bullets2))
        && t.checkExpect(this.ships3.oneExplosion(this.bullet6, this.bullets2),
            this.bullet6.explosion(2, this.bullets2));
  }

  // tests removeHitShips method
  boolean testRemoveHitShips(Tester t) {
    return t.checkExpect(this.ships1.removeHitShips(this.bullets1), this.ships1)
        && t.checkExpect(this.ships1.removeHitShips(this.bullets2), this.ships1)
        && t.checkExpect(this.ships2.removeHitShips(this.bullets1), this.ships2)
        && t.checkExpect(this.ships2.removeHitShips(this.bullets6), this.ships1);
  }

  // tests drawBullets method
  boolean testDrawBullets(Tester t) {
    return t.checkExpect(this.bullets1.drawBullets(this.ws), this.ws)
        && t.checkExpect(this.bullets2.drawBullets(this.ws),
            this.bullets1.drawBullets(this.bullet1.drawBullet(this.ws)));
  }

  // tests anyOnScreen method
  boolean testAnyOnScreen(Tester t) {
    return t.checkExpect(this.bullets1.anyOnScreen(), false)
        && t.checkExpect(this.bullets2.anyOnScreen(), true)
        && t.checkExpect(this.bullets6.anyOnScreen(), true);
  }

  // tests moveBullets method
  boolean testMoveBullets(Tester t) {
    return t.checkExpect(this.bullets1.moveBullets(), this.bullets1) && t.checkExpect(
        this.bullets2.moveBullets(), new ConsLoBullet(new Bullet(200, 258, 90, 0), this.bullets1));
  }

  // tests removeBullets method
  boolean testRemoveBullets(Tester t) {
    return t.checkExpect(this.bullets1.removeBullets(), this.bullets1) && t
        .checkExpect(this.bullets2.removeBullets(), new ConsLoBullet(this.bullet1, this.bullets1));
  }

  // tests hasCollidedWith method
  boolean testHasCollidedWith(Tester t) {
    return t.checkExpect(this.bullets1.hasCollidedWith(this.ship1), 0)
        && t.checkExpect(this.bullets2.hasCollidedWith(this.ship1), 0)
        && t.checkExpect(this.bullets6.hasCollidedWith(this.ship1), 1);
  }

  // tests makeExplosions method
  boolean testMakeExplosions(Tester t) {
    return t.checkExpect(this.bullets1.makeExplosions(this.ships1), this.bullets1)
        && t.checkExpect(this.bullets2.makeExplosions(this.ships1), this.bullets2)
        && t.checkExpect(this.bullets2.makeExplosions(this.ships2),
            this.ships2.oneExplosion(this.bullet1, this.bullets1.makeExplosions(this.ships2)))
        && t.checkExpect(this.bullets3.makeExplosions(this.ships2),
            this.ships2.oneExplosion(this.bullet2, this.bullets2.makeExplosions(this.ships2)));
  }

  // tests hasCollided method
  boolean testHasCollided2(Tester t) {
    return t.checkExpect(this.bullets1.hasCollided(this.ship1), false)
        && t.checkExpect(this.bullets6.hasCollided(this.ship1), true);
  }

  // tests makeScene method
  boolean testMakeScene(Tester t) {
    return t.checkExpect(this.game1.makeScene(),
        this.game1.drawGame().placeImageXY(this.game1.drawText(), 110, 280))
        && t.checkExpect(this.game2.makeScene(),
            this.game2.drawGame().placeImageXY(this.game2.drawText(), 110, 280));
  }

  // tests drawGame method
  boolean testDrawGame(Tester t) {
    return t.checkExpect(this.game1.drawGame(),
        this.ships1.drawShips(this.bullets1.drawBullets(this.ws)))
        && t.checkExpect(this.game10.drawGame(),
            this.ships1.drawShips(this.bullets1.drawBullets(this.ws)));
  }

  // tests drawText method
  boolean testDrawText(Tester t) {
    return t.checkExpect(this.game1.drawText(),
        new TextImage("bullets left: 10; ships destroyed: 0", 13, Color.BLACK))
        && t.checkExpect(this.game3.drawText(),
            new TextImage("bullets left: 0; ships destroyed: 20", 13, Color.BLACK));
  }

  // tests onKeyEvent method
  boolean testOnKeyEvent(Tester t) {
    return t.checkExpect(this.game1.onKeyEvent(" "), this.game1.handleSpace())
        && t.checkExpect(this.game3.onKeyEvent("a"), this.game3);
  }

  // tests handleSpace method
  boolean testHandleSpace(Tester t) {
    return t.checkExpect(this.game3.handleSpace(), this.game3)
        && t.checkExpect(this.game1.handleSpace(),
            new NBullets(9, 0, new ConsLoBullet(new Bullet(250, 300, 270.0, 0), this.bullets1),
                this.ships1, 0, this.rand1));
  }

  // tests newBullet method in NBullet class
  boolean testNewBulletNBullet(Tester t) {
    return t.checkExpect(this.game1.newBullet(),
        new ConsLoBullet(new Bullet(250, 300, 270.0, 0), this.bullets1))
        && t.checkExpect(this.game2.newBullet(),
            new ConsLoBullet(new Bullet(250, 300, 270.0, 0), this.bullets2));
  }

  // tests worldEnds method
  boolean testWorldEnds(Tester t) {
    return t.checkExpect(this.game1.worldEnds().worldEnds, false)
        && t.checkExpect(this.game3.worldEnds().worldEnds, false);
  }

  // tests drawEnding method
  boolean testDrawEnding(Tester t) {
    return t.checkExpect(this.game1.drawEnding(),
        this.game1.drawGame().placeImageXY(new TextImage("You ran out of bullets!", 20, Color.RED),
            250, 150))
        && t.checkExpect(this.game2.drawEnding(), this.game2.drawGame()
            .placeImageXY(new TextImage("You ran out of bullets!", 20, Color.RED), 250, 150));
  }

  // tests isGameOver method
  boolean testIsGameOver(Tester t) {
    return t.checkExpect(this.game1.isGameOver(), false)
        && t.checkExpect(this.game3.isGameOver(), false);
  }

  // tests onTick method
  boolean testOnTick(Tester t) {
    return t.checkExpect(this.game8.onTick(),
        new NBullets(10, 0, this.bullets1, this.ships1, 1, this.rand8))
        && t.checkExpect(this.game9.onTick(),
            new NBullets(10, 0, new ConsLoBullet(new Bullet(200, 258, 90.0, 0), this.bullets1),
                new ConsLoShip(new Ship(246, 250, true), this.ships1), 1, this.rand9));
  }

  // tests advanceTimer method
  boolean testAdvanceTimer(Tester t) {
    return t.checkExpect(this.game1.advanceTimer(),
        new NBullets(10, 0, this.bullets1, this.ships1, 1, this.rand1))
        && t.checkExpect(this.game2.advanceTimer(),
            new NBullets(5, 10, this.bullets2, this.ships3, 101, this.rand2));
  }

  // tests moveAll method
  boolean testMoveAll(Tester t) {
    return t.checkExpect(this.game1.moveAll(),
        new NBullets(10, 0, this.bullets1.moveBullets(), this.ships1.moveShips(), 0, this.rand1))
        && t.checkExpect(this.game2.moveAll(), new NBullets(5, 10, this.bullets2.moveBullets(),
            this.ships3.moveShips(), 100, this.rand2));
  }

  // tests removeOffScreen method
  boolean testRemoveOffScreen(Tester t) {
    return t.checkExpect(this.game1.removeOffScreen(),
        new NBullets(10, 0, this.bullets1.removeBullets(), this.ships1.removeShips(), 0,
            this.rand1))
        && t.checkExpect(this.game2.removeOffScreen(), new NBullets(5, 10,
            this.bullets2.removeBullets(), this.ships3.removeShips(), 100, this.rand2));
  }

  // tests spawnShips method
  boolean testSpawnShips(Tester t) {
    return t.checkExpect(this.game6.spawnShips(),
        new NBullets(10, 0, this.bullets1, new ConsLoShip(new Ship(509, 68, true), this.ships1), 0,
            this.rand6))
        && t.checkExpect(this.game7.spawnShips(), new NBullets(10, 0, this.bullets1,
            new ConsLoShip(new Ship(-9, 50, false), this.ships1), 0, this.rand7));
  }

  // tests generateShips method
  boolean testGenerateShips(Tester t) {
    return t.checkExpect(this.game3.generateShips(0), this.ships5)
        && t.checkExpect(this.game4.generateShips(1),
            new ConsLoShip(new Ship(-9, 99, false), this.ships5))
        && t.checkExpect(this.game5.generateShips(2), new ConsLoShip(new Ship(-9, 148, false),
            new ConsLoShip(new Ship(-9, 182, false), this.ships1)));
  }

  // tests randomShip method
  boolean testRandomShip(Tester t) {
    return t.checkExpect(this.game1.randomShip(), new Ship(-9, 130, false))
        && t.checkExpect(this.game2.randomShip(), new Ship(-9, 196, false));
  }

  // tests handleCollisions method
  boolean testHandleCollisions(Tester t) {
    return t.checkExpect(this.game1.handleCollisions(),
        new NBullets(10, 0 + this.ships1.howManyDestroyed(this.bullets1),
            this.bullets1.makeExplosions(this.ships1), this.ships1.removeHitShips(this.bullets1), 0,
            this.rand1))
        && t.checkExpect(this.game2.handleCollisions(),
            new NBullets(5, 10 + this.ships3.howManyDestroyed(this.bullets2),
                this.bullets2.makeExplosions(this.ships3),
                this.ships3.removeHitShips(this.bullets2), 100, this.rand2));
  }

}
